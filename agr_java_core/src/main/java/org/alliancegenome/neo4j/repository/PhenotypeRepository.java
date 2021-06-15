package org.alliancegenome.neo4j.repository;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Phenotype;
import org.alliancegenome.neo4j.entity.node.PhenotypeEntityJoin;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.model.Result;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PhenotypeRepository extends Neo4jRepository<Phenotype> {

    public static final String TOTAL_COUNT = "totalCount";
    private Logger log = LogManager.getLogger(getClass());

    public PhenotypeRepository() {
        super(Phenotype.class);
    }

    public List<String> getAllPhenotypeKeys() {
        String query = "MATCH (termName:Phenotype) return termName.primaryKey";
        log.debug("Starting Query: " + query);
        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();

        ArrayList<String> list = new ArrayList<>();

        while (i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String) map2.get("termName.primaryKey"));
        }
        log.debug("Query Finished: " + list.size());
        return list;
    }

    public Phenotype getPhenotypeTerm(String primaryKey) {

        String cypher = "MATCH p0=(termName:Phenotype)--(phenotypeEntityJoin:PhenotypeEntityJoin)-[:EVIDENCE]-(publications:Publication)" +
                " WHERE termName.primaryKey = {primaryKey}   " +
                " OPTIONAL MATCH p2=(phenotypeEntityJoin)--(g:Gene)-[:FROM_SPECIES]-(species:Species)" +
                " OPTIONAL MATCH p4=(phenotypeEntityJoin)--(feature:Feature)" +
                " OPTIONAL MATCH crossRefMatch=(phenotypeEntityJoin)--(feature:Feature)--(crossRef:CrossReference)" +
                " RETURN p0, p2, p4, crossRefMatch ";

        HashMap<String, String> map = new HashMap<>();
        map.put("primaryKey", primaryKey);

        Phenotype primaryTerm = null;

        Iterable<Phenotype> terms = query(cypher, map);
        for (Phenotype term : terms) {
            if (term.getPrimaryKey().equals(primaryKey)) {
                primaryTerm = term;
            }
        }

        if (primaryTerm == null) return null;
        return primaryTerm;
    }

    // ToDO: This query builder needs to be re-worked / simplified when we test the filtering part again.
    public Result getPhenotype(String geneID, Pagination pagination) {

        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("geneID", geneID);

        String cypher = "MATCH (phenotype:Phenotype)--(phenotypeEntityJoin:PhenotypeEntityJoin)-[:EVIDENCE]-(publications:Publication), " +
                "        (phenotypeEntityJoin)--(gene:Gene)-[:FROM_SPECIES]-(geneSpecies:Species)";

        String cypherFeatureOptional = "OPTIONAL MATCH (phenotypeEntityJoin)--(feature:Feature)--(featureCrossRef:CrossReference), " +
                "featSpecies=(feature)-[:FROM_SPECIES]-(featureSpecies:Species) ";
        String entityType = pagination.getFieldFilterValueMap().get(FieldFilter.GENETIC_ENTITY_TYPE);
        if (entityType != null && entityType.equals("allele")) {
            cypher += ", (phenotypeEntityJoin)--(feature:Feature)--(featureCrossRef:CrossReference), " +
                    "featSpecies=(feature)-[:FROM_SPECIES]-(featureSpecies:Species) ";
            cypherFeatureOptional = "";
        }
        String cypherWhereClause = "        where gene.primaryKey = {geneID} ";
        if (entityType != null && entityType.equals("gene")) {
            cypherWhereClause += "AND NOT (phenotypeEntityJoin)--(:Feature) ";
        }
        String phenotypeFilterClause = addAndWhereClauseString("phenotype.phenotypeStatement", FieldFilter.PHENOTYPE, pagination.getFieldFilterValueMap());
        if (phenotypeFilterClause != null) {
            cypherWhereClause += phenotypeFilterClause;
        }

        // add reference filter clause
        String referenceFilterClause = addAndWhereClauseORString("publications.pubModId", "publications.pubMedId", FieldFilter.FREFERENCE, pagination.getFieldFilterValueMap());
        if (referenceFilterClause != null) {
            cypherWhereClause += referenceFilterClause;
        }

        String geneticEntityFilterClause = addAndWhereClauseString("feature.symbol", FieldFilter.GENETIC_ENTITY, pagination.getFieldFilterValueMap());
        if (geneticEntityFilterClause != null) {
            cypherWhereClause += geneticEntityFilterClause;
            bindingValueMap.put("feature", pagination.getFieldFilterValueMap().get(FieldFilter.GENETIC_ENTITY));
            cypher += ", (phenotypeEntityJoin)--(feature:Feature)--(featureCrossRef:CrossReference), " +
                    "featSpecies=(feature)-[:FROM_SPECIES]-(featureSpecies:Species) ";
        }
        cypher += cypherWhereClause;
        if (geneticEntityFilterClause == null) {
            cypher += cypherFeatureOptional;
            if (cypherFeatureOptional.isEmpty()) {
                cypher += " AND ";
            } else {
                cypher += " where ";
            }
            cypher += "featureCrossRef.crossRefType = '" + GeneticEntity.CrossReferenceType.ALLELE.getDisplayName() + "' ";
        }
        cypher += "return distinct phenotype.phenotypeStatement as phenotype, " +
                "       feature.symbol, " +
                "       feature as feature, " +
                "       gene as gene, " +
                "       geneSpecies as geneSpecies, " +
                "       featureSpecies as featureSpecies, " +
                "       collect(publications.pubMedId), " +
                "       collect(publications) as publications, " +
                "       count(publications),         " +
                "       collect(publications.pubModId), " +
                "       featureCrossRef as pimaryReference " +
                " ORDER BY LOWER(phenotype.phenotypeStatement), LOWER(feature.symbol)";
        cypher += " SKIP " + pagination.getStart() + " LIMIT " + pagination.getLimit();

        return queryForResult(cypher, bindingValueMap);
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

    public Long getTotalPhenotypeCount(String geneID, Pagination pagination) {

        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("geneID", geneID);

        String baseCypher = "MATCH p0=(phenotype:Phenotype)--(phenotypeEntityJoin:PhenotypeEntityJoin)-[:EVIDENCE]-(publications:Publication), " +
                "        p2=(phenotypeEntityJoin)--(gene:Gene) " +
                "where gene.primaryKey = {geneID} ";
        // get feature-less phenotypes
        String phenotypeFilterClause = addAndWhereClauseString("phenotype.phenotypeStatement", FieldFilter.PHENOTYPE, pagination.getFieldFilterValueMap());
        if (phenotypeFilterClause != null) {
            baseCypher += phenotypeFilterClause;
            bindingValueMap.put("phenotype", pagination.getFieldFilterValueMap().get(FieldFilter.PHENOTYPE));
        }

        // add reference filter clause
        String referenceFilterClause = addAndWhereClauseORString("publications.pubModId", "publications.pubMedId", FieldFilter.FREFERENCE, pagination.getFieldFilterValueMap());
        if (referenceFilterClause != null) {
            baseCypher += referenceFilterClause;
        }

        String cypher = baseCypher + "AND NOT (phenotypeEntityJoin)--(:Feature) " +
                "return count(distinct phenotype.phenotypeStatement) as " + TOTAL_COUNT;

        Long featureLessPhenotype = 0L;

        String geneticEntityFilterClause = addWhereClauseString("feature.symbol", FieldFilter.GENETIC_ENTITY, pagination.getFieldFilterValueMap(), "WHERE");
        if (geneticEntityFilterClause == null)
            featureLessPhenotype = (Long) queryForResult(cypher, bindingValueMap).iterator().next().get(TOTAL_COUNT);

        // feature-related phenotypes
        cypher = baseCypher;

        cypher += "WITH distinct phenotype, phenotypeEntityJoin ";
        cypher += "MATCH (phenotypeEntityJoin)--(feature:Feature) ";
        if (geneticEntityFilterClause != null) {
            cypher += geneticEntityFilterClause;
            bindingValueMap.put("feature", pagination.getFieldFilterValueMap().get(FieldFilter.GENETIC_ENTITY));
        }
        cypher += "return count(distinct phenotype.phenotypeStatement+feature.symbol) as " + TOTAL_COUNT;

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

    private String getPhenotypeBaseQuery() {
        return "MATCH p0=(phenotype:Phenotype)--(phenotypeEntityJoin:PhenotypeEntityJoin)-[:EVIDENCE]-(publications:Publication), " +
                "p2=(phenotypeEntityJoin)--(gene:Gene)-[:FROM_SPECIES]-(species:Species) " +
                "where gene.primaryKey = {geneID} " +
                "OPTIONAL MATCH p4=(phenotypeEntityJoin)--(feature:Feature) ";
    }


    public long getDistinctPhenotypeCount(String geneID) {
        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("geneID", geneID);

        String cypher = getPhenotypeBaseQuery() + "return count(distinct phenotype.phenotypeStatement) as " + TOTAL_COUNT;
        return (Long) queryForResult(cypher, bindingValueMap).iterator().next().get(TOTAL_COUNT);
    }

    public List<PhenotypeEntityJoin> getAllPhenotypeAnnotations() {
        String cypher = "MATCH p0=(phenotype:Phenotype)<-[:ASSOCIATION]-(pej:PhenotypeEntityJoin)-[:EVIDENCE]->(ppj:PublicationJoin)<-[:ASSOCIATION]-(publication:Publication), " +
                " p2=(pej:PhenotypeEntityJoin)<-[:ASSOCIATION]-(gene:Gene)-[:FROM_SPECIES]->(species:Species) " +
                //"where gene.primaryKey = 'ZFIN:ZDB-GENE-990415-8' " +
                //"where gene.primaryKey = 'WB:WBGene00000898' AND phenotype.primaryKey = 'fat content increased' " +
                "OPTIONAL MATCH     p4=(pej:PhenotypeEntityJoin)--(feature:Feature)-[:CROSS_REFERENCE]->(crossRef:CrossReference) " +
                "OPTIONAL MATCH models=(ppj:PublicationJoin)-[:PRIMARY_GENETIC_ENTITY]->(agm:AffectedGenomicModel)--(agmPej:PhenotypeEntityJoin)--(phenotype:Phenotype) " +
                "OPTIONAL MATCH p6c=(agmPej:PhenotypeEntityJoin)--(:ExperimentalCondition)-[:ASSOCIATION]->(:ZECOTerm) " +
                "OPTIONAL MATCH alleles=(ppj:PublicationJoin)-[:PRIMARY_GENETIC_ENTITY]->(featureCond:Allele)-[:FROM_SPECIES]->(species:Species), " +
                " p6a=(featureCond:Allele)--(allelePej:PhenotypeEntityJoin)--(phenotype:Phenotype)  " +
                "OPTIONAL MATCH p6b=(allelePej:PhenotypeEntityJoin)--(:ExperimentalCondition)-[:ASSOCIATION]->(:ZECOTerm) " +
                "return p0, p4, p2, models, alleles, p6a, p6b, p6c ";

        Iterable<PhenotypeEntityJoin> joins = query(PhenotypeEntityJoin.class, cypher);
        List<PhenotypeEntityJoin> joinList = StreamSupport.stream(joins.spliterator(), false)
                .filter(phenotypeEntityJoin -> phenotypeEntityJoin.getGene() != null)
                .collect(Collectors.toList());
        // remove allelePej nodes that are not hanging off phenotype
        // the above OPTIONAL MATCH clause, p6b is not working
        joinList.forEach(phenotypeEntityJoin -> {
            phenotypeEntityJoin.getPhenotypePublicationJoins().forEach(publicationJoin -> {
                if (publicationJoin.getAlleles() != null)
                    publicationJoin.getAlleles().forEach(allele -> {
                        allele.setPhenotypeEntityJoins(allele.getPhenotypeEntityJoins().stream()
                                .filter(phenotypeEntityJoin1 -> phenotypeEntityJoin1.getPhenotype().getPrimaryKey().equals(phenotypeEntityJoin.getPhenotype().getPrimaryKey()))
                                .collect(Collectors.toList()));
                    });
            });
        });
        return joinList;
    }

    public List<PhenotypeEntityJoin> getAllPhenotypeAnnotationsPureAGM() {
        String cypher = "MATCH p0=(phenotype:Phenotype)--(pej:PhenotypeEntityJoin)-[:EVIDENCE]->(ppj:PublicationJoin)<-[:ASSOCIATION]-(publication:Publication), " +
                " p2=(pej:PhenotypeEntityJoin)--(agm:AffectedGenomicModel)--(:Allele)--(:Gene) " +
                //"where agm.primaryKey in ['MGI:6272038','MGI:5702925'] " +
                "where agm.primaryKey in ['ZFIN:ZDB-FISH-180831-2'] " +
                "OPTIONAL MATCH     p5=(pej:PhenotypeEntityJoin)--(:AffectedGenomicModel)-[:CROSS_REFERENCE]->(crossRef:CrossReference) " +
                "OPTIONAL MATCH modelAllele=(agm:AffectedGenomicModel)--(n)--(:Gene) where n:Allele OR n:SequenceTargetingReagent " +
                "OPTIONAL MATCH condition=(pej:PhenotypeEntityJoin)--(:ExperimentalCondition)-[:ASSOCIATION]->(zeco:ZECOTerm)" +
                "return p0,p2, p5, modelAllele, condition ";

        Iterable<PhenotypeEntityJoin> joins = query(PhenotypeEntityJoin.class, cypher);
        return StreamSupport.stream(joins.spliterator(), false).
                collect(Collectors.toList());
    }

    public List<PhenotypeEntityJoin> getAllelePhenotypeAnnotations() {
        String cypher = "MATCH p0=(phenotype:Phenotype)--(pej:PhenotypeEntityJoin)-[:EVIDENCE]->(ppj:PublicationJoin)<-[:ASSOCIATION]-(publication:Publication), " +
                " p2=(pej:PhenotypeEntityJoin)--(allele:Feature) " +
                "where allele.primaryKey in ['ZFIN:ZDB-ALT-980203-1829'] " +
                "and  phenotype.primaryKey = 'melanophore stripe broken, abnormal' " +
                "OPTIONAL MATCH gene=(allele:Feature)--(:Gene)" +
                "OPTIONAL MATCH p4=(pej:PhenotypeEntityJoin)--(allele:Feature)-[:CROSS_REFERENCE]->(crossRef:CrossReference) " +
//                "OPTIONAL MATCH modelAllele=(ppj:PublicationJoin)--(agm:AffectedGenomicModel)-[:ASSOCIATION]->(agmPej:PhenotypeEntityJoin) " +
                "OPTIONAL MATCH modelAllele=(ppj:PublicationJoin)-[:PRIMARY_GENETIC_ENTITY]->(agm:AffectedGenomicModel)-[:ASSOCIATION]->(agmPej:PhenotypeEntityJoin)--(phenotype:Phenotype) " +
                "OPTIONAL MATCH p6=(agmPej:PhenotypeEntityJoin)--(expCond:ExperimentalCondition)-[:ASSOCIATION]->(zeco:ZECOTerm)" +
                //"return p0, p2, p4, agm, expCond, zeco";
                "return p0, p2, p4, modelAllele, p6";

        Iterable<PhenotypeEntityJoin> joins = query(PhenotypeEntityJoin.class, cypher);
        List<PhenotypeEntityJoin> joinList = StreamSupport.stream(joins.spliterator(), false)
                .filter(phenotypeEntityJoin -> phenotypeEntityJoin.getAllele() != null)
                .collect(Collectors.toList());
        // remove allelePej nodes that are not hanging off the given phenotype of the allele
        // this does not work as the Model objects are shared among different annotations and
        // removing PEJs from one will automatically remove them from others
/*
        joinList.forEach(phenotypeEntityJoin -> {
            if (phenotypeEntityJoin.getPhenotypePublicationJoins() != null)
                phenotypeEntityJoin.getPhenotypePublicationJoins().forEach(publicationJoin -> {
                    if (publicationJoin.getModels() != null)
                        publicationJoin.getModels().forEach(model -> {
                            model.setPhenotypeEntityJoins(model.getPhenotypeEntityJoins().stream()
                                    .filter(phenotypeEntityJoin1 -> phenotypeEntityJoin1.getPhenotype().getPrimaryKey().equals(phenotypeEntityJoin.getPhenotype().getPrimaryKey()))
                                    .collect(Collectors.toList()));
                        });
                });
        });
*/
        return joinList;
    }
}
