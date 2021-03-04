package org.alliancegenome.neo4j.repository.indexer;

import java.util.*;
import java.util.concurrent.*;

import org.alliancegenome.es.index.site.cache.DatasetDocumentCache;
import org.alliancegenome.neo4j.entity.node.HTPDataset;
import org.alliancegenome.neo4j.repository.Neo4jRepository;
import org.apache.logging.log4j.*;

public class DatasetIndexerRepository extends Neo4jRepository<HTPDataset> {

    private final Logger log = LogManager.getLogger(getClass());

    private DatasetDocumentCache cache = new DatasetDocumentCache();
    
    public DatasetIndexerRepository() { super(HTPDataset.class); }
    
    public DatasetDocumentCache getCache() {

        log.info("Building DatasetDocumentCache");

        ExecutorService executor = Executors.newFixedThreadPool(20); // Run all at once
        
        executor.execute(new GetDatasetMapThread());
        executor.execute(new GetAssaysThread());
        executor.execute(new GetCrossReferencesThread());
        executor.execute(new GetTagsThread());
        executor.execute(new GetSpeciesThread());
        executor.execute(new GetSampleIdsThread());
        executor.execute(new GetSexThread());
        executor.execute(new GetSampleStructureThread());
        executor.execute(new GetSampleStructureRibbonTermsThread());
        executor.execute(new GetSampleStructureParentTerms());
        executor.execute(new GetStageThread());

        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("Finished Building DatasetDocumentCache");

        return cache;

    }
    
    private class GetDatasetMapThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching datasets");
            String query = " MATCH p1=(dataset:HTPDataset) " +
                    " RETURN p1; ";
    
            Iterable<HTPDataset> datasets = query(query);
    
            Map<String,HTPDataset> datasetMap = new HashMap<>();
            for (HTPDataset dataset : datasets) {
                datasetMap.put(dataset.getPrimaryKey(), dataset);
            }
    
            cache.setDatasetMap(datasetMap);
            log.info("Finished Fetching datasets");
        }
    }

    private class GetAssaysThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching assays");
            cache.setAssays(getMapSetForQuery("MATCH (dataset:HTPDataset)-[:ASSOCIATION]-(sample:HTPDatasetSample)-[:ASSAY_TYPE]-(assay:MMOTerm) \n" +
                    "RETURN distinct dataset.primaryKey as id, assay.displaySynonym as value"));
            log.info("Finished fetching assays");
        }
    }

    private class GetCrossReferencesThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching cross references");
            cache.setCrossReferences(getMapSetForQuery("MATCH (dataset:HTPDataset)-[:CROSS_REFERENCE]-(cr:CrossReference) " +
                    " WHERE cr.preferred = 'false' " +
                    " RETURN dataset.primaryKey as id, cr.name as value"));
            log.info("Finished fetching cross references");
        }
    }

    private class GetTagsThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching tags");
            cache.setTags(getMapSetForQuery("MATCH (dataset:HTPDataset)-[:CATEGORY_TAG]-(tag:CategoryTag) RETURN dataset.primaryKey as id, tag.primaryKey as value "));
            log.info("Finished Fetching tags");
        }
    }

    private class GetSpeciesThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching species");
            cache.setSpecies(getMapSetForQuery("MATCH (dataset:HTPDataset)-[:ASSOCIATION]-(sample:HTPDatasetSample)-[:FROM_SPECIES]-(species:Species) " +
                    " RETURN distinct dataset.primaryKey as id, species.name as value;"));
            log.info("Finished Fetching species");
        }
    }

    private class GetSampleIdsThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching sample ids");
            cache.setSampleIds(getMapSetForQuery("MATCH (dataset:HTPDataset)-[:ASSOCIATION]-(sample:HTPDatasetSample) " +
                    " RETURN distinct dataset.primaryKey as id, sample.sampleId as value;"));
            log.info("Finished Fetching sample ids");
        }
    }

    private class GetSexThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching sex");
            cache.setSex(getMapSetForQuery("MATCH (dataset:HTPDataset)-[:ASSOCIATION]-(sample:HTPDatasetSample) " +
                    " WHERE sample.sex <> \"\" " +
                    " RETURN distinct dataset.primaryKey as id, sample.sex as value"));
            log.info("Finished Fetching sex");
        }
    }

    private class GetSampleStructureThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching whereExpressed statement");
            cache.setWhereExpressed(getMapSetForQuery("MATCH (dataset:HTPDataset)-[:ASSOCIATION]-(:HTPDatasetSample)-[:STRUCTURE_SAMPLED]-(ebe:ExpressionBioEntity) " +
                    " RETURN distinct dataset.primaryKey as id, ebe.whereExpressedStatement as value"));
            log.info("Finished Fetching whereExpressed statement");
        }
    }

    private class GetSampleStructureRibbonTermsThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching anatomical expression ribbon terms");
            cache.setAnatomicalExpression(getMapSetForQuery("MATCH (dataset:HTPDataset)-[:ASSOCIATION]-(:HTPDatasetSample)-[:STRUCTURE_SAMPLED]-(ebe:ExpressionBioEntity)-[:ANATOMICAL_RIBBON_TERM]-(term:Ontology) " +
                    " RETURN dataset.primaryKey as id, term.name as value"));
            log.info("Finished Fetching anatomical expression ribbon terms");
        }
        
    }

    private class GetSampleStructureParentTerms implements Runnable {
        @Override
        public void run() {
            log.info("Fetching anatomical expression parent terms");
            cache.setAnatomicalExpressionWithParents(getMapSetForQuery("MATCH (dataset:HTPDataset)-[:ASSOCIATION]-(:HTPDatasetSample)-[:STRUCTURE_SAMPLED]-(ebe:ExpressionBioEntity)-[:ANATOMICAL_STRUCTURE]-(:Ontology)-[:IS_A_PART_OF_CLOSURE|IS_A_PART_OF_SELF_CLOSURE]->(term:Ontology) " +
                    " RETURN dataset.primaryKey as id, term.name as value"));
            log.info("Finished Fetching anatomical expression parent terms");
        }
    }

    private class GetStageThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching stage");
            cache.setAnatomicalExpression(getMapSetForQuery("MATCH (dataset:HTPDataset)-[:ASSOCIATION]-(sample:HTPDatasetSample)-[:STRUCTURE_SAMPLED]-(:ExpressionBioEntity)-[:ASSOCIATION]-(:BioEntityGeneExpressionJoin)-[:STAGE_RIBBON_TERM]-(term:UBERONTerm) " +
                    " RETURN dataset.primaryKey as id, term.name as value"));
            log.info("Finished Fetching stage");
        }

    }

}
