package org.alliancegenome.indexer.repository;

import org.alliancegenome.indexer.entity.node.Feature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.exception.core.MappingException;

import java.util.HashMap;

public class FeatureRepository extends Neo4jRepository<Feature> {

    public FeatureRepository() {
        super(Feature.class);
    }

    public Feature getFeature(String primaryKey) {

        try {
            Iterable<Feature> features = getEntity("primaryKey", primaryKey);
            if (features.iterator().hasNext())
                return features.iterator().next();
        } catch (MappingException e) {
            e.printStackTrace();
            log.error(e);
        }
        return null;
    }

    private final Logger log = LogManager.getLogger(getClass());
}
