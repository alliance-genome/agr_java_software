package org.alliancegenome.neo4j.repository;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.neo4j.ogm.model.Result;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

@Log4j2
public class DiseaseRepository extends Neo4jRepository<DOTerm> {

    public static final String DISEASE_INCLUDING_CHILDREN = "(diseaseParent:DOTerm)<-[:IS_A_PART_OF_CLOSURE]-(disease:DOTerm)";
    public static final String FEATURE_JOIN = " p4=(diseaseEntityJoin)--(feature:Feature)--(crossReference:CrossReference) ";
    public static final String AND_NOT_DISEASE_ENTITY_JOIN_FEATURE = " AND NOT (diseaseEntityJoin)--(:Feature) ";
    public static final String TOTAL_COUNT = "totalCount";


    private String cypherEmpirical = " AND NOT (diseaseEntityJoin)-[:FROM_ORTHOLOGOUS_GENE]-(:Gene) ";
    private String cypherViaOrthology = " ,p5 =  (diseaseEntityJoin)-[:FROM_ORTHOLOGOUS_GENE]-(orthoGene:Gene)-[:FROM_SPECIES]-(orthoSpecies:Species) ";

    private Set<DiseaseEntityJoin> allDiseaseEntityJoins = new HashSet<>(200000);
    private static Map<String, Set<String>> closureMapGO = null;
    private static Map<String, Set<String>> closureMapUberon = null;
    private static Map<String, Set<String>> closureMapUberonChild = null;
    private static Map<String, Set<String>> closureMap = null;
    private static Map<String, Set<String>> closureChildMap = null;

    static Map<FieldFilter, String> sortByMapping = new TreeMap<>();

    static {
        sortByMapping.put(FieldFilter.GENE_NAME, "gene.symbol + substring('               ', 0, size(gene.symbol))");
        sortByMapping.put(FieldFilter.SPECIES, "species.name");
        sortByMapping.put(FieldFilter.DISEASE, "disease.name");
        sortByMapping.put(FieldFilter.ASSOCIATION_TYPE, "diseaseEntityJoin.joinType");
    }

    public List<DiseaseEntityJoin> getAllDiseaseAnnotationsPureAGM() {

        String cypher = "MATCH p0=(:DOTerm)--(dej:DiseaseEntityJoin)-[:EVIDENCE]->(:PublicationJoin)<-[:ASSOCIATION]-(publication:Publication), " +
                " p2=(dej:DiseaseEntityJoin)--(agm:AffectedGenomicModel)--(:Allele)--(:Gene) " +
                //"where agm.primaryKey in ['MGI:6272038','MGI:3622062'] " +
                //"where agm.primaryKey in ['ZFIN:ZDB-FISH-180831-2'] " +
                "OPTIONAL MATCH     p5=(dej:DiseaseEntityJoin)--(:AffectedGenomicModel)-[:CROSS_REFERENCE]->(crossRef:CrossReference) " +
                "OPTIONAL MATCH modelAllele=(agm:AffectedGenomicModel)--(n)--(:Gene) where n:Allele OR n:SequenceTargetingReagent " +
                "return p0,p2, p5, modelAllele ";

        Iterable<DiseaseEntityJoin> joins = query(DiseaseEntityJoin.class, cypher);
        return StreamSupport.stream(joins.spliterator(), false).
                collect(Collectors.toList());
    }

    @Setter
    @Getter
    private class Closure {
        String parent;
        String child;
    }

    public DiseaseRepository() {
        super(DOTerm.class);
    }

    public DOTerm getDiseaseTermWithAnnotations(String primaryKey) {
        String cypher = "match p0=(root:DOTerm)--(join:DiseaseEntityJoin)-[:EVIDENCE]-(pe),"
                + " p1=(join)-[:ASSOCIATION]-(gene:Gene)-[:FROM_SPECIES]-(species:Species),"
                + " p2=(root)-[:IS_A*]->(p:DOTerm) "
                + " WHERE root.primaryKey = {primaryKey}"
                + " OPTIONAL MATCH p3=(join)-[:ASSOCIATION]-(feature:Feature) "
                + " RETURN p0, p1, p2, p3";

        HashMap<String, String> map = new HashMap<>();
        map.put("primaryKey", primaryKey);


        Iterable<DOTerm> terms = query(cypher, map);
        for (DOTerm d : terms) {
            if (d.getPrimaryKey().equals(primaryKey)) {
                return d;
            }
        }

        return null;

    }

    public Set<String> getAllDiseaseWithAnnotationsKeys() {
        String query = "MATCH (term:DOTerm)-[q:ASSOCIATION]-(dej:DiseaseEntityJoin) WHERE term.isObsolete='false' " +
                " RETURN term.primaryKey";

        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();

        Set<String> list = new HashSet<>();

        while (i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String) map2.get("term.primaryKey"));
        }
        return list;
    }

    public DOTerm getDiseaseTerm(String primaryKey) {

        String cypher = "MATCH p0=(disease:DOTerm)--(anyOtherNode) WHERE disease.primaryKey = {primaryKey}   " +
                " OPTIONAL MATCH p1=(disease)--(anyOtherNode:DiseaseEntityJoin)-[:EVIDENCE]-(eq), p2=(anyOtherNode)--(g:Gene)-[:FROM_SPECIES]-(species:Species)" +
                " OPTIONAL MATCH p4=(anyOtherNode:DiseaseEntityJoin)--(feature:Feature)" +
                " OPTIONAL MATCH p6=(anyOtherNode:DiseaseEntityJoin)--(feature:Feature)--(crossRef:CrossReference)" +
                " OPTIONAL MATCH p5=(anyOtherNode:DiseaseEntityJoin)--(orthoSpecies:Species)" +
                " OPTIONAL MATCH slim=(disease)-[:IS_A*]->(slimTerm) " +
                " where all (subset IN [{subset}] where subset in slimTerm.subset) " +
                " RETURN p0, p1, p2, p4, p5, p6, slim";

        HashMap<String, String> map = new HashMap<>();
        map.put("primaryKey", primaryKey);
        map.put("subset", DOTerm.HIGH_LEVEL_TERM_LIST_SLIM);

        DOTerm primaryTerm = null;
        List<DOTerm> highLevelTermList = new ArrayList<>(3);

        Iterable<DOTerm> terms = query(cypher, map);
        for (DOTerm term : terms) {
            if (term.getPrimaryKey().equals(primaryKey)) {
                primaryTerm = term;
            }
            if (term.getSubset().contains(DOTerm.HIGH_LEVEL_TERM_LIST_SLIM)) {
                highLevelTermList.add(term);
            }
        }

        if (primaryTerm == null) return null;
        primaryTerm.getHighLevelTermList().addAll(highLevelTermList);
        return primaryTerm;
    }

    public Map<String, Set<String>> getClosureMapping() {
        if (closureMap != null)
            return closureMap;
        //closure
        String cypher = "MATCH (diseaseParent:DOTerm)<-[:IS_A_PART_OF_CLOSURE]-(disease:DOTerm) where diseaseParent.isObsolete = 'false' " +
                " return diseaseParent.primaryKey as parent, disease.primaryKey as child ";

        List<Closure> cls = getClosures(cypher);
        closureMap = cls.stream()
                .collect(groupingBy(Closure::getParent, mapping(Closure::getChild, Collectors.toSet())));
        return closureMap;
    }

    public Map<String, Set<String>> getClosureMappingUberonChild() {
        if (closureMapUberonChild != null)
            return closureMapUberonChild;
        getClosureMappingUberon();
        return closureMapUberonChild;
    }

    public Map<String, Set<String>> getClosureMappingUberon() {
        if (closureMapUberon != null)
            return closureMapUberon;
        //closure
        String cypher = "MATCH (uberonParent:UBERONTerm)<-[:IS_A_PART_OF_CLOSURE]-(uberon:UBERONTerm) where uberonParent.isObsolete = 'false' ";
        cypher += " return uberonParent.primaryKey as parent, uberon.primaryKey as child ";

        List<Closure> cls = getClosures(cypher);
        closureMapUberon = cls.stream()
                .collect(groupingBy(Closure::getParent, mapping(Closure::getChild, Collectors.toSet())));

        closureMapUberonChild = cls.stream()
                .collect(groupingBy(Closure::getChild, mapping(Closure::getParent, Collectors.toSet())));
        return closureMapUberon;
    }

    public Map<String, Set<String>> getClosureMappingGO() {
        if (closureMapGO != null)
            return closureMapGO;
        //closure
        String cypher = "MATCH (goParent:GOTerm)<-[:IS_A_PART_OF_CLOSURE]-(go:GOTerm) where goParent.isObsolete = 'false' ";
        cypher += " return goParent.primaryKey as parent, go.primaryKey as child ";

        List<Closure> cls = getClosures(cypher);
        closureMapGO = cls.stream()
                .collect(groupingBy(Closure::getParent, mapping(Closure::getChild, Collectors.toSet())));

        return closureMapGO;
    }

    private List<Closure> getClosures(String cypher) {
        HashMap<String, String> bindingMap = new HashMap<>();
        //bindingMap.put("rootDiseaseID", "DOID:9952");
        Result result = queryForResult(cypher, bindingMap);
        return StreamSupport.stream(result.spliterator(), false)
                .map(stringObjectMap -> {
                    Closure cl = new Closure();
                    cl.setParent((String) stringObjectMap.get("parent"));
                    cl.setChild((String) stringObjectMap.get("child"));
                    return cl;
                })
                .collect(Collectors.toList());
    }


    public Map<String, Set<String>> getClosureChildToParentsMapping() {
        if (closureChildMap != null)
            return closureChildMap;
        //closure
        String cypher = "MATCH (diseaseParent:DOTerm)<-[:IS_A_PART_OF_CLOSURE]-(disease:DOTerm) where diseaseParent.isObsolete = 'false' " +
                " return diseaseParent.primaryKey as parent, disease.primaryKey as child order by disease.name";

        List<Closure> cls = getClosures(cypher);
        closureChildMap = cls.stream()
                .collect(groupingBy(Closure::getChild, mapping(Closure::getParent, Collectors.toSet())));
        return closureChildMap;
    }

    public Map<String, Set<String>> getGOClosureChildMapping() {
        if (closureChildMap != null)
            return closureChildMap;
        //closure
        String cypher = "MATCH (diseaseParent:GOTerm)<-[:IS_A_PART_OF_CLOSURE]-(disease:GOTerm) where diseaseParent.isObsolete = 'false' " +
                " return diseaseParent.primaryKey as parent, disease.primaryKey as child order by disease.name";

        List<Closure> cls = getClosures(cypher);
        closureChildMap = cls.stream()
                .collect(groupingBy(Closure::getChild, mapping(Closure::getParent, Collectors.toSet())));
        return closureChildMap;
    }

    private List<DOTerm> doAgrDoList;

    public List<DOTerm> getAgrDoSlim() {
        // cache the high-level terms of AGR Do slim
        if (doAgrDoList != null)
            return doAgrDoList;
        String cypher = "MATCH (disease:DOTerm) where disease.subset =~ '.*" + DOTerm.HIGH_LEVEL_TERM_LIST_SLIM
                + ".*' RETURN disease order by disease.name";

        Iterable<DOTerm> joins = query(cypher);

        doAgrDoList = StreamSupport.stream(joins.spliterator(), false)
                .collect(Collectors.toList());
        log.debug("AGR-DO slim: " + doAgrDoList.size());
        return doAgrDoList;

    }

    public Set<ECOTerm> getEcoTerms(List<PublicationJoin> publicationEvidenceCodeJoin) {
        StringJoiner joiner = new StringJoiner(",", "'", "'");
        publicationEvidenceCodeJoin.forEach(codeJoin -> joiner.add(codeJoin.getPrimaryKey()));
        String ids = joiner.toString();
        String cypher = "MATCH p=(pubEvCode:PublicationJoin)-[:ASSOCIATION]->(ev:ECOTerm) " +
                "where pubEvCode.primaryKey in [" + ids + "]" +
                "RETURN p";

        Iterable<ECOTerm> joins = query(ECOTerm.class, cypher);

        return StreamSupport.stream(joins.spliterator(), false).
                collect(Collectors.toSet());
    }

    private Map<String, List<ECOTerm>> ecoTermMap = new HashMap<>();

    public Map<String, List<ECOTerm>> getEcoTermMap() {
        if (ecoTermMap.size() == 0)
            populateAllPublicationJoins();
        return ecoTermMap;
    }

    private void populateAllPublicationJoins() {
        String pubCodeID = "pubCodeID";
        String ecoName = "ecoName";
        String cypher = "MATCH p=(pubEvCode:PublicationJoin)" +
                " OPTIONAL MATCH (pubEvCode:PublicationJoin)-[:ASSOCIATION]->(ev:ECOTerm) " +
                "RETURN pubEvCode.primaryKey as " + pubCodeID + ", collect(ev) as " + ecoName;

        Result result = queryForResult(cypher);
        result.forEach(resultMap -> {
            String id = (String) resultMap.get(pubCodeID);
            List<ECOTerm> terms = ecoTermMap.get(id);
            List<ECOTerm> names = new ArrayList<>();
            if (resultMap.get(ecoName) != null) {
                // if the cypher query does not have values for collect(ev) the OGM generates an empty array Objects[]
                if (resultMap.get(ecoName) instanceof ArrayList)
                    names = (List<ECOTerm>) resultMap.get(ecoName);
/*
                else
                    log.debug("Collection of ECOterms was not an ArrayList for pubEvCodeJoin ID : " + id);
*/
            }
            if (terms == null) {
                ecoTermMap.put(id, names);
            } else {
                terms.addAll(names);
            }
        });

        //Iterable<PublicationJoin> joins = neo4jSession.query(PublicationJoin.class, cypher, new HashMap<>());

        log.info("Number of PublicationJoin records retrieved: " + String.format("%,d", ecoTermMap.size()));
    }

    public List<ECOTerm> getEcoTerm(String publicationEvidenceCodeJoinID) {
        if (ecoTermMap.isEmpty()) {
            populateAllPublicationJoins();
        }
        return ecoTermMap.get(publicationEvidenceCodeJoinID);
    }

    public List<ECOTerm> getEcoTerm(List<PublicationJoin> joins) {
        if (ecoTermMap.isEmpty()) {
            populateAllPublicationJoins();
        }
        List<ECOTerm> terms = new ArrayList<>();
        joins.stream()
                .filter(join -> ecoTermMap.get(join.getPrimaryKey()) != null)
                .forEach(publicationJoin -> terms.addAll(ecoTermMap.get(publicationJoin.getPrimaryKey())));
        return terms;
    }

    public DOTerm getShallowDiseaseTerm(String id) {

        String cypher = "MATCH (disease:DOTerm) WHERE disease.primaryKey = {primaryKey}   " +
                " RETURN disease ";

        HashMap<String, String> map = new HashMap<>();
        map.put("primaryKey", id);

        Iterable<DOTerm> terms = query(cypher, map);
        if (terms == null)
            return null;
        return terms.iterator().next();
    }

    public UBERONTerm getShallowUberonTerm(String id) {

        String cypher = "MATCH (disease:UBERONTerm) WHERE disease.primaryKey = {primaryKey}   " +
                " RETURN disease ";

        HashMap<String, String> map = new HashMap<>();
        map.put("primaryKey", id);

        Iterable<UBERONTerm> terms = query(UBERONTerm.class, cypher, map);
        if (terms == null)
            return null;
        return terms.iterator().next();
    }

    public GOTerm getGOTerm(String id) {

        String cypher = "MATCH (disease:GOTerm) WHERE disease.primaryKey = {primaryKey}   " +
                " RETURN disease ";

        HashMap<String, String> map = new HashMap<>();
        map.put("primaryKey", id);

        Iterable<GOTerm> terms = query(GOTerm.class, cypher, map);
        if (terms == null)
            return null;
        return terms.iterator().next();
    }

    public String getTermDefinition(String id) {
        if (id.startsWith("DOID"))
            return getShallowDiseaseTerm(id).getDefinition();
        if (id.startsWith("UBERON"))
            return getShallowUberonTerm(id).getDefinition();
        if (id.startsWith("GO:"))
            return getGOTerm(id).getDefinition();
        return null;
    }

    public Set<String> getParentTermIDs(String doID) {
        return getClosureChildToParentsMapping().get(doID);
    }

    // Convenience method to populate the evidence codes on the publicationJoins object
    public void populatePublicationJoins(List<PublicationJoin> joins) {
        if (joins == null)
            return;

        if (ecoTermMap.isEmpty()) {
            populateAllPublicationJoins();
        }
        joins.stream()
                .filter(join -> ecoTermMap.get(join.getPrimaryKey()) != null)
                .forEach(join -> join.setEcoCode(ecoTermMap.get(join.getPrimaryKey())));
    }

    public Set<DiseaseEntityJoin> getAllDiseaseEntityJoins() {
        if (allDiseaseEntityJoins.size() > 1000)
            return allDiseaseEntityJoins;
        String cypher = "MATCH p=(disease:DOTerm)-[:ASSOCIATION]-(diseaseEntityJoin:DiseaseEntityJoin)-[:EVIDENCE]->(pubEvCode:PublicationJoin)-[:ASSOCIATION]-(publication:Publication) ";
        cypher += " where disease.isObsolete = 'false' ";
        //cypher += " AND disease.primaryKey in ['DOID:0050144','DOID:0110599','DOID:0050545'] ";
        //cypher += " AND disease.primaryKey in ['DOID:0080348'] ";
        //cypher += " AND diseaseEntityJoin.primaryKey = 'FB:FBgn0030343DOID:1838is_implicated_in'  ";
        //cypher += " AND disease.primaryKey in ['DOID:0050657'] ";
        //cypher += " AND gene.primaryKey = 'HGNC:7' ";
        //cypher += "      OPTIONAL MATCH eco   =(pubEvCode:PublicationJoin)-[:ASSOCIATION]->(ecoTerm:ECOTerm)";
        cypher += "      OPTIONAL MATCH p7    =(:CrossReference)--(diseaseEntityJoin:DiseaseEntityJoin)-[:ANNOTATION_SOURCE_CROSS_REFERENCE]-(:CrossReference)";
        cypher += "      OPTIONAL MATCH p0    =(diseaseEntityJoin:DiseaseEntityJoin)<-[:ASSOCIATION]-(gene:Gene)-[:FROM_SPECIES]->(species:Species)";
        cypher += "      OPTIONAL MATCH p1    =(diseaseEntityJoin:DiseaseEntityJoin)<-[:ASSOCIATION]-(feature:Feature)-[:CROSS_REFERENCE]->(crossReference:CrossReference) ";
        cypher += "      OPTIONAL MATCH aModel=(diseaseEntityJoin:DiseaseEntityJoin)<-[:ASSOCIATION]-(model:AffectedGenomicModel)-[:FROM_SPECIES]->(speciesModel:Species) ";
        cypher += "      OPTIONAL MATCH p4=(diseaseEntityJoin:DiseaseEntityJoin)-[:FROM_ORTHOLOGOUS_GENE]-(orthoGene:Gene)-[:FROM_SPECIES]->(orthoSpecies:Species) ";
        cypher += "      OPTIONAL MATCH p5=(pubEvCode:PublicationJoin)-[:PRIMARY_GENETIC_ENTITY]->(agm:AffectedGenomicModel) ";
        cypher += "      OPTIONAL MATCH p6=(pubEvCode:PublicationJoin)-[:PRIMARY_GENETIC_ENTITY]->(allele:Allele) ";
        cypher += " RETURN p, p0, p1, p4, p5, p6, p7, aModel";
        //cypher += " RETURN p, p0, p1, p2, p4, p5, aModel";

        long start = System.currentTimeMillis();
        Iterable<DiseaseEntityJoin> joins = query(DiseaseEntityJoin.class, cypher);

        allDiseaseEntityJoins = StreamSupport.stream(joins.spliterator(), false).
                collect(Collectors.toSet());
        log.info("Total DiseaseEntityJoinRecords: " + String.format("%,d", allDiseaseEntityJoins.size()));
        log.info("Loaded in:    " + ((System.currentTimeMillis() - start) / 1000) + " s");
        return allDiseaseEntityJoins;
    }

    public Set<DiseaseEntityJoin> getAllDiseaseAlleleEntityJoins() {
        String cypher = "MATCH p=(disease:DOTerm)-[:ASSOCIATION]-(diseaseEntityJoin:DiseaseEntityJoin)-[:EVIDENCE]->(pubEvCode:PublicationJoin)-[:ASSOCIATION]-(publication:Publication)," +
                "             p1=(diseaseEntityJoin:DiseaseEntityJoin)<-[:ASSOCIATION]-(feature:Feature)-[:CROSS_REFERENCE]->(crossReference:CrossReference) ";
        cypher += " where disease.isObsolete = 'false' ";
        //cypher += " AND disease.primaryKey in ['DOID:0050144','DOID:0110599','DOID:0050545'] ";
        //cypher += " AND disease.primaryKey in ['DOID:1838'] ";
        //cypher += " AND gene.primaryKey = 'ZFIN:ZDB-GENE-040426-1716' ";
        //cypher += "      OPTIONAL MATCH eco   =(pubEvCode:PublicationJoin)-[:ASSOCIATION]->(ecoTerm:ECOTerm)";
        cypher += "      OPTIONAL MATCH p4=(diseaseEntityJoin:DiseaseEntityJoin)-[:FROM_ORTHOLOGOUS_GENE]-(orthoGene:Gene)-[:FROM_SPECIES]->(orthoSpecies:Species) ";
        cypher += "      OPTIONAL MATCH p5=(pubEvCode:PublicationJoin)-[:PRIMARY_GENETIC_ENTITY]->(agm:AffectedGenomicModel) ";
        cypher += " RETURN p, p1, p4, p5";

        long start = System.currentTimeMillis();
        Iterable<DiseaseEntityJoin> joins = query(DiseaseEntityJoin.class, cypher);

        Set<DiseaseEntityJoin> allDiseaseEntityJoins = StreamSupport.stream(joins.spliterator(), false).
                collect(Collectors.toSet());
        log.info("Total DiseaseEntityJoinRecords: " + String.format("%,d", allDiseaseEntityJoins.size()));
        log.info("Loaded in:    " + ((System.currentTimeMillis() - start) / 1000) + " s");
        return allDiseaseEntityJoins;
    }

    public Result getDiseaseAssociation(String geneID, String diseaseID, Pagination pagination, Boolean
            diseaseViaEmpiricalData) {
        boolean isGene = geneID != null && diseaseID == null;
        HashMap<String, String> bindingValueMap = new HashMap<>();
        if (isGene)
            bindingValueMap.put("geneID", geneID);
        else
            bindingValueMap.put("diseaseID", diseaseID);


        String cypher = getCypherSelectPart(pagination, diseaseViaEmpiricalData, isGene, bindingValueMap);

        if (diseaseViaEmpiricalData == null || diseaseViaEmpiricalData) {
            if (isGene) {
                cypher += "return distinct (disease.name + diseaseEntityJoin.joinType) as nameJoin, ";
            } else {
                cypher += "return distinct (";
                cypher += createReturnListToOrder(pagination.getSortByList(), FieldFilter.GENE_NAME, FieldFilter.SPECIES, FieldFilter.DISEASE, FieldFilter.ASSOCIATION_TYPE);
                cypher += ") as nameJoin, ";
            }
        } else {
            if (isGene)
                cypher += "return distinct (disease.name + diseaseEntityJoin.joinType + orthoGene.primaryKey) as nameJoin, ";
            else {
                cypher += "return distinct (";
                cypher += createReturnListToOrder(pagination.getSortByList(), FieldFilter.GENE_NAME, FieldFilter.SPECIES, FieldFilter.DISEASE);
                cypher += ") as nameJoin, ";
            }
        }

        cypher += "     gene.symbol,  " +
                "       disease.name as diseaseName, " +
                "       disease as disease, " +
                "       gene as gene, " +
                "       species as species, " +
                "       diseaseEntityJoin as diseaseEntityJoin, " +
//                "       geneCrossRef as geneCrossReference, " +
                "       feature.symbol, " +
                "       feature as feature, " +
                "       max(diseaseEntityJoin.sortOrder) as associationSortOrder, " +
                "       collect(crossReference) as crossReferences, " +
                "       collect(evidence) as evidences, ";
        if (diseaseViaEmpiricalData != null && !diseaseViaEmpiricalData) {
            cypher += "       collect(orthoGene) as orthoGenes, " +
                    "         collect(orthoSpecies) as orthoSpecies, ";
        }

        cypher += "       count(publications),         " +
                "       collect(publications.pubModId) ";
        cypher += "order by associationSortOrder ASC, species.phylogeneticOrder ASC, LOWER(nameJoin) " + pagination.getAscending() + ", LOWER(feature.symbol)";
        cypher += " SKIP " + pagination.getStart();
        if (pagination.getLimit() != null && pagination.getLimit() > -1)
            cypher += " LIMIT " + pagination.getLimit();

        return queryForResult(cypher, bindingValueMap);
    }

    private String getCypherSelectPart(Pagination pagination, Boolean diseaseViaEmpiricalData,
                                       boolean isGene, HashMap<String, String> bindingValueMap) {
        String cypher = "MATCH p0=";
        if (isGene) {
            cypher += "(disease:DOTerm)";
        } else {
            cypher += DISEASE_INCLUDING_CHILDREN;
        }
        cypher += "--(diseaseEntityJoin:DiseaseEntityJoin)-[:EVIDENCE]-(pubEvCode:PublicationJoin), " +
                "              p1=(publications:Publication)--(pubEvCode)--(evidence:ECOTerm), " +
                "              p2=(diseaseEntityJoin)-[:ASSOCIATION]-(gene:Gene)--(species:Species) ";
/*
        "              p2=(diseaseEntityJoin)-[:ASSOCIATION]-(gene:Gene)--(species:Species), " +
                "              p3=(gene:Gene)--(geneCrossRef:CrossReference {crossRefType:'gene'}) ";
*/

        if (diseaseViaEmpiricalData != null && !diseaseViaEmpiricalData) {
            cypher += cypherViaOrthology;
        }

        String cypherFeatureOptional = "OPTIONAL MATCH p4=(diseaseEntityJoin)--(feature:Feature)--(crossReference:CrossReference) ";
        String entityType = pagination.getFieldFilterValueMap().get(FieldFilter.GENETIC_ENTITY_TYPE);
        if (entityType != null && entityType.equals("allele")) {
            cypher += ", p4=(diseaseEntityJoin)--(feature:Feature)--(crossReference:CrossReference) ";
            cypherFeatureOptional = "";
        }

        String cypherWhereClause = " where ";
        if (isGene) {
            cypherWhereClause += "gene.primaryKey = {geneID} ";
        } else {
            cypherWhereClause += "diseaseParent.primaryKey = {diseaseID} ";
        }

        if (entityType != null && entityType.equals("gene")) {
            cypherWhereClause += "AND NOT (diseaseEntityJoin)--(:Feature) ";
        }
        if (diseaseViaEmpiricalData != null && diseaseViaEmpiricalData) {
            cypherWhereClause += cypherEmpirical;
        }
        cypherWhereClause += getFilterClauses(pagination, false);

        String geneticEntityFilterClause = addAndWhereClauseString("feature.symbol", FieldFilter.GENETIC_ENTITY, pagination.getFieldFilterValueMap());
        if (geneticEntityFilterClause != null) {
            cypherWhereClause += geneticEntityFilterClause;
            bindingValueMap.put("feature", pagination.getFieldFilterValueMap().get(FieldFilter.GENETIC_ENTITY));
            cypher += ", " + FEATURE_JOIN;
        }
        cypher += cypherWhereClause;
        if (geneticEntityFilterClause == null) {
            cypher += cypherFeatureOptional;
        }
        return cypher;
    }

    private String createReturnListToOrder(List<FieldFilter> sortingFields, FieldFilter... defaultFields) {
        if (CollectionUtils.isEmpty(sortingFields))
            sortingFields = Arrays.asList(defaultFields);
        StringJoiner joiner = new StringJoiner(" + ");
        sortingFields.forEach(fieldFilter -> joiner.add(sortByMapping.get(fieldFilter)));
        final List<FieldFilter> filterElements = sortingFields;
        sortByMapping.forEach((fieldFilter, s) -> {
            if (!filterElements.contains(fieldFilter))
                joiner.add(s);
        });
        return joiner.toString();
    }

    private String getFilterClauses(Pagination pagination, boolean filterForCounting) {
        String cypherWhereClause = "";
        // add gene name filter
        cypherWhereClause += addToCypherWhereClause(pagination.getFieldFilterValueMap(), "gene.symbol", FieldFilter.GENE_NAME);

        // add disease name filter
        cypherWhereClause += addToCypherWhereClause(pagination.getFieldFilterValueMap(), "disease.name", FieldFilter.DISEASE);

        // add association name filter
        cypherWhereClause += addToCypherWhereClause(pagination.getFieldFilterValueMap(), "diseaseEntityJoin.joinType", FieldFilter.ASSOCIATION_TYPE);

        // add evidence code filter
        cypherWhereClause += addToCypherWhereClause(pagination.getFieldFilterValueMap(), "evidence.primaryKey", FieldFilter.EVIDENCE_CODE);

        if (!filterForCounting) {
            // add ortho gene filter
            cypherWhereClause += addToCypherWhereClause(pagination.getFieldFilterValueMap(), "orthoGene.symbol", FieldFilter.ORTHOLOG);

            // add ortho gene species filter
            cypherWhereClause += addToCypherWhereClause(pagination.getFieldFilterValueMap(), "orthoSpecies.name", FieldFilter.ORTHOLOG_SPECIES);
        }

        // add reference filter clause
        String referenceFilterClause = addAndWhereClauseORString("publications.pubModId", "publications.pubMedId", FieldFilter.FREFERENCE, pagination.getFieldFilterValueMap());
        if (referenceFilterClause != null) {
            cypherWhereClause += referenceFilterClause;
        }

        return cypherWhereClause;
    }

    private String addToCypherWhereClause(BaseFilter baseFilter, String s, FieldFilter disease) {
        String diseaseFilterClause = addAndWhereClauseString(s, disease, baseFilter);
        if (diseaseFilterClause != null) {
            return diseaseFilterClause;
        }
        return "";
    }

    private String addAndWhereClauseORString(String eitherElement, String orElement, FieldFilter
            fieldFilter, BaseFilter baseFilter) {
        String eitherClause = addWhereClauseString(eitherElement, fieldFilter, baseFilter, null);
        if (eitherClause == null)
            return null;
        String orClause = addWhereClauseString(orElement, fieldFilter, baseFilter, null);
        if (orClause == null)
            return null;
        return "AND (" + eitherClause + " OR " + orClause + ") ";
    }

    public Long getTotalDiseaseCount(String geneID, Pagination pagination, boolean empiricalDisease) {
        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("geneID", geneID);

        String baseCypher = "MATCH p0=(disease:DOTerm)--(diseaseEntityJoin:DiseaseEntityJoin)-[:EVIDENCE]-(pubEvCode:PublicationJoin), " +
                "              p1=(publications:Publication)--(pubEvCode:PublicationJoin)--(evidence:ECOTerm), " +
                "              p2=(diseaseEntityJoin)--(gene:Gene)-[:FROM_SPECIES]-(species:Species) ";
        if (!empiricalDisease) {
            baseCypher += cypherViaOrthology;
        }
        baseCypher += "where gene.primaryKey = {geneID} ";
        // get feature-less diseases

        baseCypher += getFilterClauses(pagination, true);

        String cypher = baseCypher + AND_NOT_DISEASE_ENTITY_JOIN_FEATURE;
        if (empiricalDisease) {
            cypher += cypherEmpirical;
            cypher += "return count(distinct disease.name + diseaseEntityJoin.joinType) as " + TOTAL_COUNT;
        } else {
            // add ortho gene filter
            cypher += addToCypherWhereClause(pagination.getFieldFilterValueMap(), "orthoGene.symbol", FieldFilter.ORTHOLOG);

            // add ortho gene species filter
            cypher += addToCypherWhereClause(pagination.getFieldFilterValueMap(), "orthoSpecies.name", FieldFilter.ORTHOLOG_SPECIES);
            cypher += "return count(distinct disease.name + diseaseEntityJoin.joinType + orthoGene.primaryKey) as " + TOTAL_COUNT;
        }
        Long featureLessPhenotype = 0L;

        String geneticEntityFilterClause = addWhereClauseString("feature.symbol", FieldFilter.GENETIC_ENTITY, pagination.getFieldFilterValueMap(), "WHERE");
        if (geneticEntityFilterClause == null)
            featureLessPhenotype = (Long) queryForResult(cypher, bindingValueMap).iterator().next().get(TOTAL_COUNT);

        // feature-related phenotypes
        cypher = baseCypher;
        if (empiricalDisease)
            cypher += cypherEmpirical;

        cypher += "WITH distinct disease, diseaseEntityJoin ";
        cypher += "MATCH (diseaseEntityJoin)--(feature:Feature) ";
        if (geneticEntityFilterClause != null) {
            cypher += geneticEntityFilterClause;
            bindingValueMap.put("feature", pagination.getFieldFilterValueMap().get(FieldFilter.GENETIC_ENTITY));
        }
        cypher += "return count(distinct disease.name+feature.symbol) as " + TOTAL_COUNT;

        Long featurePhenotype = (Long) queryForResult(cypher, bindingValueMap).iterator().next().get(TOTAL_COUNT);
        String entityType = pagination.getFieldFilterValueMap().get(FieldFilter.GENETIC_ENTITY_TYPE);
        if (entityType != null) {
            switch (entityType) {
                case "allele":
                    return featurePhenotype;
                case "gene":
                    return featureLessPhenotype;
                default:
                    break;
            }
        }
        return featureLessPhenotype + featurePhenotype;
    }

    public Long getTotalDistinctDiseaseCount(String geneID, boolean empiricalDisease) {
        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("geneID", geneID);

        String baseCypher = "MATCH p0=(disease:DOTerm)--(diseaseEntityJoin:DiseaseEntityJoin)--(pubEvJoin:PublicationJoin), " +
                "              p1=(evidence:ECOTerm)--(pubEvJoin:PublicationJoin)--(publications:Publication), " +
                "              p2=(diseaseEntityJoin)--(gene:Gene)-[:FROM_SPECIES]-(species:Species) ";
        if (!empiricalDisease) {
            baseCypher += cypherViaOrthology;
        }
        baseCypher += "where gene.primaryKey = {geneID} ";

        String cypher = baseCypher;
        if (empiricalDisease) {
            cypher += cypherEmpirical;
        }
        cypher += "return count(distinct disease.name ) as " + TOTAL_COUNT;
        return (Long) queryForResult(cypher, bindingValueMap).iterator().next().get(TOTAL_COUNT);
    }

    public Long getTotalDiseaseCount(String diseaseID, Pagination pagination) {
        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("diseaseID", diseaseID);

        // get feature-less diseases
        String filterClauses = getFilterClauses(pagination, true);


        //String cypherAll = getCypherSelectPart(pagination, null, false, bindingValueMap);
        String cypherAll = "MATCH " + DISEASE_INCLUDING_CHILDREN + "--" +
                "(diseaseEntityJoin:DiseaseEntityJoin), " +
                "(diseaseEntityJoin)-[:ASSOCIATION]-(gene:Gene)  " +
                "where diseaseParent.primaryKey = {diseaseID} ";
        cypherAll += AND_NOT_DISEASE_ENTITY_JOIN_FEATURE;
        cypherAll += "return count(distinct diseaseEntityJoin) as " + TOTAL_COUNT;

        String geneticEntityFilterClause = addWhereClauseString("feature.symbol", FieldFilter.GENETIC_ENTITY, pagination.getFieldFilterValueMap(), "WHERE");
        Long featureLessPhenotype = 0L;
        if (geneticEntityFilterClause == null) {
            featureLessPhenotype = (Long) queryForResult(cypherAll, bindingValueMap).iterator().next().get(TOTAL_COUNT);
        }

        // feature-related phenotypes
        cypherAll = "MATCH " + DISEASE_INCLUDING_CHILDREN + "--" +
                "(diseaseEntityJoin:DiseaseEntityJoin), " +
                "(diseaseEntityJoin)-[:ASSOCIATION]-(gene:Gene),  ";
        cypherAll += FEATURE_JOIN +
                " where diseaseParent.primaryKey = {diseaseID} ";
        cypherAll += "return count(distinct diseaseEntityJoin) as " + TOTAL_COUNT;

        Long featurePhenotype = 0L;
        if (geneticEntityFilterClause == null) {
            featurePhenotype = (Long) queryForResult(cypherAll, bindingValueMap).iterator().next().get(TOTAL_COUNT);
        }
        return featureLessPhenotype + featurePhenotype;
    }

    public DiseaseSummary getDiseaseSummary(String geneId, DiseaseSummary.Type type) {
        DiseaseSummary summary = new DiseaseSummary();
        summary.setType(type);
        summary.setNumberOfAnnotations(getTotalDiseaseCount(geneId, new Pagination(), type.equals(DiseaseSummary.Type.EXPERIMENT)));
        summary.setNumberOfEntities(getTotalDistinctDiseaseCount(geneId, type.equals(DiseaseSummary.Type.EXPERIMENT)));
        return summary;
    }

    public DiseaseEntityJoin getDiseaseEntityJoinByID(String diseaseEntityJoinID) {

        String cypher = "MATCH p0=(diseaseEntityJoin:DiseaseEntityJoin)-[:ASSOCIATION]-(gene:Gene), " +
                "p1=(disease:DOTerm)--(diseaseEntityJoin:DiseaseEntityJoin)--(pubEvJoin:PublicationJoin), " +
                "p2=(evidence:ECOTerm)--(pubEvJoin:PublicationJoin)--(publications:Publication) " +
                "WHERE diseaseEntityJoin.primaryKey = {diseaseEntityJoinID} " +
                "OPTIONAL MATCH p3=(diseaseEntityJoin:DiseaseEntityJoin)--(feature:Feature)--(crossReference:CrossReference) " +
                "RETURN p0, p1, p3, p2 ";

        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("diseaseEntityJoinID", diseaseEntityJoinID);

        Iterable<DiseaseEntityJoin> joins = query(DiseaseEntityJoin.class, cypher, bindingValueMap);
        return joins.iterator().next();
    }
}
