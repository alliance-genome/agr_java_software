# Phase 4: Coding Variant Annotation

**Status**: Complete

## Goal

Handle coding variants: missense, synonymous, stop_gained, start_lost, stop_retained, frameshift, inframe indels.

## Files to Create

### 1. `bio/CodonTable.java`
- Standard genetic code: 64 codons -> 20 amino acids + stop
- `char translate(String codon)` — returns single-letter AA or '*' for stop
- `String threeLetterCode(char aa)` — for HGVS protein notation

### 2. `bio/AminoAcid.java`
- Single letter to three letter mapping (e.g., 'I' -> "Ile", 'V' -> "Val")
- Handle '*' as "Ter"

### 3. `bio/SequenceUtils.java`
- `reverseComplement(String seq)` for minus-strand transcripts
- Case normalization

### 4. `annotation/CodingAnnotator.java`
- Given variant position in a transcript's CDS:
  1. Compute CDS-relative position (accounting for strand, CDS segment ordering, phase)
  2. Extract reference codon context from CDS (the 3 bases surrounding the variant)
  3. Apply variant to get alternate codon(s)
  4. Translate both ref and alt codons
  5. Classify consequence

**Classification logic:**
- Same AA -> `synonymous_variant`
- Different AA -> `missense_variant`
- Alt is stop codon -> `stop_gained`
- Ref is start codon (ATG) and alt is not -> `start_lost`
- Ref is stop codon and alt is different stop -> `stop_retained_variant`
- Indel, length % 3 == 0, no frameshift -> `inframe_insertion` or `inframe_deletion`
- Indel, length % 3 != 0 -> `frameshift_variant`

**Output fields:**
- `Amino_acids`: e.g., "I/V" (ref/alt single-letter)
- `Codons`: e.g., "Atc/Gtc" (variant base uppercase, context lowercase)
- `CDS_position`: position within CDS
- `Protein_position`: CDS_position / 3 (1-based)
- `cDNA_position`: position within transcript

### 5. `reference/ReferenceGenome.java`
- Wraps `IndexedFastaSequenceFile` (htsjdk)
- `String getSequence(String chr, int start, int end)` — fetches bases from reference
- Used for CDS reconstruction and codon context

### 6. `reference/BamRefChecker.java` (optional)
- Opens BAM with `SamReaderFactory` if `--bam` provided
- Checks if VCF REF matches FASTA ref
- Populates GIVEN_REF, USED_REF, BAM_EDIT fields
- If no BAM provided, GIVEN_REF/USED_REF come from FASTA only, BAM_EDIT stays empty

## CDS Reconstruction Logic

**Plus strand transcript:**
1. Sort CDS segments by start position
2. Concatenate sequences in order
3. Apply phase offset from first CDS segment

**Minus strand transcript:**
1. Sort CDS segments by start position (reverse order)
2. Reverse complement each segment's sequence
3. Concatenate
4. Apply phase offset from first CDS segment (which is the 3'-most genomic segment)

## VCF Allele to VEP Allele Conversion

Strip common VCF padding to get the actual variant:
- `REF=AAGGAA, ALT=A` -> deletion of `AGGAA`, CSQ Allele = `-`
- `REF=A, ALT=ATCG` -> insertion of `TCG`, CSQ Allele = `TCG`
- `REF=A, ALT=T` -> substitution, CSQ Allele = `T`

## Acceptance Criteria

- missense_variant: correct amino acid change, codons, positions
- synonymous_variant: same AA, correct codon display
- stop_gained: `*` / "Ter" in amino acid field
- start_lost: variant at ATG start codon
- frameshift_variant: indel not multiple of 3
- inframe_deletion/insertion: indel multiple of 3
- All fields match VEP output for coding variants across all species
