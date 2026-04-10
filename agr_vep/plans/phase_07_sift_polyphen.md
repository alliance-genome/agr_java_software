# Phase 7: SIFT/PolyPhen + Gene-Level Consequence

**Status**: Not started

## Goal

Add SIFT/PolyPhen prediction scores, compute Gene_level_consequence, populate remaining CSQ fields.

## Files to Create

### 1. `plugin/SiftPolyPhenProvider.java` (interface)
```java
public interface SiftPolyPhenProvider {
    SiftPolyPhenResult getScores(String transcriptId, int proteinPosition, String refAA, String altAA);
}
```
- Returns: PolyPhen_prediction, PolyPhen_score, SIFT_prediction, SIFT_score

### 2. Implementation (TBD — API vs flat file)

**Option A: `ApiSiftPolyPhenProvider.java`**
- Connects to AGR curation API (like the `ProtFuncTranscriptNameHTP` VEP plugin)
- Caches results per-transcript to avoid repeated calls
- The VEP plugin uses `pass=V3pDbSlrmp2ln8#25` — this appears to be an API token

**Option B: Flat file provider**
- Read precomputed SIFT/PolyPhen scores from a file
- Avoids API dependency, faster

Decision deferred until we understand exactly where ProtFuncTranscriptNameHTP gets its data.

### 3. `plugin/NoOpSiftPolyPhenProvider.java`
- Returns empty strings — fallback when no score source is available

**Note:** SIFT/PolyPhen scores are present for ALL 7 species, not just HUMAN:

| Species | PolyPhen Coverage | SIFT Coverage |
|---------|------------------|---------------|
| HUMAN | 25.3% | 25.2% |
| SGD | 17.7% | 16.3% |
| WB | 15.7% | 14.3% |
| FB | 1.4% | 1.4% |
| MGI | 0.1% | 0.2% |
| RGD | 0.1% | 0.3% |
| ZFIN | 0.2% | 0.2% |

The `ProtFuncTranscriptNameHTP` plugin fetches scores for all species, coverage varies by coding density.

### 4. `plugin/GeneLevelConsequence.java`
- For each allele+gene combination across all transcripts:
  - Find the most severe consequence (using ConsequenceSeverity ranking)
  - Set this as field 22 (`Gene_level_consequence`) on ALL CSQ entries for that allele+gene
- This is how `--flag_pick_allele_gene` manifests in the output

### 5. `plugin/TranscriptNameResolver.java`
- Maps transcript feature IDs to display names
- Source: GFF3 `Name` attribute on transcript features
- Example: `ENSEMBL:ENSDART00000163142` -> transcript Name from GFF3
- Populates field 35 (`transcript_name`)

### 6. `reference/BamRefChecker.java` (optional)
- If `--bam` provided: opens BAM with `SamReaderFactory`, checks for RNA editing events
- `GIVEN_REF` (field 25): the REF from the VCF (minus padding base for indels)
- `USED_REF` (field 26): the actual reference from FASTA
- `BAM_EDIT` (field 27): populated if BAM evidence suggests an edit (currently zero occurrences across all species, but supported for future use)

## SIFT/PolyPhen Output Values

**PolyPhen predictions:**
- `probably_damaging` (score >= 0.908)
- `possibly_damaging` (score >= 0.446)
- `benign` (score < 0.446)

**SIFT predictions:**
- `deleterious` (score < 0.05)
- `tolerated` (score >= 0.05)

## Acceptance Criteria

- Gene_level_consequence correctly computed for all allele+gene pairs
- SIFT/PolyPhen scores populated for HUMAN missense variants
- SIFT/PolyPhen fields empty for non-HUMAN species
- transcript_name populated from GFF3
- GIVEN_REF/USED_REF correctly populated
- Full 38-field CSQ matches VEP output for HUMAN data
