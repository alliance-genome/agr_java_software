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
	 * transcript_map table. VEP looks up transcript_name by transcript stable_id from
	 * a database; this overrides the GFF-based Name attribute to match.
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
		// Apply to all transcripts. Mark nameFromTmap so OutputFactory knows the
		// source (transcript_name is only emitted when the name came from TMAP,
		// matching Perl's ProtFuncTranscriptNameHTP plugin which queries the DB).
		int applied = 0;
		int visited = 0;
		int sampleMiss = 0;
		int sampleHit = 0;
		for (OverlapDetector<TranscriptModel> detector : perChromosomeDetectors.values()) {
			for (TranscriptModel tm : detector.getAll()) {
				visited++;
				String override = nameMap.get(tm.getTranscriptId());
				if (override != null) {
					tm.setName(override);
					tm.setNameFromTmap(true);
					applied++;
					if (sampleHit < 3) {
						Trace.log("TMAP.hit",
							"tid=%s name=%s", tm.getTranscriptId(), override);
						sampleHit++;
					}
				} else if (sampleMiss < 5) {
					Trace.log("TMAP.miss",
						"tid=%s gff_name=%s", tm.getTranscriptId(), tm.getName());
					sampleMiss++;
				}
			}
		}
		log.info("Transcript name map: visited={} applied={} (first 3 hits + 5 misses traced if -Dvep.trace=true)",
			visited, applied);
		log.info("Transcript name map: {} mappings loaded, {} names overridden", lineCount, applied);
	}

	public void logSummary() {
		log.info("Gene model loaded: {} genes, {} transcripts across {} chromosomes",
			geneCount, transcriptCount, perChromosomeDetectors.size());
	}
}
