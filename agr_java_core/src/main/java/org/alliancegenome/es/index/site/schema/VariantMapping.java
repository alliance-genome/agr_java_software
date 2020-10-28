package org.alliancegenome.es.index.site.schema;

import java.io.IOException;

public class VariantMapping extends Mapping {

    public VariantMapping(Boolean pretty) {
        super(pretty);
    }

    @Override
    public void buildMapping() {
        try {
            builder.startObject().startObject("properties");
            new FieldBuilder(builder, "category", "keyword").symbol().autocomplete().keyword().build();
            new FieldBuilder(builder, "name", "keyword").build();
            new FieldBuilder(builder, "name_key", "keyword").build();
            new FieldBuilder(builder, "alterationType", "text").keyword().build();
            new FieldBuilder(builder, "genes", "text").keyword().build();
            new FieldBuilder(builder, "samples", "keyword").build();
            buildVcfFields();
            builder.endObject().endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void buildVcfFields() throws IOException {
        new FieldBuilder(builder, "chromosome", "keyword").build();
        new FieldBuilder(builder, "startPos", "integer").build();
        new FieldBuilder(builder, "endPos", "integer").build();
        new FieldBuilder(builder, "refNuc", "keyword").notIndexed().build();
        new FieldBuilder(builder, "varNuc", "keyword").notIndexed().build();
        new FieldBuilder(builder, "qual", "keyword").notIndexed().build();
        new FieldBuilder(builder, "documentVariantType", "keyword").notIndexed().build();
        new FieldBuilder(builder, "filter", "keyword").notIndexed().build();
        new FieldBuilder(builder, "refPep", "keyword").notIndexed().build();
        new FieldBuilder(builder, "aa", "keyword").notIndexed().build(); //ancestral allele
        new FieldBuilder(builder, "MA", "keyword").notIndexed().build(); //minor allele
        new FieldBuilder(builder, "MAF", "double").notIndexed().build(); //minor allele frequency
        new FieldBuilder(builder, "MAC", "integer").notIndexed().build();
        new FieldBuilder(builder, "evidence", "keyword").notIndexed().build();
        new FieldBuilder(builder, "clinicalSignificance", "keyword").notIndexed().build();
        buildVariantConsequences();
    }

    protected void buildVariantConsequences() throws IOException {
        builder.startObject("consequences");
        builder.startObject("properties");
        new FieldBuilder(builder, "feature", "keyword").build();
        new FieldBuilder(builder, "consequence", "keyword").build();
        new FieldBuilder(builder, "featureType", "keyword").build();
        new FieldBuilder(builder, "allele", "keyword").build();
        new FieldBuilder(builder, "aminoAcids", "keyword").notIndexed().build();
        new FieldBuilder(builder, "sift", "keyword").notIndexed().build();
        new FieldBuilder(builder, "polyphen", "keyword").notIndexed().build();
        new FieldBuilder(builder, "varPep", "keyword").build();
        new FieldBuilder(builder, "impact", "keyword").build();
        new FieldBuilder(builder, "symbol", "keyword").notIndexed().build();
        new FieldBuilder(builder, "gene", "keyword").build();
        new FieldBuilder(builder, "biotype", "keyword").notIndexed().build();
        new FieldBuilder(builder, "exon", "keyword").build();
        new FieldBuilder(builder, "intron", "keyword").build();
        new FieldBuilder(builder, "HGVSg", "keyword").build();
        new FieldBuilder(builder, "HGVSp", "keyword").build();
        new FieldBuilder(builder, "HGVSc", "keyword").build();
        new FieldBuilder(builder, "cDNAPosition", "keyword").notIndexed().build();
        new FieldBuilder(builder, "CDSPosition", "keyword").notIndexed().build();
        new FieldBuilder(builder, "proteinPosition", "keyword").notIndexed().build();
        new FieldBuilder(builder, "codon", "keyword").notIndexed().build();
        new FieldBuilder(builder, "existingVariation", "keyword").notIndexed().build();
        new FieldBuilder(builder, "distance", "keyword").notIndexed().build();
        new FieldBuilder(builder, "strand", "keyword").notIndexed().build();
        new FieldBuilder(builder, "flags", "keyword").notIndexed().build();
        new FieldBuilder(builder, "symbolSource", "keyword").notIndexed().build();
        new FieldBuilder(builder, "HGNCId", "keyword").build();
        new FieldBuilder(builder, "source", "keyword").notIndexed().build();
        new FieldBuilder(builder, "HGVSOffset", "keyword").notIndexed().build();
        new FieldBuilder(builder, "RefSeqMatch", "keyword").notIndexed().build();
        new FieldBuilder(builder, "refSeqOffset", "keyword").notIndexed().build();
        new FieldBuilder(builder, "givenRef", "keyword").notIndexed().build();
        new FieldBuilder(builder, "usedRef", "keyword").notIndexed().build();
        new FieldBuilder(builder, "BAMEdit", "keyword").notIndexed().build();
        buildVariantEffects();
        builder.endObject();
        builder.endObject();
    }

    protected void buildVariantEffects() throws IOException {
        builder.startObject("variantEffects");
        builder.startObject("properties");
        new FieldBuilder(builder,"consequence", "keyword").build();
        new FieldBuilder(builder,"featureType", "keyword").build();
        builder.endObject();
        builder.endObject();
    }

}
