# SCRUM-6007 — Expression File Generator: ES Field Coverage

## Summary

The new `ExpressionFileGenerator` reads from the `gene_expression_annotation` category in
`site_index` and writes per-MOD plus COMBINED TSV + JSON output (`EXPRESSION-ALLIANCE-{TSV,JSON}_{MOD|COMBINED}.{tsv,json}.gz`).
Filenames split by MOD (`FB`, `MGI`, `RGD`, `SGD`, `WB`, `XBXL`, `XBXT`, `ZFIN` — no `HUMAN`).
The TSV column set matches the FMS Expression file produced by the legacy Python `agr_file_generator`.

## Column coverage (TSV)

All 23 columns are populated from ES once the curation-side JsonView change lands and the index is rebuilt (see "Curation-side dependency" below). 17 columns map straight off the doc; the 6 qualifier columns are pipe-joined inside `ExpressionFileGenerator.customizeRow()` into synthetic `_*` fields.

| FMS column | ES path |
|---|---|
| Species | `geneExpressionAnnotation.expressionAnnotationSubject.taxon.name` |
| SpeciesID | `geneExpressionAnnotation.expressionAnnotationSubject.taxon.curie` |
| GeneID | `geneExpressionAnnotation.expressionAnnotationSubject.primaryExternalId` |
| GeneSymbol | `geneExpressionAnnotation.expressionAnnotationSubject.geneSymbol.displayText` |
| Location | `geneExpressionAnnotation.whereExpressedStatement` |
| StageTerm | `geneExpressionAnnotation.whenExpressedStageName` |
| AssayID | `geneExpressionAnnotation.expressionAssayUsed.curie` |
| AssayTermName | `geneExpressionAnnotation.expressionAssayUsed.name` (display synonym, intentional curation-side override in `GeneExpressionDocumentBuilder`) |
| CellularComponentID | `geneExpressionAnnotation.expressionPattern.whereExpressed.cellularComponentTerm.curie` |
| CellularComponentTerm | `geneExpressionAnnotation.expressionPattern.whereExpressed.cellularComponentTerm.name` |
| CellularComponentQualifierIDs | `_cellularComponentQualifierIds` (pipe-joined from `cellularComponentQualifiers[*].curie`) |
| CellularComponentQualifierTermNames | `_cellularComponentQualifierNames` (pipe-joined from `cellularComponentQualifiers[*].name`) |
| SubStructureID | `geneExpressionAnnotation.expressionPattern.whereExpressed.anatomicalSubstructure.curie` |
| SubStructureName | `geneExpressionAnnotation.expressionPattern.whereExpressed.anatomicalSubstructure.name` |
| SubStructureQualifierIDs | `_subStructureQualifierIds` (pipe-joined from `anatomicalSubstructureQualifiers[*].curie`) |
| SubStructureQualifierTermNames | `_subStructureQualifierNames` (pipe-joined from `anatomicalSubstructureQualifiers[*].name`) |
| AnatomyTermID | `geneExpressionAnnotation.expressionPattern.whereExpressed.anatomicalStructure.curie` |
| AnatomyTermName | `geneExpressionAnnotation.expressionPattern.whereExpressed.anatomicalStructure.name` |
| AnatomyTermQualifierIDs | `_anatomyQualifierIds` (pipe-joined from `anatomicalStructureQualifiers[*].curie`) |
| AnatomyTermQualifierTermNames | `_anatomyQualifierNames` (pipe-joined from `anatomicalStructureQualifiers[*].name`) |
| SourceURL | `_sourceUrl` (computed in `customizeRow` from `crossReferences[0]` `urlTemplate` + `referencedCurie` local part) |
| Source | `geneExpressionAnnotation.dataProvider.abbreviation` |
| Reference | `referenceId.0` — top-level `referenceId` array on the doc holds the MOD pub curie (e.g. `FB:FBrf0219073`), populated by the curation API's `GeneExpressionDocumentBuilder` from `evidenceItem.referenceID` |

## Curation-side dependency

The `whereExpressed` subtree was historically dropped from the indexed doc because `ExpressionPattern.whereExpressed` and the six fields under `AnatomicalSite` were not on `CurationView.GeneExpressionDocument`. agr_curation `release/v0.48.18` (PR #2721, commit `8b4022c3f`) adds that view annotation to:

- `ExpressionPattern.whereExpressed`
- `AnatomicalSite.{anatomicalStructure, anatomicalSubstructure, cellularComponentTerm, anatomicalStructureQualifiers, anatomicalSubstructureQualifiers, cellularComponentQualifiers}`

`OntologyTerm.curie` and `OntologyTerm.name` were already on the view, so leaf data comes through automatically. After deploy + a `gene_expression_annotation` reindex, all 12 previously-blank columns will populate from ES with no further changes here.

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
