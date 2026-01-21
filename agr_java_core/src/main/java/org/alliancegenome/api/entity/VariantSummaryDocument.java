package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.neo4j.view.PublicView;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({
	"category", "subCategory", "variant", "allele", "primaryKey", "nameKey", "name",
	"variantName", "alterationType", "species", "chromosome", "variantType",
	"molecularConsequence", "genes", "geneIds", "geneSynonyms", "geneCrossReferences"
})
@JsonView(value = {PublicView.VariantAPI.class, CurationView.VariantIndexerView.class})
public class VariantSummaryDocument extends ESDocument {

	{
		category = "variant_summary";
	}
	protected String subCategory;

	private Allele allele;
	private CuratedVariantGenomicLocationAssociation variant;

}
