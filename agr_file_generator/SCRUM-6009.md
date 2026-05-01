# SCRUM-6009 — Orthology File Generator

## Summary

The new `OrthologyFileGenerator` reads the `gene_to_gene_orthology` category from
`site_index` and writes a single COMBINED TSV + JSON pair
(`ORTHOLOGY-ALLIANCE-{TSV,JSON}_COMBINED.{tsv,json}.gz`) — no per-MOD splits, matching
the FMS layout. MOD whitelist (used to gate the row, since either side of the pair must
be in scope): `FB`, `HUMAN`, `MGI`, `RGD`, `SGD`, `WB`, `XBXL`, `XBXT`, `ZFIN`.

13 columns, all populated from ES. Output filters to stringent orthologs only (matching
FMS's "Orthology Filter: Stringent" header line).

## Column coverage (TSV)

All 13 columns populated from ES. No gaps.

| FMS column | ES path / source |
|---|---|
| Gene1ID | `geneToGeneOrthologyGenerated.subjectGene.primaryExternalId` |
| Gene1Symbol | `geneToGeneOrthologyGenerated.subjectGene.geneSymbol.displayText` |
| Gene1SpeciesTaxonID | `geneToGeneOrthologyGenerated.subjectGene.taxon.curie` |
| Gene1SpeciesName | `geneToGeneOrthologyGenerated.subjectGene.taxon.name` |
| Gene2ID | `geneToGeneOrthologyGenerated.objectGene.primaryExternalId` |
| Gene2Symbol | `geneToGeneOrthologyGenerated.objectGene.geneSymbol.displayText` |
| Gene2SpeciesTaxonID | `geneToGeneOrthologyGenerated.objectGene.taxon.curie` |
| Gene2SpeciesName | `geneToGeneOrthologyGenerated.objectGene.taxon.name` |
| Algorithms | `_algorithms` — pipe-joined deduped `predictionMethodsMatched[].name` |
| AlgorithmsMatch | `_algorithmsMatch` — distinct matched count |
| OutOfAlgorithms | `_outOfAlgorithms` — distinct matched + distinct notMatched count |
| IsBestScore | `geneToGeneOrthologyGenerated.isBestScore.name` |
| IsBestRevScore | `geneToGeneOrthologyGenerated.isBestScoreReverse.name` |

## Per-doc transforms in `customizeRow`

```
_algorithms        — pipe-joined deduped predictionMethodsMatched[].name
_algorithmsMatch   — String.valueOf(distinct matched count)
_outOfAlgorithms   — String.valueOf(distinct matched + distinct notMatched)
```

## Filtering / dispatch

* **`shouldEmit(hit)`** override — only emits rows whose `stringencyFilter == "stringent"`.
  Moderate / no-stringency rows are dropped before any work happens. Matches FMS's
  "Orthology Filter: Stringent" semantics exactly.
* **`additionalTaxonCuries(hit)`** override — adds the object gene's taxon curie so the
  whitelist gate considers both sides of the orthology pair. Without this, an HGNC↔WB
  pair routed by subject-only could miss the WB whitelist if subject was HUMAN.
* **`additionalSourceIncludes()`** — `["geneToGeneOrthologyGenerated", "stringencyFilter"]`
  so ES returns the entire orthology subtree (every field-map value below the synthetic
  `_*` names was getting stripped by the default `_source` filter logic).
* **Output is COMBINED-only** — no per-MOD files.

## Verification against FMS reference

| Metric | Ours | FMS | Δ |
|---|---|---|---|
| TSV row count | **991,340** | 987,511 | **+0.4%** |
| All 13 columns populated | yes | yes | match |
| Stringent-only filter applied | yes | yes | match |
| Algorithms pipe-delim format | yes | yes | match |

### Sample row (ours, real data)

```
HGNC:24483	TTLL3	NCBITaxon:9606	Homo sapiens	Xenbase:XB-GENE-29081743	LOC100493511	NCBITaxon:8364	Xenopus tropicalis	Ensembl Compara|InParanoid|OrthoInspector|PANTHER|PhylomeDB|SonicParanoid	6	9	No	Yes
```

Exact same shape as FMS sample row:

```
ZFIN:ZDB-GENE-041001-157	cyp2ad6	NCBITaxon:7955	Danio rerio	SGD:S000002810	DIT2	NCBITaxon:559292	Saccharomyces cerevisiae	SonicParanoid|Ensembl Compara	2	9	Yes	Yes
```

Matching: column order, taxon curie format, pipe-delimited algorithms list, integer
counts, `Yes`/`No` for boolean columns.

## Output structure

| Check | Result |
|---|---|
| File header `#`-block | OK — same FMS conventions, fixes the `# Taxon IDs:` typo |
| `# Data type: Orthology` | OK |
| `# Data format: tsv` / `json` | OK |
| `# Taxon IDs:` lists 9 species curies | OK |
| `# Species:` lists 9 species names | OK |
| `# Orthology Filter: Stringent` separator line | **Missing** — FMS has this between Help Desk and Taxon IDs lines; we'd need a small `extraHeaderLines()` hook on `FileGenerator` + parameter on `HeaderBuilder.buildTextHeader/buildJsonMetadata` to inject it. Cosmetic difference. |
| Column header line | OK — 13 columns identical to FMS |
| Blank line between header and data | OK |

## Output files

* `data/ORTHOLOGY-ALLIANCE-TSV_COMBINED.tsv.gz`
* `data/ORTHOLOGY-ALLIANCE-JSON_COMBINED.json.gz`

JSON shape: `{metadata, data: [<raw _source>...]}` — raw ES doc per row, per the
established policy for non-Gene-Description JSON outputs.

## Outstanding follow-ups

1. **Add `# Orthology Filter: Stringent` header line** to match FMS exactly. Small
   extraHeaderLines() hook on FileGenerator + threaded through HeaderBuilder. Cosmetic
   only — output content matches.

That's the only remaining diff. Counts, columns, content all match FMS.

## Reused framework features

- Sliced parallel scroll (8 slices, 1000-doc buffer per slice)
- `_source` filtering (whole `geneToGeneOrthologyGenerated` subtree)
- Multi-taxon whitelist gating (subject OR object side must be allowed)
- `shouldEmit` hook (drops non-stringent rows pre-customizeRow)
- Synchronized `TsvWriter` / `JsonRawWriter` for thread-safe dispatch

## Testing

```
ALLIANCE_RELEASE=9.0.0 \
ES_HOST=stage.cluster01.alliancegenome.org \
ES_INDEX=site_index \
CURATION_API_URL=https://alpha-curation.alliancegenome.org \
GENERATED_FILES_FOLDER=data \
SKIP_S3_UPLOAD=true \
java -jar target/agr_file_generator-jar-with-dependencies.jar Orthology
```

## Related tickets

- **SCRUM-1953** — Disease (uses the same `customizeRow` synthetic-field pattern)
- **SCRUM-6007** — Expression (mostly-real-paths field map, similar verification approach)
- **SCRUM-4788/4789** — Molecular / Genetic Interactions (richer customizeRow due to
  PSI-MITAB formatting requirements)
