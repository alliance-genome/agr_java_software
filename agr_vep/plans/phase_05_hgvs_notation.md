# Phase 5: HGVS Notation Generation

**Status**: Complete

## Goal

Generate HGVSc (c. coding), HGVSp (p. protein), and HGVSg (g. genomic) notations matching VEP output.

## Files to Create

### 1. `hgvs/HgvsGenerator.java`
- Facade coordinating all three HGVS types
- Respects `--shift_hgvs 0` (no 3' shifting)
- Respects `--remove_hgvsp_version` (strip version from protein references)

### 2. `hgvs/HgvsCodingNotation.java`
- Format: `<transcript_id>:c.<notation>`
- Transcript ID includes source prefix: `ENSEMBL:ENST00000367975.7`

**Notation patterns:**
- CDS substitution: `c.520A>G`
- CDS deletion: `c.1234_1236del`
- CDS insertion: `c.541_542insATG`
- Intronic: `c.429-276del`, `c.541+394_541+395insC`
- 5'UTR: `c.-10A>G`
- 3'UTR: `c.*170T>C`
- Intronic relative to UTR: `c.-10+6_-10+7insT`

**Position calculation:**
- CDS positions: 1-based from ATG start codon
- Intronic positions: relative to nearest exon boundary (e.g., `c.429-276` means 276 bases before exon boundary at CDS position 429)
- UTR positions: negative for 5'UTR (relative to start), `*` prefix for 3'UTR (relative to stop)

### 3. `hgvs/HgvsProteinNotation.java`
- Format varies by species:
  - HUMAN: `ENSP00000356953:p.Gly140Glu` (with protein accession)
  - Other species: `:p.Ile174Val` (colon prefix, no accession)
- Three-letter amino acid codes
- Special cases:
  - Synonymous: `p.Arg85%3D` (URL-encoded `=` sign)
  - Stop gained: `p.Cys7Ter`
  - Frameshift: `p.Gly140fs` or `p.Gly140GlufsTer5`
- `--remove_hgvsp_version`: strip version number from protein accession

### 4. `hgvs/HgvsGenomicNotation.java`
- Format: `<accession>:g.<notation>`
- Uses NC_* accession from `ContigAccessionMap`
- Examples:
  - Substitution: `NC_000001.11:g.161362342G>A`
  - Deletion: `NC_000001.11:g.12345_12347del`
  - Insertion: `NC_000001.11:g.12345_12346insATG`
  - Delins: `NC_000001.11:g.12345_12347delinsATGC`

## Key Implementation Details

- `--shift_hgvs 0`: do NOT apply 3' shifting. Report the variant exactly where it falls.
- Protein accession comes from GFF3 `protein_id` attribute on CDS features
- For non-coding transcripts: only HGVSc uses `n.` notation instead of `c.`
  - Example: `ENSEMBL:ENST00000470743.5:n.553G>A`

## Acceptance Criteria

- HGVSc matches VEP for SNPs, insertions, deletions, delins
- HGVSc intronic notation correct (relative to exon boundaries)
- HGVSp three-letter codes correct, synonymous uses `%3D`
- HGVSg uses correct NC_* accessions per species
- Non-coding transcripts use `n.` notation
- HUMAN protein accessions included, other species omitted
