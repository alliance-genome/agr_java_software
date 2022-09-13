package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;

public abstract class Ontology extends Neo4jEntity {

	public static boolean isGOTerm(String termID) {
		if (termID == null)
			return false;
		return termID.startsWith("GO:");
	}

	public static boolean isAoOrStageTerm(String termID) {
		if (termID == null)
			return false;
		return termID.startsWith("UBERON:");
	}

}
