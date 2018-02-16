package org.alliancegenome.shared.neo4j.repository;

import org.alliancegenome.shared.neo4j.entity.node.Feature;

public class FeatureRepository extends Neo4jRepository<Feature> {

	public FeatureRepository() {
		super(Feature.class);
	}

	public Feature getFeature(String primaryKey) {
		
		Iterable<Feature> features = getEntity("primaryKey", primaryKey);
		if (features.iterator().hasNext()) {
			return features.iterator().next();
		}

		return null;
	}

}
