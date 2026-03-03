# PostgreSQL Database Expert - Agent Memory

## Database Overview
- Two databases: `alpha` (staging) and `production` (prod) on localhost, user `postgres`
- Curation system with JOINED inheritance pattern (entity hierarchy tables)

## Key Tables: Gene-to-Gene Orthology
- See [orthology-tables.md](orthology-tables.md) for detailed schema
- `genetogeneorthology`: 4,381,640 rows (both alpha and production identical)
  - Has `obsolete`, `internal`, `subjectgene_id`, `objectgene_id`, audit columns
  - Currently 0 rows are obsolete or internal (100% pass filter)
- `genetogeneorthologygenerated`: 4,381,640 rows (1:1 with parent)
  - Has `isbestscore_id`, `isbestscorereverse_id`, `confidence_id`, `strictfilter`, `moderatefilter`
  - Only 3 distinct values for isbestscore, 2 for isbestscorereverse, 3 for confidence
- Three ManyToMany join tables (prediction methods): matched (9.8M), not_matched (29.5M), not_called (12.1M)
  - Avg per record: matched ~2.25, not_matched ~7.39, not_called ~2.83

## Table Sizes
- genetogeneorthologygenerated: 285MB table, 496MB total (with indexes)
- genetogeneorthology: 382MB table, 599MB total
- predictionmethodsnotmatched: 1,723MB total (largest join table)
- predictionmethodsmatched: 625MB, predictionmethodsnotcalled: 788MB

## ID Space
- IDs range from 121,063,246 to 202,172,923 (81M range, 5.4% density)
- Large gap: 133,269,750 to 200,000,000 (67M gap, likely from data reload)
- Small gaps of 2-7 between consecutive IDs in contiguous regions

## Performance Findings (Orthology Indexer)
- OFFSET/LIMIT at high offsets: ~4.5s per page fetch at offset 3M (degrades linearly)
- Fetch all IDs (index-only scan, no join): ~0.5s for 4.38M IDs
- Fetch all IDs (with join + filter): ~5.7s for 4.38M IDs
- Batch load 10 IDs by IN clause: ~2ms (nested loop index scan)
- Batch load ~340 IDs by range: ~3.7ms
- Join table batch load (matched): ~0.27ms for ~1000 rows
- No index on obsolete/internal columns (not needed since all rows are false)

## Indexer Pattern (Allele as Reference)
- AlleleSummaryCurationIndexer already uses batch-by-IDs pattern
- Pattern: getAllIds() -> partition into batches -> findByIds(batchIds)
- See `/Users/olinblodgett/git/agr_java_software/agr_indexer/src/main/java/org/alliancegenome/indexer/indexers/curation/AlleleSummaryCurationIndexer.java`

## Links
- [Orthology Table Details](orthology-tables.md)
