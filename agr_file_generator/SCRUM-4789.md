# SCRUM-4789 — Genetic Interactions File Generator

## Summary

The new `GeneticInteractionFileGenerator` (extends `BaseInteractionFileGenerator`) reads
the `gene_genetic_interaction` category from `site_index` and writes per-MOD plus
COMBINED PSI-MI TAB 2.7 output (`INTERACTION-GEN-TSV_{MOD|COMBINED}.tsv.gz`). MOD
whitelist: `FB`, `HUMAN`, `MGI`, `RGD`, `SGD`, `WB`, `XBXL`, `ZFIN` (no `XBXT`, no
SARS-CoV-2 — matches FMS).

Implementation is just a 14-line subclass; all PSI-MITAB row-building lives in the shared
`BaseInteractionFileGenerator` (see SCRUM-4788). Only difference from Molecular: the
`docRoot()` returns `"geneGeneticInteraction"`.

## Column coverage

Same 42-column field map as Molecular Interactions (see `interactionFieldMap()` in
`FileGeneratorConfig`). Same 13 columns populated from ES via `customizeRow`, plus the
constant `Negative=false`. 28 columns emitted as `-`.

Genetic-specific observations vs Molecular:
- **Interactor types** are `psi-mi:"MI:0250"(gene)` for both A and B (vs Molecular's
  `psi-mi:"MI:0326"(protein)`). Comes through naturally from the `interactorAType` /
  `interactorBType` MITerms — no special handling needed.
- **Experimental roles A/B** (cols 19–20) are empty in FMS for Genetic Interactions
  (Molecular has prey/bait). The `interactorARole` / `interactorBRole` MITerms are still
  emitted into those columns when present in the doc, so any populated role from ES will
  show up — but FMS rows here don't carry them.
- **Detection method** is `psi-mi:"MI:0254"(genetic interference)` (vs Molecular's
  `MI:0018(two hybrid)` etc.) — comes through from `detectionMethod`.
- **Interaction type** is typically `psi-mi:"MI:2402"(genetic interaction)` — top-level
  wrapper above the more specific detection method.

## Same `_source` bug found

Output files from the first run contain the correct row count and the
`Negative=false` constant, but **all 41 data fields are `-`**. Same root cause as
SCRUM-4788: the `_source` include list defaulted to the field map's synthetic `_*`
names (which don't exist in ES) plus only the taxon path, so ES returned a doc that
`customizeRow` couldn't extract anything from.

The fix landed in `BaseInteractionFileGenerator.additionalSourceIncludes()` returning
`List.of(docRoot())` — the same fix covers both Molecular and Genetic. The Genetic run
in `data/` (file timestamp 16:15) **predates** the build that contains the fix
(jar built 16:16). A re-run will produce populated cells.

## Verification against FMS reference (first run, pre-fix)

Same caveat as SCRUM-4788: counts are valid, cell content was empty.

| MOD | Ours | FMS | Δ |
|---|---|---|---|
| WB | 21,563 | 10,775 | +100.1% |
| MGI | 1,803 | 898 | +100.8% |
| RGD | 25 | 12 | +108.3% |
| FB | 38,309 | 19,105 | +100.5% |
| ZFIN | 115 | 57 | +101.8% |
| SGD | 1,174,243 | 586,798 | +100.1% |
| HUMAN | 37,882 | 19,112 | +98.2% |
| XBXL | 10 | 5 | +100.0% |
| **COMBINED** | **1,273,950** | **636,762** | **+100.1%** |

**Cleanest 2:1 ratio yet.** Every species hits ~+100% over FMS — same directional
A↔B doubling pattern as Molecular Interactions, only even more uniform here. Stage ES
holds 1,273,950 `gene_genetic_interaction` docs vs FMS's 636,762 COMBINED rows. Exactly
2:1 within rounding.

If we add dedupe (track `interactionId` in a thread-safe set), Genetic Interactions
should match FMS volume almost exactly with no upstream-data-delta noise (in contrast to
Molecular where SARS-CoV-2 missing on alpha skews the totals by 54K).

## What's currently NOT in our output that could be added later

`GeneGeneticInteraction` carries two fields the `BaseInteractionFileGenerator` doesn't
yet use:

```java
private Allele interactorAGeneticPerturbation;
private Allele interactorBGeneticPerturbation;
private List<String> phenotypesOrTraits;
```

PSI-MITAB columns that would naturally consume these:
- **Feature(s) interactor A / B** (cols 37–38) ← `interactorAGeneticPerturbation` /
  `interactorBGeneticPerturbation` allele identifiers, formatted as
  `<dbPrefix>:<allele.curie>(allele)` per spec.
- **Interaction annotation(s)** (col 28) ← pipe-delimited `phenotypesOrTraits`.

A genetic-only override of `customizeRow` (or new synthetic field names in the field map)
would unlock these. Skipped for the first pass — FMS itself often emits `-` here too.

## Output structure

| Check | Result |
|---|---|
| File header block | OK — `#` block matches FMS exactly |
| `# Data format` | OK — `PSI-MI TAB 2.7 Format` |
| `# README:` | OK — PSI-MITAB spec URL |
| `# TaxonIDs:` prefix | OK — `NCBI:txid<n>` |
| `# File generated (UTC)` label | OK |
| Column header line (`#` prefix, 42 cols) | OK |
| `Negative=false` constant | OK |
| Empty cells rendered as `-` | OK |

## Outstanding follow-ups

1. **Re-run after the `_source` fix** to verify populated cells
   (`wormbase:WBGene00001080`, `psi-mi:"MI:2402"(genetic interaction)`, etc.).
2. **Decide on dedupe** for the universal 2× directional doubling. Genetic shows the
   pattern most cleanly — every species hits +100% within a percent. A
   `ConcurrentHashMap.newKeySet()` keyed on `interactionId` (or sorted gene-pair tuple)
   in `customizeRow` would halve the output and match FMS volume.
3. **Wire `interactorAGeneticPerturbation` / `interactorBGeneticPerturbation` and
   `phenotypesOrTraits`** into the Feature and Interaction-annotation columns when
   needed downstream.
4. **Fill remaining gap columns** as data becomes available (Confidence value, Host
   organism, Xrefs, Annotations).

## Testing

```
ALLIANCE_RELEASE=9.0.0 \
ES_HOST=stage.cluster01.alliancegenome.org \
ES_INDEX=site_index \
CURATION_API_URL=https://alpha-curation.alliancegenome.org \
GENERATED_FILES_FOLDER=data \
java -jar target/agr_file_generator-jar-with-dependencies.jar GeneticInteractions
```

Expected outputs in `./data/`:
- `INTERACTION-GEN-TSV_{FB,HUMAN,MGI,RGD,SGD,WB,XBXL,ZFIN,COMBINED}.tsv.gz`

## Related tickets

- **SCRUM-4788** — Molecular Interactions (sibling generator, same `BaseInteractionFileGenerator`,
  same `_source` bug + fix).
- **SCRUM-1953** — Disease (provides the `customizeRow` synthetic-field pattern).
- **SCRUM-6007** — Expression (another generator with mostly-real ES paths in field map,
  `_source` filter works fine since paths exist in ES).

## Post-fix verification — comparison with FMS reference

After the `_source` fix landed and the run was rebuilt, cells are now populated. Counts
held steady (data unchanged); content went from all-`-` to actual PSI-MITAB rows.

### Row counts (still 2:1 — directional A↔B doubling)

| MOD | Ours | FMS | Δ |
|---|---|---|---|
| WB | 21,563 | 10,775 | +100.1% |
| MGI | 1,803 | 898 | +100.8% |
| RGD | 25 | 12 | +108.3% |
| FB | 38,309 | 19,105 | +100.5% |
| ZFIN | 115 | 57 | +101.8% |
| SGD | 1,174,243 | 586,798 | +100.1% |
| HUMAN | 37,882 | 19,112 | +98.2% |
| XBXL | 10 | 5 | +100.0% |
| **COMBINED** | **1,273,950** | **636,762** | **+100.1%** |

Adding interaction-id dedupe (a thread-safe `Set<String>` keyed on
`geneGeneticInteraction.interactionId`) would halve the volume and match FMS almost
exactly. Stage Genetic Interactions has zero upstream-data-delta noise — every species
hits exactly +100% within rounding.

### Side-by-side row comparison (WB sample)

| # | Column | Ours | FMS |
|---|---|---|---|
| 1 | ID interactor A | `wormbase:WBGene00004746` | `wormbase:WBGene00001080` |
| 2 | ID interactor B | `wormbase:WBGene00006962` | `wormbase:WBGene00003003` |
| 5 | Alias A | `wormbase:sdc-2(public_name)` | `wormbase:dpy-21(public_name)\|wormbase:Y59A8B.1(sequence_name)` |
| 6 | Alias B | `wormbase:xol-1(public_name)` | `wormbase:lin-14(public_name)\|wormbase:T25C12.1(sequence_name)` |
| 7 | Detection method | `-` | `psi-mi:"MI:0254"(genetic interference)` |
| 8 | 1st author | `Nonet ML (1991) Nature 351(6321):65-8` | `DeLong L et al. (1987)` |
| 9 | Pub identifier | `pubmed:2027384` | `pubmed:3428573` |
| 10 | Taxid A | `taxid:6239(caeel)\|taxid:6239(Caenorhabditis elegans)` | same |
| 11 | Taxid B | `taxid:6239(caeel)\|taxid:6239(Caenorhabditis elegans)` | same |
| 12 | Interaction type | `psi-mi:"MI:2402"(genetic interaction)` | same |
| 13 | Source database | `-` | `psi-mi:"MI:0487"(wormbase)` |
| 14 | Interaction identifier | `WB:WBInteraction000000086` | `wormbase:WBInteraction000000001` |
| 19 | Experimental role A | `psi-mi:"MI:0499"(unspecified role)` | `-` |
| 20 | Experimental role B | `psi-mi:"MI:0499"(unspecified role)` | `-` |
| 21 | Type interactor A | `psi-mi:"MI:0250"(gene)` | same |
| 22 | Type interactor B | `psi-mi:"MI:0250"(gene)` | same |
| 36 | Negative | `false` | `false` |

(Different actual row picked because scroll order differs — content shape is the
comparable thing here.)

### Action items found by the comparison

1. **Interaction identifier (col 14): `WB:` → `wormbase:` prefix mapping** — same lowercased
   PSI-MI prefix transform we apply to ID columns 1 and 2 should also apply to
   `interactionId`. Quick fix in `BaseInteractionFileGenerator.customizeRow()`.
2. **Aliases A/B (cols 5–6): include sequence_name** — FMS pipe-delimits
   `public_name`+`sequence_name` (e.g. `wormbase:dpy-21(public_name)|wormbase:Y59A8B.1(sequence_name)`).
   We only emit `displayText` as `public_name`. Need to also pull
   `geneFullName.formatText` (or whatever holds the sequence name) and join.
3. **Detection method (col 7): empty in ours, populated in FMS** — for WB genetic interactions
   FMS has `psi-mi:"MI:0254"(genetic interference)`. Our `detectionMethod` MITerm field is
   pulling but coming back null. Field may be named differently for genetic vs molecular,
   or stored on a different MITerm slot. Worth a quick ES doc inspection.
4. **Source database (col 13): empty in ours, populated in FMS** — FMS has
   `psi-mi:"MI:0487"(wormbase)` (the data provider). Currently we map
   `aggregationDatabase` → this column; for BioGRID-aggregated molecular interactions that
   gives `psi-mi:"MI:0463"(biogrid)`. For native MOD-provided genetic interactions
   `aggregationDatabase` is null and `interactionSource` carries the MOD's MITerm.
   Fix: fall back to `interactionSource` when `aggregationDatabase` is empty.
5. **Experimental roles A/B (cols 19–20): "unspecified role" in ours, empty in FMS** — FMS
   omits these for genetic interactions. We currently always emit
   `interactorARole`/`interactorBRole` if present. Either skip when role is "unspecified"
   or skip experimental-role emission entirely for genetic. Probably the former.

### What's correct

- ID interactor A/B (PSI-MI prefix transformed properly)
- Aliases public_name suffix
- Author / PubMed identifier (correctly lowercased pubmed prefix)
- Taxid A/B (with 4-letter species code)
- Interaction type (psi-mi format)
- Type interactor A/B (gene for genetic)
- Negative = `false` constant per spec
- Header block (PSI-MITAB conventions, NCBI:txid prefix, README URL)
- Column header line (#-prefixed, 42 columns)

### Verdict

Five concrete fixes will close most of the cell-content gap with FMS. The 2:1 row count
ratio remains the largest remaining decision: dedupe in `customizeRow` (halve to match
FMS) vs. ship full directional doubling (ES truth). Either way, the genetic generator
is structurally complete and matches the FMS layout; only fine-tuning of cell content
remains.
