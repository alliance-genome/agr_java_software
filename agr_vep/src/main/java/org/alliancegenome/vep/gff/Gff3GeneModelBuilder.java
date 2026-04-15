package org.alliancegenome.vep.gff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

	// Raw (un-URL-decoded) Name attribute values, keyed by decoded ID. Populated
	// during preprocessGff3. htsjdk's Gff3Codec URL-decodes all attribute values, but
	// Perl VEP reads GFF attributes verbatim — so `Name=l(2)gl` stays `l(2)gl` and
	// `Name=MF%28ALPHA%292` stays `MF%28ALPHA%292`. Capturing the raw string here lets
	// Java pass it through exactly the way Perl does.
	private final Map<String, String> rawNameByFeatureId = new HashMap<>();

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
					String symbol = getRawName(feature);
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

				tm.setName(getRawName(feature));
				getAttr(feature, "protein_id").ifPresent(tm::setProteinId);
				// VEP VariationEffect.pm line 959: skip start_lost for cds_start_NF transcripts
				if (getAttr(feature, "cds_start_NF").isPresent()) {
					tm.setCdsStartNF(true);
				}
				// VEP VariationEffect.pm line 1278: skip stop codon overlap for cds_end_NF transcripts
				if (getAttr(feature, "cds_end_NF").isPresent()) {
					tm.setCdsEndNF(true);
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

				// VEP BaseGXF.pm line 542: only include CDS segments whose start position
				// falls within an exon (overlap($s, $e, $cds_start, $cds_start)).
				// GFF files sometimes have CDS segments with boundaries that don't fit
				// within exons (e.g., FB FBtr0303882 CDS 9999107-10001428 vs exon
				// 9999110-10001433). Perl excludes these; without the exclusion, the
				// cdna_coding_start computation is off by the excluded CDS length.
				tm.getCdsSegments().removeIf(cds -> {
					int cdsStart = cds.getStart();
					for (ExonModel ex : tm.getExons()) {
						if (ex.getStart() <= cdsStart && cdsStart <= ex.getEnd()) {
							return false; // keep
						}
					}
					return true; // exclude
				});

				tm.sortAndIndex();
				tm.setLoadOrder(transcriptCount);

				// VEP Transcript fields: cdna_coding_start and start_Exon->phase
				// cdna_coding_start = cDNA position where coding begins (after 5'UTR).
				// Must iterate exons in TRANSCRIPTION order (5' → 3'):
				//	 + strand: ascending genomic order
				//	 - strand: descending genomic order
				if (!tm.getCdsSegments().isEmpty() && !tm.getExons().isEmpty()) {
					int cdsGenomicStart = tm.isPositiveStrand()
						? tm.getCdsSegments().get(0).getStart()
						: tm.getCdsSegments().get(tm.getCdsSegments().size() - 1).getEnd();
					List<ExonModel> txOrderExons = new ArrayList<>(tm.getExons());
					if (!tm.isPositiveStrand()) {
						Collections.reverse(txOrderExons);
					}
					int cdnaBases = 0;
					for (ExonModel exon : txOrderExons) {
						if (tm.isPositiveStrand()) {
							if (exon.getEnd() < cdsGenomicStart) {
								cdnaBases += exon.getEnd() - exon.getStart() + 1;
							} else {
								cdnaBases += cdsGenomicStart - exon.getStart() + 1;
								break;
							}
						} else {
							if (exon.getStart() > cdsGenomicStart) {
								cdnaBases += exon.getEnd() - exon.getStart() + 1;
							} else {
								cdnaBases += exon.getEnd() - cdsGenomicStart + 1;
								break;
							}
						}
					}
					tm.setCdnaCodingStart(cdnaBases);
					// VEP phases: two distinct values needed.
					//
					// $transcript->start_Exon->phase (Transcript.pm line 2283-2286) returns
					// get_all_Exons()->[0].phase — the FIRST EXON IN TRANSCRIPT ORDER's phase.
					// Its phase is set to the GFF phase of the CDS matching that exon (via
					// BaseGXF.pm overlap check), or -1 if no CDS matches. Used for cds_start
					// offset in BaseTranscriptVariation.pm line 263.
					//
					// $translation->start_Exon->phase is the phase of the exon containing
					// the translation start = first matched CDS segment's phase. Used for
					// translateable_seq N-padding in Transcript.pm line 917-920.
					ExonModel firstExonTxOrder = tm.isPositiveStrand()
						? tm.getExons().get(0)
						: tm.getExons().get(tm.getExons().size() - 1);
					int transcriptStartPhaseGff = -1;
					for (CdsSegment cds : tm.getCdsSegments()) {
						int cdsStart = cds.getStart();
						if (firstExonTxOrder.getStart() <= cdsStart && cdsStart <= firstExonTxOrder.getEnd()) {
							transcriptStartPhaseGff = cds.getPhase();
							break;
						}
					}
					// $translation->start_Exon: phase from first CDS segment in transcript order.
					int translationStartPhaseGff = tm.isPositiveStrand()
						? tm.getCdsSegments().get(0).getPhase()
						: tm.getCdsSegments().get(tm.getCdsSegments().size() - 1).getPhase();

					// VEP BaseGXF.pm _convert_phase: GFF3 phase 1↔2 swap for Ensembl phase.
					// GFF3 phase = bases forward to next codon; Ensembl phase = bases of prior codon at start.
					int transcriptStartPhase = transcriptStartPhaseGff;
					if (transcriptStartPhase == 1) transcriptStartPhase = 2;
					else if (transcriptStartPhase == 2) transcriptStartPhase = 1;
					tm.setStartExonPhase(transcriptStartPhase);

					int translationStartPhase = translationStartPhaseGff;
					if (translationStartPhase == 1) translationStartPhase = 2;
					else if (translationStartPhase == 2) translationStartPhase = 1;
					tm.setTranslationStartExonPhase(translationStartPhase);
				}
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

				// Capture raw Name verbatim (before fixAttributes) so we can later emit
				// Perl-identical SYMBOL/transcript_name without htsjdk's URL-decode round-trip.
				captureRawName(attrStr);

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

	/**
	 * Walk the raw (pre-fix) attribute string, extract ID and Name values verbatim,
	 * and store rawName keyed by URL-decoded ID. IDs are decoded to match what
	 * htsjdk's Gff3Feature.getID() returns later; Name is stored raw because Perl
	 * VEP emits it verbatim to CSQ.
	 */
	private void captureRawName(String attrStr) {
		String rawId = null;
		String rawName = null;
		int n = attrStr.length();
		int i = 0;
		while (i < n) {
			int eq = attrStr.indexOf('=', i);
			if (eq < 0) break;
			int semi = attrStr.indexOf(';', eq + 1);
			if (semi < 0) semi = n;
			String key = attrStr.substring(i, eq);
			String value = attrStr.substring(eq + 1, semi);
			if ("ID".equals(key)) {
				rawId = value;
			} else if ("Name".equals(key)) {
				rawName = value;
			}
			if (rawId != null && rawName != null) break;
			i = semi + 1;
		}
		if (rawId != null && rawName != null) {
			String decodedId;
			try {
				decodedId = URLDecoder.decode(rawId, StandardCharsets.UTF_8);
			} catch (IllegalArgumentException e) {
				decodedId = rawId;
			}
			rawNameByFeatureId.put(decodedId, rawName);
		}
	}

	/**
	 * Look up the raw (un-URL-decoded) Name attribute for a feature by its htsjdk-decoded ID.
	 * Falls back to htsjdk's decoded name if we didn't capture a raw value.
	 */
	private String getRawName(Gff3Feature feature) {
		String id = feature.getID();
		if (id != null) {
			String raw = rawNameByFeatureId.get(id);
			if (raw != null) return raw;
		}
		return feature.getName();
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
