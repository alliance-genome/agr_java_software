# Indexer Patterns Detail

## Base Indexer Flow
1. Constructor sets up ObjectMapper, ES BulkProcessor
2. `runIndex()` or `run()` calls abstract `index()`
3. `index()` creates queue of work items (page numbers or batch indices)
4. `initiateThreading(queue)` spawns N threads, each calling `startSingleThread(queue)`
5. Threads consume from queue, call API, call `indexDocuments()`

## Allele Batch-by-IDs Pattern (AlleleSummaryCurationIndexer)
- Interface: `AlleleDocumentInterface` (from curation_api JAR)
- Methods used: `getAllIds()` -> `SearchResponse<Long>`, `findSummaryByIds(List<Long>)` -> `SearchResponse<AlleleSummaryDocument>`
- Field: `List<List<Long>> idBatches` stored at class level
- Queue contains batch indices (0, 1, 2, ...) as strings
- Note: IDs are `Long` (database IDs), not String curies

## GeneToGeneOrthology Current OFFSET/LIMIT Pattern
- Interface: `GeneToGeneOrthologyDocumentInterface` (from curation_api JAR)
- Methods used: `findDocument(page, limit, params)` -> `SearchResponse<GeneToGeneOrthologyDocument>`
- Extra enrichment in `startSingleThread()`:
  - Adds `hasExpressionAnnotations` and `hasDiseaseAnnotations` booleans to each geneAnnotation map entry
  - Filters results against `allNeoGeneIDs` (checks objectGene identifier)
- Extra setup APIs called in `index()`:
  - `geneExpressionApi.geneExpressionAnnotationMap()` -> Set<String>
  - `geneDiseaseApi.geneDiseaseAnnotationMap()` -> Set<String>
  - `baseService.getAllNeoGeneIDs()` -> Set<String>

## GeneToGeneOrthologyDocumentInterface (from curation_api JAR)
- Package: `org.alliancegenome.curation_api.interfaces.document`
- Currently provides: `findDocument(page, limit, params)`
- Will need new methods: `getAllIds()` and `findByIds(List<Long>)` (to be added on curation API side)

## GeneToGeneOrthologyService (local helper, NOT used by main indexer)
- Uses `GeneToGeneOrthologyGeneratedInterface` (different from `GeneToGeneOrthologyDocumentInterface`)
- Calls `findForPublic(page, limit, params)` on raw entity, not document
- Not directly relevant to the indexer conversion
