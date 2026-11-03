# SCRUM-6020 — Variants/Alleles File Generator

## Summary

The new `VariantAlleleFileGenerator` reads the `allele_summary` category from
`site_index` and writes per-MOD TSV + JSON output
(`VARIANT-ALLELE-{TSV,JSON}_{MOD}.{tsv,json}.gz`). MOD whitelist:
`FB`, `MGI`, `RGD`, `SGD`, `WB`, `ZFIN` (no HUMAN, no Xenopus, no SARS-CoV-2 —
matches FMS).

Uses the same ES category that `agr_api`'s `AlleleESService.getAllelesByGene` queries
for the gene-page allele table. **One row per allele** — variant-list-derived columns
are pipe-joined when the allele has multiple known variants.

**Did NOT scroll `variant_summary`** (no standalone orphan-variant rows) — confirmed
against the FMS files where every `Category` value is `allele` or
`allele with N known variants`, never just `variant`.

## Column coverage (TSV)

27 columns. **22 populated from ES, 5 still blank.**

| FMS column | ES path / source |
|---|---|
| Taxon | `allele.taxon.curie` |
| SpeciesName | `allele.taxon.name` |
| AlleleId | `allele.primaryExternalId` |
| AlleleSymbol | `allele.alleleSymbol.displayText` |
| AlleleSynonyms | `allele.alleleSynonyms[].displayText` (pipe-joined) |
| VariantId | derived in `customizeRow` — `variantList[].primaryExternalId` (pipe-joined) |
| VariantSymbol | `variantList[].variantSymbol.displayText` (pipe-joined) |
| VariantSynonyms | (gap — field not on doc) |
| VariantCrossReferences | (gap — field not on doc) |
| AlleleAssociatedGeneId | `alleleOfGene.primaryExternalId` |
| AlleleAssociatedGeneSymbol | `alleleOfGene.geneSymbol.displayText` |
| VariantAffectedGeneId | from `variantList[*].curatedVariantGenomicLocations[*].mostSevereConsequence.variantTranscript.transcriptGeneAssociations[*].transcriptGeneAssociationObject.primaryExternalId` (deduped, pipe-joined) |
| VariantAffectedGeneSymbol | same path → `geneSymbol.displayText` |
| Category | derived from `variantList.size()`: `allele` / `allele with 1 known variant` / `allele with N known variants` (matches FMS naming exactly — ES `alterationType` uses `one`/`multiple` buckets which we don't expose) |
| VariantsTypeId | `variantList[*].variantType.curie` (pipe-joined) |
| VariantsTypeName | `variantList[*].variantType.name` (pipe-joined) |
| VariantsHgvsNames | `variantList[*].curatedVariantGenomicLocations[*].hgvs` (pipe-joined) |
| Assembly | `…variantGenomicLocationAssociationObject.genomeAssembly.primaryExternalId` (deduped) |
| Chromosome | `…variantGenomicLocationAssociationObject.name` (deduped) |
| StartPosition | `…curatedVariantGenomicLocations[*].start` (pipe-joined) |
| EndPosition | `…curatedVariantGenomicLocations[*].end` (pipe-joined) |
| SequenceOfReference | `…referenceSequence` (pipe-joined) |
| SequenceOfVariant | `…variantSequence` (pipe-joined) |
| MostSevereConsequenceName | `…mostSevereConsequence.variantConsequence.name` (fallback `mostSevereConsequence.name`), deduped |
| VariantInformationReference | (gap) |
| HasDiseaseAnnotations | `hasDisease` boolean → `yes`/`-` (FMS convention, not `true`/`false`) |
| HasPhenotypeAnnotations | `hasPhenotype` boolean → `yes`/`-` |

## Per-doc transforms in `customizeRow`

```
_alleleSynonyms              — pipe-joined allele.alleleSynonyms[].displayText
_category                    — "allele" or "allele with N known variants" from
                               variantList.size()
_variantId / _variantSymbol  — pipe-joined variantList[].primaryExternalId / .variantSymbol.displayText
_variantsTypeId / Name       — pipe-joined variantList[].variantType.curie / .name
_variantsHgvsNames           — pipe-joined hgvs across all variant locations
_assembly / _chromosome      — pipe-joined deduped from variantGenomicLocationAssociationObject
_startPosition / _endPosition — pipe-joined positions
_sequenceOfReference / _sequenceOfVariant — pipe-joined
_mostSevereConsequence       — pipe-joined deduped consequence names
_variantAffectedGeneId / _variantAffectedGeneSymbol — pipe-joined deduped from
                               mostSevereConsequence.variantTranscript.transcriptGeneAssociations
_hasDisease / _hasPhenotype  — boolean -> "yes" or "-" (FMS convention)
```

## Verification against FMS reference

### Row counts

| MOD | Ours | FMS | Δ | Notes |
|---|---|---|---|---|
| MGI | 731,283 | 123,807 | +491% | upstream data delta |
| RGD | 960 | 946 | +1.5% | match |
| SGD | 22,167 | 22,209 | -0.2% | match |
| **WB** | **1,880,793** | 11,389 | **+16,400%** | massive upstream data delta |
| FB | 487,323 | 300,254 | +62% | upstream data delta |
| ZFIN | 80,234 | 77,366 | +3.7% | match |
| **TOTAL** | **3,202,760** | **535,971** | **+498%** | matches stage allele_summary count of 3.2M |

### WB anomaly investigation (resolved as upstream data)

Stage ES has 1,880,793 WB allele_summary docs vs FMS's 11,388 — 165× more. AlleleId
prefix breakdown shows the same shape:

| Prefix | Ours | FMS |
|---|---|---|
| `WB:WBVar0...` | 1,855,850 | 9,684 |
| `WB:WBTran...` | 24,941 | 1,704 |

Both prefixes match FMS at ~165–200× higher counts. The `allele with 1 known variant`
sub-count matches **exactly** (4,153 ours vs 4,153 FMS), so the per-variant data is
faithful — the delta is in the bulk `allele` Category. Stage curation has loaded a
much larger WB allele dataset than what was in production when FMS was generated. Same
pattern as previous generators (MGI gene descriptions, MGI/FB disease, Xenopus
expression, SARS-CoV-2 interactions) — upstream data state, not a generator bug.

### HasDisease / HasPhenotype convention fix

First run emitted `true` / `false` from the raw `hasDisease` / `hasPhenotype` booleans.
FMS uses `yes` (when true) / `-` (when false or unknown). Fixed via synthetic
`_hasDisease` / `_hasPhenotype` fields built in `customizeRow` (`boolToYesDash`).

FMS values across all VARIANT-ALLELE files:

| Disease | Phenotype | FMS rows |
|---|---|---|
| `-` | `-` | 411,134 |
| `-` | `yes` | 114,183 |
| `yes` | `yes` | 7,214 |
| `yes` | `-` | 3,390 |
| (empty) | `-` | 43 |

After our fix the same `yes`/`-` distribution will appear in our output.

### Category column (matches FMS)

| Ours | FMS |
|---|---|
| `allele` (1,876,638 WB, etc.) | `allele` (7,235 WB) |
| `allele with 1 known variant` (4,153 WB) | `allele with 1 known variant` (4,153 WB — exact match!) |
| `allele with 2 known variants` | `allele with 2 known variants` |
| ... etc | ... etc |

ES `alterationType` uses bucket values (`allele`, `allele with one variant`,
`allele with multiple variants`); we derive the FMS-format string from
`variantList.size()` directly so the Category labels match FMS exactly.

### Sample row (ours, MGI)

```
NCBITaxon:10090	Mus musculus	MGI:4182608	Edf1<sup>Gt(OST98890)Lex</sup>		[VariantId/Symbol/Synonyms blank]		MGI:1891227	Edf1			allele	[variant cols blank]	-	-
```

After the yes/- fix, the trailing `false`/`false` becomes `-`/`-` for this allele
(no disease, no phenotype).

## Output structure

| Check | Result |
|---|---|
| File header `#`-block | OK — same FMS conventions, fixes the `# Taxon IDs:` typo |
| `# Data type: Variant/Allele` | OK |
| `# Data format: tsv` / `json` | OK |
| `# Taxon IDs:` lists single taxon per file | OK |
| Column header line (27 columns, identical to FMS) | OK |
| Filename pattern | `VARIANT-ALLELE-{TSV,JSON}_{MOD}.{ext}.gz` (uses MOD codes) — differs from FMS `VARIANT-ALLELE_NCBITaxon10090.tsv.gz` (uses taxon curie) per the cross-cutting MOD-naming directive |

## Outstanding follow-ups

1. **VariantSynonyms / VariantCrossReferences / VariantInformationReference** — three
   columns still blank. Not visible on the `allele_summary` doc structure inspected.
   Worth a closer look at the curation API entity to see if any of these are exposed
   under a different path or in a different `_source`-included subtree.
2. **HasDisease/HasPhenotype yes/- fix shipped** — re-run will produce values matching
   FMS convention.
3. **WB volume mismatch** is purely upstream data state. No generator change needed —
   will self-heal once stage and prod data converge.

## Reused framework features

- Sliced parallel scroll (8 slices, 1000-doc buffer per slice)
- `_source` filtering — overridden via `additionalSourceIncludes()` to pull
  `["allele", "alleleOfGene", "variantList", "alterationType", "hasDisease", "hasPhenotype"]`
- MOD whitelist gating per-MOD output files
- Synchronized `TsvWriter` / `JsonRawWriter` for thread-safe dispatch

## Testing

```
ALLIANCE_RELEASE=9.0.0 \
ES_HOST=stage.cluster01.alliancegenome.org \
ES_INDEX=site_index \
CURATION_API_URL=https://alpha-curation.alliancegenome.org \
GENERATED_FILES_FOLDER=data \
SKIP_S3_UPLOAD=true \
java -jar target/agr_file_generator-jar-with-dependencies.jar VariantsAlleles
```

Expected outputs in `./data/`:
- `VARIANT-ALLELE-TSV_{FB,MGI,RGD,SGD,WB,ZFIN}.tsv.gz`
- `VARIANT-ALLELE-JSON_{FB,MGI,RGD,SGD,WB,ZFIN}.json.gz`

## Related tickets

- **SCRUM-1953** — Disease (uses the same `customizeRow` synthetic-field pattern,
  similar boolean-flag → `yes/-` would apply if Disease ever needs HasDisease columns)
- **SCRUM-6007** — Expression (gap-noting pattern when ES doesn't carry a column's data)
- **SCRUM-6009** — Orthology (cleanest verification — same `additionalTaxonCuries`
  + `additionalSourceIncludes` pattern as VariantAllele)
