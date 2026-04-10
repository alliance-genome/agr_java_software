package org.alliancegenome.vep.gff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.alliancegenome.vep.model.CdsSegment;
import org.alliancegenome.vep.model.ExonModel;
import org.alliancegenome.vep.model.GeneModel;
import org.alliancegenome.vep.model.TranscriptModel;

import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.annotation.Strand;
import htsjdk.tribble.gff.Gff3Codec;
import htsjdk.tribble.gff.Gff3Feature;
import lombok.extern.log4j.Log4j2;
import net.nilosplace.process_display.ProcessDisplayHelper;

@Log4j2
public class Gff3GeneModelBuilder {

	private static final Set<String> GENE_TYPES = Set.of(
		"gene", "pseudogene", "tRNA_gene", "snoRNA_gene", "ncRNA_gene",
		"lincRNA_gene", "lncRNA_gene", "transposable_element_gene",
		"miRNA_gene", "rRNA_gene", "snRNA_gene", "piRNA_gene"
	);

	// Transcript-level types to include, verified against ORIG VEP v111 output.
	// Types NOT in ORIG: pre_miRNA, circular_ncRNA, antisense_RNA, scRNA, unconfirmed_transcript
	private static final Set<String> TRANSCRIPT_TYPES = Set.of(
		"mRNA", "lnc_RNA", "lncRNA", "lincRNA", "ncRNA", "transcript",
		"pseudogenic_transcript", "pseudogenic_tRNA", "pseudogenic_rRNA",
		"tRNA", "snoRNA", "rRNA", "snRNA",
		"miRNA", "miRNA_primary_transcript", "piRNA",
		"J_gene_segment", "nc_primary_transcript",
		"processed_transcript", "aberrant_processed_transcript"
	);

	public GeneModel build(String gffPath) throws Exception {
		log.info("Loading GFF3 gene model from: {}", gffPath);

		// Preprocess GFF3 to fix invalid attribute values
		File cleanGff = preprocessGff3(gffPath);

		GeneModel model = new GeneModel();
		Map<String, String> geneSymbols = new HashMap<>();
		Map<String, String> geneCuries = new HashMap<>();
		int transcriptCount = 0;
		int skippedNoBiotype = 0;
		int skippedNoExons = 0;

		try (AbstractFeatureReader<Gff3Feature, ?> reader =
				AbstractFeatureReader.getFeatureReader(cleanGff.getAbsolutePath(), null, new Gff3Codec(), false)) {

			ProcessDisplayHelper ph = new ProcessDisplayHelper();
			ph.startProcess("Loading Gff3Feature's");
			for (Gff3Feature feature : reader.iterator()) {
				String type = feature.getType();
				ph.progressProcess();
				if (GENE_TYPES.contains(type)) {
					String geneId = feature.getID();
					String symbol = feature.getName();
					String curie = getAttr(feature, "gene_id").orElse(getAttr(feature, "curie").orElse(null));
					if (geneId != null) {
						geneSymbols.put(geneId, symbol);
						geneCuries.put(geneId, curie);
						model.addGene(geneId, symbol, curie);
					}
				}

				if (!TRANSCRIPT_TYPES.contains(type)) {
					continue;
				}

				String biotype = determineBiotype(feature, type);
				if (biotype == null) {
					skippedNoBiotype++;
					continue;
				}

				TranscriptModel tm = new TranscriptModel();
				tm.setId(feature.getID());
				tm.setChr(feature.getContig());
				tm.setStart(feature.getStart());
				tm.setEnd(feature.getEnd());
				tm.setPositiveStrand(feature.getStrand() != Strand.NEGATIVE);
				tm.setSource(feature.getSource());
				tm.setBiotype(biotype);
				// Detect mitochondrial chromosomes for codon table selection
				String contig = feature.getContig().toLowerCase();
				if (contig.contains("mitochondrion") || contig.equals("mt") || contig.equals("chrm")
						|| contig.equals("chrmt") || contig.contains("mitochondrion_genome")) {
					tm.setCodonTable(2);
				}

				String transcriptId = getAttr(feature, "transcript_id").orElse(null);
				String curie = getAttr(feature, "curie").orElse(null);
				if (transcriptId != null) {
					tm.setTranscriptId(transcriptId);
				} else if (curie != null) {
					tm.setTranscriptId(curie);
				} else {
					tm.setTranscriptId(feature.getID());
				}

				tm.setName(feature.getName());
				getAttr(feature, "protein_id").ifPresent(tm::setProteinId);
				// VEP VariationEffect.pm line 959: skip start_lost for cds_start_NF transcripts
				if (getAttr(feature, "cds_start_NF").isPresent()) {
					tm.setCdsStartNF(true);
				}

				for (Gff3Feature parent : feature.getParents()) {
					String parentId = parent.getID();
					if (geneSymbols.containsKey(parentId)) {
						tm.setGeneId(parentId);
						tm.setGeneSymbol(geneSymbols.get(parentId));
						tm.setGeneCurie(geneCuries.get(parentId));
						break;
					}
				}

				if (tm.getGeneSymbol() == null) {
					getAttr(feature, "gene").ifPresent(tm::setGeneSymbol);
				}

				for (Gff3Feature child : feature.getChildren()) {
					String childType = child.getType();
					if ("exon".equals(childType)) {
						tm.getExons().add(new ExonModel(child.getStart(), child.getEnd()));
					} else if ("CDS".equals(childType)) {
						int phase = child.getPhase() >= 0 ? child.getPhase() : 0;
						tm.getCdsSegments().add(new CdsSegment(child.getStart(), child.getEnd(), phase));
						if (tm.getProteinId() == null) {
							getAttr(child, "protein_id").ifPresent(tm::setProteinId);
						}
					}
				}

				if (tm.getExons().isEmpty()) {
					skippedNoExons++;
					continue;
				}

				tm.sortAndIndex();
				tm.setLoadOrder(transcriptCount);
				model.addTranscript(tm);
				transcriptCount++;
			}
			ph.finishProcess();
		} finally {
			cleanGff.delete();
		}

		if (skippedNoBiotype > 0) {
			log.info("Skipped {} transcripts with undetermined biotype", skippedNoBiotype);
		}
		if (skippedNoExons > 0) {
			log.info("Skipped {} transcripts with no exons", skippedNoExons);
		}
		log.info("GFF3 loaded: {} transcripts registered", transcriptCount);
		model.logSummary();
		return model;
	}

	/**
	 * Preprocess GFF3 to fix known issues that htsjdk's Gff3Codec cannot handle:
	 * - Unescaped semicolons in attribute values (should be %3B)
	 * - Invalid URL-encoded sequences
	 */
	private File preprocessGff3(String gffPath) throws Exception {
		File tempFile = File.createTempFile("vep_gff3_", ".gff3");
		tempFile.deleteOnExit();
		long fixedLines = 0;

		try (BufferedReader reader = openGff3(gffPath);
			 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile), 1 << 16)) {

			ProcessDisplayHelper ph = new ProcessDisplayHelper();
			ph.startProcess("Preprocessing Gff3");
			String line;
			while ((line = reader.readLine()) != null) {
				ph.progressProcess();
				if (line.startsWith("#") || line.isEmpty()) {
					writer.write(line);
					writer.newLine();
					continue;
				}

				// Split into 9 tab-delimited fields
				int lastTab = nthIndexOf(line, '\t', 8);
				if (lastTab < 0) {
					writer.write(line);
					writer.newLine();
					continue;
				}

				String prefix = line.substring(0, lastTab + 1);
				String attrStr = line.substring(lastTab + 1);

				// Skip lines with empty/whitespace-only attributes (e.g., FB mitochondrion gene)
				if (attrStr.trim().isEmpty()) {
					fixedLines++;
					continue;
				}

				String fixedAttrs = fixAttributes(attrStr);
				if (!fixedAttrs.equals(attrStr)) {
					fixedLines++;
				}

				writer.write(prefix);
				writer.write(fixedAttrs);
				writer.newLine();
			}
			ph.finishProcess();
		}

		if (fixedLines > 0) {
			log.info("Preprocessed GFF3: fixed {} lines with invalid attributes", fixedLines);
		}
		return tempFile;
	}

	/**
	 * Fix attribute string issues that htsjdk's Gff3Codec cannot handle:
	 * 1. Unescaped semicolons in values (should be %3B)
	 * 2. Truncated/invalid URL-encoded sequences (bare % not followed by two hex chars)
	 */
	private String fixAttributes(String attrStr) {
		// Fix invalid % sequences first (e.g., "%2" should be "%252" to escape the %)
		attrStr = fixInvalidPercent(attrStr);

		// Fix unescaped semicolons in values
		String[] parts = attrStr.split(";");
		if (parts.length <= 1) {
			return attrStr;
		}

		StringBuilder fixed = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (i == 0) {
				fixed.append(parts[i]);
			} else if (parts[i].contains("=")) {
				fixed.append(";").append(parts[i]);
			} else {
				fixed.append("%3B").append(parts[i]);
			}
		}
		return fixed.toString();
	}

	/**
	 * Fix bare % characters that aren't valid URL encoding.
	 * A valid URL-encoded sequence is % followed by exactly two hex digits.
	 * Any % not followed by two hex digits gets escaped to %25.
	 */
	private String fixInvalidPercent(String s) {
		if (!s.contains("%")) {
			return s;
		}

		StringBuilder sb = new StringBuilder(s.length() + 16);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '%') {
				if (i + 2 < s.length()
					&& isHexDigit(s.charAt(i + 1))
					&& isHexDigit(s.charAt(i + 2))) {
					sb.append(c); // valid %XX
				} else {
					sb.append("%25"); // escape the bare %
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private boolean isHexDigit(char c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}

	private int nthIndexOf(String str, char c, int n) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == c) {
				count++;
				if (count == n) {
					return i;
				}
			}
		}
		return -1;
	}

	private String determineBiotype(Gff3Feature feature, String type) {
		Optional<String> biotype = getAttr(feature, "biotype");
		if (biotype.isPresent()) {
			return biotype.get();
		}
		biotype = getAttr(feature, "transcript_type");
		if (biotype.isPresent()) {
			return biotype.get();
		}
		biotype = getAttr(feature, "transcript_biotype");
		if (biotype.isPresent()) {
			return biotype.get();
		}

		String ltype = type.toLowerCase();
		if ("mrna".equals(ltype)) {
			return "protein_coding";
		}
		if ("ncrna".equals(ltype)) {
			return getAttr(feature, "ncrna_class").orElse("ncRNA");
		}
		if ("lncrna".equals(ltype)) {
			return "lncRNA";
		}
		if (ltype.endsWith("_gene_segment")) {
			String prefix = ltype.replace("_gene_segment", "");
			return "IG_" + prefix.toUpperCase() + "_gene";
		}

		switch (type) {
			case "lnc_RNA": return "lnc_RNA";
			case "lincRNA": return "lincRNA";
			case "transcript": return "transcript";
			case "pseudogenic_transcript": return "pseudogenic_transcript";
			case "pseudogenic_tRNA": return "pseudogenic_transcript";
			case "pseudogenic_rRNA": return "pseudogenic_transcript";
			case "processed_transcript": return "processed_transcript";
			case "aberrant_processed_transcript": return "processed_transcript";
			case "unconfirmed_transcript": return "unconfirmed_transcript";
			case "miRNA": return "miRNA";
			case "miRNA_primary_transcript": return "miRNA";
			case "snoRNA": return "snoRNA";
			case "snRNA": return "snRNA";
			case "rRNA": return "rRNA";
			case "tRNA": return "tRNA";
			case "nc_primary_transcript": return "ncRNA";
			case "piRNA": return "ncRNA";
			default:
				return getAttr(feature, "gbkey").orElse(null);
		}
	}

	private Optional<String> getAttr(Gff3Feature feature, String key) {
		try {
			return feature.getUniqueAttribute(key);
		} catch (IllegalArgumentException e) {
			// Multiple values — return first
			var values = feature.getAttribute(key);
			if (values != null && !values.isEmpty()) {
				return Optional.of(values.get(0));
			}
			return Optional.empty();
		}
	}

	private BufferedReader openGff3(String path) throws Exception {
		FileInputStream fis = new FileInputStream(path);
		if (path.endsWith(".gz")) {
			return new BufferedReader(new InputStreamReader(new GZIPInputStream(fis, 65536), StandardCharsets.UTF_8), 1 << 16);
		}
		return new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8), 1 << 16);
	}
}
