package org.alliancegenome.vep.csq;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import htsjdk.variant.vcf.VCFHeaderLineCount;
import htsjdk.variant.vcf.VCFHeaderLineType;

public class CsqHeaderWriter {

	private static final String CSQ_FORMAT =
		"Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|" +
		"EXON|INTRON|HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|" +
		"Amino_acids|Codons|Existing_variation|DISTANCE|STRAND|FLAGS|" +
		"Gene_level_consequence|SYMBOL_SOURCE|HGNC_ID|GIVEN_REF|USED_REF|" +
		"BAM_EDIT|SOURCE|HGVS_OFFSET|HGVSg|PolyPhen_prediction|PolyPhen_score|" +
		"SIFT_prediction|SIFT_score|transcript_name|Genomic_end_position|" +
		"Genomic_start_position|";

	public static void addHeaders(VCFHeader header, String mod) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

		header.addMetaDataLine(new VCFHeaderLine("VEP",
			"\"agr_vep\" time=\"" + timestamp + "\""));

		header.addMetaDataLine(new VCFInfoHeaderLine("CSQ",
			VCFHeaderLineCount.UNBOUNDED,
			VCFHeaderLineType.String,
			"Consequence annotations from AGR VEP. Format: " + CSQ_FORMAT + mod + "_GFF.refseq.gff.gz"));
	}

	private CsqHeaderWriter() {
	}
}
