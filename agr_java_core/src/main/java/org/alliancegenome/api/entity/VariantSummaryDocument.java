package org.alliancegenome.api.entity;

import java.util.Set;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.neo4j.view.PublicView;

import com.fasterxml.jackson.annotation.JsonProperty;
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
	private CuratedVariantGenomicLocationAssociation variant;
	private Allele allele;
	private String primaryKey;
	
	@JsonProperty("name_key")
	private String nameKey;
	
	private String name;
	private String variantName;
	private String alterationType;
	private String species;
	private String chromosome;
	private Set<String> variantType;
	private Set<String> molecularConsequence;
	private Set<String> genes;
	private Set<String> geneIds;
	private Set<String> geneSynonyms;
	private Set<String> geneCrossReferences;

}
