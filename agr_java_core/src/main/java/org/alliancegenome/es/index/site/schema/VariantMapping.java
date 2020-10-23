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
            new Mapping.FieldBuilder(builder, "category", "keyword").symbol().autocomplete().keyword().build();
            new Mapping.FieldBuilder(builder, "name", "keyword").build();
            new Mapping.FieldBuilder(builder, "name_key", "keyword").build();
            new Mapping.FieldBuilder(builder, "alterationType", "keyword").build();
            new Mapping.FieldBuilder(builder, "genes", "keyword").build();
            new Mapping.FieldBuilder(builder, "samples", "keyword").build();
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
        new FieldBuilder(builder, "refNuc", "keyword").build();
        new FieldBuilder(builder, "varNuc", "keyword").build();
        new FieldBuilder(builder, "qual", "keyword").build();
        new FieldBuilder(builder, "documentVariantType", "keyword").build();
        new FieldBuilder(builder, "filter", "keyword").build();
        new FieldBuilder(builder, "refPep", "keyword").build();
        new FieldBuilder(builder, "aa", "keyword").build(); //ancestral allele
        new FieldBuilder(builder, "MA", "keyword").build(); //minor allele
        new FieldBuilder(builder, "MAF", "double").build(); //minor allele frequency
        new FieldBuilder(builder, "MAC", "integer").build();
        new FieldBuilder(builder, "evidence", "keyword").build();
        new FieldBuilder(builder, "clinicalSignificance", "keyword").build();
        buildVariantConsequences();
    }

    protected void buildVariantConsequences() throws IOException {
        builder.startObject("consequences");
        builder.startObject("properties");
        new FieldBuilder(builder, "feature", "keyword").build();
        new FieldBuilder(builder, "consequence", "keyword").build();
        new FieldBuilder(builder, "featureType", "keyword").build();
        new FieldBuilder(builder, "allele", "keyword").build();
        new FieldBuilder(builder, "aminoAcids", "keyword").build();
        new FieldBuilder(builder, "sift", "keyword").build();
        new FieldBuilder(builder, "polyphen", "keyword").build();
        new FieldBuilder(builder, "varPep", "keyword").build();
        new FieldBuilder(builder, "impact", "keyword").build();
        new FieldBuilder(builder, "symbol", "keyword").build();
        new FieldBuilder(builder, "gene", "keyword").build();
        new FieldBuilder(builder, "biotype", "keyword").build();
        new FieldBuilder(builder, "exon", "keyword").build();
        new FieldBuilder(builder, "intron", "keyword").build();
        new FieldBuilder(builder, "HGVSg", "keyword").build();
        new FieldBuilder(builder, "HGVSp", "keyword").build();
        new FieldBuilder(builder, "HGVSc", "keyword").build();
        new FieldBuilder(builder, "cDNAPosition", "keyword").build();
        new FieldBuilder(builder, "CDSPosition", "keyword").build();
        new FieldBuilder(builder, "proteinPosition", "keyword").build();
        new FieldBuilder(builder, "codon", "keyword").build();
        new FieldBuilder(builder, "existingVariation", "keyword").build();
        new FieldBuilder(builder, "distance", "keyword").build();
        new FieldBuilder(builder, "strand", "keyword").build();
        new FieldBuilder(builder, "flags", "keyword").build();
        new FieldBuilder(builder, "symbolSource", "keyword").build();
        new FieldBuilder(builder, "HGNCId", "keyword").build();
        new FieldBuilder(builder, "source", "keyword").build();
        new FieldBuilder(builder, "HGVSOffset", "keyword").build();
        new FieldBuilder(builder, "RefSeqMatch", "keyword").build();
        new FieldBuilder(builder, "refSeqOffset", "keyword").build();
        new FieldBuilder(builder, "givenRef", "keyword").build();
        new FieldBuilder(builder, "usedRef", "keyword").build();
        new FieldBuilder(builder, "BAMEdit", "keyword").build();
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
