package org.alliancegenome.core.es.schema;

import java.io.IOException;

public class VariantMapping extends Mapping {

	public VariantMapping(Boolean pretty) {
		super(pretty);
	}

	@Override
	public void buildMapping() {
		try {
			builder.startObject();
			builder.startObject("properties");
			new FieldBuilder(builder, "category", "keyword").symbol().autocomplete().keyword().build();
			new FieldBuilder(builder, "name", "keyword").build();
			new FieldBuilder(builder, "nameKey", "keyword").build();
			new FieldBuilder(builder, "alterationType", "text").keyword().build();
			new FieldBuilder(builder, "genes", "text").keyword().build();
			new FieldBuilder(builder, "geneIds", "keyword").build();
			new FieldBuilder(builder, "associatedPhenotype", "text").keyword().sortLowercase().build();
			new FieldBuilder(builder, "diseaseTerms.name", "text").keyword().sortLowercase().build();
			new FieldBuilder(builder, "hasDisease", "boolean").build();
			new FieldBuilder(builder, "hasPhenotype", "boolean").build();
			new FieldBuilder(builder, "alterationTypeSortOrder", "integer").sortLowercase().build();
			new FieldBuilder(builder, "symbol", "text").analyzer("symbols").autocomplete().htmlSmoosh().keyword().keywordAutocomplete().sortSmartAlpha().build(); // allele, gene

			// allele: dynamic false prevents indexing the deep curation API Allele tree
			// Only map fields actually queried in ES
			builder.startObject("allele");
			builder.field("dynamic", false);
			builder.startObject("properties");
			// VariantSummaryDocument: queried via MatchQuery on allele.primaryExternalId
			new FieldBuilder(builder, "primaryExternalId", "text").keyword().build();
			new FieldBuilder(builder, "alleleSynonyms.displayText", "text").keyword().sortLowercase().build();

			builder.endObject();
			builder.endObject();

			builder.startObject("variantList");
			builder.field("dynamic", false);
			builder.startObject("properties");
			new FieldBuilder(builder, "variantType.name", "text").keyword().sortLowercase().build();
			new FieldBuilder(builder, "curatedVariantGenomicLocations.predictedVariantConsequences.vepConsequences.name", "text").keyword().sortLowercase().build();
			new FieldBuilder(builder, "curatedVariantGenomicLocations.hgvs", "text").keyword().sortSmartAlpha().build();

			builder.endObject();
			builder.endObject();

			// variant: dynamic false prevents indexing the deep Variant entity tree
			builder.startObject("variant");
			builder.field("dynamic", false);
			builder.startObject("properties");
			new FieldBuilder(builder, "variantType.name", "text").keyword().sortLowercase().build();
			new FieldBuilder(builder, "curatedVariantGenomicLocations.hgvs", "text").keyword().sortSmartAlpha().build();
			new FieldBuilder(builder, "curatedVariantGenomicLocations.start", "integer").build();
			new FieldBuilder(builder, "curatedVariantGenomicLocations.end", "integer").build();
			new FieldBuilder(builder, "curatedVariantGenomicLocations.variantGenomicLocationAssociationObject.name", "text").keyword().build();
			builder.endObject();
			builder.endObject();

			// consequence: dynamic false, only map fields queried by AlleleVariantIndexService
			builder.startObject("consequence");
			builder.field("dynamic", false);
			builder.startObject("properties");
			new FieldBuilder(builder, "variantTranscript.name", "text").keyword().sortSmartAlpha().build();
			new FieldBuilder(builder, "variantTranscript.transcriptType.name", "text").keyword().sortLowercase().build();
			new FieldBuilder(builder, "variantTranscript.transcriptGeneAssociations.transcriptGeneAssociationObject.geneSymbol.displayText", "text").keyword().sortSmartAlpha().build();
			new FieldBuilder(builder, "intronExonLocation", "text").keyword().build();
			new FieldBuilder(builder, "vepConsequences.name", "text").keyword().sortLowercase().build();
			new FieldBuilder(builder, "vepImpact.name", "text").keyword().sortLowercase().build();
			new FieldBuilder(builder, "siftPrediction.name", "text").keyword().sortLowercase().build();
			new FieldBuilder(builder, "siftScore", "float").build();
			new FieldBuilder(builder, "polyphenPrediction.name", "text").keyword().sortLowercase().build();
			new FieldBuilder(builder, "polyphenScore", "float").build();
			builder.endObject();
			builder.endObject();

			builder.endObject();
			builder.endObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
