# Document Indexing Pattern: Batch-by-IDs

## Pattern Overview
Replace OFFSET/LIMIT pagination with:
1. `getAllIds()` - lightweight native SQL returning only IDs
2. `findByIds(List<Long> ids)` - batch fetch with WHERE IN clause

## Layers (using Allele as reference implementation)

### Interface (JAX-RS)
- Path: `/allele/document`
- `POST /ids` -> `SearchResponse<Long> getAllIds()`
- `POST /summary/byids` -> `SearchResponse<AlleleSummaryDocument> findSummaryByIds(@RequestBody List<Long> ids)`
- Uses `@JsonView(CurationView.AlleleSummaryDocument.class)`

### Controller
- Calls service.getAllIds(), wraps in SearchResponse
- Calls service.findByIds(), converts entities to ES documents via builder

### Service
- Thin delegation to DAO methods

### DAO
- `getAllXxxIds()`: native SQL `SELECT id FROM ... ORDER BY id` -> `List<Long>`
- `findByIds()`: native SQL with `WHERE id IN :ids` -> entities/DTOs
- Uses `entityManager.createNativeQuery(sql)` (inherited from BaseSQLDAO)

### Indexer (consumer side)
- Step 1: Call getAllIds() to get full list
- Step 2: Partition IDs into batches using `partition(allIds, bufferSize)`
- Step 3: Queue batch indices, thread workers call findByIds(batchIds)
- No more page number calculation or OFFSET queries

## Key Files (Allele reference)
- Interface: `curation_api/interfaces/document/AlleleDocumentInterface.java`
- Controller: `curation_api/controllers/document/AlleleDocumentController.java`
- Service: `curation_api/services/AlleleService.java` (getAllAlleleSummaryIds, findAllelesForSummaryByIds)
- DAO: `curation_api/dao/AlleleDAO.java` (native queries)
- Indexer: `agr_indexer/.../curation/AlleleSummaryCurationIndexer.java`

## Key Files (Orthology - needs migration)
- Interface: `curation_api/interfaces/document/GeneToGeneOrthologyDocumentInterface.java`
- Controller: `curation_api/controllers/document/GeneToGeneOrthologyDocumentController.java`
- Service: `curation_api/services/orthology/GeneToGeneOrthologyGeneratedService.java`
- DAO: `curation_api/dao/orthology/GeneToGeneOrthologyGeneratedDAO.java`
- Entity: `curation_api/model/entities/orthology/GeneToGeneOrthologyGenerated.java` (extends GeneToGeneOrthology -> AuditedObject)
- Builder: `curation_api/model/document/builders/GeneToGeneOrthologyDocumentBuilder.java`
- ES Doc: `curation_api/model/document/es/GeneToGeneOrthologyDocument.java`
- Indexer: `agr_indexer/.../curation/GeneToGeneOrthologyIndexer.java`
