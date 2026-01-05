package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.view.View;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({ "category", "variant" })
@JsonView(value = { View.TransgenicAllelesDocument.class })
public class VariantSummaryDocument extends ESDocument {

	{
		category = "variant_summary";
	}

	public CuratedVariantGenomicLocationAssociation variant;
	public Allele allele;

}
