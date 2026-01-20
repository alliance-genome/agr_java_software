package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.PublicView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NodeEntity
@Schema(name = "CrossReference", description = "POJO that represents CrossReferences")
public class CrossReference extends Neo4jEntity {

	@JsonView({ PublicView.API.class, PublicView.Interaction.class, PublicView.Expression.class, CurationView.VariantIndexerView.class }) private String crossRefCompleteUrl;

	@JsonView({ PublicView.Interaction.class }) private String localId;

	@JsonView({ PublicView.Interaction.class }) private String globalCrossRefId;

	@JsonView({ PublicView.Interaction.class }) private String prefix;

	@JsonView({ PublicView.API.class, PublicView.Interaction.class, CurationView.VariantIndexerView.class }) private String name;

	@JsonView({ PublicView.API.class, PublicView.Interaction.class, CurationView.VariantIndexerView.class }) private String displayName;

	@JsonView({ PublicView.Interaction.class }) private String primaryKey;

	@JsonView({ PublicView.Interaction.class }) private String crossRefType;

	private Boolean loadedDB;
	private Boolean curatedDB;

	@Override
	public String toString() {
		return localId + ":" + displayName;
	}
}
