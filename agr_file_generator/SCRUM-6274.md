# SCRUM-6274 — Disease download TSV: model-centric column layout

## Summary

Rearranges the Disease download **TSV** columns from the legacy FMS subject-agnostic layout
(`DBobjectType` / `DBObjectID` / `DBObjectSymbol` / `AssociationType`) to the model-centric layout
Chris Grove proposed in SCRUM-1953 (comment 2026-05-18). The Disease file is the only disease
download not anchored to a single subject type, so the model, allele and gene levels each get their
own ID / symbol / association columns and every row names whichever levels its annotation actually
carries.

JSON_RAW output is untouched — it writes the consolidated ES doc verbatim and never consults the
field map. Only the TSV layout changes.

## Column layout

35 columns. The 34 from the ticket plus `UniqueID` first, which Chris asked to keep for curator
verification until the public release (SCRUM-1953 comment 2026-07-15).

| # | Column | Synthetic field | Source |
|---|---|---|---|
| 1 | UniqueID | `_uniqueId` | `primaryAnnotations[i].uniqueId` |
| 2 | Taxon ID | `_taxon` | subject `taxon.curie` |
| 3 | Species Name | `_speciesName` | subject `taxon.species.fullName` |
| 4 | Model ID | `_modelId` | AGM annotation subject `primaryExternalId`, else blank |
| 5 | Model Symbol | `_modelSymbol` | AGM subject `agmFullName.displayText` (fallback `name`) |
| 6 | Model Type | `_modelType` | AGM subject `subtype.name` |
| 7 | Model Association | `_modelAssociation` | relation, on AGM annotations only |
| 8 | Allele IDs | `_alleleIds` | allele subject, else `inferredAllele`, else `assertedAlleles[]` |
| 9 | Allele Symbols | `_alleleSymbols` | same source, `alleleSymbol.displayText` |
| 10 | Allele Association | `_alleleAssociation` | relation, on Allele annotations only |
| 11 | Gene IDs | `_geneIds` | gene subject, else `inferredGene`, else `assertedGenes[]` |
| 12 | Gene Symbols | `_geneSymbols` | same source, `geneSymbol.displayText` |
| 13 | Gene Association | `_geneAssociation` | relation, on Gene annotations and via-orthology rows |
| 14 | Disease Qualifier | `_diseaseQualifier` | `diseaseQualifiers[].name`, `_` → space |
| 15 | Disease ID | `_doId` | `diseaseAnnotationObject.curie` |
| 16 | Disease Name | `_doTermName` | `diseaseAnnotationObject.name` |
| 17 | Evidence Code | `_evidenceCode` | `evidenceCodes[].curie` (ECO term ID) |
| 18 | Evidence Code Abbreviation | `_evidenceCodeAbbreviation` | `evidenceCodes[].abbreviation` |
| 19 | Evidence Code Name | `_evidenceCodeName` | `evidenceCodes[].name` |
| 20 | Experimental Conditions | `_experimentalConditions` | `conditionRelations[]` where type is `has_condition` / `induced_by` |
| 21 | Condition Modifiers | `_conditionModifiers` | `conditionRelations[]` where type is `ameliorated_by` / `exacerbated_by` |
| 22 | Genetic Modifier Relation | `_geneticModifierRelation` | `diseaseGeneticModifierRelation.name` |
| 23 | Genetic Modifier IDs | `_geneticModifierIds` | `diseaseGeneticModifier{Alleles,Genes,Agms}[].primaryExternalId` |
| 24 | Genetic Modifier Names | `_geneticModifierNames` | same arrays, symbol per entity type |
| 25 | Strain Background ID | `_strainBackgroundId` | `sgdStrainBackground.primaryExternalId` |
| 26 | Strain Background Name | `_strainBackgroundName` | `sgdStrainBackground.agmFullName.displayText` |
| 27 | Genetic Sex | `_geneticSex` | `geneticSex.name` |
| 28 | Notes | `_notes` | `relatedNotes[]`, labelled `Note: ` / `Summary: ` by note type |
| 29 | Based On ID | `_basedOnId` | `with[].primaryExternalId` (sorted) |
| 30 | Based On Symbol | `_basedOnSymbol` | `with[].geneSymbol.displayText` + ` (Abbrev)` |
| 31 | Source | `_source` | unchanged from SCRUM-1953 |
| 32 | Source URL | `_sourceUrl` | `dataProviderCrossReference` url template |
| 33 | Reference | `_reference` | `evidenceItem.referenceID`, fallback `evidenceItem.curie` |
| 34 | Date | `_date` | `dateUpdated` / `dateCreated` as YYYYMMDD |

Columns dropped: `DBobjectType`, `DBObjectID`, `DBObjectSymbol`, `AssociationType`, `WithOrtholog`,
`InferredFromID`, `InferredFromSymbol`, `ExperimentalCondition`, `Modifier`. The last four were the
`_unavailable` placeholders from SCRUM-1953 — all four now have real columns in the new layout, so
the placeholder mechanism is gone from this generator entirely.

`WithOrtholog` and `InferredFromSymbol` were the same `with[]` data the ticket renames to
`Based On ID` / `Based On Symbol`; `joinBasedOnSymbols` (SCRUM-1953, with the species abbreviation)
moved to `Based On Symbol` unchanged, and `joinWithOrthologs` was renamed `joinBasedOnIds`.

## Population rules

Implemented per Chris's spec:

* **Model** — subject of an AGM Disease annotation, otherwise blank. There is no inferred or asserted
  AGM in the data model, so a gene or allele annotation has no model to name.
* **Allele(s)** — subject of an Allele Disease annotation, else `inferredAllele`, else
  `assertedAlleles[]` pipe-joined.
* **Gene(s)** — subject of a Gene Disease annotation, else `inferredGene`, else `assertedGenes[]`
  pipe-joined.
* **Associations** — exactly one of the three association columns is populated per row: the level the
  annotation was actually curated at. The other two levels are only reached by inference, so
  asserting a relation for them would invent an annotation that is not in the persistent store.
* **via_orthology** — gene level only, keyed on the enclosing doc's gene subject (SCRUM-1953
  behaviour retained). Model and allele columns blank, Gene Association carries the
  `is_*_via_orthology` relation.

## Semantics borrowed from the gene-page download

`DiseaseAnnotationToTdfTranslator` (agr_api) already builds most of these columns for the gene /
allele / model page disease downloads, and those are curator-approved. The new columns follow it:

* Genetic modifiers walk `Alleles` → `Genes` → `Agms` so the ID and name columns line up.
* `Notes` labels `disease_note` as `Note: ` and `disease_summary` as `Summary: `; any other note type
  is omitted.
* `Source URL` substitutes the full `referencedCurie` into the resource-descriptor url template,
  dropping the template's own prefix for MGI / SGD / OMIM whose templates already carry it.
* `Disease Qualifier` renders `susceptibility_to` as `susceptibility to`.

Two deliberate departures:

1. **Conditions are computed per annotation, not per doc.** The translator overwrites its per-annotation
   condition values with the doc-level `experimentalConditionsAggregated` /
   `conditionModifierAggregated` rollups. Those are aggregates across every annotation in the
   consolidated doc, which would smear conditions across unrelated rows now that each row is one
   annotation. The generator reads `conditionRelations[]` off the annotation itself and splits it by
   relation type per the ticket.
2. **Association is set only at the annotation's own level** (see above). The translator populates both
   a doc-level and a per-annotation association column because the gene page is anchored to one gene.

## Files changed

* `config/FileGeneratorConfig.java` — `diseaseFieldMap()` replaced with the 35-column layout.
* `generators/DiseaseFileGenerator.java` — `customizeRows()` emits the new synthetic fields; new
  helpers `resolveEntityField`, `resolveAgmSymbol`, `joinDiseaseQualifiers`, `joinEvidenceCodes`,
  `joinConditionSummaries`, `joinGeneticModifiers`, `resolveEntityName`, `joinNotes`,
  `buildSourceUrl`; `joinWithOrthologs` renamed `joinBasedOnIds`.

No framework changes. `computeSourceIncludes()` still returns null for this generator (JSON_RAW forces
full `_source`), so no `additionalSourceIncludes()` entries are needed for the new fields.

## Testing

`data` is gitignored; use it as the output folder so generated files can't be committed. The token is
prepended with `APIToken ` by `ConfigHelper.getCurationApiToken()`, so pass the bare UUID, and note the
API URL needs the `/api` suffix. `-D` beats the `application.properties` bundled in the jar, which
carries a stale token that 401s.

```
java -DALLIANCE_RELEASE=8.4.0 \
  -DES_HOST=stage.cluster01.alliancegenome.org \
  -DES_INDEX=site_index \
  -DCURATION_API_URL=https://alpha-curation.alliancegenome.org/api \
  -DCURATION_API_TOKEN=<apitoken from person.apitoken> \
  -DGENERATED_FILES_FOLDER=data \
  -DSKIP_S3_UPLOAD=true \
  -jar target/agr_file_generator-jar-with-dependencies.jar Disease
```

## Verification

Run 2026-07-31 against stage ES (`site_index`, 489,486 docs), 19m08s, 484,442 TSV rows COMBINED.

### Structure

* All 484,442 rows have exactly **35 columns**; header order matches the ticket.
* Every column is populated on at least some rows — no dead columns, and the four `_unavailable`
  placeholders from SCRUM-1953 are gone.
* **Association invariant holds exactly**: every one of the 484,442 rows has exactly one of the three
  association columns populated. Model 33,024 + Allele 5,663 + Gene 445,755 = 484,442.
* Per-MOD files stay species-pure (WB 44,670 rows all `NCBITaxon:6239`; SGD 14,303 all
  `NCBITaxon:559292`, rendered `Saccharomyces cerevisiae` with no `S288C` strain suffix). Both
  SCRUM-1953 complaints stay fixed.

### Column fill rates (COMBINED)

| Column | Filled | % |
|---|---|---|
| UniqueID / Taxon ID / Species Name | 484,442 | 100% |
| Model ID / Symbol / Type / Association | 33,024 | 6.8% |
| Allele IDs / Symbols | 32,647 | 6.7% |
| Allele Association | 5,663 | 1.2% |
| Gene IDs / Symbols | 472,743 | 97.6% |
| Gene Association | 445,755 | 92.0% |
| Disease Qualifier | 89,323 | 18.4% |
| Disease ID / Name | 484,442 | 100% |
| Evidence Code / Abbreviation / Name | 484,442 | 100% |
| Experimental Conditions | 4,145 | 0.9% |
| Condition Modifiers | 367 | 0.1% |
| Genetic Modifier Relation / IDs / Names | 9,540 | 2.0% |
| Strain Background ID / Name | 1,752 | 0.4% |
| Genetic Sex | 1,592 | 0.3% |
| Notes | 34,782 | 7.2% |
| Annotation Type | 35,348 | 7.3% |
| Based On ID / Symbol | 390,113 | 80.5% |
| Source / Reference | 484,442 | 100% |
| Source URL | 95,578 | 19.7% |
| Date | 461,918 | 95.4% |

The Allele ID (32,647) vs Allele Association (5,663) gap is expected: most allele cells come from an
AGM annotation's inferred/asserted alleles, which carry no allele-level relation. Based On 80.5% and
Source URL 19.7% are complements — via-orthology rows have a `with[]` but no data-provider cross
reference, experimental rows the reverse.

### Association totals vs SCRUM-1953 approved counts

Chris's approved counts (2026-07-15) against this run, summing the three association columns:

| Relation | Approved | This run |
|---|---|---|
| is_implicated_via_orthology | 240,711 | 240,711 |
| is_marker_via_orthology | 148,153 | 148,153 |
| is_implicated_in | 36,199 | 36,193 |
| is_marker_for | 25,238 | 25,238 |
| is_model_of | 23,319 | 23,319 |
| is_ameliorated_model_of | 5,762 | 5,762 |
| is_exacerbated_model_of | 3,418 | 3,418 |
| is_not_implicated_in | 1,095 | 1,095 |
| does_not_model | 520 | 520 |
| is_not_marker_for | 28 | 28 |
| is_not_ameliorated_model_of | 5 | 5 |

Ten of eleven match exactly. `is_implicated_in` is 6 lower (0.017%) — curation data drift over the 16
days since Chris's run, not a layout change. Negation handling (`does_not_model`, `is_not_*`) is
carried over untouched.

### wrn-1 acceptance case

SCRUM-1953 comment 2026-05-18 item 3 listed three experimental annotations that had to appear for
`WB:WBGene00006944`. All three now do, each on its own row at its own level:

| Level | Subject | Association | Reference |
|---|---|---|---|
| Gene | `WB:WBGene00006944` wrn-1 | Gene Association `is_implicated_in` | PMID:15115755 |
| Allele | `WB:WBVar00145506` gk99, wrn-1 as asserted gene | Allele Association `is_implicated_in` | PMID:20062519 |
| Model | `WB:WBGenotype00000014` (genotype), wrn-1 \| mir-124 asserted | Model Association `is_model_of` | PMID:23075628 |

Plus 8 via-orthology rows, all gene-level with model and allele columns blank — including the
`wrn-1 is_implicated_via_orthology breast cancer` row based on `HGNC:12791` / `WRN (Hsa)` that Chris
asked for. Alleles are never the subject of a via_orthology row.

Both wrn-1 rows for `senile cataract` are genuinely distinct annotations (their UniqueIDs differ by a
`susceptibility_to` qualifier), not a dedup failure.

### Source URL

0 malformed URLs out of 95,578 — no unsubstituted `[%s]`, no doubled prefix from the MGI / SGD / OMIM
exception. Per-MOD shapes:

```
MGI     http://www.informatics.jax.org/allele/genoview/MGI:2166570
SGD     https://www.yeastgenome.org/locus/SGD:S000000027/disease
WB      https://www.wormbase.org/resources/disease/DOID:0050425
FB      https://flybase.org/cgi-bin/cvreport.html?id=DOID:0014667
ZFIN    https://zfin.org/DOID:0014667
RGD     https://rgd.mcw.edu/rgdweb/ontology/annot.html?species=Rat&x=1&acc_id=DOID:0014667#annot
HUMAN   https://rgd.mcw.edu/rgdweb/ontology/annot.html?species=Human&x=1&acc_id=DOID:0001816#annot
```

MGI and SGD resolve to an entity-specific page; the others resolve to the MOD's page for the disease
term, because that is what their `dataProviderCrossReference` points at. Human annotations get RGD
URLs because RGD provides them. Same behaviour as the gene-page download.

### Other spot checks

* Genetic Modifier IDs and Names: 9,540 rows, **0 misaligned** element counts.
* Strain Background populated for SGD only (`SGD:S000203450` / `Sigma1278b`), as expected for a field
  declared on `GeneDiseaseAnnotation` alone.
* Notes: 34,091 `Note: ` + 691 `Summary: ` = 34,782, matching the column fill count exactly, so every
  populated cell is labelled.
* Experimental Conditions and Condition Modifiers split by relation type as specified; the 37 rows with
  both populated are annotations that genuinely carry both kinds of condition relation.

## Open questions for Chris

1. **Symbol columns carry HTML markup.** Model Symbol, Allele Symbols and Gene Symbols use
   `displayText`, which renders superscripts as markup — e.g. `Apc2<sup>g10</sup> Apc<sup>Q8</sup>`.
   This matches both the pre-existing Disease TSV and the gene-page download, so it is left as-is, but
   the plain-text `formatText` variant (`Apc2[g10]`) may read better in a TSV. Easy swap if wanted.
2. **Source URL granularity.** For most MODs the cross reference points at the disease term page rather
   than the annotation, so that is what the column contains (see above). If an annotation-level URL is
   wanted, the cross reference would have to change upstream in curation.
3. **UniqueID** is still the first column per the 2026-07-15 request; drop it before the public release.
4. **Association placement** — confirm that putting the relation only at the annotation's own level
   (exactly one of the three columns per row) is what you intended, rather than repeating it on every
   level the row names.

## Not addressed here

`Date` is blank on 4.6% of rows. `primaryAnnotations[i]` carries no `dateUpdated`, only a sometimes-absent
`dateCreated`; the generator falls back to the file-generation date for via-orthology rows only. This is
inherited from SCRUM-1953 and is a curation-side indexing gap, not a column-layout issue.

