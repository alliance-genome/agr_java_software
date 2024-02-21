package org.alliancegenome.neo4j.repository;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.core.util.FileHelper;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.neo4j.ogm.model.Result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.joining;

@Slf4j
public class GeneRepository extends Neo4jRepository<Gene> {

	public static final String GOSLIM_AGR = "goslim_agr";
	public static final String CELLULAR_COMPONENT = "CELLULAR_COMPONENT";
	public static final String OTHER_LOCATIONS = "other locations";
	public static final String GO_OTHER_LOCATIONS_ID = "GO:otherLocations";

	private LinkedHashMap<String, String> aoOrderList;
	private Map<String, Integer> aoOrderedPositionList;
	private LinkedHashMap<String, String> goCcList;
	private List<String> goTermOrderedList;
	private Map<String, Integer> goCCOrderedPositionList;

	Map<String, String> stageMap;
	List<UBERONTerm> stageList;

	static List<String> stageOrder = new ArrayList<>();

	static {
		stageOrder.add("embryo stage");
		stageOrder.add("post embryonic, pre-adult");
		stageOrder.add("post-juvenile adult stage");
	}

	public GeneRepository() {
		super(Gene.class);
	}

	public Gene getOneGene(String primaryKey) {
		HashMap<String, String> map = new HashMap<>();

		map.put("primaryKey", primaryKey);
		String query = """
			MATCH (q:Species)-[:FROM_SPECIES]-(g:Gene {primaryKey: $primaryKey})
			OPTIONAL MATCH (g)-[:ANNOTATED_TO]-(soTerm:SOTerm)
			WITH g, q, COLLECT(soTerm) AS soTerms
			OPTIONAL MATCH (g)-[:ALSO_KNOWN_AS]-(synonym:Synonym)
			WITH g, q, soTerms, COLLECT(synonym) AS synonyms
			OPTIONAL MATCH (g)-[:ALSO_KNOWN_AS]-(secondaryId:SecondaryId)
			WITH g, q, soTerms, synonyms, COLLECT(secondaryId) AS secondaryIds
			OPTIONAL MATCH (g)-[:ASSOCIATION]-(genomicLocation:GenomicLocation)
			WITH g, q, soTerms, synonyms, secondaryIds, COLLECT(genomicLocation) AS genomicLocations
			OPTIONAL MATCH (g)-[:CROSS_REFERENCE]->(crossRef:CrossReference)
			RETURN q AS Species,
			       soTerms AS SOTerms,
			       synonyms AS Synonyms,
			       secondaryIds AS SecondaryIds,
			       genomicLocations AS GenomicLocations,
			       g AS Gene,
			       COLLECT(crossRef) AS CrossReferences
			""";

		Iterable<Gene> genes = query(query, map);
		if (genes.iterator().hasNext()) {
			Gene gene = genes.iterator().next();
			if (gene.getPrimaryKey().equals(primaryKey)) {
				Iterable<CrossReference> crossReferences = query(CrossReference.class, query, map);
				gene.setCrossReferences(StreamSupport.stream(crossReferences.spliterator(), false).toList());
				Iterable<Synonym> synonyms = query(Synonym.class, query, map);
				gene.setSynonyms(StreamSupport.stream(synonyms.spliterator(), false).toList());
				Iterable<SecondaryId> secondaryIds = query(SecondaryId.class, query, map);
				gene.setSecondaryIds(StreamSupport.stream(secondaryIds.spliterator(), false).toList());
				Iterable<GenomeLocation> genomicLocations = query(GenomeLocation.class, query, map);
				gene.setGenomeLocations(StreamSupport.stream(genomicLocations.spliterator(), false).toList());
				Iterable<Species> species = query(Species.class, query, map);
				gene.setSpecies(species.iterator().next());
				Iterable<SOTerm> soTerm = query(SOTerm.class, query, map);
				gene.setSoTerm(soTerm.iterator().next());
				return gene;
			}
		}
		return null;
	}


	public Gene getShallowGene(String primaryKey) {
		HashMap<String, String> map = new HashMap<>();

		map.put("primaryKey", primaryKey);
		String query = " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene) WHERE g.primaryKey = $primaryKey RETURN p1";

		Iterable<Gene> genes = query(query, map);
		for (Gene g : genes) {
			if (g.getPrimaryKey().equals(primaryKey)) {
				return g;
			}
		}
		return null;
	}

	public Gene getOneGeneBySecondaryId(String secondaryIdPrimaryKey) {
		HashMap<String, String> map = new HashMap<>();

		map.put("primaryKey", secondaryIdPrimaryKey);
		String query = " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)-[:ALSO_KNOWN_AS]-(s:SecondaryId) WHERE s.primaryKey = $primaryKey "
			+ "OPTIONAL MATCH p2=(g:Gene)--(:SOTerm) "
			+ "OPTIONAL MATCH p3=(g:Gene)--(:Synonym) "
			+ "OPTIONAL MATCH p4=(g:Gene)--(:SecondaryId) "
			+ "OPTIONAL MATCH p5=(g:Gene)--(:GenomicLocation) "
			+ "OPTIONAL MATCH p6=(g:Gene)--(:CrossReference) "
			+ "RETURN p1, p2, p3, p4, p5, p6";

		Iterable<Gene> genes = query(query, map);
		for (Gene g : genes) {
			for (SecondaryId s : g.getSecondaryIds()) {
				if (s.getPrimaryKey().equals(secondaryIdPrimaryKey)) {
					return g;
				}
			}

		}
		return null;
	}

	public List<BioEntityGeneExpressionJoin> getExpressionAnnotationsByTaxon(String taxonID, String
		termID, Pagination pagination) {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("taxon", taxonID);
		String query = " MATCH p1=(species:Species)--(gene:Gene)-->(s:BioEntityGeneExpressionJoin)--(t) " +
			"WHERE gene.taxonId = $taxon ";
		query += " OPTIONAL MATCH p2=(t:ExpressionBioEntity)-->(o:Ontology) ";
		query += " RETURN s, p1, p2 ";
		Iterable<BioEntityGeneExpressionJoin> joins = query(BioEntityGeneExpressionJoin.class, query, parameters);


		List<BioEntityGeneExpressionJoin> joinList = new ArrayList<>();
		for (BioEntityGeneExpressionJoin join : joins) {
			// the setter of gene.species is not called in neo4j...
			// Thus, setting it manually
			//join.getGene().setSpeciesName(join.getGene().getSpecies().getName());
			joinList.add(join);
		}
		return joinList;
	}

/*
	private boolean passFilter(BioEntityGeneExpressionJoin
									   bioEntityGeneExpressionJoin, Map<FieldFilter, String> fieldFilterValueMap) {
		Map<FieldFilter, FilterComparator<BioEntityGeneExpressionJoin, String>> map = new HashMap<>();
		map.put(FieldFilter.FSPECIES, (join, filterValue) -> join.getGene().getSpecies().getName().toLowerCase().contains(filterValue.toLowerCase()));
		map.put(FieldFilter.GENE_NAME, (join, filterValue) -> join.getGene().getSymbol().toLowerCase().contains(filterValue.toLowerCase()));
		map.put(FieldFilter.TERM_NAME, (join, filterValue) -> join.getEntity().getWhereExpressedStatement().toLowerCase().contains(filterValue.toLowerCase()));
		map.put(FieldFilter.STAGE, (join, filterValue) -> join.getStage().getPrimaryKey().toLowerCase().contains(filterValue.toLowerCase()));
		map.put(FieldFilter.ASSAY, (join, filterValue) -> join.getAssay().getDisplaySynonym().toLowerCase().contains(filterValue.toLowerCase()));
		map.put(FieldFilter.FREFERENCE, (join, filterValue) -> join.getPublications().getPubId().toLowerCase().contains(filterValue.toLowerCase()));
		map.put(FieldFilter.INERACTION_SOURCE, (join, filterValue) -> join.getCrossReference().getDisplayName().toLowerCase().contains(filterValue.toLowerCase()));

		if (fieldFilterValueMap == null || fieldFilterValueMap.size() == 0)
			return true;
		for (FieldFilter filter : fieldFilterValueMap.keySet()) {
			if (!map.get(filter).compare(bioEntityGeneExpressionJoin, fieldFilterValueMap.get(filter)))
				return false;
		}
		return true;
	}
*/

	public List<BioEntityGeneExpressionJoin> getExpressionAnnotationSummary(String geneID) {
		String query = " MATCH p1=(gene:Gene)-->(s:BioEntityGeneExpressionJoin)--(t) ";
		query += "WHERE gene.primaryKey = '" + geneID + "'";
		query += " OPTIONAL MATCH p2=(t:ExpressionBioEntity)--(o:Ontology) ";
		query += " RETURN s, p1, p2 order by gene.taxonID, gene.symbol ";

		Iterable<BioEntityGeneExpressionJoin> joins = query(BioEntityGeneExpressionJoin.class, query);

		List<BioEntityGeneExpressionJoin> joinList = new ArrayList<>();
		for (BioEntityGeneExpressionJoin join : joins) {
			joinList.add(join);
		}
		return joinList;
	}

	public Gene getOrthologyGene(String primaryKey) {
		HashMap<String, String> map = new HashMap<>();

		map.put("primaryKey", primaryKey);
		String query = "";

		query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey = $primaryKey";
		query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
		query += " RETURN p1, p3, p4";

		Iterable<Gene> genes = query(query, map);
		for (Gene g : genes) {
			if (g.getPrimaryKey().equals(primaryKey)) {
				return g;
			}
		}
		return null;
	}

	public List<Gene> getOrthologyGenes(List<String> geneIDs) {
		HashMap<String, String> map = new HashMap<>();

		StringJoiner geneJoiner = new StringJoiner(",", "[", "]");
		geneIDs.forEach(geneID -> geneJoiner.add("'" + geneID + "'"));

		//String query = " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey in " + geneJoiner;
		String query = " MATCH p1=(q:Species)<-[:FROM_SPECIES]-(g:Gene)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), " +
			"p3=(g:Gene)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]->(q2:Species) WHERE g.primaryKey in " + geneJoiner;
		query += " RETURN p1, p3";

		Iterable<Gene> genes = query(query, map);
		List<Gene> geneList = StreamSupport.stream(genes.spliterator(), false)
			.filter(gene -> geneIDs.contains(gene.getPrimaryKey()))
			.collect(Collectors.toList());

		return geneList;
	}

	public List<Gene> getAllOrthologyGenes() {
		HashMap<String, String> map = new HashMap<>();

		String query = " MATCH p1=(q:Species)<-[:FROM_SPECIES]-(g:Gene)-[o:ORTHOLOGOUS]->(g2:Gene)-[:FROM_SPECIES]->(q2:Species)";
		//query += " where g.primaryKey = 'ZFIN:ZDB-GENE-001103-1' ";
		query += " RETURN p1 ";

		Iterable<Gene> genes = query(query, map);
		List<Gene> geneList = StreamSupport.stream(genes.spliterator(), false)
			.collect(Collectors.toList());
		log.info("ORTHOLOGOUS genes: " + String.format("%,d", geneList.size()));
		return geneList;
	}

	public List<Gene> getAllParalogyGenes() {
		HashMap<String, String> map = new HashMap<>();

		String query = """ 
					MATCH p1=(q:Species)<-[:FROM_SPECIES]-(g:Gene)-[o:PARALOGOUS]->(g2:Gene)-[:FROM_SPECIES]->(q2:Species)
					//where g.primaryKey = 'SGD:S000001467'
					RETURN p1
			""";

		Iterable<Gene> genes = query(query, map);
		List<Gene> geneList = StreamSupport.stream(genes.spliterator(), false)
			.collect(Collectors.toList());
		log.info("PARALOGOUS genes: " + String.format("%,d", geneList.size()));
		return geneList;
	}

	public MultiKeyMap<String, Map<String, Set<String>>> getAllOrthologyGeneJoin() {

		String query = " MATCH p1=(g:Gene)<-[:ASSOCIATION]-(s:OrthologyGeneJoin)-[:MATCHED]-(a:OrthoAlgorithm), " +
			" p2=(g2:Gene)-[:ASSOCIATION]->(s:OrthologyGeneJoin) ";
		//query += " where g.primaryKey = 'ZFIN:ZDB-GENE-001103-1' ";
		//query += " where g.primaryKey = 'MGI:109583' ";
		query += " OPTIONAL MATCH p3=(s:OrthologyGeneJoin)-[:NOT_MATCHED]-(b:OrthoAlgorithm) ";
		//query += " OPTIONAL MATCH p4=(s:OrthologyGeneJoin)-[:NOT_CALLED]-(c:OrthoAlgorithm) ";
		query += " RETURN g.primaryKey, g2.primaryKey, collect(a.name) as match, " +
			" collect(b.name) as notMatch ";
/*
		query += " RETURN g.primaryKey, g2.primaryKey, collect(a.name) as match, " +
				" collect(b.name) as notMatch, collect(c.name) as notCalled ";
*/

		Result result = queryForResult(query);
		MultiKeyMap<String, Map<String, Set<String>>> map = new MultiKeyMap<>();
		StreamSupport.stream(result.spliterator(), false).forEach(join -> {
			Map<String, Set<String>> predictionMap = new HashMap<>();
			Set<String> matches = new HashSet<>(Arrays.asList((String[]) join.get("match")));
			predictionMap.put("match", matches);

			Object notMatchesO = join.get("notMatch");
			if (notMatchesO != null) {
				Set<String> notMatchesStrings = null;
				if (((Object[]) join.get("notMatch")).length > 0) {
					String[] notMatches = (String[]) join.get("notMatch");
					notMatchesStrings = new HashSet<>(Arrays.asList(notMatches));
					predictionMap.put("notMatch", notMatchesStrings);
				}
			}
			map.put((String) join.get("g.primaryKey"), (String) join.get("g2.primaryKey"), predictionMap);
		});
		log.info("ORTHOLOGOUS genes: " + String.format("%,d", map.size()));
		return map;
	}

	public MultiKeyMap<String, Map<String, Set<String>>> getAllParalogyGeneJoin() {

		String query = """
			MATCH p1=(g:Gene)<-[:ASSOCIATION]-(s:ParalogyGeneJoin)-[:MATCHED]-(a:ParaAlgorithm),
				p2=(g2:Gene)-[:ASSOCIATION]->(s:ParalogyGeneJoin)
				//where g.primaryKey = 'SGD:S000001467'
				// where g.primaryKey = 'MGI:109583'
				OPTIONAL MATCH p3=(s:ParalogyGeneJoin)-[:NOT_MATCHED]-(b:ParaAlgorithm)
				RETURN g.primaryKey, g2.primaryKey, collect(a.name) as match,
				collect(b.name) as notMatch
				""";

		Result result = queryForResult(query);
		MultiKeyMap<String, Map<String, Set<String>>> map = new MultiKeyMap<>();
		StreamSupport.stream(result.spliterator(), false).forEach(join -> {
			Map<String, Set<String>> predictionMap = new HashMap<>();
			Set<String> matches = new HashSet<>(Arrays.asList((String[]) join.get("match")));
			predictionMap.put("match", matches);

			Object notMatchesO = join.get("notMatch");
			if (notMatchesO != null) {
				Set<String> notMatchesStrings;
				if (((Object[]) join.get("notMatch")).length > 0) {
					String[] notMatches = (String[]) join.get("notMatch");
					notMatchesStrings = new HashSet<>(Arrays.asList(notMatches));
					predictionMap.put("notMatch", notMatchesStrings);
				}
			}
			map.put((String) join.get("g.primaryKey"), (String) join.get("g2.primaryKey"), predictionMap);
		});
		log.info("PARALOGOUS genes: " + String.format("%,d", map.size()));
		return map;
	}


	public List<String> getAllGeneKeys() {
		String query = "MATCH (g:Gene)-[:FROM_SPECIES]-(q:Species) RETURN distinct g.primaryKey";
		Result r = queryForResult(query);
		Iterator<Map<String, Object>> i = r.iterator();
		ArrayList<String> list = new ArrayList<>();

		while (i.hasNext()) {
			Map<String, Object> map2 = i.next();
			list.add((String) map2.get("g.primaryKey"));
		}
		return list;
	}

	public List<String> getAllAgmKeys() {
		String query = "MATCH (g:AffectedGenomicModel) RETURN distinct g.primaryKey";
		Result r = queryForResult(query);
		Iterator<Map<String, Object>> i = r.iterator();
		ArrayList<String> list = new ArrayList<>();

		while (i.hasNext()) {
			Map<String, Object> map2 = i.next();
			list.add((String) map2.get("g.primaryKey"));
		}
		return list;
	}

	public Map<String, String> getGoSlimList(String goType) {
		// cache the complete GO CC list.
		if (goCcList != null)
			return goCcList;
		String cypher = "MATCH (goTerm:GOTerm) " +
			"where all (subset IN ['" + GOSLIM_AGR + "'] where subset in goTerm.subset)	 RETURN goTerm ";

		Iterable<GOTerm> joins = query(GOTerm.class, cypher);

		// used for sorting the GO terms according to the order in the java script file.
		// feels pretty hacky to me but the obo file does not contain sorting info...
		List<String> goTermOrderedList = getGoTermListFromJavaScriptFile();
		goCcList = StreamSupport.stream(joins.spliterator(), false)
			.filter(goTerm -> goTerm.getType().equals(goType))
			.sorted((o1, o2) -> goTermOrderedList.indexOf(o1.getPrimaryKey()) < goTermOrderedList.indexOf(o2.getPrimaryKey()) ? -1 : 1)
			.collect(Collectors.toMap(GOTerm::getPrimaryKey, GOTerm::getName, (s, s2) -> s, LinkedHashMap::new));
		return goCcList;
	}

	private List<String> getGoTermListFromJavaScriptFile() {
		if (goTermOrderedList != null)
			return goTermOrderedList;

		String url = "https://raw.githubusercontent.com/geneontology/ribbon/master/src/data/agr.js";
		List<String> content = new ArrayList<>();
		URL oracle;
		try {
			oracle = new URL(url);
			BufferedReader in = new BufferedReader(
				new InputStreamReader(oracle.openStream()));

			String inputLine;
			while ((inputLine = in.readLine()) != null)
				content.add(inputLine);
			in.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		String pattern = "(.*)(GO:[0-9]*)(.*)";
		Pattern p = Pattern.compile(pattern);
		goTermOrderedList = content.stream()
			.filter(line -> {
				Matcher m = p.matcher(line);
				return m.matches();
			})
			.map(line -> {
				Matcher m = p.matcher(line);
				m.matches();
				return m.group(2);
			})
			.collect(Collectors.toList());
		return goTermOrderedList;
	}

	public Map<String, String> getGoCCSlimList() {
		Map<String, String> goSlimList = getGoSlimList(CELLULAR_COMPONENT.toLowerCase());
		goSlimList.put(GO_OTHER_LOCATIONS_ID, OTHER_LOCATIONS);
		return goSlimList;
	}

	public Map<String, String> getGoCCSlimListWithoutOther() {
		return getGoSlimList(CELLULAR_COMPONENT.toLowerCase());
	}

	public List<Gene> getGenes(OrthologyFilter filter) {

		String query = getAllGenesQuery(filter);
		query += " RETURN gene order by gene.taxonID, gene.symbol ";
		query += " SKIP " + (filter.getStart() - 1) + " limit " + filter.getRows();

		Iterable<Gene> genes = query(query);
		return StreamSupport.stream(genes.spliterator(), false)
			.map(gene -> {
				//gene.setSpeciesName(SpeciesType.fromTaxonId(gene.getTaxonId()).getName());
				return gene;
			})
			.collect(Collectors.toList());
	}

	public int getGeneCount(OrthologyFilter filter) {
		String query = getAllGenesQuery(filter);
		query += " RETURN count(gene) ";
		long count = queryCount(query);
		return (int) count;
	}

	private String getAllGenesQuery(OrthologyFilter filter) {
		StringJoiner taxonJoiner = new StringJoiner(",", "[", "]");
		String taxonClause = null;
		if (filter.getTaxonIDs() != null) {
			filter.getTaxonIDs().forEach(taxonID -> taxonJoiner.add("'" + taxonID + "'"));
			taxonClause = taxonJoiner.toString();
		}
		String query = " MATCH (gene:Gene) ";
		if (taxonClause != null) {
			query += "WHERE gene.taxonId in " + taxonClause;
		}
		return query;
	}

	public List<String> getGeneIDs(OrthologyFilter filter) {
		String query = getAllGenesQuery(filter);
		query += " RETURN gene order by gene.taxonID, gene.symbol ";
		query += " SKIP " + (filter.getStart() - 1) + " limit " + filter.getRows();

		Iterable<Gene> genes = query(query);
		return StreamSupport.stream(genes.spliterator(), false)
			.map(gene -> gene.getPrimaryKey())
			.collect(Collectors.toList());
	}

	public Map<String, String> getStageList() {
		if (stageMap != null)
			return stageMap;

		String cypher = "match p=(uber:UBERONTerm)-[:STAGE_RIBBON_TERM]-(:BioEntityGeneExpressionJoin) return distinct uber";

		Iterable<UBERONTerm> terms = query(UBERONTerm.class, cypher);
		if (!StreamSupport.stream(terms.spliterator(), false).allMatch(uberonTerm ->
			stageOrder.indexOf(uberonTerm.getName()) > -1)) {
			String expectedValues = stageOrder.stream().collect(joining(", "));
			throw new RuntimeException("One or more stage name in UBERON has changed: \nFound values: " +
				StreamSupport.stream(terms.spliterator(), false)
					.map(UBERONTerm::getName)
					.collect(joining(", ")) + " Expected Values: " + expectedValues);
		}

		stageMap = StreamSupport.stream(terms.spliterator(), false)
			.sorted(Comparator.comparingInt(o ->
				stageOrder.indexOf(o.getName())))
			.collect(Collectors.toMap(UBERONTerm::getPrimaryKey, UBERONTerm::getName, (e1, e2) -> e2, LinkedHashMap::new));

		return stageMap;
	}

	public List<UBERONTerm> getStageTermList() {
		if (stageList != null)
			return stageList;

		String cypher = "match p=(uber:UBERONTerm)-[:STAGE_RIBBON_TERM]-(:BioEntityGeneExpressionJoin) return distinct uber";

		Iterable<UBERONTerm> terms = query(UBERONTerm.class, cypher);
		if (!StreamSupport.stream(terms.spliterator(), false).allMatch(uberonTerm ->
			stageOrder.indexOf(uberonTerm.getName()) > -1)) {
			String expectedValues = String.join(", ", stageOrder);
			throw new RuntimeException("One or more stage name in UBERON has changed: \nFound values: " +
				StreamSupport.stream(terms.spliterator(), false)
					.map(UBERONTerm::getName)
					.collect(joining(", ")) + " Expected Values: " + expectedValues);
		}

		stageList = StreamSupport.stream(terms.spliterator(), false)
			.sorted(Comparator.comparingInt(o ->
				stageOrder.indexOf(o.getName())))
			.collect(Collectors.toList());

		return stageList;
	}

	public LinkedHashMap<String, String> getOrderAoTermList() {
		if (aoOrderList != null)
			return aoOrderList;

		return FileHelper.getAOTermList();
	}

	public LinkedHashMap<String, String> getOrderGoTermList() {
		if (goCcList != null)
			return goCcList;
		return FileHelper.getGOTermList();
	}

	private Map<String, Integer> getOrderedAoTermList() {
		if (aoOrderedPositionList != null)
			return aoOrderedPositionList;
		aoOrderedPositionList = new HashMap<>();
		int index = 0;
		final LinkedHashMap<String, String> orderAoTermList = getOrderAoTermList();
		for (String id : orderAoTermList.keySet()) {
			aoOrderedPositionList.put(id, index++);
		}
		return aoOrderedPositionList;
	}

	public List<UBERONTerm> getFullAoTermList() {
		String cypher = "match p=(uber:UBERONTerm)-[:ANATOMICAL_RIBBON_TERM]-(:ExpressionBioEntity) return distinct uber";

		Iterable<UBERONTerm> terms = query(UBERONTerm.class, cypher);
		List<UBERONTerm> map = StreamSupport.stream(terms.spliterator(), false)
			.sorted(Comparator.comparing(o -> getOrderedAoTermList().get(o.getPrimaryKey())))
			.collect(Collectors.toList());
		return map;
	}

	public Map<String, String> getFullGoList() {
		String cypher = "match p=(uber:GOTerm)-[:CELLULAR_COMPONENT_RIBBON_TERM]-(:ExpressionBioEntity) return distinct uber";
		Iterable<GOTerm> terms = query(GOTerm.class, cypher);
		String alwaysLast = "other locations";
		return StreamSupport.stream(terms.spliterator(), false)
			.sorted((o1, o2) -> {
				if (o1.getName().equalsIgnoreCase(alwaysLast)) {
					return 1;
				}
				if (o2.getName().equalsIgnoreCase(alwaysLast)) {
					return -1;
				}
				return o1.getName().compareToIgnoreCase(o2.getName());
			})
			.collect(Collectors.toMap(GOTerm::getPrimaryKey, GOTerm::getName, (x, y) -> x + ", " + y, LinkedHashMap::new));
	}

	public Map<String, Integer> getGoOrderedList() {
		if (goCCOrderedPositionList != null)
			return goCCOrderedPositionList;
		goCCOrderedPositionList = new HashMap<>();
		int index = 0;
		final LinkedHashMap<String, String> orderGoTermList = getOrderGoTermList();
		for (String id : orderGoTermList.keySet()) {
			goCCOrderedPositionList.put(id, index++);
		}
		return goCCOrderedPositionList;
	}

	public List<GOTerm> getFullGoTermList() {
		final Map<String, Integer> goOrderedList = getGoOrderedList();
		StringJoiner joiner = new StringJoiner(",");
		goOrderedList.forEach((id, integer) -> joiner.add("'" + id + "'"));
		String cypher = "match p=(uber:GOTerm) where uber.primaryKey in [" + joiner.toString() + "] return distinct uber";
		Iterable<GOTerm> terms = query(GOTerm.class, cypher);
		return StreamSupport.stream(terms.spliterator(), false)
			// exclude the GO-CC root term
			.filter(goTerm -> !goTerm.getPrimaryKey().equals("GO:0005575"))
			.sorted(Comparator.comparing(o -> {
				return goOrderedList.get(o.getPrimaryKey());
			}))
			.collect(Collectors.toList());
	}


	public List<Gene> getAllGenes() {
		String cypher = " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene) "
			+ "OPTIONAL MATCH p5=(g:Gene)--(:CrossReference) "
			+ "RETURN p1, p5 limit 10000000 ";

		Iterable<Gene> joins = query(Gene.class, cypher);
		return StreamSupport.stream(joins.spliterator(), false).
			collect(Collectors.toList());
	}

	public List<Gene> getAllGenes(List<String> taxonIDs) {
		if (CollectionUtils.isEmpty(taxonIDs))
			return null;
		Map<String, Object> params = new HashMap<>();
		params.put("ids", taxonIDs);
		String cypher = " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(x:CrossReference ) "
			+ " WHERE q.primaryKey IN $ids "
			+ "RETURN p1";

		Iterable<Gene> joins = query(Gene.class, cypher, params);
		return StreamSupport.stream(joins.spliterator(), false).
			collect(Collectors.toList());
	}


	public List<BioEntityGeneExpressionJoin> getAllExpressionAnnotations() {
		//String cypher = " MATCH p1=(q:Species)<-[:FROM_SPECIES]-(gene:Gene)-->(s:BioEntityGeneExpressionJoin)--(t), " +
		//		" entity = (s:BioEntityGeneExpressionJoin)--(exp:ExpressionBioEntity)--(o:Ontology) ";

		String cypher = "MATCH p1=(q:Species)<-[:FROM_SPECIES]-(gene:Gene)-[:ASSOCIATION]->(s:BioEntityGeneExpressionJoin)--(t), "
			+ "entity = (s:BioEntityGeneExpressionJoin)<-[:ASSOCIATION]-(exp:ExpressionBioEntity)-->(o:Ontology) "
			+ "WHERE (o:GOTerm OR o:UBERONTerm ) ";
		//cypher += "AND gene.primaryKey in ['SGD:S000004489','MGI:108359','MGI:1859288','MGI:1859314','MGI:109617','MGI:2669849','MGI:94903','MGI:94904','MGI:2153518','MGI:95461','MGI:95667','MGI:95668','MGI:96086','MGI:96109','MGI:96170','MGI:96171','MGI:96172','MGI:96173','MGI:96175','MGI:96176','MGI:96177','MGI:96178','MGI:96179','MGI:96180','MGI:96182','MGI:107730','MGI:96183','MGI:96184','MGI:96186','MGI:96188','MGI:96190','MGI:96192','MGI:96193','MGI:96196','MGI:96199','MGI:96201','MGI:96202','MGI:96203','MGI:96207','MGI:96209','MGI:104867','MGI:96785','MGI:1316721','MGI:1888519','MGI:1100513','MGI:103220','MGI:103219','MGI:109160','MGI:97168','MGI:97169','MGI:97351','MGI:1270158','MGI:97352','MGI:1921811','MGI:108011','MGI:1918718','MGI:2149033','MGI:2149032','MGI:2149035','MGI:2149036','MGI:97451','MGI:97487','MGI:97488','MGI:97490','MGI:102851','MGI:1100882','MGI:109340','MGI:1100498','MGI:101896','MGI:101895','MGI:102564','MGI:101894','MGI:2148204','MGI:109632','MGI:98769','MGI:1350935','MGI:108013','MGI:1277163','MGI:1890816','RGD:1562672','RGD:619932','RGD:1303178','RGD:62387','RGD:3331','RGD:3332','RGD:619768','ZFIN:ZDB-GENE-050419-191','ZFIN:ZDB-GENE-050208-140','ZFIN:ZDB-GENE-050417-212','ZFIN:ZDB-GENE-060118-2','ZFIN:ZDB-GENE-050913-153','ZFIN:ZDB-GENE-050522-28','ZFIN:ZDB-GENE-040628-4','ZFIN:ZDB-GENE-980526-330','ZFIN:ZDB-GENE-000128-8','ZFIN:ZDB-GENE-980526-280','ZFIN:ZDB-GENE-990415-49','ZFIN:ZDB-GENE-020117-1','ZFIN:ZDB-GENE-980526-216','ZFIN:ZDB-GENE-980526-6','ZFIN:ZDB-GENE-980526-167','ZFIN:ZDB-GENE-980526-40','ZFIN:ZDB-GENE-020117-2','ZFIN:ZDB-GENE-020509-2','ZFIN:ZDB-GENE-980526-299','ZFIN:ZDB-GENE-030131-5304','ZFIN:ZDB-GENE-001020-1','ZFIN:ZDB-GENE-060825-142','ZFIN:ZDB-GENE-990415-97','ZFIN:ZDB-GENE-000823-8','ZFIN:ZDB-GENE-990415-4','ZFIN:ZDB-GENE-000823-5','ZFIN:ZDB-GENE-000823-3','ZFIN:ZDB-GENE-000823-9','ZFIN:ZDB-GENE-000823-2','ZFIN:ZDB-GENE-990415-101','ZFIN:ZDB-GENE-990415-104','ZFIN:ZDB-GENE-990415-105','ZFIN:ZDB-GENE-980526-70','ZFIN:ZDB-GENE-000823-6','ZFIN:ZDB-GENE-000329-2','ZFIN:ZDB-GENE-980526-291','ZFIN:ZDB-GENE-990415-109','ZFIN:ZDB-GENE-990415-110','ZFIN:ZDB-GENE-990415-111','ZFIN:ZDB-GENE-000822-3','ZFIN:ZDB-GENE-000329-17','ZFIN:ZDB-GENE-000822-2','ZFIN:ZDB-GENE-990415-112','ZFIN:ZDB-GENE-980526-533','ZFIN:ZDB-GENE-000328-5','ZFIN:ZDB-GENE-990415-116','ZFIN:ZDB-GENE-990415-117','ZFIN:ZDB-GENE-990415-120','ZFIN:ZDB-GENE-980526-214','ZFIN:ZDB-GENE-990415-121','ZFIN:ZDB-GENE-040724-40','ZFIN:ZDB-GENE-001206-2','ZFIN:ZDB-GENE-051220-1','ZFIN:ZDB-GENE-050417-210','ZFIN:ZDB-GENE-041014-332','ZFIN:ZDB-GENE-050114-3','ZFIN:ZDB-GENE-050114-2','ZFIN:ZDB-GENE-040718-149','ZFIN:ZDB-GENE-080613-1','ZFIN:ZDB-GENE-060503-853','ZFIN:ZDB-GENE-040409-1','ZFIN:ZDB-GENE-040415-1','ZFIN:ZDB-GENE-040415-2','ZFIN:ZDB-GENE-980526-26','ZFIN:ZDB-GENE-980526-492','ZFIN:ZDB-GENE-040615-1','ZFIN:ZDB-GENE-010404-1','ZFIN:ZDB-GENE-980526-321','ZFIN:ZDB-GENE-030127-1','ZFIN:ZDB-GENE-980526-400','ZFIN:ZDB-GENE-980526-406','ZFIN:ZDB-GENE-030508-1','ZFIN:ZDB-GENE-081022-10','ZFIN:ZDB-GENE-080917-54','ZFIN:ZDB-GENE-990415-122','ZFIN:ZDB-GENE-050407-3','ZFIN:ZDB-GENE-990714-27','ZFIN:ZDB-GENE-980526-372','ZFIN:ZDB-GENE-980526-370','ZFIN:ZDB-GENE-980526-140','ZFIN:ZDB-GENE-010108-1','FB:FBgn0000014','FB:FBgn0000015','FB:FBgn0000028','FB:FBgn0260642','FB:FBgn0004862','FB:FBgn0000166','FB:FBgn0038592','FB:FBgn0036274','FB:FBgn0000439','FB:FBgn0000606','FB:FBgn0041156','FB:FBgn0001170','FB:FBgn0038852','FB:FBgn0264005','FB:FBgn0002522','FB:FBgn0011278','FB:FBgn0008651','FB:FBgn0052105','FB:FBgn0051481','FB:FBgn0025334','FB:FBgn0020912','FB:FBgn0003267','FB:FBgn0003339','FB:FBgn0287186','FB:FBgn0019650','FB:FBgn0003944','FB:FBgn0015561','FB:FBgn0086680','FB:FBgn0004053','FB:FBgn0004054','WB:WBGene00044330','WB:WBGene00000439','WB:WBGene00000440','WB:WBGene00000429','WB:WBGene00000446','WB:WBGene00000451','WB:WBGene00022837','WB:WBGene00000431','WB:WBGene00019864','WB:WBGene00002988','WB:WBGene00003024','WB:WBGene00003377','WB:WBGene00004011','WB:WBGene00004024','WB:WBGene00006652','WB:WBGene00006744','WB:WBGene00006870','WB:WBGene00006970']	 ";
		//cypher += " AND gene.primaryKey in ['MGI:109583','ZFIN:ZDB-GENE-980526-166','RGD:61995','ZFIN:ZDB-GENE-030131-3776', 'ZFIN:ZDB-GENE-030616-47','FB:FBgn0026379','WB:WBGene00000913','SGD:S000005072'] ";
		//cypher += "  where gene.primaryKey = 'RGD:2129' ";
		//cypher += "OPTIONAL MATCH crossReference = (s:BioEntityGeneExpressionJoin)--(crossRef:CrossReference) ";
		cypher += "return p1, entity";

		long start = System.currentTimeMillis();
		Iterable<BioEntityGeneExpressionJoin> joins = query(BioEntityGeneExpressionJoin.class, cypher);

		List<BioEntityGeneExpressionJoin> allBioEntityExpressionJoins = StreamSupport.stream(joins.spliterator(), false).
			collect(Collectors.toList());
		log.info("Total BioEntityGeneExpressionJoin nodes: " + String.format("%,d", allBioEntityExpressionJoins.size()));
		log.info("Loaded in:  " + ((System.currentTimeMillis() - start) / 1000) + " s");
		return allBioEntityExpressionJoins;
	}

	public List<String> getAllMethods() {
		String query = " MATCH (algorithm:OrthoAlgorithm) return distinct(algorithm) ";
		Iterable<OrthoAlgorithm> algorithms = query(OrthoAlgorithm.class, query);
		return StreamSupport.stream(algorithms.spliterator(), false)
			.map(OrthoAlgorithm::getName)
			.collect(Collectors.toList());
	}

	public List<String> getAllParalogyMethods() {
		String query = " MATCH (algorithm:ParaAlgorithm) return distinct(algorithm) ";
		Iterable<ParaAlgorithm> algorithms = query(ParaAlgorithm.class, query);
		return StreamSupport.stream(algorithms.spliterator(), false)
			.map(ParaAlgorithm::getName)
			.collect(Collectors.toList());
	}

	public List<AffectedGenomicModel> getAllAffectedModelsAllele() {
		String query = " MATCH p=(:AffectedGenomicModel)-[:MODEL_COMPONENT]-(a:Allele)--(gene:Gene)," +
			" q=(a:Allele)-[:FROM_SPECIES]->(:Species) ";
		//query += " where gene.primaryKey = 'RGD:620268' ";
		query += " return p, q ";
		Iterable<AffectedGenomicModel> algorithms = query(AffectedGenomicModel.class, query);
		return StreamSupport.stream(algorithms.spliterator(), false)
			.collect(Collectors.toList());
	}

	public List<AffectedGenomicModel> getAllAffectedModelsSTR() {
		String query = " MATCH p=(:AffectedGenomicModel)-[:SEQUENCE_TARGETING_REAGENT]-(:SequenceTargetingReagent)--(gene:Gene)-[:FROM_SPECIES]->(:Species)";
		//query += " where gene.primaryKey = 'MGI:88059' ";
		query += " return p ";
		Iterable<AffectedGenomicModel> algorithms = query(AffectedGenomicModel.class, query);
		return StreamSupport.stream(algorithms.spliterator(), false)
			.collect(Collectors.toList());
	}

	@FunctionalInterface
	public interface FilterComparator<T, U> {
		boolean compare(T o, U oo);

		default FilterComparator<T, U> thenCompare(FilterComparator<T, U> other) {
			Objects.requireNonNull(other);
			return (FilterComparator<T, U> & Serializable) (c1, c2) -> {
				boolean res = compare(c1, c2);
				return (!res) ? res : other.compare(c1, c2);
			};
		}
	}

	class GoHighLevelTerms {
		@JsonProperty("class_id")
		private String id;
		@JsonProperty("class_label")
		private String label;
		private String separator;
	}


}
