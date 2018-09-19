package org.alliancegenome.neo4j.repository;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.model.Result;

public class GeneRepository extends Neo4jRepository<Gene> {

    public static final String GOSLIM_AGR = "goslim_agr";
    public static final String CELLULAR_COMPONENT = "CELLULAR_COMPONENT";
    public static final String OTHER_LOCATIONS = "other locations";
    public static final String GO_OTHER_LOCATIONS_ID = "GO:otherLocations";
    private final Logger log = LogManager.getLogger(getClass());

    public GeneRepository() {
        super(Gene.class);
    }

    public Gene getOneGene(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";

        query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p5=(g)--(s:DiseaseEntityJoin)--(feature:Feature)";
        query += " OPTIONAL MATCH p2=(do:DOTerm)--(s:DiseaseEntityJoin)-[:EVIDENCE]-(ea)";
        query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
        query += " OPTIONAL MATCH p6=(g)--(s:PhenotypeEntityJoin)--(tt) ";
        query += " OPTIONAL MATCH p8=(g)--(s:PhenotypeEntityJoin)--(ff:Feature)";
        query += " OPTIONAL MATCH p9=(g)--(s:GOTerm)-[:IS_A|:PART_OF*]->(parent:GOTerm)";
        query += " OPTIONAL MATCH p10=(g)--(s:BioEntityGeneExpressionJoin)--(t) ";
        query += " OPTIONAL MATCH p11=(t:ExpressionBioEntity)--(term:Ontology) ";
        query += " RETURN p1, p2, p3, p4, p5, p6, p8, p9, p10, p11";

        Iterable<Gene> genes = query(query, map);
        for (Gene g : genes) {
            if (g.getPrimaryKey().equals(primaryKey)) {
                return g;
            }
        }

        return null;
    }

    public Gene getExpressionGene(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";

        query += " MATCH p1=(g:Gene)-->(s:BioEntityGeneExpressionJoin)--(t) WHERE g.primaryKey = {primaryKey}";
        query += " RETURN p1";

        Iterable<Gene> genes = query(query, map);
        for (Gene g : genes) {
            if (g.getPrimaryKey().equals(primaryKey)) {
                return g;
            }
        }

        return null;
    }

    public List<BioEntityGeneExpressionJoin> getExpressionAnnotations(List<String> geneIDs,String termID, Pagination pagination) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        geneIDs.forEach(geneID -> sj.add("'" + geneID + "'"));

/*
        StringJoiner sjTerm = new StringJoiner(",", "[", "]");
        termIDs.forEach(geneID -> sjTerm.add("'" + geneID + "'"));

*/
        String query = " MATCH p1=(species:Species)--(gene:Gene)-->(s:BioEntityGeneExpressionJoin)--(t) " +
                "WHERE gene.primaryKey in " + sj.toString();
        query += " OPTIONAL MATCH p2=(t:ExpressionBioEntity)--(o:Ontology) ";
/*
        if(termIDs != null && termIDs.size() > 0) {
            query += " OPTIONAL MATCH slim=(ontology)-[:PART_OF|IS_A*]->(slimTerm) " +
                    " where all (primaryKey IN ";
            query += sjTerm.toString() + " where primaryKey = slimTerm.primaryKey) ";
        }
*/
        query += " RETURN s, p1, p2";
        Iterable<BioEntityGeneExpressionJoin> joins = neo4jSession.query(BioEntityGeneExpressionJoin.class, query, new HashMap<>());


        List<BioEntityGeneExpressionJoin> joinList = new ArrayList<>();
        for (BioEntityGeneExpressionJoin join : joins) {
            // the setter of gene.species is not called in neo4j...
            // Thus, setting it manually
            join.getGene().setSpeciesName(join.getGene().getSpecies().getName());
            join.getPublication().setPubIdFromId();
            joinList.add(join);
        }

        // filtering
        joinList = joinList.stream()
                .filter(join -> passFilter(join, pagination.getFieldFilterValueMap()))
                .collect(Collectors.toList());


        // sorting
        HashMap<FieldFilter, Comparator<BioEntityGeneExpressionJoin>> sortingMapping = new LinkedHashMap<>();
        sortingMapping.put(FieldFilter.FSPECIES, Comparator.comparing(o -> o.getGene().getTaxonId().toUpperCase()));
        sortingMapping.put(FieldFilter.GENE_NAME, Comparator.comparing(o -> o.getGene().getSymbol().toUpperCase()));
        sortingMapping.put(FieldFilter.TERM_NAME, Comparator.comparing(o -> o.getEntity().getWhereExpressedStatement().toUpperCase()));
        sortingMapping.put(FieldFilter.STAGE, Comparator.comparing(o -> o.getStage().getPrimaryKey().toUpperCase()));
        sortingMapping.put(FieldFilter.ASSAY, Comparator.comparing(o -> o.getAssay().getName().toUpperCase()));
        sortingMapping.put(FieldFilter.REFERENCE, Comparator.comparing(o -> o.getPublication().getPubId().toUpperCase()));
        sortingMapping.put(FieldFilter.SOURCE, Comparator.comparing(o -> o.getGene().getDataProvider().toUpperCase()));

        Comparator<BioEntityGeneExpressionJoin> comparator = null;
        FieldFilter sortByField = pagination.getSortByField();
        if (sortByField != null) {
            comparator = sortingMapping.get(sortByField);
/*
            if (!pagination.getAsc())
                comparator.reversed();
*/
        }
        if (comparator != null)
            joinList.sort(comparator);
        for (FieldFilter fieldFilter : sortingMapping.keySet()) {
            if (sortByField != null && sortByField.equals(fieldFilter)) {
                continue;
            }
            Comparator<BioEntityGeneExpressionJoin> comp = sortingMapping.get(fieldFilter);
            if (comparator == null)
                comparator = comp;
            else
                comparator = comparator.thenComparing(comp);
        }
        joinList.sort(comparator);

        // check for rollup term existence
        // Check for GO terms
        if (termID != null && !termID.isEmpty()) {
            // At the moment we are expecting only a single termID
            // GO term check
            if (Ontology.isGOTerm(termID))
                joinList = joinList.stream()
                        .filter(join -> isChildOfRollupGOTerm(join, termID))
                        .collect(Collectors.toList());
            // AO / stage term check
            if (Ontology.isAoOrStageTerm(termID)) {
                // check AO ribbon term list
                List<BioEntityGeneExpressionJoin> aoJoinList = joinList.stream()
                        .filter(join ->
                                join.getEntity().getAoTermList().stream().map(UBERONTerm::getPrimaryKey).anyMatch(s -> s.equals(termID))
                        )
                        .collect(Collectors.toList());
                // check stage term list
                if(aoJoinList.size() == 0) {
                    joinList = joinList.stream()
                            .filter(join -> join.getStageTerm().getPrimaryKey().equals(termID))
                            .collect(Collectors.toList());
                }
            }
        }
        // pagination
        List<BioEntityGeneExpressionJoin> paginatedJoinList;
        if (pagination.isCount()) {
            paginatedJoinList = joinList;
        } else {
            paginatedJoinList = joinList.stream()
                    .skip(pagination.getStart())
                    .limit(pagination.getLimit())
                    .collect(Collectors.toList());
        }
        return paginatedJoinList;
    }

    private boolean isChildOfRollupOtherGOTerm(BioEntityGeneExpressionJoin join) {
        StringJoiner sj = new StringJoiner(",");

        getGoCCSlimListWithoutOther().keySet().forEach(s ->
                sj.add("'" + s + "'")
        );

        String entityKey = join.getEntity().getGoTerm().getPrimaryKey();
        String cypher = " MATCH p1=(entity:ExpressionBioEntity)-[:CELLULAR_COMPONENT]-(ontology:GOTerm) " +
                " WHERE ontology.primaryKey = '" + entityKey + "' " +
                "OPTIONAL MATCH slim=(ontology)-[:PART_OF|IS_A*]->(slimTerm) " +
                "where any (primaryKey in [" + sj.toString() + "] where primaryKey in slimTerm.primaryKey) " +
                "RETURN slim";
        Iterable<GOTerm> terms = neo4jSession.query(GOTerm.class, cypher, new HashMap<>());
        return !(terms != null && terms.spliterator().getExactSizeIfKnown() > 0);
    }

    private boolean isChildOfRollupGOTerm(BioEntityGeneExpressionJoin join, String termID) {
        if (join.getEntity().getGoTerm() == null)
            return false;
        if (termID.equals(GO_OTHER_LOCATIONS_ID))
            return isChildOfRollupOtherGOTerm(join);
        String cypher = " MATCH p1=(entity:ExpressionBioEntity)-[:CELLULAR_COMPONENT]-(ontology:GOTerm) " +
                " WHERE ontology.primaryKey = '" + join.getEntity().getGoTerm().getPrimaryKey() + "' " +
                "OPTIONAL MATCH slim=(ontology)-[:PART_OF|IS_A*]->(slimTerm) " +
                "where all (primaryKey in ['" + termID + "'] where primaryKey in slimTerm.primaryKey) " +
                "RETURN ontology, slim";
        Iterable<GOTerm> terms = neo4jSession.query(GOTerm.class, cypher, new HashMap<>());
        for (GOTerm term : terms) {
            if (termID.equals(term.getPrimaryKey()))
                return true;
        }
        return false;
    }

    private boolean passFilter(BioEntityGeneExpressionJoin bioEntityGeneExpressionJoin, Map<FieldFilter, String> fieldFilterValueMap) {
        Map<FieldFilter, FilterComparator<BioEntityGeneExpressionJoin, String>> map = new HashMap<>();
        map.put(FieldFilter.FSPECIES, (join, filterValue) -> join.getGene().getSpeciesName().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.GENE_NAME, (join, filterValue) -> join.getGene().getSymbol().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.TERM_NAME, (join, filterValue) -> join.getEntity().getWhereExpressedStatement().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.STAGE, (join, filterValue) -> join.getStage().getPrimaryKey().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.ASSAY, (join, filterValue) -> join.getAssay().getName().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.FREFERENCE, (join, filterValue) -> join.getPublication().getPubId().toLowerCase().contains(filterValue.toLowerCase()));
        map.put(FieldFilter.FSOURCE, (join, filterValue) -> join.getGene().getDataProvider().toLowerCase().contains(filterValue.toLowerCase()));

        if (fieldFilterValueMap == null || fieldFilterValueMap.size() == 0)
            return true;
        for (FieldFilter filter : fieldFilterValueMap.keySet()) {
            if (!map.get(filter).compare(bioEntityGeneExpressionJoin, fieldFilterValueMap.get(filter)))
                return false;
        }
        return true;
    }

    public List<BioEntityGeneExpressionJoin> getExpressionAnnotationSummary(String geneID) {
        String query = " MATCH p1=(gene:Gene)-->(s:BioEntityGeneExpressionJoin)--(t) ";
        query += "WHERE gene.primaryKey = '" + geneID + "'";
        query += " OPTIONAL MATCH p2=(t:ExpressionBioEntity)--(o:Ontology) ";
        query += " RETURN s, p1, p2 order by gene.taxonID, gene.symbol ";

        Iterable<BioEntityGeneExpressionJoin> joins = neo4jSession.query(BioEntityGeneExpressionJoin.class, query, new HashMap<>());

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

        String query = " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey in " + geneJoiner;
        query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
        query += " RETURN p1, p3, p4";

        Iterable<Gene> genes = query(query, map);
        List<Gene> geneList = StreamSupport.stream(genes.spliterator(), false)
                .filter(gene -> geneIDs.contains(gene.getPrimaryKey()))
                .collect(Collectors.toList());

        return geneList;
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
        query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
        query += " RETURN p1, p2, p3, p4, p5";

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

    public List<InteractionGeneJoin> getInteractions(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);

        String query = "MATCH p1=(sp1:Species)-[:FROM_SPECIES]-(g:Gene)-[iw:INTERACTS_WITH]->(g2:Gene)-[:FROM_SPECIES]-(sp2:Species), p2=(g:Gene)-->(igj:InteractionGeneJoin)--(s) where g.primaryKey = {primaryKey} and iw.uuid = igj.primaryKey RETURN p1, p2";
        //String query = "MATCH p1=(g:Gene)-[iw:INTERACTS_WITH]->(g2:Gene), p2=(g:Gene)-->(igj:InteractionGeneJoin)--(s) where g.primaryKey = {primaryKey} and iw.uuid = igj.primaryKey RETURN p1, p2";

        Iterable<Gene> genes = query(query, map);

        for (Gene g : genes) {
            if (g.getPrimaryKey().equals(primaryKey)) {
                return g.getInteractions();
            }
        }
        return null;

    }

    private LinkedHashMap<String, String> goCcList;

    public Map<String, String> getGoSlimList(String goType) {
        // cache the complete GO CC list.
        if (goCcList != null)
            return goCcList;
        String cypher = "MATCH (goTerm:GOTerm) " +
                "where all (subset IN ['" + GOSLIM_AGR + "'] where subset in goTerm.subset)  RETURN goTerm ";

        Iterable<GOTerm> joins = neo4jSession.query(GOTerm.class, cypher, new HashMap<>());

        // used for sorting the GO terms according to the order in the java script file.
        // feels pretty hacky to me but the obo file does not contain sorting info...
        List<String> goTermOrderedList = getGoTermListFromJavaScriptFile();
        goCcList = StreamSupport.stream(joins.spliterator(), false)
                .filter(goTerm -> goTerm.getType().equals(goType))
                .sorted((o1, o2) -> goTermOrderedList.indexOf(o1.getPrimaryKey()) < goTermOrderedList.indexOf(o2.getPrimaryKey()) ? -1 : 1)
                .collect(Collectors.toMap(GOTerm::getPrimaryKey, GOTerm::getName, (s, s2) -> s, LinkedHashMap::new));
        return goCcList;
    }

    // cache variable
    private List<String> goTermOrderedList;

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


    public List<String> getGOParentTerms(ExpressionBioEntity entity) {
        List<String> parentList = new ArrayList<>();
        getGoCCSlimListWithoutOther().keySet().forEach(termID -> {
            String query = " MATCH p1=(entity:ExpressionBioEntity)-[:" + CELLULAR_COMPONENT + "]-(ontology:GOTerm) ";
            query += "WHERE ontology.primaryKey = '" + entity.getGoTerm().getPrimaryKey() + "'";
            query += " OPTIONAL MATCH slim=(ontology)-[:PART_OF|IS_A*]->(slimTerm) " +
                    //" where all (primaryKey IN [" + joiner.toString() + "] where primaryKey in slimTerm.primaryKey) ";
                    " where all (primaryKey IN ['";
            query += termID + "'] where primaryKey in slimTerm.primaryKey) ";
            query += " RETURN p1, slim ";
            Iterable<GOTerm> joins = neo4jSession.query(GOTerm.class, query, new HashMap<>());
            List<String> list = StreamSupport.stream(joins.spliterator(), false)
                    .filter(goTerm -> goTerm.getPrimaryKey().equals(termID))
                    .map(GOTerm::getPrimaryKey)
                    .collect(Collectors.toList());
            parentList.addAll(list);
        });
        if (parentList.isEmpty()) {
            parentList.add(GO_OTHER_LOCATIONS_ID);
        }
        return parentList;
    }

    public Map<String, String> getStageList() {
        String cypher = "match p=(uber:UBERONTerm)-[:STAGE_RIBBON_TERM]-(:BioEntityGeneExpressionJoin) return distinct uber";

        Iterable<UBERONTerm> terms = neo4jSession.query(UBERONTerm.class, cypher, new HashMap<>());
        Map<String, String> map = StreamSupport.stream(terms.spliterator(), false)
                .collect(Collectors.toMap(UBERONTerm::getPrimaryKey, UBERONTerm::getName));
        return map;
    }

    public Map<String, String> getFullAoList() {
        String cypher = "match p=(uber:UBERONTerm)-[:ANATOMICAL_RIBBON_TERM]-(:ExpressionBioEntity) return distinct uber";

        Iterable<UBERONTerm> terms = neo4jSession.query(UBERONTerm.class, cypher, new HashMap<>());
        Map<String, String> map = StreamSupport.stream(terms.spliterator(), false)
                .collect(Collectors.toMap(UBERONTerm::getPrimaryKey, UBERONTerm::getName));
        return map;
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
