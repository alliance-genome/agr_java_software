package org.alliancegenome.neo4j.entity.node;

import org.neo4j.ogm.annotation.NodeEntity;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter @Setter
public class SOTerm extends Ontology {

	private String primaryKey;
	private String name;
}
