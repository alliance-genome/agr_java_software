package org.alliancegenome.neo4j.entity;

import org.alliancegenome.neo4j.view.PublicView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "EntitySummary", description = "POJO that represents Entity Summary")
public class EntitySummary {

	@JsonView({ PublicView.Default.class }) private long numberOfAnnotations;
	@JsonView({ PublicView.Default.class }) private long numberOfEntities;

}
