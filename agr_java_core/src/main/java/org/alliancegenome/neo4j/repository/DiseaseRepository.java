package org.alliancegenome.neo4j.repository;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.DiseaseEntityJoin;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.PublicationEvidenceCodeJoin;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.model.Result;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

public class DiseaseRepository extends Neo4jRepository<DOTerm> {

    public static final String DISEASE_INCLUDING_CHILDREN = "(diseaseParent:DOTerm)<-[:IS_A_PART_OF_CLOSURE]-(disease:DOTerm)";
    public static final String FEATURE_JOIN = " p4=(diseaseEntityJoin)--(feature:Feature)--(crossReference:CrossReference) ";
    public static final String AND_NOT_DISEASE_ENTITY_JOIN_FEATURE = " AND NOT (diseaseEntityJoin)--(:Feature) ";
    private Logger log = LogManager.getLogger(getClass());
    public static final String TOTAL_COUNT = "totalCount";

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

    //  public List<DOTerm> getAllTerms() {
    //      String cypher = "match (root:DOTerm) " +
    //              "optional match (parent:DOTerm)<-[parentRelation:IS_A]-(root:DOTerm), " +
    //              "(child:DOTerm)-[childRelation:IS_A]->(root:DOTerm), " +
    //              "(synonym:Synonym)<-[synonymRelation:ALSO_KNOWN_AS]-(root:DOTerm) " +
    //              "return root, parent, " +
    //              "parentRelation, child, childRelation, synonym, synonymRelation";
    //      return (List<DOTerm>) query(cypher);
    //  }
    //

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

    private Set<DiseaseEntityJoin> allDiseaseEntityJoins = new HashSet<>(200000);
    private static Map<String, Set<String>> closureMapGO = null;
    private static Map<String, Set<String>> closureMapUberon = null;
    private static Map<String, Set<String>> closureMapUberonChild = null;
    private static Map<String, Set<String>> closureMap = null;
    private static Map<String, Set<String>> closureChildMap = null;

    public Set<DiseaseEntityJoin> getDiseaseAssociations(String diseaseID, Pagination pagination) {
        Set<DiseaseEntityJoin> allDiseaseEntityJoinSet = getAllDiseaseEntityJoins();
        return allDiseaseEntityJoinSet;
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


    public Map<String, Set<String>> getClosureChildMapping() {
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
        System.out.println("AGR-DO slim: " + doAgrDoList.size());
        return doAgrDoList;

    }

    public Set<ECOTerm> getEcoTerms(List<PublicationEvidenceCodeJoin> publicationEvidenceCodeJoin) {
        StringJoiner joiner = new StringJoiner(",", "'", "'");
        publicationEvidenceCodeJoin.forEach(codeJoin -> joiner.add(codeJoin.getPrimaryKey()));
        String ids = joiner.toString();
        String cypher = "MATCH p=(pubEvCode:PublicationEvidenceCodeJoin)-[:ASSOCIATION]->(ev:ECOTerm) " +
                "where pubEvCode.primaryKey in [" + ids + "]" +
                "RETURN p";

        Iterable<ECOTerm> joins = neo4jSession.query(ECOTerm.class, cypher, new HashMap<>());

        return StreamSupport.stream(joins.spliterator(), false).
                collect(Collectors.toSet());
    }

    private Map<String, List<ECOTerm>> ecoTermMap = new HashMap<>();

    private void populateAllPubEvidenceCodeJoins() {
        String pubCodeID = "pubCodeID";
        String ecoName = "ecoName";
        String cypher = "MATCH p=(pubEvCode:PublicationEvidenceCodeJoin)-[:ASSOCIATION]->(ev:ECOTerm) " +
                "RETURN pubEvCode.primaryKey as " + pubCodeID + ", collect(ev) as " + ecoName;

        Result result = queryForResult(cypher, new HashMap<>());
        result.forEach(resultMap -> {
            String id = (String) resultMap.get(pubCodeID);
            List<ECOTerm> terms = ecoTermMap.get(id);
            List<ECOTerm> names = (List<ECOTerm>) resultMap.get(ecoName);
            if (terms == null) {
                ecoTermMap.put(id, names);
            } else {
                terms.addAll(names);
            }
        });

        //Iterable<PublicationEvidenceCodeJoin> joins = neo4jSession.query(PublicationEvidenceCodeJoin.class, cypher, new HashMap<>());

        System.out.println("Number of PublicationEvidenceCodeJoin records retrieved: " + String.format("%,d", ecoTermMap.size()));
    }

    public List<ECOTerm> getEcoTerm(String publicationEvidenceCodeJoinID) {
        if (ecoTermMap.isEmpty()) {
            populateAllPubEvidenceCodeJoins();
        }
        return ecoTermMap.get(publicationEvidenceCodeJoinID);
    }

    public List<ECOTerm> getEcoTerm(List<PublicationEvidenceCodeJoin> joins) {
        if (ecoTermMap.isEmpty()) {
            populateAllPubEvidenceCodeJoins();
        }
        List<ECOTerm> terms = new ArrayList<>();
        joins.forEach(publicationEvidenceCodeJoin -> terms.addAll(ecoTermMap.get(publicationEvidenceCodeJoin.getPrimaryKey())));
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

    @Setter
    @Getter
    class Closure {
        String parent;
        String child;
    }

    public Set<DiseaseEntityJoin> getAllDiseaseEntityJoins() {
        if (allDiseaseEntityJoins.size() > 1000)
            return allDiseaseEntityJoins;
        String cypher = "MATCH p=(disease:DOTerm)--(diseaseEntityJoin:DiseaseEntityJoin)<-[:ASSOCIATION]-(gene:Gene)--(species:Species)," +
                "              p2=(diseaseEntityJoin:DiseaseEntityJoin)-[:EVIDENCE]->(pubEvCode:PublicationEvidenceCodeJoin)," +
                "              p3=(publication:Publication)-[:ASSOCIATION]->(pubEvCode:PublicationEvidenceCodeJoin)";
        cypher += " where disease.isObsolete = 'false' ";
        //cypher += " AND disease.primaryKey = 'DOID:9952' ";
        cypher += "        OPTIONAL MATCH p1=(diseaseEntityJoin:DiseaseEntityJoin)--(feature:Feature)--(crossReference:CrossReference) " +
                "        OPTIONAL MATCH p4=(diseaseEntityJoin:DiseaseEntityJoin)-[:FROM_ORTHOLOGOUS_GENE]-(orthoGene:Gene)-[:FROM_SPECIES]-(orthoSpecies:Species) " +
                "RETURN p, p1, p2, p3, p4 ";

        long start = System.currentTimeMillis();
        Iterable<DiseaseEntityJoin> joins = neo4jSession.query(DiseaseEntityJoin.class, cypher, new HashMap<>());

        allDiseaseEntityJoins = StreamSupport.stream(joins.spliterator(), false).
                collect(Collectors.toSet());
        System.out.println("Total DiseaseEntityJoinRecords: " + String.format("%,d", allDiseaseEntityJoins.size()));
        System.out.println("Loaded in:  " + ((System.currentTimeMillis() - start) / 1000) + " s");
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
        cypher += "--(diseaseEntityJoin:DiseaseEntityJoin)-[:EVIDENCE]-(pubEvCode:PublicationEvidenceCodeJoin), " +
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

    static Map<FieldFilter, String> sortByMapping = new TreeMap<>();

    static {
        sortByMapping.put(FieldFilter.GENE_NAME, "gene.symbol + substring('               ', 0, size(gene.symbol))");
        sortByMapping.put(FieldFilter.SPECIES, "species.name");
        sortByMapping.put(FieldFilter.DISEASE, "disease.name");
        sortByMapping.put(FieldFilter.ASSOCIATION_TYPE, "diseaseEntityJoin.joinType");
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

    private String cypherEmpirical = " AND NOT (diseaseEntityJoin)-[:FROM_ORTHOLOGOUS_GENE]-(:Gene) ";
    private String cypherViaOrthology = " ,p5 =  (diseaseEntityJoin)-[:FROM_ORTHOLOGOUS_GENE]-(orthoGene:Gene)-[:FROM_SPECIES]-(orthoSpecies:Species) ";

    public Long getTotalDiseaseCount(String geneID, Pagination pagination, boolean empiricalDisease) {
        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("geneID", geneID);

        String baseCypher = "MATCH p0=(disease:DOTerm)--(diseaseEntityJoin:DiseaseEntityJoin)-[:EVIDENCE]-(pubEvCode:PublicationEvidenceCodeJoin), " +
                "              p1=(publications:Publication)--(pubEvCode:PublicationEvidenceCodeJoin)--(evidence:ECOTerm), " +
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

        String baseCypher = "MATCH p0=(disease:DOTerm)--(diseaseEntityJoin:DiseaseEntityJoin)--(pubEvJoin:PublicationEvidenceCodeJoin), " +
                "              p1=(evidence:ECOTerm)--(pubEvJoin:PublicationEvidenceCodeJoin)--(publications:Publication), " +
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
                "p1=(disease:DOTerm)--(diseaseEntityJoin:DiseaseEntityJoin)--(pubEvJoin:PublicationEvidenceCodeJoin), " +
                "p2=(evidence:ECOTerm)--(pubEvJoin:PublicationEvidenceCodeJoin)--(publications:Publication) " +
                "WHERE diseaseEntityJoin.primaryKey = {diseaseEntityJoinID} " +
                "OPTIONAL MATCH p3=(diseaseEntityJoin:DiseaseEntityJoin)--(feature:Feature)--(crossReference:CrossReference) " +
                "RETURN p0, p1, p3, p2 ";

        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("diseaseEntityJoinID", diseaseEntityJoinID);

        Iterable<DiseaseEntityJoin> joins = neo4jSession.query(DiseaseEntityJoin.class, cypher, bindingValueMap);
        return joins.iterator().next();
    }
}
