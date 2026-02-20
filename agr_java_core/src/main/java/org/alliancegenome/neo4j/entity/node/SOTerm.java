package org.alliancegenome.neo4j.entity.node;


import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.neo4j.view.PublicView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
@Schema(name = "SOTerm", description = "POJO that represents the SO Term")
public class SOTerm extends Ontology {

	public static final String INSERTION = "SO:0000667";
	public static final String DELETION = "SO:0000159";

	@JsonView({ PublicView.Default.class, CurationView.VariantSummaryDocument.class })
	@JsonProperty(value = "id") private String primaryKey;
	@JsonView({ PublicView.Default.class, CurationView.VariantSummaryDocument.class }) private String name;

	public boolean isInsertion() {
		return primaryKey.equals(INSERTION);
	}

	public boolean isDeletion() {
		return primaryKey.equals(DELETION);
	}
}
