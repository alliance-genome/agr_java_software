package org.alliancegenome.vep.debug;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.alliancegenome.vep.annotation.OutputFactory;
import org.alliancegenome.vep.csq.CsqEntry;
import org.alliancegenome.vep.gff.Gff3GeneModelBuilder;
import org.alliancegenome.vep.model.GeneModel;
import org.alliancegenome.vep.reference.ContigAccessionMap;
import org.alliancegenome.vep.reference.ReferenceGenome;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

/**
 * Debug runner: processes a VCF file with [TRACE] logging to stderr.
 *
 * Usage:
 *   java -Dvep.trace=true org.alliancegenome.vep.debug.DebugVep \
 *     <input.vcf> <gff.gz> <fasta> <output.vcf> [synonyms.txt]
 *
 * Stderr: trace lines (matching Perl format)
 * Stdout: minimal CSQ output for diffing against Perl VCF
 */
public class DebugVep {

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.err.println("Usage: DebugVep <input.vcf> <gff.gz> <fasta> <output.vcf> [synonyms.txt]");
			System.exit(1);
		}

		String inputVcf = args[0];
		String gffPath = args[1];
		String fastaPath = args[2];
		String outputVcf = args[3];
		String synonymsPath = args.length > 4 ? args[4] : null;

		System.err.println("[DEBUG] Loading GFF: " + gffPath);
		Gff3GeneModelBuilder builder = new Gff3GeneModelBuilder();
		GeneModel geneModel = builder.build(gffPath);

		System.err.println("[DEBUG] Loading FASTA: " + fastaPath);
		ReferenceGenome reference = new ReferenceGenome(fastaPath);

		ContigAccessionMap contigMap = ContigAccessionMap.fromFasta(fastaPath, synonymsPath);
		OutputFactory annotator = new OutputFactory(geneModel, reference, contigMap, "DEBUG");

		System.err.println("[DEBUG] Processing VCF: " + inputVcf);
		try (VCFFileReader reader = new VCFFileReader(new File(inputVcf), false);
				PrintWriter out = new PrintWriter(outputVcf)) {
			out.println("#CHROM\tPOS\tREF\tALT\tCSQ");
			for (VariantContext vc : reader) {
				List<CsqEntry> entries = annotator.annotate(vc);
				for (CsqEntry entry : entries) {
					out.printf("%s\t%d\t%s\t%s\t%s%n",
						vc.getContig(), vc.getStart(),
						vc.getReference().getBaseString(),
						vc.getAlternateAlleles().get(0).getBaseString(),
						entry.toVcfString());
				}
			}
		}
		System.err.println("[DEBUG] Done. Output: " + outputVcf);
	}
}
