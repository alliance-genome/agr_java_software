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
import org.alliancegenome.neo4j.view.View;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({
	"category", "subCategory", "variant", "allele", "primaryKey", "nameKey", "name",
	"variantName", "alterationType", "species", "chromosome", "variantType",
	"molecularConsequence", "genes", "geneIds", "geneSynonyms", "geneCrossReferences"
})
@JsonView(value = {View.VariantAPI.class})
public class VariantSummaryDocument extends ESDocument {

	{
		category = "variant_summary";
	}
	protected String subCategory;

	@JsonView(value = {View.VariantAPI.class})
	private CuratedVariantGenomicLocationAssociation variant;

	@JsonView(value = {View.VariantAPI.class})
	private Allele allele;

	@JsonView(value = {View.VariantAPI.class})
	private String primaryKey;

	@JsonView(value = {View.VariantAPI.class})
	@JsonProperty("name_key")
	private String nameKey;

	@JsonView(value = {View.VariantAPI.class})
	private String name;

	@JsonView(value = {View.VariantAPI.class})
	private String variantName;

	@JsonView(value = {View.VariantAPI.class})
	private String alterationType;

	@JsonView(value = {View.VariantAPI.class})
	private String species;

	@JsonView(value = {View.VariantAPI.class})
	private String chromosome;

	@JsonView(value = {View.VariantAPI.class})
	private Set<String> variantType;

	@JsonView(value = {View.VariantAPI.class})
	private Set<String> molecularConsequence;

	@JsonView(value = {View.VariantAPI.class})
	private Set<String> genes;

	@JsonView(value = {View.VariantAPI.class})
	private Set<String> geneIds;

	@JsonView(value = {View.VariantAPI.class})
	private Set<String> geneSynonyms;

	@JsonView(value = {View.VariantAPI.class})
	private Set<String> geneCrossReferences;

}
