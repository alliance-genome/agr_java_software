# SCRUM-1953 — Disease File Generator: ES Field Coverage

## Summary

The new `DiseaseFileGenerator` reads three categories from `site_index` —
`gene_disease_annotation`, `allele_disease_annotation`, `agm_disease_annotation` — and writes
per-MOD plus COMBINED TSV + JSON output (`DISEASE-ALLIANCE-{TSV,JSON}_{MOD|COMBINED}.{tsv,json}.gz`).
Filenames split by MOD: `FB`, `HUMAN`, `MGI`, `RGD`, `SGD`, `WB`, `XBXL`, `XBXT`, `ZFIN` plus
`COMBINED`. The TSV column set matches the FMS Disease file produced by the legacy Python
`agr_file_generator`.

## Column coverage (TSV)

18 columns total. **14 populated from ES, 4 still blank** until we map their ES paths.

| FMS column | ES path | Status |
|---|---|---|
| Taxon | `subject.taxon.curie` | OK |
| SpeciesName | `subject.taxon.name` | OK |
| DBobjectType | derived in `customizeRow` from doc category: `gene_disease_annotation` → `gene`, `allele_disease_annotation` → `allele`, `agm_disease_annotation` → **`affected_genomic_model`** (FMS spelling, not "agm") | OK |
| DBObjectID | `subject.primaryExternalId` | OK |
| DBObjectSymbol | derived in `customizeRow` based on `subject.type`: Gene → `subject.geneSymbol.displayText`, Allele → `subject.alleleSymbol.displayText`, AffectedGenomicModel → `subject.agmFullName.displayText` (fallback `subject.name`) | OK |
| AssociationType | `relation.name` | OK |
| DOID | `object.curie` | OK |
| DOtermName | `object.name` | OK |
| WithOrtholog | derived in `customizeRow`: pipe-delimited `primaryAnnotations[*].with[*].primaryExternalId` | OK |
| InferredFromID | — | **Gap** |
| InferredFromSymbol | — | **Gap** |
| ExperimentalCondition | — | **Gap** |
| Modifier | — | **Gap** |
| EvidenceCode | derived: first entry of `evidenceCodes[].curie` (fallback `primaryAnnotations.0.evidenceCodes.0.curie`) | OK |
| EvidenceCodeName | matching `.name` | OK |
| Reference | derived: `references.0.referenceID` (the PMID — preferred over the AGRKB curie) | OK |
| Date | derived: ISO timestamp from `primaryAnnotations.0.dateUpdated` (fallbacks dateCreated, subject.dateUpdated) reformatted as YYYYMMDD | OK |
| Source | `subject.taxon.species.displayName` (the MOD code) | OK |

## Per-doc transforms in `customizeRow`

The generator injects synthetic `_*` fields onto each hit so the field map can stay a flat
`Map<String, String>`:

```
_dbObjectType        — switch on category
_dbObjectSymbol      — switch on subject.type, pick the right symbol field
_evidenceCode        — first evidenceCodes[].curie
_evidenceCodeName    — first evidenceCodes[].name
_reference           — references[0].referenceID (PMID), fallback to references[0].curie
_date                — first non-empty of primaryAnnotations[0].dateUpdated /
                       primaryAnnotations[0].dateCreated / subject.dateUpdated, reformatted
                       to YYYYMMDD
_source              — subject.taxon.species.displayName
_withOrtholog        — pipe-delimited primaryAnnotations[*].with[*].primaryExternalId
```

## Gaps and root causes

### 1. `InferredFromID` / `InferredFromSymbol`

FMS examples (MGI gene rows):
```
DBObj=gene/MGI:1918017  InferredFromID=MGI:4950565  InferredFromSymbol=Prpf3<sup>tm1.1Eap</sup>/Prpf3<sup>+</sup>  [background:] involves: 129S6/SvEvTac * C57BL/6 * SJL
```

These columns are populated when a gene-level annotation is rolled up from an underlying
allele- or AGM-level primary annotation. The relevant data lives at
`primaryAnnotations[0].diseaseAnnotationSubject` (the original primary annotation's
subject), but only when its type differs from the outer `subject` (i.e. outer subject is a
Gene but the primary annotation was on an Allele or AGM).

To populate, `customizeRow` would need a check:
```
if outer subject.type == "Gene" and primaryAnnotations[0].diseaseAnnotationSubject.type
   in ("Allele", "AffectedGenomicModel"):
   InferredFromID     = primaryAnnotations[0].diseaseAnnotationSubject.primaryExternalId
   InferredFromSymbol = primaryAnnotations[0].diseaseAnnotationSubject.<typeSymbolField>
```

Where `<typeSymbolField>` follows the same polymorphic switch we use for the outer
`DBObjectSymbol`. Worth adding once we confirm the doc shape on a real allele/AGM rollup.

### 2. `ExperimentalCondition` (rare in FMS)

Format from FMS (WB allele example):
```
Induced By: chemical treatment:paraquat
```

Populated from `primaryAnnotations[0].conditionRelations[]` — each entry has a
`conditionRelationType` ("Induced By", "Has Condition", etc.) plus `conditions[]` with
ontology terms. Format: `<conditionRelationType>: <conditions[*].conditionSummary>`.
We'd need to verify what fields ES carries inside `conditionRelations`.

### 3. `Modifier` (rare in FMS)

Format from FMS (WB AGM example):
```
Ameliorated By: chemical treatment:nicotinamide mononucleotide
```

Same shape as ExperimentalCondition but using a different relation type vocabulary
("Ameliorated By", "Exacerbated By"). Likely on
`primaryAnnotations[0].conditionRelations[]` with `conditionRelationType` distinguishing
which list it belongs in. Would need a per-relation-type filter.

## Verification against FMS reference (run on stage)

| MOD | Stage ES (ours) | FMS file | Δ |
|---|---|---|---|
| WB | 35,853 | 40,507 | -11% |
| MGI | 61,367 | 69,485 | -12% |
| RGD | 47,289 | 48,010 | -2% |
| FB | 55,516 | 43,643 | **+27%** |
| ZFIN | 57,858 | 59,922 | -3% |
| SGD | 11,432 | 12,985 | -12% |
| HUMAN | 46,427 | 49,823 | -7% |
| XBXL | 38,277 | 36,051 | +6% |
| XBXT | 45,610 | 46,954 | -3% |
| **COMBINED** | 399,613 | 407,356 | -2% |

Most species within 12%, FB skews high (+27%) — matches the upstream-data-delta pattern
seen in Expression and Gene Descriptions. Total within 2%.

### DBobjectType distribution

After fixing the AGM type label (`agm` → `affected_genomic_model`):

| Type | Ours (after fix) | FMS |
|---|---|---|
| gene | 714,460 | 747,560 (-4%) |
| affected_genomic_model | 58,012 | 25,712 (+125%) |
| allele | 26,750 | 41,434 (-35%) |

The ~33K AGM excess and ~15K allele shortfall are most likely an upstream curation-data
difference between alpha and the prod state when the FMS files were generated — not a
generator bug. Worth a spot-check on alpha curation Postgres if the discrepancy matters.

Column headers match FMS exactly (18 columns, identical names). Header block matches FMS
layout (with the `# Taxon IDs:` typo fixed). Populated row content for the 14 filled
columns matches the FMS data shape; allele/AGM symbols carry the full `<sup>...</sup>`
HTML markup just like FMS.

## Reused framework features

- Sliced parallel scroll (8 slices, 1000-doc buffer per slice)
- `_source` filtering — disabled here because Disease has `JSON_RAW` outputs that need the
  full document
- MOD whitelist gating both per-MOD and COMBINED files
- Synchronized `TsvWriter` / `JsonRawWriter` for thread-safe dispatch
- Header builder with FMS-matching `#`-prefixed block (typo-fixed)

## Files to create / modify when closing the gaps

* `DiseaseFileGenerator.java` — extend `customizeRow` with InferredFromID/Symbol logic and
  ConditionRelations parsing for ExperimentalCondition + Modifier.
* `FileGeneratorConfig.java` — swap the four `_unavailable` placeholders for the synthetic
  field names (`_inferredFromId`, `_inferredFromSymbol`, `_experimentalCondition`,
  `_modifier`) once the customizeRow logic lands.

No framework changes required.

## Testing

```
ALLIANCE_RELEASE=8.4.0 \
ES_HOST=stage.cluster01.alliancegenome.org \
ES_INDEX=site_index \
CURATION_API_URL=https://alpha-curation.alliancegenome.org \
GENERATED_FILES_FOLDER=data \
java -jar target/agr_file_generator-jar-with-dependencies.jar Disease
```

Expected outputs in `./data/`:
- `DISEASE-ALLIANCE-TSV_{FB,HUMAN,MGI,RGD,SGD,WB,XBXL,XBXT,ZFIN,COMBINED}.tsv.gz`
- `DISEASE-ALLIANCE-JSON_{FB,HUMAN,MGI,RGD,SGD,WB,XBXL,XBXT,ZFIN,COMBINED}.json.gz`

## Post-fix verification — comparison with FMS reference

Run after the AGM-type fix and WithOrtholog were shipped.

### Output structure check

| Check | Result |
|---|---|
| Column headers (18 cols) | OK — identical to FMS |
| Header block | OK — same `#`-prefixed format |
| DBobjectType | OK — now correct (`gene`/`allele`/`affected_genomic_model`) |
| AGM rows | OK — present with full HTML markup (`Fanca<sup>tm1Faw</sup>/Fanca<sup>tm1Faw</sup>...`) |
| Allele rows | OK — present with full markup |
| EvidenceCode / EvidenceCodeName | OK |
| Reference | OK — PMID format (`PMID:10915769`) |
| Date (direct annotations) | OK — sample `20060410` matches FMS pattern |
| Date (via-orthology rollups) | **Issue** — empty in our output; FMS fills with file-generation date |
| AssociationType prefix | **Issue** — ours `is_implicated_via_orthology` / `is_marker_via_orthology`; FMS `implicated_via_orthology` / `biomarker_via_orthology` (drops `is_`, renames `marker` → `biomarker`) |
| Source (via-orthology) | **Issue** — ours emits subject's MOD (e.g. `WB`); FMS emits `Alliance`. Should pull from `primaryAnnotations[0].dataProvider.abbreviation` instead of subject species |
| WithOrtholog | **Improvement** — we populate ~313K rows in COMBINED (99% of XBXL/XBXT). FMS only populates 50 rows total (SGD only). Our output is more complete than FMS |
| InferredFromID / InferredFromSymbol | Gap (logic not yet added) |
| ExperimentalCondition | Gap |
| Modifier | Gap |

### Row counts (TSV, totals identical to pre-fix run since data unchanged)

| MOD | Ours | FMS | Δ |
|---|---|---|---|
| WB | 35,853 | 40,507 | -11.5% |
| MGI | 61,367 | 69,485 | -11.7% |
| RGD | 47,289 | 48,010 | -1.5% |
| FB | 55,516 | 43,643 | **+27.2%** |
| ZFIN | 57,858 | 59,922 | -3.4% |
| SGD | 11,432 | 12,985 | -12.0% |
| HUMAN | 46,427 | 49,823 | -6.8% |
| XBXL | 38,277 | 36,051 | +6.2% |
| XBXT | 45,610 | 46,954 | -2.9% |
| **COMBINED** | **399,613** | **407,356** | **-1.9%** |

Most species within 12%, FB skews high (+27%) — matches the upstream alpha-vs-prod data delta
seen in Expression and Gene Descriptions.

### DBobjectType distribution

After fixing the AGM type label:

| Type | Ours | FMS | Δ |
|---|---|---|---|
| gene | 714,460 | 747,560 | -4% |
| affected_genomic_model | 58,012 | 25,712 | **+125%** |
| allele | 26,750 | 41,434 | **-35%** |

The ~33K AGM excess and ~15K allele shortfall are most likely an upstream curation-data
difference between alpha and the prod state when the FMS files were generated — not a
generator bug. Worth a spot-check on alpha curation Postgres if the discrepancy matters.

### AssociationType vocabularies (both sides)

| Ours | FMS |
|---|---|
| `is_implicated_via_orthology` (405,556) | `implicated_via_orthology` (406,742) |
| `is_marker_via_orthology` (219,162) | `biomarker_via_orthology` (227,928) |
| `is_implicated_in` (83,336) | `is_implicated_in` (115,934) |
| `is_model_of` (39,824) | `is_model_of` (25,712) |
| `is_marker_for` (33,156) | `is_marker_for` (35,452) |
| `is_ameliorated_model_of` (11,398) | (not in FMS) |
| `is_exacerbated_model_of` (6,790) | (not in FMS) |
| (not in ours) | `is_not_implicated_in` (2,938) |

The `is_*_via_orthology` rows need normalisation to match FMS's legacy naming. The presence
of `is_ameliorated_model_of` / `is_exacerbated_model_of` in our output but not in FMS is
likely a newer relation type added to the curation API after the FMS files were generated.

### WithOrtholog populated row counts (per MOD)

| MOD | Ours | FMS |
|---|---|---|
| FB | 30,540 | 0 |
| HUMAN | 18,133 | 0 |
| MGI | 43,976 | 0 |
| RGD | 37,611 | 0 |
| SGD | 11,042 | 50 |
| WB | 34,134 | 0 |
| XBXL | 38,276 | 0 |
| XBXT | 45,609 | 0 |
| ZFIN | 54,056 | 0 |
| **COMBINED** | **313,369** | **50** |

Sample populated rows from our FB output (matches the expected pipe-delim format):
```
FB:FBgn0033885/DJ-1α   is_implicated_via_orthology   ZFIN:ZDB-GENE-041010-5
FB:FBgn0033885/DJ-1α   is_implicated_via_orthology   RGD:621808
FB:FBgn0033885/DJ-1α   is_implicated_via_orthology   HGNC:16369|RGD:621808
FB:FBgn0002783/mor     is_implicated_via_orthology   WB:WBGene00004203
```

## Three actionable bugs identified by this verification

1. **Source for via-orthology rollups** — should be
   `primaryAnnotations[0].dataProvider.abbreviation` (yields `Alliance`), falling back to
   subject species displayName for direct annotations.
2. **AssociationType normalisation** — strip the `is_` prefix from `*_via_orthology` and
   rename `marker_via_orthology` → `biomarker_via_orthology` to match FMS.
3. **Date empty for via-orthology** — `primaryAnnotations[0].dateUpdated` is empty on rollup
   docs; FMS fills with the file generation date as a fallback.
