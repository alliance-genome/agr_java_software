package org.alliancegenome.api.tests.integration;

import htsjdk.variant.variantcontext.VariantContext;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.site.dao.VariantESDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.junit.Assert.assertNotNull;


public class VariantESTest {

    private VariantESDAO variantESDAO = new VariantESDAO();

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();

        //alleleService = new AlleleService();

    }

    @Test
    public void checkAllelesBySpecies() {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder bool = boolQuery();

        bool.filter(new TermQueryBuilder("category", "allele"));
        bool.must(new TermQueryBuilder("variant.gene.id.keyword", "WB:WBGene00004054"));
        searchSourceBuilder.query(bool);
//        SearchSourceBuilder buil = searchSourceBuilder.aggregation(AggregationBuilders.terms("keys1").field("transcriptLevelConsequences.geneLevelConsequence.keyword"));

        Pagination pagination = new Pagination();
        pagination.addFieldFilter(FieldFilter.MOLECULAR_CONSEQUENCE, ".50");
        JsonResultResponse<Allele> list = variantESDAO.performQuery(searchSourceBuilder, new Pagination());
        assertNotNull(list);
    }

    @Test
    public void checkDistinctValues() {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder bool = boolQuery();

        bool.filter(new TermQueryBuilder("category", "allele"));
        //bool.must(new TermQueryBuilder("variant.gene.id.keyword", "WB:WBGene00004054"));
        searchSourceBuilder.query(bool);
//        SearchSourceBuilder buil = searchSourceBuilder.aggregation(AggregationBuilders.terms("keys1").field("transcriptLevelConsequences.geneLevelConsequence.keyword"));

        Map<String, Map<String, Integer>> list = variantESDAO.getHistogram(searchSourceBuilder);
        assertNotNull(list);
    }

    public void checkVariantDetailRetrieval() {
        Variant variant = variantESDAO.getVariant("NC_005111.4:g.539868A>C");
        assertNotNull(variant);

        htsjdk.variant.variantcontext.Allele refAllele = htsjdk.variant.variantcontext.Allele.create("ATTGCACTTACTACATCACGCTCCCAACTGGAAAAGCAATTTCCGAAAAACCGCATTTTTTTGATTAATGGGAAAAGTTGTAGTGATATTAAAACAAAAGTAATACTTATCTCATAGTAAATGC*", true);
        htsjdk.variant.variantcontext.Allele variantAllele = htsjdk.variant.variantcontext.Allele.create("A", false);
        List<htsjdk.variant.variantcontext.Allele> alleleList = List.of(refAllele, variantAllele);
        String attributeName = "CSQ";
        String attributeValue = "-|intron_variant|MODIFIER|ZC13.2|WB:WBGene00022503|Transcript|WB:ZC13.2.1|protein_coding||1/4|WB:ZC13.2.1:c.348+92_349-17del|||||||||1||intron_variant|||TTGCACTTACTACATCACGCTCCCAACTGGAAAAGCAATTTCCGAAAAACCGCATTTTTTTGATTAATGGGAAAAGTTGTAGTGATATTAAAACAAAAGTAATACTTATCTCATAGTAAATGC|TTGCACTTACTACATCACGCTCCCAACTGGAAAAGCAATTTCCGAAAAACCGCATTTTTTTGATTAATGGGAAAAGTTGTAGTGATATTAAAACAAAAGTAATACTTATCTCATAGTAAATGC||WB_GFF.refseq.gff.gz||NC_003284.9:g.881176_881298del|||||881298|881176|ZC13.2.1|";
        Map<String, String> csqMap = new HashMap<>();
        csqMap.put(attributeName, attributeValue);
        EnumSet<VariantContext.Validation> validationSet = EnumSet.of(VariantContext.Validation.ALLELES);

        //VariantContext context = new VariantContext("unknown","WB:WBVar02079625", "X", 881175, 881298, alleleList, null,1.0, Set.of(), csqMap,false, validationSet);
    }

}
/*
CSQ header
Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON|HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|DISTANCE|STRAND|FLAGS|Gene_level_consequence|SYMBOL_SOURCE|HGNC_ID|GIVEN_REF|USED_REF|BAM_EDIT|SOURCE|HGVS_OFFSET|HGVSg|PolyPhen_prediction|PolyPhen_score|SIFT_prediction|SIFT_score|Genomic_end_position|Genomic_start_position|transcript_name|RGD_GFF.refseq.gff.gz`
-|intron_variant|MODIFIER|ZC13.2|WB:WBGene00022503|Transcript|WB:ZC13.2.1|protein_coding||1/4|WB:ZC13.2.1:c.348+92_349-17del|||||||||1||intron_variant|||TTGCACTTACTACATCACGCTCCCAACTGGAAAAGCAATTTCCGAAAAACCGCATTTTTTTGATTAATGGGAAAAGTTGTAGTGATATTAAAACAAAAGTAATACTTATCTCATAGTAAATGC|TTGCACTTACTACATCACGCTCCCAACTGGAAAAGCAATTTCCGAAAAACCGCATTTTTTTGATTAATGGGAAAAGTTGTAGTGATATTAAAACAAAAGTAATACTTATCTCATAGTAAATGC||WB_GFF.refseq.gff.gz||NC_003284.9:g.881176_881298del|||||881298|881176|ZC13.2.1|
Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON|HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|DISTANCE|STRAND|FLAGS|Gene_level_consequence|SYMBOL_SOURCE|HGNC_ID|GIVEN_REF|USED_REF|BAM_EDIT|SOURCE|HGVS_OFFSET|HGVSg|PolyPhen_prediction|PolyPhen_score|SIFT_prediction|SIFT_score|Genomic_end_position|Genomic_start_position|transcript_name|RGD_GFF.refseq.gff.gz`
T|intron_variant|MODIFIER|LOC302022|RGD:1593261|Transcript|RefSeq:XM_017598466.1|protein_coding||2/2|RefSeq:XM_017598466.1:c.*256+4594T>A|||||||||-1||intron_variant|||A|A||RGD_GFF.refseq.gff.gz||NC_005111.4:g.49033A>T|||||49033|49033|XM_017598466.1|
T|intron_variant|MODIFIER|LOC302022|RGD:1593261|Transcript|RefSeq:XM_017598467.1|protein_coding||2/2|RefSeq:XM_017598467.1:c.*245+4594T>A|||||||||-1||intron_variant|||A|A||RGD_GFF.refseq.gff.gz||NC_005111.4:g.49033A>T|||||49033|49033|XM_017598467.1|
T|intron_variant&non_coding_transcript_variant|MODIFIER|LOC302022|RGD:1593261|Transcript|RefSeq:XR_001840603.1|transcript||2/2|RefSeq:XR_001840603.1:n.558+4594T>A|||||||||-1||intron_variant|||A|A||RGD_GFF.refseq.gff.gz||NC_005111.4:g.49033A>T|||||49033|49033|XR_001840603.1|
T|intron_variant&non_coding_transcript_variant|MODIFIER|LOC302022|RGD:1593261|Transcript|RefSeq:XR_001840604.1|transcript||2/3|RefSeq:XR_001840604.1:n.558+4594T>A|||||||||-1||intron_variant|||A|A||RGD_GFF.refseq.gff.gz||NC_005111.4:g.49033A>T|||||49033|49033|XR_001840604.1|



 */