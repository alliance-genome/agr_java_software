package org.alliancegenome.api.entity;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.neo4j.view.PublicView;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({
	"category", "subCategory", "variant", "allele", "primaryKey", "nameKey", "name",
	"variantName", "alterationType", "species", "chromosome", "variantType",
	"molecularConsequence", "genes", "geneIds", "geneSynonyms", "geneCrossReferences"
})
@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
public class VariantSummaryDocument extends ESDocument {

	{
		category = "variant_summary";
	}

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	@Override
	public String getCategory() {
		return category;
	}

	protected String subCategory;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private CuratedVariantGenomicLocationAssociation variant;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private Allele allele;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private String primaryKey;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	@JsonProperty("name_key")
	private String nameKey;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private String name;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private String variantName;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private String alterationType;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private String species;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private String chromosome;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private Set<String> variantType;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private Set<String> molecularConsequence;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private Set<String> genes;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private Set<String> geneIds;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private Set<String> geneSynonyms;

	@JsonView(value = {PublicView.VariantAPI.class, PublicView.AlleleVariantSequenceConverterForES.class})
	private Set<String> geneCrossReferences;

}
