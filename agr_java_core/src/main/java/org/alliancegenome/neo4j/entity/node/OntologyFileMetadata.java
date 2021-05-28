package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import lombok.*;

@Getter
@Setter
@NodeEntity
@Schema(name="OntologyFileMetadata", description="POJO that represents the Ontology File Metadata")
public class OntologyFileMetadata extends Neo4jEntity {

}
