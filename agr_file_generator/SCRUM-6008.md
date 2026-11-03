# SCRUM-6008 — Gene Descriptions File Generator

## Summary

The new `GeneDescriptionFileGenerator` reads the `gene_search_result` category from
`site_index` and writes per-MOD TSV + TXT + JSON output
(`GENE-DESCRIPTION-{TSV,TXT,JSON}_{MOD}.{tsv,txt,json}.gz`). MOD whitelist:
`FB`, `HUMAN`, `MGI`, `RGD`, `SGD`, `WB`, `XBXL`, `XBXT`, `ZFIN`. No COMBINED file —
matches FMS, where Gene Descriptions only emits per-species files.

This was the first concrete generator built on the new framework — the `customizeRow`
synthetic-field pattern, the field map convention, the per-MOD dispatch, and the
species-name → curie alias resolution all started here.

## Column coverage (TSV / TXT)

3 columns. All populated from ES.

| FMS column | ES path / source |
|---|---|
| Gene ID | `curie` |
| Gene Symbol | `symbol` |
| Gene Description | `geneDescription` (with fallback to `automatedGeneDescription` when the primary is empty — handled in `customizeRow` by patching `geneDescription` onto the JsonNode) |

JSON output uses `Format.JSON_MAPPED` — the row array is built from the same field map
(only the description fields), wrapped in `{metadata, data: [...]}` with the standard
metadata block.

TXT output (`Format.TXT`) currently emits the same single-line tab-delimited row body as
TSV. The legacy `agr_genedescriptions` Python tool used a 3-line-per-gene layout, but
that was a one-off convention from the retiring tool with no precedent in
`agr_file_generator`. We kept TXT identical to TSV body; will revisit if downstream
consumers complain.

## Per-doc transforms in `customizeRow`

```
Override on customizeRow patches geneDescription onto the JsonNode when:
  - geneDescription is empty AND
  - automatedGeneDescription is non-empty
This makes the field map's "Gene Description" -> "geneDescription" lookup hit either
the curated description or the fallback automated one, without needing a second column.

additionalSourceIncludes() returns ["automatedGeneDescription"] so ES returns the
fallback field even though the field map only references geneDescription.
```

## Filtering / dispatch

* **`taxonPath()`** = `"taxonId"` — most gene_search_result docs don't carry the curie
  directly. `resolveTaxonCurie` falls through to the `species` field (a name string
  like "Caenorhabditis elegans") and uses `SpeciesLookup.taxonForName(name)` to resolve
  the curie via the alias map.
* **Species alias map** is what makes SARS-CoV-2 (gene_search_result has
  `species: "SARS-CoV-2"`, but `Species.taxon.name = "Severe acute respiratory
  syndrome coronavirus 2"`) and SGD (similar `S288C` strain difference) resolve
  correctly.
* `additionalSourceIncludes()` adds `automatedGeneDescription` so the description
  fallback works.

## Verification against FMS reference

| MOD | Stage ES | Our output | FMS file | Notes |
|---|---|---|---|---|
| MGI | 632,909 | 632,910 | 90,777 | upstream stage data delta (~7×) |
| RGD | 61,463 | 61,463 | 61,463 | exact match |
| WB | 49,164 | 49,165 | 48,769 | within 1% |
| HUMAN | 44,586 | 44,587 | 44,314 | within 1% |
| ZFIN | 37,898 | 37,899 | 37,918 | within 0.1% |
| FB | 31,507 | 31,508 | 30,227 | within 4% |
| XBXL | 26,863 | 26,864 | 26,870 | within 0.1% |
| XBXT | 21,538 | 21,539 | 21,566 | within 0.1% |
| SGD | 8,097 | 8,099 | 7,167 | within 13% — alpha state |

All species line up with stage ES counts almost exactly. Discrepancies vs FMS reflect
upstream data state differences between alpha curation and the prod state when the FMS
files were generated.

### Sample row (ours, WB)

```
WB:WBGene00000001	aap-1	Enables protein kinase binding activity. Involved in dauer larval development; determination of adult lifespan; and insulin receptor signaling pathway. Part of phosphatidylinositol 3-kinase complex. Is expressed in anchor cell; intestine; and neurons. Human ortholog(s) of this gene implicated in several diseases, including SHORT syndrome; agammaglobulinemia 7; carcinoma (multiple); endometrial cancer (multiple); and immunodeficiency 36. Orthologous to several human genes including PIK3R2 (phosphoinositide-3-kinase regulatory subunit 2) and PIK3R3 (phosphoinositide-3-kinase regulatory subunit 3).
```

Identical shape to FMS sample row — same fields, same content, same tab delimitation.

## Output structure

| Check | Result |
|---|---|
| File header `#`-block | OK — same FMS conventions, fixes the `# Taxon IDs:` typo |
| `# Data type: Gene Descriptions` | OK |
| `# Data format:` | OK — `tsv` / `txt` / `json` |
| `# README:` | OK — paragraph describing how Alliance generates descriptions |
| `# Taxon IDs:` lists single taxon per file | OK |
| `# Species:` lists the species name | OK |
| `# Alliance Database Version:` | OK — from `ALLIANCE_RELEASE` env / ConfigHelper |
| Column header line (`Gene ID\tGene Symbol\tGene Description`) | OK |
| Blank line between header and data | OK |

## Output files

Per MOD, three formats:
- `GENE-DESCRIPTION-TSV_{MOD}.tsv.gz`
- `GENE-DESCRIPTION-TXT_{MOD}.txt.gz`
- `GENE-DESCRIPTION-JSON_{MOD}.json.gz`

JSON shape: `{metadata, data: [{"Gene ID": ..., "Gene Symbol": ..., "Gene Description": ...}, ...]}`
— the only generator using `Format.JSON_MAPPED`. All other generators use
`Format.JSON_RAW` (raw `_source` dump). The mapped shape was chosen because
gene_search_result docs carry many fields we don't need; the JSON would be cluttered
with unrelated payload.

(Note: the legacy `agr_genedescriptions` tool produced a stats-summary JSON shape
with `overall_properties`, `general_stats`, etc. That shape can't be derived from ES
alone and was a one-off from the retiring tool. Our row-based shape is a clean break.)

## Reused framework features

- Sliced parallel scroll (8 slices, 1000-doc buffer per slice)
- `_source` filtering — narrowed to the few fields we actually use
- MOD whitelist gating
- Species name-to-curie alias map (covers SARS-CoV-2 and SGD strain-name mismatch)
- `customizeRow` patches the JsonNode for description fallback

## Testing

```
ALLIANCE_RELEASE=9.0.0 \
ES_HOST=stage.cluster01.alliancegenome.org \
ES_INDEX=site_index \
CURATION_API_URL=https://alpha-curation.alliancegenome.org \
GENERATED_FILES_FOLDER=data \
SKIP_S3_UPLOAD=true \
java -jar target/agr_file_generator-jar-with-dependencies.jar GeneDescriptions
```

Expected outputs in `./data/`:
- `GENE-DESCRIPTION-{TSV,TXT,JSON}_{FB,HUMAN,MGI,RGD,SGD,WB,XBXL,XBXT,ZFIN}.{tsv,txt,json}.gz`

## Related tickets

- **SCRUM-6010** — Parent infrastructure ticket (framework setup)
- **SCRUM-1953** — Disease (used the same `customizeRow` synthetic-field pattern)
- **SCRUM-6007** — Expression
- **SCRUM-6009** — Orthology
- **SCRUM-4788/4789** — Molecular / Genetic Interactions
- **SCRUM-6019** — Variants VCF
- **SCRUM-6020** — Variants/Alleles
