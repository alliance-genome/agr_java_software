# Phase 6: Splice Variant Classification

**Status**: Complete

## Goal

Correctly classify splice-site and splice-region variants with compound `&`-delimited consequences.

## Files to Create

### 1. `annotation/SpliceAnnotator.java`

Determines splice-related consequences based on variant position relative to intron/exon boundaries.

**Splice site definitions (relative to intron):**

| Position | Consequence | IMPACT |
|----------|-------------|--------|
| +1, +2 (5' end of intron) | `splice_donor_variant` | HIGH |
| -1, -2 (3' end of intron) | `splice_acceptor_variant` | HIGH |
| +3 to +6 (5' end of intron) | `splice_donor_region_variant` | LOW |
| +5 specifically | `splice_donor_5th_base_variant` | LOW |
| Exon: last 3 bases or first 3 bases | `splice_region_variant` | LOW |
| Intron: +3 to +8 or -3 to -8 | `splice_region_variant` | LOW |
| ~3-40 bases upstream of 3' splice site | `splice_polypyrimidine_tract_variant` | LOW |

**Compound consequence combinations found in data:**
- `splice_region_variant&intron_variant`
- `splice_region_variant&synonymous_variant`
- `splice_region_variant&5_prime_UTR_variant`
- `splice_region_variant&non_coding_transcript_exon_variant`
- `splice_region_variant&intron_variant&non_coding_transcript_variant`
- `splice_polypyrimidine_tract_variant&intron_variant`
- `splice_polypyrimidine_tract_variant&intron_variant&non_coding_transcript_variant`
- `splice_region_variant&splice_polypyrimidine_tract_variant&intron_variant`
- `splice_region_variant&splice_polypyrimidine_tract_variant&intron_variant&non_coding_transcript_variant`
- `splice_donor_region_variant&intron_variant`
- `splice_donor_region_variant&intron_variant&non_coding_transcript_variant`
- `splice_donor_5th_base_variant&intron_variant`
- `splice_donor_5th_base_variant&intron_variant&non_coding_transcript_variant`
- `splice_donor_variant&coding_sequence_variant`
- `splice_donor_variant&non_coding_transcript_variant`
- `splice_acceptor_variant&non_coding_transcript_variant`
- `missense_variant&splice_region_variant`
- `stop_gained&splice_region_variant`

## Integration

- Called from `TranscriptAnnotator` after determining basic location
- Splice consequences are added as compound terms via `&` to the base consequence
- The most severe consequence in the compound determines the IMPACT level

## Acceptance Criteria

- All splice-related compound consequences from the data are correctly produced
- Splice donor/acceptor positions calculated correctly for both plus and minus strand transcripts
- Polypyrimidine tract region correctly identified
- IMPACT levels correct (HIGH for donor/acceptor, LOW for region variants)
