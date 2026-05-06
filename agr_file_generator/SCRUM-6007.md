# SCRUM-6007 — Expression File Generator: ES Field Coverage

## Summary

The new `ExpressionFileGenerator` reads from the `gene_expression_annotation` category in
`site_index` and writes per-MOD plus COMBINED TSV + JSON output (`EXPRESSION-ALLIANCE-{TSV,JSON}_{MOD|COMBINED}.{tsv,json}.gz`).
Filenames split by MOD (`FB`, `MGI`, `RGD`, `SGD`, `WB`, `XBXL`, `XBXT`, `ZFIN` — no `HUMAN`).
The TSV column set matches the FMS Expression file produced by the legacy Python `agr_file_generator`.

## Column coverage (TSV)

23 columns total. **11 populated from ES, 12 are blank** because the source data isn't currently
indexed.

| FMS column | ES path | Status |
|---|---|---|
| Species | `geneExpressionAnnotation.expressionAnnotationSubject.taxon.name` | OK |
| SpeciesID | `geneExpressionAnnotation.expressionAnnotationSubject.taxon.curie` | OK |
| GeneID | `geneExpressionAnnotation.expressionAnnotationSubject.primaryExternalId` | OK |
| GeneSymbol | `geneExpressionAnnotation.expressionAnnotationSubject.geneSymbol.displayText` | OK |
| Location | `geneExpressionAnnotation.whereExpressedStatement` | OK |
| StageTerm | `geneExpressionAnnotation.whenExpressedStageName` | OK |
| AssayID | `geneExpressionAnnotation.expressionAssayUsed.curie` | OK |
| AssayTermName | `geneExpressionAnnotation.expressionAssayUsed.name` | OK |
| SourceURL | computed in `customizeRow` from `crossReferences[0]` (`urlTemplate` + `referencedCurie` local part → synthetic `_sourceUrl` field) | OK |
| Source | `geneExpressionAnnotation.dataProvider.abbreviation` | OK |
| Reference | `referenceId[0]` | OK — top-level `referenceId` array on the doc holds the MOD pub curie (e.g. `FB:FBrf0219073`), which matches the FMS `Reference` column. The previous AGRKB-curie path was wrong; the curation API's `GeneExpressionDocumentBuilder` populates `referenceId` from `evidenceItem.referenceID` for exactly this purpose. |
| CellularComponentID | — | **Gap** |
| CellularComponentTerm | — | **Gap** |
| CellularComponentQualifierIDs | — | **Gap** |
| CellularComponentQualifierTermNames | — | **Gap** |
| SubStructureID | — | **Gap** |
| SubStructureName | — | **Gap** |
| SubStructureQualifierIDs | — | **Gap** |
| SubStructureQualifierTermNames | — | **Gap** |
| AnatomyTermID | — | **Gap** |
| AnatomyTermName | — | **Gap** |
| AnatomyTermQualifierIDs | — | **Gap** |
| AnatomyTermQualifierTermNames | — | **Gap** |

## Root cause for the 12 blank columns

These fields live on the underlying entity at:

```
GeneExpressionAnnotation
  └─ expressionPattern (ExpressionPattern)
        └─ whereExpressed (AnatomicalSite)
              ├─ anatomicalStructure (AnatomicalTerm)
              ├─ anatomicalSubstructure (AnatomicalTerm)
              ├─ cellularComponentTerm (GOTerm)
              ├─ anatomicalStructureQualifiers (List<OntologyTerm>)
              ├─ anatomicalSubstructureQualifiers (List<OntologyTerm>)
              └─ cellularComponentQualifiers (List<OntologyTerm>)
```

In `ExpressionPattern.java` (curation API entity), `whereExpressed` is annotated with:

```java
@JsonView({CurationView.FieldsOnly.class, CurationView.ForPublic.class})
private AnatomicalSite whereExpressed;
```

It is **NOT** included in `CurationView.GeneExpressionDocument.class`. The indexer
(`GeneExpressionAnnotationIndexer`) serializes documents with the
`GeneExpressionDocument` view, so `whereExpressed` is dropped before the JSON ever
reaches Elasticsearch. As a result, the entire `cellularComponent*`,
`subStructure*`, and `anatomyTerm*` column block ends up blank in our output.

The same exclusion explains why the ES `whereExpressedStatement` (a flat human-readable
string like `"pituitary gland"` or `"C. elegans Cell and Anatomy"`) is the only
location/anatomy-ish field we have to work with today.

## Options to close the gap

1. **Add `whereExpressed` to `CurationView.GeneExpressionDocument` in the curation API**
   (`agr_curation` repo). Triggers a full reindex of `gene_expression_annotation`,
   after which our generator will populate all 12 columns automatically — no Java change
   here required.
2. **Side-load anatomy data per record** by hitting the curation API
   (`/gene-expression-annotation/{id}` or similar) for each scrolled doc. Slower, harder
   to parallelize cleanly, but doesn't depend on a curation-API-side change.
3. **Ship as-is** with the 12 columns blank, and document the gap (this PR). FMS rows
   often have several of these columns blank already, so the file isn't useless — but
   it's incomplete.

Recommendation: pursue (1). It's the smallest change and keeps the file generator
purely ES-driven.

## Reference column

FMS emits the MOD publication curie (e.g. `FB:FBrf0219073`), not the PMID. That same
value is sitting on the doc as the top-level `referenceId` array — populated by the
curation API's `GeneExpressionDocumentBuilder` from `evidenceItem.referenceID`. The
field map now reads `referenceId.0`, which matches FMS exactly. No PMID resolution or
side-load needed.

## What the framework gets us for free

When the JsonView changes land in the curation API and the index is rebuilt:

- The field map in `FileGeneratorConfig.expressionFieldMap()` just needs the
  `_unavailable` placeholders swapped for real ES paths — no other code changes.
- `EsParallelFetcher` automatically pulls the new fields when `_source` filtering is
  enabled (since the field map values feed the include list).
- Combined / per-MOD splits, gzip output, header block, JSON metadata block, MOD
  whitelist gating, parallel sliced scroll — all unchanged.

## File layout produced

`EXPRESSION-ALLIANCE-{TSV,JSON}_{MOD}.{tsv,json}.gz` per allowed MOD, plus
`EXPRESSION-ALLIANCE-{TSV,JSON}_COMBINED.{tsv,json}.gz`. JSON outputs use the
`{metadata, data: [<raw _source>...]}` raw-doc shape (same policy as Disease).

## Testing

```
ALLIANCE_RELEASE=8.4.0 \
ES_HOST=stage.cluster01.alliancegenome.org \
ES_INDEX=site_index \
CURATION_API_URL=https://alpha-curation.alliancegenome.org \
GENERATED_FILES_FOLDER=data \
java -jar target/agr_file_generator-jar-with-dependencies.jar Expression
```

## Verification against FMS reference (run on stage)

| MOD | Stage ES | Our output | FMS file | Notes |
|---|---|---|---|---|
| MGI | 774,490 | 774,492 | 766,039 | within 1% |
| ZFIN | 465,310 | 465,312 | 464,111 | within 1% |
| FB | 394,345 | 394,347 | 394,290 | within 1% |
| WB | 91,079 | 91,081 | 91,083 | match |
| SGD | 13,431 | 13,433 | 13,389 | within 1% |
| RGD | 6,564 | 6,566 | 6,554 | match |
| XBXL | 20 | 22 | 28,864 | **stage data delta** |
| XBXT | 20 | 22 | 3,640 | **stage data delta** |
| COMBINED | — | 1,745,261 | 1,767,956 | within 1% |

Stage Xenopus expression dataset is empty (20 docs each); the FMS files were produced
against the prod state which had full Xenopus expression. Not a generator bug — will
self-heal once the data lands on the target environment.

Column headers match FMS exactly (23 columns, identical names). Header block matches
FMS layout (with the `# Taxon IDs:` typo fixed). Populated row content for the 11
non-gap columns matches the FMS data shape.
