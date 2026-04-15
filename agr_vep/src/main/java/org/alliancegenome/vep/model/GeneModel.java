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

	// Populated by Gff3GeneModelBuilder: transcript_id -> last-seen raw Name from
	// GFF file order. Mirrors Perl's SplitInput.pm transcript_map table (one row
	// per non-exon GFF line with a transcript_id) combined with the plugin's
	// `while fetchrow_arrayref` last-wins iteration.
	private Map<String, String> gffTranscriptIdToName = new HashMap<>();

	private int transcriptCount = 0;
	private int geneCount = 0;

	public void setGffTranscriptIdToName(Map<String, String> map) {
		this.gffTranscriptIdToName = map;
	}

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
	 * Apply transcript_name overrides to every TranscriptModel, mirroring the Perl
	 * ProtFuncTranscriptNameHTP plugin's DB lookup.
	 *
	 * Perl path: SplitInput.pm walks the current GFF and inserts one (transcript_id,
	 * Name) row per non-exon line with a transcript_id attribute — no UNIQUE constraint
	 * — then ProtFuncTranscriptNameHTP.pm does `SELECT transcript_name FROM transcript_map
	 * WHERE transcript_id = ?` and keeps the LAST row seen. So when multiple GFF lines
	 * share the same transcript_id (e.g., SGD paralogs sharing RefSeq:NM_001179345.3
	 * across chrI and chrVIII, or FB transcripts that reference a shared RefSeq entry),
	 * every TranscriptModel with that transcript_id resolves to the GFF's *last*
	 * Name for that key — not the Name on its own GFF line.
	 *
	 * Java path:
	 *   1. gffTranscriptIdToName (filled during GFF parse, LinkedHashMap with last-wins)
	 *      holds the fresh per-transcript_id mapping.
	 *   2. The external TSV fills in any transcript_ids the GFF doesn't carry (edge
	 *      cases where the DB has entries not in the current GFF).
	 *   3. Every TranscriptModel whose transcript_id is in the merged map gets
	 *      tm.setName(mappedValue) — this may override the per-line Name that
	 *      Gff3GeneModelBuilder set earlier, which is what Perl does.
	 *
	 * This fixes the FB stale-TMAP problem (GFF wins over drifted TSV like
	 * CG33303-RB -> Ost1-RB) without breaking the SGD paralog case (both chrI and
	 * chrVIII variants end up with the same last-wins name, matching Perl).
	 */
	public void applyTranscriptNameMap(String tsvFilePath) {
		// Load the external TSV (may be null/missing — that's fine, GFF still wins).
		Map<String, String> tsvMap = new HashMap<>();
		int tsvLineCount = 0;
		if (tsvFilePath != null) {
			java.io.File file = new java.io.File(tsvFilePath);
			if (file.exists()) {
				try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
					String line;
					while ((line = br.readLine()) != null) {
						String[] parts = line.split("\t", 2);
						if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
							tsvMap.put(parts[0], parts[1]); // last entry wins for duplicates
							tsvLineCount++;
						}
					}
				} catch (Exception e) {
					log.warn("Failed to load transcript name map: {}", e.getMessage());
				}
			} else {
				log.warn("Transcript name map not found: {}", tsvFilePath);
			}
		}

		// Merge: start from TSV (fallback source), overlay with GFF (authoritative).
		Map<String, String> merged = new HashMap<>(tsvMap);
		merged.putAll(gffTranscriptIdToName);

		int visited = 0;
		int overriddenFromGff = 0;
		int overriddenFromTsvOnly = 0;
		int unset = 0;
		int sampleHit = 0;
		int sampleTsvOnly = 0;
		for (OverlapDetector<TranscriptModel> detector : perChromosomeDetectors.values()) {
			for (TranscriptModel tm : detector.getAll()) {
				visited++;
				String tid = tm.getTranscriptId();
				String resolved = merged.get(tid);
				if (resolved == null) {
					unset++;
					continue;
				}
				tm.setName(resolved);
				tm.setNameFromTmap(true);
				boolean fromGff = gffTranscriptIdToName.containsKey(tid);
				if (fromGff) {
					overriddenFromGff++;
					if (sampleHit < 3) {
						Trace.log("TMAP.gff_applied",
							"tid=%s name=%s tsv_value=%s",
							tid, resolved, tsvMap.get(tid));
						sampleHit++;
					}
				} else {
					overriddenFromTsvOnly++;
					if (sampleTsvOnly < 3) {
						Trace.log("TMAP.tsv_only",
							"tid=%s name=%s", tid, resolved);
						sampleTsvOnly++;
					}
				}
			}
		}
		log.info("Transcript name map: visited={} gff_applied={} tsv_only={} unset={} ({} TSV rows, {} GFF entries)",
			visited, overriddenFromGff, overriddenFromTsvOnly, unset, tsvLineCount, gffTranscriptIdToName.size());
	}

	public void logSummary() {
		log.info("Gene model loaded: {} genes, {} transcripts across {} chromosomes",
			geneCount, transcriptCount, perChromosomeDetectors.size());
	}
}
