package org.alliancegenome.neo4j.repository;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.model.Result;

import java.util.*;

public class DiseaseRepository extends Neo4jRepository<DOTerm> {

    private Logger log = LogManager.getLogger(getClass());
    public static final String TOTAL_COUNT = "totalCount";

    public DiseaseRepository() {
        super(DOTerm.class);
    }

    //  public List<DOTerm> getAllDiseaseTerms(int start, int maxSize) {
    //      String cypher = "match (root:DOTerm) WHERE  root.is_obsolete = 'false' " +
    //              "WITH root SKIP " + start + " LIMIT " + maxSize + " " +
    //              "optional match (diseaseGeneJoin:DiseaseEntityJoin)-[q:ASSOCIATION]->(root), " +
    //              "(gene:Gene)-[geneDiseaseRelation:ASSOCIATION]->(diseaseGeneJoin), " +
    //              "(publication:Publication)<-[publicationRelation]-(diseaseGeneJoin), " +
    //              "(evidence:EvidenceCode)<-[evidenceRelation:EVIDENCE]-(diseaseGeneJoin), " +
    //              "(species:Species)<-[speciesRelation:FROM_SPECIES]-(gene), " +
    //              "(root)-[crossReferenceRelation:CROSS_REFERENCE]->(crossReference:CrossReference) " +
    //              "return distinct root, q,diseaseGeneJoin, geneDiseaseRelation, gene, publicationRelation, publication, evidenceRelation, evidence, " +
    //              "species, speciesRelation, crossReference, crossReferenceRelation ";
    //      return (List<DOTerm>) query(cypher);
    //  }

    //  public List<DOTerm> getAllDiseaseTerms() {
    //      String cypher = "match (root:DOTerm) " +
    //              "optional match (a:Association)-[q:ASSOCIATION]->(root), " +
    //              "(m:Gene)-[qq:ASSOCIATION]->(a), " +
    //              "(p:Publication)<-[qqq*]-(a), " +
    //              "(e:EvidenceCode)<-[ee:EVIDENCE]-(a), " +
    //              "(s:Species)<-[ss:FROM_SPECIES]-(m), " +
    //              "(root)-[ex:CROSS_REFERENCE]->(exx:CrossReference), " +
    //              "(root)-[synonymRelation:ALSO_KNOWN_AS]->(synonym:Synonym)  " +
    //              "optional match (parent:DOTerm)<-[parentRelation:IS_A]-(root:DOTerm), " +
    //              "(child:DOTerm)-[childRelation:IS_A]->(root:DOTerm) " +
    //              "return distinct root, q,a,qq,m,qqq,p, ee, e, s, ss, ex, exx, parent, " +
    //              "parentRelation, child, childRelation, synonymRelation, synonym ";
    //      return (List<DOTerm>) query(cypher);
    //  }


    //  public List<DOTerm> getDiseaseTermsWithAnnotations() {
    //      String cypher = "match (root:DOTerm),  " +
    //              "(a:Association)-[q:ASSOCIATION]->(root), " +
    //              "(gene:Gene)-[geneAssociation:ASSOCIATION]->(a), " +
    //              "(publication:Publication)<-[qqq]-(a), " +
    //              "(evidence:EvidenceCode)<-[evidenceRelation:EVIDENCE]-(a), " +
    //
    //              "(root)-[parentChildRelation:IS_A*]->(parent:DOTerm) " +
    //              "return root, q,a, geneAssociation,gene ,qqq, publication, evidenceRelation, evidence, " +
    //              "species, speciesRelation, parentChildRelation, parent";
    //
    //      return (List<DOTerm>) query(cypher);

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
    //  public long getDiseaseTermsWithAnnotationsCount() {
    //      String cypher = "match (root:DOTerm), " +
    //              "(a:Association)-[q:ASSOCIATION]->(root), " +
    //              "(m:Gene)-[qq:ASSOCIATION]->(a), " +
    //              "(p:Publication)<-[qqq*]-(a), " +
    //              "(e:EvidenceCode)<-[ee:EVIDENCE]-(a), " +
    //              "(s:Species)<-[ss:FROM_SPECIES]-(m), " +
    //              "(root)-[ex:CROSS_REFERENCE]->(exx:CrossReference) " +
    //              "return count(root)";
    //      Long s = queryCount(cypher);
    //      return s;
    //  }

    public List<String> getAllDiseaseKeys() {
        String query = "MATCH (term:DOTerm) WHERE term.is_obsolete='false' RETURN term.primaryKey";
        log.debug("Starting Query: " + query);
        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();

        ArrayList<String> list = new ArrayList<>();

        while (i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String) map2.get("term.primaryKey"));
        }
        log.debug("Query Finished: " + list.size());
        return list;
    }

    public Set<String> getAllDiseaseWithAnnotationsKeys() {
        String query = "MATCH (term:DOTerm)-[q:ASSOCIATION]-(dej:DiseaseEntityJoin) WHERE term.is_obsolete='false' " +
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

    public Result getDiseaseAssociation(String geneID, String diseaseID, Pagination pagination, Boolean diseaseViaEmpiricalData) {
        boolean isGene = geneID != null && diseaseID == null;
        HashMap<String, String> bindingValueMap = new HashMap<>();
        if (isGene)
            bindingValueMap.put("geneID", geneID);
        else
            bindingValueMap.put("diseaseID", diseaseID);


        String cypher = "MATCH p0=(disease:DOTerm)--(diseaseEntityJoin:DiseaseEntityJoin)-[:EVIDENCE]-(publication:Publication), " +
                "              p1=(diseaseEntityJoin)--(evidence:EvidenceCode), " +
                "              p2=(diseaseEntityJoin)--(gene:Gene) ";

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
            cypherWhereClause += "disease.primaryKey = {diseaseID} ";
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
            cypher += ", p4=(diseaseEntityJoin)--(feature:Feature)--(crossReference:CrossReference) ";
        }
        cypher += cypherWhereClause;
        if (geneticEntityFilterClause == null) {
            cypher += cypherFeatureOptional;
        }
        if (diseaseViaEmpiricalData)
            cypher += "return distinct (disease.name + diseaseEntityJoin.joinType) as nameJoin, ";
        else
            cypher += "return distinct (disease.name + diseaseEntityJoin.joinType + orthoGene.primaryKey) as nameJoin, ";

        cypher += "       disease.name as diseaseName, " +
                "       disease as disease, " +
                "       feature.symbol, " +
                "       feature as feature, " +
                "       collect(diseaseEntityJoin) as diseaseEntityJoin, " +
                "       collect(crossReference) as crossReferences, " +
                "       collect(publication.pubMedId), " +
                "       collect(publication) as publications, " +
                "       collect(evidence) as evidences, ";
        if (!diseaseViaEmpiricalData) {
            cypher += "       collect(orthoGene) as orthoGenes, " +
                    "         collect(orthoSpecies) as orthoSpecies, ";
        }
        cypher += "       count(publication),         " +
                "       collect(publication.pubModId) " +
                "order by LOWER(nameJoin), LOWER(feature.symbol)";
        cypher += " SKIP " + pagination.getStart();
        if (pagination.getLimit() != null && pagination.getLimit() > -1)
            cypher += " LIMIT " + pagination.getLimit();

        return queryForResult(cypher, bindingValueMap);
    }

    private String getFilterClauses(Pagination pagination, boolean filterForCounting) {
        String cypherWhereClause = "";
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
        String referenceFilterClause = addAndWhereClauseORString("publication.pubModId", "publication.pubMedId", FieldFilter.FREFERENCE, pagination.getFieldFilterValueMap());
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

    private String addAndWhereClauseORString(String eitherElement, String orElement, FieldFilter fieldFilter, BaseFilter baseFilter) {
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

        String baseCypher = "MATCH p0=(disease:DOTerm)--(diseaseEntityJoin:DiseaseEntityJoin)-[:EVIDENCE]-(publication:Publication), " +
                "              p1=(diseaseEntityJoin)--(evidence:EvidenceCode), " +
                "              p2=(diseaseEntityJoin)--(gene:Gene)-[:FROM_SPECIES]-(species:Species) ";
        if (!empiricalDisease) {
            baseCypher += cypherViaOrthology;
        }
        baseCypher += "where gene.primaryKey = {geneID} ";
        // get feature-less diseases

        baseCypher += getFilterClauses(pagination, true);

        String cypher = baseCypher + " AND NOT (diseaseEntityJoin)--(:Feature) ";
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
}
