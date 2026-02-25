package org.alliancegenome.es.index.site.schema;

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
			new FieldBuilder(builder, "name_key", "keyword").build();
			new FieldBuilder(builder, "alterationType", "text").keyword().build();
			new FieldBuilder(builder, "genes", "text").keyword().build();
			new FieldBuilder(builder, "geneIds", "keyword").build();
			new FieldBuilder(builder, "associatedPhenotype", "text").keyword().sort().build();
			new FieldBuilder(builder, "diseaseTerms.name", "text").keyword().sort().build();
			new FieldBuilder(builder, "sequenceSummaryCategory", "keyword").symbol().autocomplete().keyword().build(); // sequence_summary
			
			// allele: dynamic false prevents indexing the deep curation API Allele tree
			// Only map fields actually queried in ES
			builder.startObject("allele");
			builder.field("dynamic", false);
			builder.startObject("properties");
			// VariantSummaryDocument: queried via MatchQuery on allele.primaryExternalId
			new FieldBuilder(builder, "primaryExternalId", "text").keyword().build();
			builder.endObject();
			builder.endObject();

			// variant: dynamic false prevents indexing the deep variant association tree
			// Only map fields actually queried in ES
			builder.startObject("variant");
			builder.field("dynamic", false);
			builder.startObject("properties");
			// VariantSummaryDocument: queried via TermQuery on variant.hgvs.keyword
			new FieldBuilder(builder, "hgvs", "text").keyword().build();
			// AlleleVariantSequence: queried via TermQuery on variant.gene.id.keyword
			builder.startObject("gene");
			builder.startObject("properties");
			new FieldBuilder(builder, "id", "text").keyword().build();
			builder.endObject();
			builder.endObject();
			// AlleleVariantSequence: aggregation on variant.variantType.name.keyword
			builder.startObject("variantType");
			builder.startObject("properties");
			new FieldBuilder(builder, "name", "text").keyword().build();
			builder.endObject();
			builder.endObject();
			builder.endObject();
			builder.endObject();

			// consequence: dynamic false for now, add explicit field mappings as UI sorting/filtering is implemented
			builder.startObject("consequence");
			builder.field("dynamic", false);
			builder.startObject("properties");
			builder.endObject();
			builder.endObject();

			builder.endObject();
			builder.endObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
