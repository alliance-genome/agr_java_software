package org.alliancegenome.indexer.variant.scripts;

import java.io.File;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonProcessingException;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;

public class TestLineConvert {

	public static void main(String[] args) throws JsonProcessingException {
		
		VCFFileReader reader = new VCFFileReader(new File("/Volumes/Cardano_Backup/Variants/HUMAN.v2.vep.chr2.vcf.gz"), false);
		VCFInfoHeaderLine header = reader.getFileHeader().getInfoHeaderLine("CSQ");
		System.out.println(header.getDescription());
		String format = header.getDescription().split("Format: ")[1];
		
		//System.out.println(format);
		
		String[] formats = format.split("\\|");
		
		
		
		CloseableIterator<VariantContext> iter1 = reader.iterator();
		while(iter1.hasNext()) {
			VariantContext vc = iter1.next();
			HashMap<String, String> map = new HashMap<String, String>();
			//System.out.println(vc.getAttribute("CSQ", ""));
			//System.out.println(vc.getAttributeAsList("CSQ"));
			for(String s: vc.getAttributeAsStringList("CSQ", "")) {
				if(s.length() > 0) {
					String[] infos = s.split("\\|");

					//System.out.println(format.length());
					//System.out.println(s.length());
					for(int i = 0; i < formats.length; i++) {
						map.put(formats[i], infos[i]);
					}
				}
			}
			System.out.println(map);
		}
		
		//String csq = "[T|synonymous_variant|LOW|FAM110C|ENSG00000184731|Transcript|ENST00000327669|protein_coding|1/2||ENST00000327669.5:c.396G>A|ENSP00000328347.4:p.Lys132%3D|516|396|132|K|aaG/aaA|||-1||HGNC|HGNC:33340||Ensembl||C|C|||||2:g.45990C>T, T|intron_variant&non_coding_transcript_variant|MODIFIER|FAM110C|ENSG00000184731|Transcript|ENST00000461026|processed_transcript||1/1|ENST00000461026.1:n.64+817G>A|||||||||-1||HGNC|HGNC:33340||Ensembl||C|C|||||2:g.45990C>T, T|synonymous_variant|LOW|FAM110C|642273|Transcript|NM_001077710.3|protein_coding|1/2||NM_001077710.3:c.396G>A|NP_001071178.2:p.Lys132%3D|516|396|132|K|aaG/aaA|||-1||EntrezGene|HGNC:33340||RefSeq||C|C|||||2:g.45990C>T, T|synonymous_variant|LOW|FAM110C|642273|Transcript|XM_011510372.2|protein_coding|1/2||XM_011510372.2:c.396G>A|XP_011508674.1:p.Lys132%3D|601|396|132|K|aaG/aaA|||-1||EntrezGene|HGNC:33340||RefSeq||C|C|||||2:g.45990C>T, T|synonymous_variant|LOW|FAM110C|642273|Transcript|XM_017004689.1|protein_coding|2/3||XM_017004689.1:c.396G>A|XP_016860178.1:p.Lys132%3D|793|396|132|K|aaG/aaA|||-1||EntrezGene|HGNC:33340||RefSeq||C|C|||||2:g.45990C>T, T|synonymous_variant|LOW|FAM110C|642273|Transcript|XM_017004690.1|protein_coding|2/3||XM_017004690.1:c.396G>A|XP_016860179.1:p.Lys132%3D|541|396|132|K|aaG/aaA|||-1||EntrezGene|HGNC:33340||RefSeq||C|C|||||2:g.45990C>T, T|synonymous_variant|LOW|FAM110C|642273|Transcript|XM_017004691.1|protein_coding|1/3||XM_017004691.1:c.396G>A|XP_016860180.1:p.Lys132%3D|601|396|132|K|aaG/aaA|||-1||EntrezGene|HGNC:33340||RefSeq||C|C|||||2:g.45990C>T, T|synonymous_variant|LOW|FAM110C|642273|Transcript|XM_017004692.1|protein_coding|1/2||XM_017004692.1:c.396G>A|XP_016860181.1:p.Lys132%3D|601|396|132|K|aaG/aaA|||-1||EntrezGene|HGNC:33340||RefSeq||C|C|||||2:g.45990C>T, T|non_coding_transcript_exon_variant|MODIFIER|FAM110C|642273|Transcript|XR_001738890.1|misc_RNA|1/4||XR_001738890.1:n.601G>A||601|||||||-1||EntrezGene|HGNC:33340||RefSeq||C|C|||||2:g.45990C>T]";
		//ObjectMapper mapper = new ObjectMapper();
		//String jsonArray = mapper.writeValueAsString(csq);

		//String[] csqs = csq.split(",");
		

		
		
		//System.out.println(jsonArray);
	}

}
