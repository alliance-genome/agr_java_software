package org.alliancegenome.neo4j.entity.node;

import java.util.Date;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.PublicView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
@Schema(name = "ModFileMetadata", description = "POJO that represents the ModFileMetaData")
public class ModFileMetadata extends Neo4jEntity {

	@JsonView({ PublicView.API.class })
	@Convert(value = DateConverter.class)
	@JsonProperty(value = "releaseDate") private Date dateProduced;
	@JsonView({ PublicView.API.class })
	@JsonProperty(value = "mod") private String dataSubType;
	@JsonView({ PublicView.API.class })
	@JsonProperty(value = "releaseVersion") private String release;
	@JsonView({ PublicView.API.class })
	@JsonProperty(value = "type") private String dataType;

}
