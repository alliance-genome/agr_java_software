package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"category", "variant"})
@JsonView(value = {org.alliancegenome.curation_api.view.View.TransgenicAllelesDocument.class})
public class VariantSummaryDocument extends ESDocument {

	{
		category = "variant_summary";
	}

	public VariantSummaryDocument() {

	}

	public CuratedVariantGenomicLocationAssociation variant;


}
