# Phase 2: GFF3 Gene Model Loading

**Status**: Complete

## Goal

Parse a GFF3 file into an in-memory gene model with interval trees for fast genomic overlap queries.

## Key Dependency

htsjdk 4.2.0 provides:
- `htsjdk.tribble.gff.Gff3Codec` — full GFF3 parser with parent-child relationships
- `htsjdk.samtools.util.OverlapDetector` — interval tree for coordinate overlap queries
- `htsjdk.samtools.reference.IndexedFastaSequenceFile` — indexed FASTA access

No external GFF3 library needed.

## Files to Create

### 1. `model/GeneModel.java`
- Per-chromosome `OverlapDetector<TranscriptModel>`
- Gene ID -> gene symbol map
- Gene ID -> biotype map
- Method: `List<TranscriptModel> getOverlappingTranscripts(String chr, int start, int end)`

### 2. `model/TranscriptModel.java`
- Fields: id, name, geneId, geneSymbol, biotype, strand, chr, start, end
- Sorted lists of `ExonModel` and `CdsSegment`
- Exon count, intron boundaries (computed from exon gaps)
- Methods:
  - `String getExonNumber(int pos)` — returns "5/12" format
  - `String getIntronNumber(int pos)` — returns "4/11" format
  - `boolean isCoding()` — has CDS segments
  - `List<int[]> getIntronIntervals()` — computed from exon gaps

### 3. `model/ExonModel.java`
- Fields: ordinal, start, end, chr

### 4. `model/CdsSegment.java`
- Fields: start, end, phase, ordinal

### 5. `gff/Gff3GeneModelBuilder.java`
- Use `AbstractFeatureReader.getFeatureReader(gffPath, new Gff3Codec(), false)` to iterate features
- Build hierarchy: gene -> mRNA/transcript -> exon/CDS
- GFF3 types to handle:
  - Gene-level: `gene`, `pseudogene`
  - Transcript-level: `mRNA`, `transcript`, `lnc_RNA`, `ncRNA`, `tRNA`, `rRNA`
  - Sub-features: `exon`, `CDS`
- Attribute extraction: `ID`, `Parent`, `gene_id`, `Name`, `biotype`/`gene_biotype`
- For each transcript, collect and sort exons/CDSs by genomic position
- Register transcripts in the appropriate chromosome's OverlapDetector
- Handle both Ensembl-style IDs (`ENSDART00000163142`) and RefSeq-style (`NM_182905.6`)

### 6. `reference/ContigAccessionMap.java`
- Maps chromosome names (1, 2, X, chrI) to NC_* accessions (NC_000001.11, NC_007117.7)
- Source: GFF3 `##sequence-region` directives or FASTA sequence dictionary
- Needed for HGVSg notation (Phase 5)

## Performance Notes

- GFF3 model loaded once into memory, then VCF records streamed through
- `OverlapDetector` queries are O(log n + k) where k = overlapping features
- Largest genome (human) GFF3 model should fit in ~2-4GB

## Acceptance Criteria

- Load each species' GFF3 file successfully
- Query overlapping transcripts for known genomic positions
- Correctly identify gene symbols, transcript IDs, biotypes
- Unit tests with small GFF3 excerpts
