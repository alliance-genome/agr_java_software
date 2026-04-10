package org.alliancegenome.vep.config;

import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Getter
@Log4j2
public class VepConfig {

	private String vcfPath;
	private String gffPath;
	private String fastaPath;
	private String bamPath;
	private String outputPath;
	private String mod;

	public static VepConfig fromArgs(String[] args) {
		VepConfig config = new VepConfig();

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "--vcf":
					config.vcfPath = args[++i];
					break;
				case "--gff":
					config.gffPath = args[++i];
					break;
				case "--fasta":
					config.fastaPath = args[++i];
					break;
				case "--bam":
					config.bamPath = args[++i];
					break;
				case "--output":
					config.outputPath = args[++i];
					break;
				case "--mod":
					config.mod = args[++i];
					break;
				default:
					log.error("Unknown argument: {}", args[i]);
					printUsage();
					return null;
			}
		}

		if (config.vcfPath == null || config.gffPath == null || config.fastaPath == null
				|| config.outputPath == null || config.mod == null) {
			log.error("Missing required arguments");
			printUsage();
			return null;
		}

		if (!Files.exists(Path.of(config.vcfPath))) {
			log.error("VCF file not found: {}", config.vcfPath);
			return null;
		}
		if (!Files.exists(Path.of(config.gffPath))) {
			log.error("GFF file not found: {}", config.gffPath);
			return null;
		}
		if (!Files.exists(Path.of(config.fastaPath))) {
			log.error("FASTA file not found: {}", config.fastaPath);
			return null;
		}
		if (config.bamPath != null && !Files.exists(Path.of(config.bamPath))) {
			log.error("BAM file not found: {}", config.bamPath);
			return null;
		}

		return config;
	}

	private static void printUsage() {
		System.err.println("Usage: agr_vep --vcf <file> --gff <file> --fasta <file> [--bam <file>] --output <file> --mod <MOD>");
		System.err.println();
		System.err.println("Required:");
		System.err.println("  --vcf <file>     Input VCF file");
		System.err.println("  --gff <file>     GFF3 gene model file");
		System.err.println("  --fasta <file>   Reference genome FASTA file");
		System.err.println("  --output <file>  Output annotated VCF file");
		System.err.println("  --mod <MOD>      Species/MOD name (SGD, FB, WB, ZFIN, HUMAN, RGD, MGI)");
		System.err.println();
		System.err.println("Optional:");
		System.err.println("  --bam <file>     BAM file for RNA editing detection");
	}
}
