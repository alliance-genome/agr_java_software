package org.alliancegenome.core.es.schema;

import java.io.IOException;

// Only the fields AlleleVariantIndexService filters, sorts and aggregates on (gene page allele details table).
// Everything else stays in _source but is not indexed; notIndexed parents keep only the sub-fields that are queried.
public class SequenceSummaryMapping extends Mapping {

	public SequenceSummaryMapping(Boolean pretty) {
		super(pretty);
	}

	@Override
	public void buildMapping() {
		try {
			builder.startObject();
			builder.field("dynamic", false);
			builder.startObject("properties");
			new FieldBuilder(builder, "category", "text").notIndexed().keyword().build();
			new FieldBuilder(builder, "geneIds", "keyword").build();
			new FieldBuilder(builder, "symbol", "text").analyzer("symbols").sortSmartAlpha().ignoreAbove(8191).build();
			new FieldBuilder(builder, "alterationType", "text").notIndexed().keyword().build();
			new FieldBuilder(builder, "alterationTypeSortOrder", "integer").build();
			new FieldBuilder(builder, "hasDisease", "boolean").build();
			new FieldBuilder(builder, "hasPhenotype", "boolean").build();

			builder.startObject("allele");
			builder.startObject("properties");
			new FieldBuilder(builder, "alleleSynonyms.displayText", "text").build();
			builder.endObject();
			builder.endObject();

			builder.startObject("variant");
			builder.startObject("properties");
			new FieldBuilder(builder, "variantType.name", "text").notIndexed().keyword().sortLowercase().build();
			new FieldBuilder(builder, "curatedVariantGenomicLocations.hgvs", "text").sortSmartAlpha().ignoreAbove(8191).build();
			builder.endObject();
			builder.endObject();

			builder.startObject("consequence");
			builder.startObject("properties");
			new FieldBuilder(builder, "variantTranscript.name", "text").sortSmartAlpha().build();
			new FieldBuilder(builder, "variantTranscript.transcriptType.name", "text").notIndexed().keyword().build();
			new FieldBuilder(builder, "variantTranscript.transcriptGeneAssociations.transcriptGeneAssociationObject.geneSymbol.displayText", "text").notIndexed().keyword().build();
			new FieldBuilder(builder, "intronExonLocation", "text").build();
			new FieldBuilder(builder, "vepConsequences.name", "text").notIndexed().keyword().sortLowercase().build();
			new FieldBuilder(builder, "vepImpact.name", "text").notIndexed().keyword().build();
			new FieldBuilder(builder, "siftPrediction.name", "text").notIndexed().keyword().build();
			new FieldBuilder(builder, "polyphenPrediction.name", "text").notIndexed().keyword().build();
			builder.endObject();
			builder.endObject();

			builder.endObject();
			builder.endObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
