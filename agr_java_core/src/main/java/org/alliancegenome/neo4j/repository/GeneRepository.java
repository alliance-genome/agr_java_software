package org.alliancegenome.neo4j.repository;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.OrthologyFilter;
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

@Log4j2
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
        String query = " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene) WHERE g.primaryKey = {primaryKey} "
                + "OPTIONAL MATCH p2=(g:Gene)--(:SOTerm) "
                + "OPTIONAL MATCH p3=(g:Gene)--(:Synonym) "
                + "OPTIONAL MATCH p4=(g:Gene)--(:Chromosome) "
                + "OPTIONAL MATCH loc=(g:Gene)--(:GenomicLocation)--(:Chromosome) "
                + "OPTIONAL MATCH p5=(g:Gene)--(:CrossReference) "
                + "RETURN p1, p2, p3, p4, p5, loc";

        Iterable<Gene> genes = query(query, map);
        for (Gene g : genes) {
            if (g.getPrimaryKey().equals(primaryKey)) {
                return g;
            }
        }
        return null;
    }


    public Gene getShallowGene(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene) WHERE g.primaryKey = {primaryKey} "
                + "RETURN p1";

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
        String query = " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)-[:ALSO_KNOWN_AS]-(s:SecondaryId) WHERE s.primaryKey = {primaryKey} "
                + "OPTIONAL MATCH p2=(g:Gene)--(:SOTerm) "
                + "OPTIONAL MATCH p3=(g:Gene)--(:Synonym) "
                + "OPTIONAL MATCH p4=(g:Gene)--(:Chromosome) "
                + "OPTIONAL MATCH p5=(g:Gene)--(:CrossReference) "
                + "RETURN p1, p2, p3, p4, p5";

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
                "WHERE gene.taxonId = {taxon} ";
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
        map.put(FieldFilter.SOURCE, (join, filterValue) -> join.getCrossReference().getDisplayName().toLowerCase().contains(filterValue.toLowerCase()));

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

        query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey = {primaryKey}";
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
        System.out.println("ORTHOLOGOUS genes: " + String.format("%,d", map.size()));
        return map;
    }

    public Set<Gene> getOrthologyByTwoSpecies(String speciesOne, String speciesTwo) {

        speciesOne = SpeciesType.getTaxonId(speciesOne);
        speciesTwo = SpeciesType.getTaxonId(speciesTwo);
        HashMap<String, String> map = new HashMap<>();
        map.put("speciesID", speciesOne);
        map.put("homologSpeciesID", speciesTwo);
        map.put("strict", "true");
        String query = "";

        query += " MATCH p1=(g:Gene)-[ortho:ORTHOLOGOUS]-(gh:Gene), ";
        query += "p4=(g)--(s:OrthologyGeneJoin)--(gh:Gene), " +
                "p2=(g)-[:FROM_SPECIES]-(gs:Species), " +
                "p3=(gh)-[:FROM_SPECIES]-(ghs:Species), " +
                "p5=(s)--(algorithm:OrthoAlgorithm) ";
        query += " where g.taxonId = {speciesID} and   gh.taxonId = {homologSpeciesID} and ortho.strictFilter = {strict} ";
        //query += "return g, ortho, gh, s, algorithm";
        query += "return g";

        Iterable<Gene> genes = query(query, map);
        Set<Gene> geneListp = new HashSet<>();
        genes.forEach(gene -> geneListp.add(gene));
        Set<Gene> geneList = new HashSet<>();
        for (Gene g : genes) {
            if (g.getTaxonId().equals(speciesOne) && (g.getOrthoGenes() != null && g.getOrthoGenes().size() > 0)) {
                if (log.isDebugEnabled())
                    g.getOrthoGenes().forEach(orthologous -> {
                        log.debug(orthologous.getGene1().getPrimaryKey() + " " + orthologous.getGene2().getPrimaryKey());
                    });
                geneList.add(g);
            }
        }
        return geneList;
    }

    public Set<Gene> getOrthologyBySingleSpecies(String speciesOne) {

        speciesOne = SpeciesType.getTaxonId(speciesOne);
        HashMap<String, String> map = new HashMap<>();
        map.put("speciesID", speciesOne);
        String query = "";

        query += "MATCH p1=(g:Gene)-[ortho:ORTHOLOGOUS]-(gh:Gene), ";
        query += "p2=(g)--(s:OrthologyGeneJoin)--(gh:Gene), " +
                "p3=(g)-[:FROM_SPECIES]-(gs:Species), " +
                "p4=(gh)-[:FROM_SPECIES]-(ghs:Species), " +
                "p5=(s)--(algorithm:OrthoAlgorithm) ";
        query += " where g.taxonId = {speciesID} ";
        query += "return p1, p2, p3, p4, p5";

        Iterable<Gene> genes = query(query, map);
        Set<Gene> geneList = new HashSet<>();
        for (Gene g : genes) {
            if (g.getSpecies().getPrimaryKey().equals(speciesOne)) {
                geneList.add(g);
            }
        }
        return geneList;
    }

    public HashMap<String, Gene> getGene(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";

        query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p5=(g)--(s:DiseaseEntityJoin)--(feature:Feature)";
        query += " OPTIONAL MATCH p2=(do:DOTerm)--(s:DiseaseEntityJoin)-[:EVIDENCE]-(ea)";
        query += " OPTIONAL MATCH p6=(g)--(s:GenomicLocation)--(c:Chromosome)";
        query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
        query += " RETURN p1, p2, p3, p4, p5, p6";

        HashMap<String, Gene> retMap = new HashMap<>();

        Iterable<Gene> genes = query(query, map);
        for (Gene g : genes) {
            retMap.put(g.getPrimaryKey(), g);
        }
        return retMap;
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

    public List<String> getAllGeneKeys(String species) {
        String query = "MATCH (g:Gene)-[:FROM_SPECIES]-(species:Species) WHERE species.name = {species} RETURN distinct g.primaryKey";

        HashMap<String, String> params = new HashMap<>();
        params.put("species", species);

        Result r = queryForResult(query, params);
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
                "where all (subset IN ['" + GOSLIM_AGR + "'] where subset in goTerm.subset)  RETURN goTerm ";

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

        return ConfigHelper.getAOTermList();
    }

    public LinkedHashMap<String, String> getOrderGoTermList() {
        if (goCcList != null)
            return goCcList;
        return ConfigHelper.getGOTermList();
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

    public List<BioEntityGeneExpressionJoin> getAllExpressionAnnotations() {
        //String cypher = " MATCH p1=(q:Species)<-[:FROM_SPECIES]-(gene:Gene)-->(s:BioEntityGeneExpressionJoin)--(t), " +
        //      " entity = (s:BioEntityGeneExpressionJoin)--(exp:ExpressionBioEntity)--(o:Ontology) ";

        String cypher = "MATCH p1=(q:Species)<-[:FROM_SPECIES]-(gene:Gene)-[:ASSOCIATION]->(s:BioEntityGeneExpressionJoin)--(t), "
                + "entity = (s:BioEntityGeneExpressionJoin)<-[:ASSOCIATION]-(exp:ExpressionBioEntity)-->(o:Ontology) "
                + "WHERE (o:GOTerm OR o:UBERONTerm ) ";

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

    public List<AffectedGenomicModel> getAllAffectedModelsAllele() {
        String query = " MATCH p=(:AffectedGenomicModel)-[:MODEL_COMPONENT]-(:Allele)--(gene:Gene)";
        //query += " where gene.primaryKey = 'RGD:620268' ";
        query += " return p ";
        Iterable<AffectedGenomicModel> algorithms = query(AffectedGenomicModel.class, query);
        return StreamSupport.stream(algorithms.spliterator(), false)
                .collect(Collectors.toList());
    }

    public List<AffectedGenomicModel> getAllAffectedModelsSTR() {
        String query = " MATCH p=(:AffectedGenomicModel)-[:SEQUENCE_TARGETING_REAGENT]-(:SequenceTargetingReagent)--(gene:Gene)";
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
