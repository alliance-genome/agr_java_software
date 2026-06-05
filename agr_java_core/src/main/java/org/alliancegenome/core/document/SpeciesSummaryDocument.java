package org.alliancegenome.core.document;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.core.view.PublicView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonView({PublicView.API.class})
public class SpeciesSummaryDocument extends ESDocument {
	{
		category = "species_summary";
	}

	private String name;
	private String displayName;
	private String taxonID;
	private String taxonIDPart;
	private String abbreviation;
	private Integer phylogeneticOrder;
}
