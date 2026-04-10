# agr_vep — Java VEP Reimplementation

## Overview

Standalone Java module in `agr_java_software` that replaces Ensembl VEP (Perl, v111) for AGR's variant consequence annotation pipeline.

**Input**: VCF + GFF3 + FASTA + BAM (optional) per species, all from FMS
**Output**: Annotated VCF with CSQ INFO field (38 pipe-delimited fields)

All 7 AGR species (SGD, FB, WB, ZFIN, HUMAN, RGD, MGI) use identical VEP flags. htsjdk 4.2.0 (already a project dependency) provides VCF I/O, GFF3 parsing, FASTA access, BAM reading, and interval trees.

## Input File Sources (FMS Data Types)

| FMS Data Type | VEP Input | Required | Description |
|---------------|-----------|----------|-------------|
| `HTVCF` | VCF | Yes | High-throughput variant calls per species |
| `GFF` | GFF3 | Yes | RefSeq gene models per species |
| `FASTA` | FASTA | Yes | RefSeq reference genome per species |
| `MOD-GFF-BAM-MODEL` + `MOD-GFF-BAM-KNOWN` | BAM | Optional | Merged into a single BAM per species for RNA editing detection |

The two FMS BAM files (model + known) are merged into one BAM per species before use. Only 4 species have real BAMs: **ZFIN, HUMAN, RGD, MGI**. The other 3 (SGD, FB, WB) use a dummy BAM file. Full scan of current VEP output shows zero BAM_EDIT values across all species, but BAM support is retained for completeness.

## Phase Tracker

| Phase | Description | Status |
|-------|-------------|--------|
| [Phase 1](phase_01_module_skeleton.md) | Module skeleton + VCF pass-through | Complete |
| [Phase 2](phase_02_gff3_gene_model.md) | GFF3 gene model loading | Complete |
| [Phase 3](phase_03_noncoding_annotation.md) | Non-coding consequence annotation | Complete |
| [Phase 4](phase_04_coding_annotation.md) | Coding variant annotation | Complete |
| [Phase 5](phase_05_hgvs_notation.md) | HGVS notation generation | Complete |
| [Phase 6](phase_06_splice_classification.md) | Splice variant classification | Complete |
| [Phase 7](phase_07_sift_polyphen.md) | SIFT/PolyPhen + gene-level consequence | Not started |
| [Phase 8](phase_08_performance.md) | Performance optimization | Not started |
| [Phase 9](phase_09_future_improvements.md) | Future improvements / backlog | Ongoing |
| Testing | Parameterized consequence tests per MOD | In progress |

## Current Testing Status (2026-04-08)

Test framework using parameterized JUnit tests. Each MOD has test VCF files (up to 10,000 lines per consequence type) extracted from ORIG VEP output.

- **VepAnnotationTest**: 12/12 pass
- **SgdConsequenceTest**: 35/36 pass (1 fail — start_lost&inframe_deletion)
- **WbConsequenceTest**: 118/128 pass (10 fail)
- **ZfinConsequenceTest**: 88/92 pass (4 fail)
- **RgdConsequenceTest**: 119/134 pass (15 fail)
- **FbConsequenceTest**: 78/105 pass (27 fail)
- **MgiConsequenceTest**: @Ignore (24GB heap, 3min load) — 90 test files
- **HumanConsequenceTest**: @Ignore (large GFF) — 94 test files

Run: `mvn test -pl agr_vep -am -ntp` (~65 sec)

### SIFT/PolyPhen Predictions (Phase 7)

Memory-mapped prediction lookup implemented. Match rates against ORIG VEP output:
- **SGD**: 39,119/39,119 (100%)
- **WB**: 333,125/333,133 (100%)
- **FB**: 86,344/87,632 (98.5%)

### FB CDS Issue

FB transcript FB:FBtr0070094 (minus strand) has a 19-base first CDS segment (487462-487480, phase 0) that starts with ATG. VEP excludes this segment from its CDS — removing it gives exact position match (protein pos 1192 vs our 1198). This 18-nucleotide offset affects all coding annotations for FB transcripts with this pattern. Root cause needs investigation in VEP's Ensembl GFF parser vs our Gff3GeneModelBuilder.

### Full Output Comparison (SGD — Perl VEP vs Java VEP)

All 150,513 variants present in both outputs. Key differences:

| Category | Affected | Priority | Description |
|----------|----------|----------|-------------|
| Transcript ordering | ~88K | **P1** | Multi-transcript variants have CSQ entries in different order. Cascades to SYMBOL, Gene, STRAND, Consequence, IMPACT, EXON, PolyPhen, SIFT differences |
| HGVSg insertion off-by-1 | ~64K | **P2** | Java adds 1 to both start/end for insertions (82_83 → 83_84) |
| HGVSp for frameshifts | 15,528 | **P3** | Java: p.?60?fsTer?, Perl: p.Phe61SerfsTer? (proper notation) |
| Codons/AA empty for indels | ~15K | **P3** | Java doesn't populate Codons/Amino_acids for insertions/deletions |
| cDNA/CDS/Protein position | ~15K | **P4** | Java: single position (309), Perl: range (309-310) for insertions |
| HGVSg accession version | 239,443 | **P4** | Java: NC_001133, Perl: NC_001133.9 (missing version suffix) |
| GIVEN_REF/USED_REF | 7,255 | **P5** | Perl: empty for insertions, Java: - |
| Chromosome naming | all | **P5** | chrmt vs chrMt |
| Multi-allelic handling | 568 | **P5** | Entry count (52) and allele trimming (516) edge cases |

## Package Structure

```
org.alliancegenome.vep/
  Main.java                         -- CLI entry point
  config/
    VepConfig.java                  -- input file paths, MOD name, options
  model/
    GeneModel.java                  -- per-chromosome OverlapDetector<TranscriptModel>
    TranscriptModel.java            -- transcript with sorted exons, CDS segments, strand
    ExonModel.java                  -- exon with ordinal
    CdsSegment.java                 -- CDS segment with phase
  gff/
    Gff3GeneModelBuilder.java       -- htsjdk Gff3Codec -> GeneModel
  reference/
    ReferenceGenome.java            -- wraps IndexedFastaSequenceFile
    ContigAccessionMap.java         -- chr name -> NC_* accession mapping
    BamRefChecker.java              -- optional --bam for RNA editing detection
  annotation/
    VariantAnnotator.java           -- orchestrator: VariantContext -> List<CsqEntry>
    TranscriptAnnotator.java        -- annotate variant against one transcript
    ConsequenceClassifier.java      -- position -> SO consequence terms
    ConsequenceSeverity.java        -- severity ranking, IMPACT, flag_pick_allele_gene
    CodingAnnotator.java            -- codon context, amino acid changes
    SpliceAnnotator.java            -- splice region/donor/acceptor classification
  hgvs/
    HgvsGenerator.java              -- facade for c., p., g. notation
    HgvsCodingNotation.java         -- c. notation
    HgvsProteinNotation.java        -- p. notation
    HgvsGenomicNotation.java        -- g. notation (NC_*:g.POS...)
  bio/
    CodonTable.java                 -- standard genetic code
    AminoAcid.java                  -- 1-letter / 3-letter conversion
    SequenceUtils.java              -- reverse complement, etc.
  plugin/
    SiftPolyPhenProvider.java       -- interface for SIFT/PolyPhen scores
    GeneLevelConsequence.java       -- most severe consequence per allele+gene
    TranscriptNameResolver.java     -- GFF3 Name attribute -> display name
  csq/
    CsqBuilder.java                 -- builds 38-field pipe-delimited string
    CsqHeaderWriter.java            -- ##INFO=<ID=CSQ,...> header
  vcf/
    VcfAnnotationPipeline.java      -- read VCF -> annotate -> write VCF
```

## Key VEP Behaviors to Match

| Flag | Behavior |
|------|----------|
| `--distance 0` | Only annotate variants overlapping features, no upstream/downstream |
| `--flag_pick_allele_gene` | Pick best consequence per allele+gene (populates Gene_level_consequence) |
| `--shift_hgvs 0` | No 3' shifting of HGVS notation |
| `--check_ref` | Verify REF allele matches FASTA reference |
| `--numbers` | Exon/intron numbers as "5/12" format |

## CSQ Field Format (38 fields)

```
Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON|
HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|
Existing_variation|DISTANCE|STRAND|FLAGS|Gene_level_consequence|SYMBOL_SOURCE|
HGNC_ID|GIVEN_REF|USED_REF|BAM_EDIT|SOURCE|HGVS_OFFSET|HGVSg|
PolyPhen_prediction|PolyPhen_score|SIFT_prediction|SIFT_score|transcript_name|
Genomic_end_position|Genomic_start_position|<MOD>_GFF.refseq.gff.gz
```

## Analysis Documents

Detailed analysis of input and output files:
- `HTVCF_Analysis_9.0.0.md` — input VCF analysis (125M records across 7 species)
- `HTPOSTVEPVCF_Analysis_9.0.0.md` — VEP output analysis (111M records, consequence distributions, SIFT/PolyPhen coverage)

### Key Data Findings

- **ZFIN assembly mismatch**: HTVCF uses GRCz10, VEP ran against GRCz11 — 81% of records dropped. Upstream data issue.
- **SIFT/PolyPhen is present for ALL species** (not just HUMAN): HUMAN 25%, SGD 18%, WB 16%, FB 1.4%, MGI/RGD/ZFIN <1%
- **HUMAN CSQ expansion**: ~37 annotations per record (ClinVar variants span many transcripts)
- **Input format varies**: VCF 4.1 vs 4.2, different chr naming (numeric, roman numeral, arm notation), different INFO fields
- **Record preservation is excellent** except ZFIN: FB/RGD/SGD/WB have zero loss

## Test Data

- Input: `/Users/olinblodgett/Desktop/FMS/HTP/<MOD>.vcf`
- Expected: `/Users/olinblodgett/Desktop/FMS/HTP/<MOD>.vep.vcf.gz`
- 7 species, use `tabix` for random-region sampling
- Start validation with SGD (smallest, 150K records), work up to MGI (83M records)
