# Phase 8: Performance Optimization

**Status**: Not started

## Goal

Handle MGI's 83M VCF records efficiently. Target: under 2 hours on a single machine.

## Strategies

### 1. Streaming VCF Processing
- Use `VCFFileReader.iterator()` — never load all records into memory
- Process one record at a time, write immediately

### 2. Pre-built Interval Trees
- One `OverlapDetector` per chromosome, built once during GFF3 loading
- O(log n + k) per query where k = overlapping features

### 3. Lazy CDS Computation
- Only reconstruct the full CDS sequence for a transcript when a coding variant hits it
- Cache the computed CDS per transcript (most transcripts will never be needed)
- The majority of variants are intergenic or intronic — no CDS work needed

### 4. FASTA Caching
- htsjdk's `IndexedFastaSequenceFile` uses the .fai index for random access
- Avoid re-reading the same regions

### 5. Batch SIFT/PolyPhen Lookups
- If using API: collect transcript IDs, batch-fetch scores
- If using flat file: load into memory-mapped structure

### 6. Threading Model
- Simple approach first: sequential single-threaded processing
- The annotation work is CPU-bound (not I/O-bound like ES indexing)
- Single-threaded avoids synchronization overhead on shared GeneModel
- If needed later: process different chromosomes in parallel (each has its own OverlapDetector)

### 7. Memory Budget
- GFF3 model for largest genome (human): ~2-4GB estimated
- VCF is streamed, not buffered
- FASTA accessed via index (no full load)
- JVM heap recommendation: `-Xmx8g`

## Performance Expectations

| Species | VCF Records | Expected Time |
|---------|-------------|---------------|
| SGD | 150K | < 1 min |
| WB | 1.7M | ~5 min |
| ZFIN | 3.2M | ~10 min |
| FB | 4.4M | ~15 min |
| HUMAN | 2.5M | ~10 min |
| RGD | 15.9M | ~30 min |
| MGI | 83M | < 2 hrs |

The simple intergenic/intron variants (vast majority) are very fast — just an interval tree lookup returning no overlaps. Only coding variants require CDS reconstruction and codon translation.

## Profiling

- Use `-Xlog:gc` to monitor GC pressure
- Profile with `async-profiler` if performance targets not met
- Key hotspots to watch: GFF3 loading time, interval tree query rate, CSQ string building

## Acceptance Criteria

- Process SGD end-to-end in under 1 minute
- Process MGI end-to-end in under 2 hours
- Peak memory usage under 8GB for any species
- No OOM errors on any species
