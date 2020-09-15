package org.alliancegenome.neo4j.repository;

import org.alliancegenome.es.index.site.cache.DatasetDocumentCache;
import org.alliancegenome.neo4j.entity.node.HTPDataset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DatasetIndexerRepository extends Neo4jRepository {

    private final Logger log = LogManager.getLogger(getClass());

    public DatasetIndexerRepository() { super(HTPDataset.class); }

    public Map<String, HTPDataset> getDatasetMap() {
        String query = " MATCH p1=(dataset:HTPDataset) " +
                " OPTIONAL MATCH pCrossRef=(dataset)-[:CROSS_REFERENCE]-(crossReference:CrossReference) " +
                " RETURN p1, pCrossRef; ";

        Iterable<HTPDataset> datasets = query(query);

        Map<String,HTPDataset> datasetMap = new HashMap<>();
        for (HTPDataset dataset : datasets) {
            datasetMap.put(dataset.getPrimaryKey(), dataset);
        }

        return datasetMap;
    }

    public DatasetDocumentCache getCache() {
        DatasetDocumentCache cache = new DatasetDocumentCache();

        log.info("Fetching datasets");
        cache.setDatasetMap(getDatasetMap());

        log.info("Fetching tags");
        cache.setTags(getTags());

        log.info("Fetching species");
        cache.setSpecies(getSpecies());

        log.info("Fetching age");
        cache.setAge(getAge());

        log.info("Fetching sex");
        cache.setSex(getSex());

        return cache;
    }

    public Map<String, Set<String>> getTags() {
        return getMapSetForQuery("MATCH (dataset:HTPDataset)-[:CATEGORY_TAG]-(tag:CategoryTag) RETURN dataset.primaryKey as id, tag.primaryKey as value ");
    }

    public Map<String, Set<String>> getSpecies() {
        return getMapSetForQuery("MATCH (dataset:HTPDataset)-[:ASSOCIATION]-(sample:HTPDatasetSample)-[:FROM_SPECIES]-(species:Species) " +
                " RETURN distinct dataset.primaryKey as id, species.name as value;");
    }

    public Map<String, Set<String>> getAge() {
        return getMapSetForQuery("MATCH (dataset:HTPDataset)-[:ASSOCIATION]-(sample:HTPDatasetSample) " +
                " WHERE sample.sampleAge <> \"\" " +
                "RETURN distinct dataset.primaryKey as id, sample.sampleAge as value");
    }

    public Map<String, Set<String>> getSex() {
        return getMapSetForQuery("MATCH (dataset:HTPDataset)-[:ASSOCIATION]-(sample:HTPDatasetSample) " +
                " WHERE sample.sex <> \"\" " +
                "RETURN distinct dataset.primaryKey as id, sample.sex as value");
    }


}
