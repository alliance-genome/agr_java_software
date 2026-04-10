# Phase 3: Non-Coding Consequence Annotation

**Status**: Complete

## Goal

Produce correct CSQ entries for non-coding variants: intergenic, intronic, UTR, and non-coding transcript variants.

## Files to Create

### 1. `annotation/VariantAnnotator.java`
- Main orchestrator: `VariantContext` -> `List<CsqEntry>`
- For each ALT allele:
  - Convert VCF allele to VEP allele representation:
    - SNP: alt base directly (e.g., `A`)
    - Deletion: `-` (VCF `AAGGAA/A` -> CSQ `-`)
    - Insertion: inserted bases only (VCF `A/ATCG` -> CSQ `TCG`)
  - Find overlapping transcripts from GeneModel
  - If none: produce `intergenic_variant` CSQ entry
  - If overlapping: delegate to `TranscriptAnnotator` for each transcript
- Group results by allele+gene for Gene_level_consequence (Phase 7)

### 2. `annotation/TranscriptAnnotator.java`
- Given variant + TranscriptModel, determine location:
  - In CDS exon -> delegate to CodingAnnotator (Phase 4)
  - In UTR exon (before CDS start) -> `5_prime_UTR_variant`
  - In UTR exon (after CDS end) -> `3_prime_UTR_variant`
  - In intron -> `intron_variant`
  - In non-coding transcript exon -> `non_coding_transcript_exon_variant`
  - In non-coding transcript intron -> `intron_variant&non_coding_transcript_variant`
- Compute exon/intron number ("5/12" format) via TranscriptModel

### 3. `annotation/ConsequenceClassifier.java`
- Maps variant location + type to SO consequence terms
- Handles `&`-delimited compound consequences
- All consequence types from the data:
  - `intergenic_variant` (MODIFIER)
  - `intron_variant` (MODIFIER)
  - `3_prime_UTR_variant` (MODIFIER)
  - `5_prime_UTR_variant` (MODIFIER)
  - `non_coding_transcript_exon_variant` (MODIFIER)
  - `non_coding_transcript_variant` (MODIFIER, always in compound)

### 4. `annotation/ConsequenceSeverity.java`
- VEP severity ranking (highest to lowest):
  ```
  transcript_ablation > splice_acceptor_variant > splice_donor_variant >
  stop_gained > frameshift_variant > stop_lost > start_lost >
  inframe_insertion > inframe_deletion > missense_variant >
  splice_region_variant > stop_retained_variant > synonymous_variant >
  non_coding_transcript_exon_variant > intron_variant >
  5_prime_UTR_variant > 3_prime_UTR_variant > intergenic_variant
  ```
- IMPACT mapping: HIGH, MODERATE, LOW, MODIFIER

### 5. `csq/CsqBuilder.java`
- Builds the 38-field pipe-delimited string
- For intergenic: most fields empty, populate Allele, Consequence, IMPACT, Gene_level_consequence, HGVSg, GIVEN_REF, USED_REF, Genomic_start/end_position

### 6. `csq/CsqHeaderWriter.java`
- `##INFO=<ID=CSQ,...>` with 38-field Format description
- `##VEP="v111" time="..."` version header
- `##VEP-command-line='...'` command header

### 7. Update `vcf/VcfAnnotationPipeline.java`
- Wire up VariantAnnotator between VCF read and write
- Add CSQ INFO field to each VariantContext before writing
- Handle multi-allelic sites: comma-separated CSQ entries per allele

## Key Behavioral Notes

- `--distance 0`: do NOT annotate upstream/downstream, only features that overlap the variant
- Intergenic variants have empty SYMBOL, Gene, Feature_type, Feature, BIOTYPE, SOURCE fields
- Gene_level_consequence for intergenic is `intergenic_variant`
- SOURCE field value is the GFF filename (e.g., `ZFIN_GFF.refseq.gff.gz`) for transcript-overlapping variants, empty for intergenic

## Acceptance Criteria

- Correctly annotates intergenic variants (no overlapping features)
- Correctly annotates intron variants with intron number
- Correctly annotates UTR variants (3' and 5')
- Correctly annotates non-coding transcript variants
- CSQ field format matches VEP output exactly
- VCF headers include proper CSQ definition
