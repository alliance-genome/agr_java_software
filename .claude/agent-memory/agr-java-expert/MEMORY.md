# AGR Java Software - Agent Memory

## Project Structure
- Multi-module Maven project (Java 21, Quarkus 3.13.3)
- Key modules: agr_java_core, agr_indexer, agr_api, agr_cacher, agr_elasticsearch_util, agr_variant_indexer
- Curation API dependency: `org.alliancegenome:agr_curation_api` version `v0.45.11`
- ES version: 7.17.28, Neo4j OGM 4.0.8

## Indexer Architecture (see [indexer-patterns.md](indexer-patterns.md))
- Base class: `Indexer` (abstract Thread) in `agr_indexer/src/main/java/org/alliancegenome/indexer/indexers/Indexer.java`
- Provides: `partition()`, `initiateThreading()`, `indexDocuments()`, `indexDocument()`, `customizeObjectMapper()`
- Config: `IndexerConfig` enum defines threadCount, bufferSize, bulkActions, concurrentRequests, bulkSize per indexer
- REST proxy: Uses `si.mazi.rescu.RestProxyFactory` with curation API interfaces
- REST config: `RestConfig` sets Jackson ObjectMapper with JavaTimeModule, NON_NULL/NON_EMPTY, auth token

## Two Pagination Patterns
1. **OFFSET/LIMIT (old)**: `findDocument(page, limit, params)` - used by GeneToGeneOrthology, Variant, Disease, GeneExpression, etc.
2. **Batch-by-IDs (new)**: `getAllIds()` -> `partition()` -> `findSummaryByIds(batchIds)` - used by AlleleSummaryCurationIndexer

## GeneToGeneOrthologyDocument - Two Versions
- **Curation API**: `org.alliancegenome.curation_api.model.document.es.GeneToGeneOrthologyDocument` (from JAR) - used by indexer
- **Local**: `org.alliancegenome.api.entity.GeneToGeneOrthologyDocument` (in agr_java_core) - used by API/query side
- Local one has: stringencyFilter, geneAnnotations (List<Map>), geneAnnotationsMap, geneToGeneOrthologyGenerated

## Key Services
- `BaseService`: Provides `getAllNeoGeneIDs()`, `getAllNeoAlleleIDs()` etc. with file caching
- Enrichment APIs: `GeneExpressionAnnotationCrudInterface.geneExpressionAnnotationMap()`, `GeneDiseaseAnnotationCrudInterface.geneDiseaseAnnotationMap()`
