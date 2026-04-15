package org.alliancegenome.vep.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.vep.debug.Trace;

import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.OverlapDetector;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class GeneModel {

	private final Map<String, OverlapDetector<TranscriptModel>> perChromosomeDetectors = new HashMap<>();
	private final Map<String, String> geneIdToSymbol = new HashMap<>();
	private final Map<String, String> geneIdToCurie = new HashMap<>();
	private final Map<String, String> contigNormMap = new HashMap<>();

	private int transcriptCount = 0;
	private int geneCount = 0;

	public void addGene(String geneId, String symbol, String curie) {
		if (symbol != null) {
			geneIdToSymbol.put(geneId, symbol);
		}
		if (curie != null) {
			geneIdToCurie.put(geneId, curie);
		}
		geneCount++;
	}

	public void addTranscript(TranscriptModel transcript) {
		String chr = transcript.getChr();
		OverlapDetector<TranscriptModel> detector = perChromosomeDetectors.computeIfAbsent(
			chr, k -> new OverlapDetector<>(0, 0));
		Interval interval = new Interval(chr, transcript.getStart(), transcript.getEnd());
		detector.addLhs(transcript, interval);
		// Build case-insensitive lookup map
		contigNormMap.put(chr.toLowerCase(), chr);
		transcriptCount++;
	}

	public String normalizeContig(String chr) {
		if (perChromosomeDetectors.containsKey(chr)) return chr;
		String normalized = contigNormMap.get(chr.toLowerCase());
		return normalized != null ? normalized : chr;
	}

	public List<TranscriptModel> getOverlappingTranscripts(String chr, int start, int end) {
		OverlapDetector<TranscriptModel> detector = perChromosomeDetectors.get(chr);
		if (detector == null) {
			// Try case-insensitive lookup
			String normalized = contigNormMap.get(chr.toLowerCase());
			if (normalized != null) {
				detector = perChromosomeDetectors.get(normalized);
			}
		}
		if (detector == null) {
			return List.of();
		}
		// For insertions, start > end. OverlapDetector needs start <= end,
		// so query with normalized range, then re-filter using VEP's overlap formula
		// (overlap = end >= featStart AND start <= featEnd).
		// For insertions (start > end), this excludes transcripts where the insertion
		// boundary is exactly at the transcript edge (matches Perl behavior).
		int queryStart = Math.min(start, end);
		int queryEnd = Math.max(start, end);
		Interval query = new Interval(chr, queryStart, queryEnd);
		Set<TranscriptModel> overlaps = detector.getOverlaps(query);
		List<TranscriptModel> result = new ArrayList<>();
		for (TranscriptModel tm : overlaps) {
			// VEP overlap (VariationEffect.pm line 80-84):
			//   ($f1_end >= $f2_start) and ($f1_start <= $f2_end)
			if (end >= tm.getStart() && start <= tm.getEnd()) {
				result.add(tm);
			}
		}
		return result;
	}

	public String getGeneSymbol(String geneId) {
		return geneIdToSymbol.get(geneId);
	}

	public String getGeneCurie(String geneId) {
		return geneIdToCurie.get(geneId);
	}

	public int getTranscriptCount() {
		return transcriptCount;
	}

	public int getGeneCount() {
		return geneCount;
	}

	public Set<String> getChromosomes() {
		return perChromosomeDetectors.keySet();
	}

	/**
	 * Apply transcript name overrides from the VEP ProtFuncTranscriptNameHTP plugin's
	 * transcript_map table. Perl's pipeline regenerates that DB table from the current
	 * GFF at the start of each run (agr_vep_pipeline/ModVep/SplitInput.pm), so in
	 * practice the DB value equals the GFF Name attribute. A stale external TSV file
	 * can drift from the GFF (e.g., the FB TMAP had 157 transcripts still using old
	 * CG-number names while the GFF had current FlyBase symbols), so we use the GFF
	 * Name (already captured during GFF load) as the authoritative source and only
	 * fall back to the TSV file when the GFF did not provide a Name for a transcript.
	 * For duplicate entries (same ID, multiple names), last entry wins (matching VEP's while loop).
	 */
	public void applyTranscriptNameMap(String tsvFilePath) {
		if (tsvFilePath == null) return;
		java.io.File file = new java.io.File(tsvFilePath);
		if (!file.exists()) {
			log.warn("Transcript name map not found: {}", tsvFilePath);
			return;
		}
		Map<String, String> nameMap = new HashMap<>();
		int lineCount = 0;
		try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split("\t", 2);
				if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
					nameMap.put(parts[0], parts[1]); // last entry wins for duplicates
					lineCount++;
				}
			}
		} catch (Exception e) {
			log.warn("Failed to load transcript name map: {}", e.getMessage());
			return;
		}
		// Hybrid apply:
		//   - If GFF already set a Name for the transcript, keep it (authoritative,
		//     matches what Perl's DB would return since SplitInput.pm rebuilds from GFF).
		//   - Else, fall back to the TSV mapping (handles transcripts missing a GFF
		//     Name but present in the TMAP, e.g. pseudogene entries the GFF omits).
		//   In both cases, mark nameFromTmap so OutputFactory emits transcript_name.
		int kept = 0;        // transcripts with GFF Name retained (no override applied)
		int filled = 0;      // transcripts with no GFF Name, filled from TSV
		int filledMissing = 0; // transcripts with no GFF Name and no TSV entry (still unset)
		int visited = 0;
		int sampleHit = 0;
		int sampleOverride = 0;
		for (OverlapDetector<TranscriptModel> detector : perChromosomeDetectors.values()) {
			for (TranscriptModel tm : detector.getAll()) {
				visited++;
				String gffName = tm.getName();
				if (gffName != null && !gffName.isEmpty()) {
					// GFF wins. Mark as-if from TMAP so OutputFactory emits it.
					tm.setNameFromTmap(true);
					kept++;
					if (sampleHit < 3) {
						Trace.log("TMAP.gff_kept",
							"tid=%s gff_name=%s tsv_value=%s",
							tm.getTranscriptId(), gffName, nameMap.get(tm.getTranscriptId()));
						sampleHit++;
					}
					continue;
				}
				String override = nameMap.get(tm.getTranscriptId());
				if (override != null) {
					tm.setName(override);
					tm.setNameFromTmap(true);
					filled++;
					if (sampleOverride < 3) {
						Trace.log("TMAP.filled_from_tsv",
							"tid=%s name=%s", tm.getTranscriptId(), override);
						sampleOverride++;
					}
				} else {
					filledMissing++;
				}
			}
		}
		log.info("Transcript name map: visited={} gff_kept={} filled_from_tsv={} unset={} ({} TSV rows loaded)",
			visited, kept, filled, filledMissing, lineCount);
	}

	public void logSummary() {
		log.info("Gene model loaded: {} genes, {} transcripts across {} chromosomes",
			geneCount, transcriptCount, perChromosomeDetectors.size());
	}
}
