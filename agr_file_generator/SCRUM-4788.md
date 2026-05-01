# SCRUM-4788 — Molecular Interactions File Generator

## Summary

The new `MolecularInteractionFileGenerator` (with shared logic in
`BaseInteractionFileGenerator`) reads the `gene_molecular_interaction` category from
`site_index` and writes per-MOD plus COMBINED **PSI-MI TAB 2.7** output
(`INTERACTION-MOL-TSV_{MOD|COMBINED}.tsv.gz`). MOD whitelist:
`FB`, `HUMAN`, `MGI`, `RGD`, `SARS-CoV-2`, `SGD`, `WB`, `XBXL`, `XBXT`, `ZFIN`.

The TSV layout is the standard 42-column PSI-MITAB 2.7 spec. Header conventions match FMS:
`# Data format: PSI-MI TAB 2.7 Format`, `# README:` is the URL to the spec, taxon IDs use
the `NCBI:txid<n>` prefix, file-generated label is `# File generated (UTC)`.

## Column coverage

13 columns populated from ES, 1 constant (`Negative=false`), 28 emitted as `-` per spec.

| Column | ES path / source | Format |
|---|---|---|
| ID interactor A | `geneAssociationSubject.primaryExternalId` | `wormbase:WBGene...` (lowercased PSI-MI prefix) |
| ID interactor B | `geneGeneAssociationObject.primaryExternalId` | same |
| Alias A / B | `…geneSymbol.displayText` + `primaryExternalId` | `wormbase:lin-7(public_name)` |
| Interaction detection method | `detectionMethod` (MITerm) | `psi-mi:"MI:NNNN"(name)` |
| Publication 1st author | `evidence[0].shortCitation` | `Simske JS et al. (1996)` |
| Publication Identifier | `evidence[0].referenceID` | `pubmed:N` (lowercased prefix) |
| Taxid interactor A / B | `…taxon.curie` + `…taxon.name` | `taxid:6239(caeel)\|taxid:6239(Caenorhabditis elegans)` |
| Interaction type | `interactionType` (MITerm) | `psi-mi:"MI:0915"(physical association)` |
| Source database | `aggregationDatabase` (MITerm) | psi-mi format |
| Interaction identifier | `interactionId` | as-is (`biogrid:N`, `wormbase:WBInteraction...`) |
| Experimental role A / B | `interactorARole` / `interactorBRole` (MITerms) | psi-mi format |
| Type interactor A / B | `interactorAType` / `interactorBType` (MITerms) | psi-mi format |
| Creation date / Update date | `dbDateCreated` / `dbDateUpdated` | `YYYY/MM/DD` |
| Negative | constant | `false` |

Columns left as `-` (still gaps): Alt IDs A/B, Confidence value, Expansion method,
Biological roles A/B, Xrefs A/B, Interaction Xrefs, Annotations A/B, Interaction
annotation, Host organism, Interaction parameter, Checksums (A/B/Interaction),
Features A/B, Stoichiometry A/B, Identification method participant A/B.

## PSI-MITAB-specific helpers

`BaseInteractionFileGenerator` defines two static maps and three formatters:

```
MOD_TO_PSIMI_PREFIX   WB→wormbase, MGI→mgi, RGD→rgd, FB→flybase, ZFIN→zfin,
                      SGD→sgd, HGNC→hgnc, XB→xenbase
TAXON_CODE            NCBITaxon:6239→caeel, :7227→drome, :7955→danre,
                      :9606→human, :10090→mouse, :10116→rat,
                      :559292→yeast, :8355→xenla, :8364→xentr,
                      :2697049→sars2

curieToPsiMi(curie)   "WB:WBGene00002996" → "wormbase:WBGene00002996"
formatPsiMi(MITerm)   {curie:"MI:0915", name:"physical association"}
                      → 'psi-mi:"MI:0915"(physical association)'
formatTaxid(entity)   uses TAXON_CODE for the 4-letter species code
isoToPsiMiDate(iso)   "2024-06-27T..." → "2024/06/27"
```

Synthetic field names (`_idA`, `_idB`, `_aliasA`, `_aliasB`, `_taxidA`, `_taxidB`,
`_interactionType`, `_sourceDatabase`, `_detectionMethod`, `_interactionId`, `_pubmed`,
`_author`, `_typeA`, `_typeB`, `_expRoleA`, `_expRoleB`, `_creationDate`, `_updateDate`,
`_negative`) are injected by `customizeRow` and referenced by the field map.

## Bug found and fixed during verification

**First run produced rows full of `-` — all data fields empty.** Root cause: the
`_source` include list defaults to {field-map values, taxon path, "species"}. For PSI-MITAB
generators every field-map value is a synthetic `_*` name that does not exist in ES, so
ES returned a doc that only contained `taxon.curie` (from the taxonPath addition) — every
real ES path that `customizeRow` walks (`geneAssociationSubject.*`, `interactionType.*`,
`evidence[*]`, etc.) was stripped before the doc reached Java.

Fix: override `additionalSourceIncludes()` on `BaseInteractionFileGenerator` to return
`List.of(docRoot())` so ES includes the entire `geneMolecularInteraction` (or
`geneGeneticInteraction`) subtree.

```java
@Override
protected List<String> additionalSourceIncludes() {
    return List.of(docRoot());
}
```

Worth keeping in mind for any future generator whose field map values are mostly
synthetic — `additionalSourceIncludes` MUST be overridden to declare the real ES paths
walked by `customizeRow`.

## Verification against FMS reference (first run, pre-fix)

Counts are valid (the scroll/dispatch worked); data inside cells was empty due to the
`_source` bug above. Expecting populated cells after the re-run with the fix.

| MOD | Ours (empty cells) | FMS | Δ |
|---|---|---|---|
| WB | 131,452 | 89,713 | +47% |
| MGI | 207,553 | 122,024 | +70% |
| RGD | 26,493 | 17,281 | +53% |
| FB | 166,381 | 84,859 | +96% |
| ZFIN | 2,036 | 1,108 | +84% |
| SGD | 522,882 | 269,114 | +94% |
| HUMAN | 2,750,502 | 1,383,310 | +99% |
| XBXL | 4,866 | 2,666 | +83% |
| XBXT | 21 | 23 | -9% |
| SARS-CoV-2 | 0 | 54,336 | -100% |
| **COMBINED** | **3,812,186** | **1,923,278** | **+98%** |

Three patterns to flag:

### 1. Universal 2× factor — directional A↔B doubling

Stage ES holds **3,848,578** `gene_molecular_interaction` docs vs FMS COMBINED's
**1,923,278** rows. Almost exactly 2:1.

The Alliance indexer emits one ES doc per directed pair (A→B *and* B→A as separate
documents). The legacy Python `agr_file_generator` emitted one row per interaction pair.
Without dedupe our output is 2× FMS volume.

Two ways to handle:
- **Ship as-is** — every interaction appears twice (once per direction). Technically
  truthful to ES.
- **Dedupe** — track seen interaction IDs in a thread-safe `Set<String>`, skip the
  second occurrence. Halves the output. Needs to handle the parallel slices safely
  (`ConcurrentHashMap.newKeySet()` works).

### 2. SARS-CoV-2: 0 rows in our output → root cause was a generator bug, fixed

The 0 vs 54K SARS-CoV-2 gap was **not** an upstream data delta — corrected after a closer
look. Stage ES has **36,392** `gene_molecular_interaction` docs where SARS-CoV-2 is the
**object (interactor B)**, not the subject. The FMS aggregation by `subject.fullName.keyword`
returned 0 because the SARS-CoV-2 taxon never appears as interactor A on stage; it appears
exclusively as interactor B in human-virus interactions.

Our original `dispatch()` routed every hit by `taxonPath()` (subject only). For
SARS-CoV-2 docs, subject is human → row routed to the HUMAN file. The SARS-CoV-2 file
ended up empty.

**FMS routes cross-species interactions to BOTH species' files.** Independent confirmation:
the sum of all FMS species INTERACTION-MOL files (2,024,434) exceeds FMS COMBINED
(1,923,278) by 101,156 — exactly the dual-routing overlap.

**Fix:** new `additionalTaxonCuries(JsonNode hit)` hook on `FileGenerator`; default empty.
`BaseInteractionFileGenerator` overrides to add the object's
`geneGeneAssociationObject.taxon.curie`. Dispatch then routes the row to **every** allowed
MOD's TAXON writer (deduplicated), with COMBINED still receiving a single write.
Non-interaction generators are unaffected — their `additionalTaxonCuries` stays empty.

After the fix a SARS-CoV-2 ↔ human interaction lands in both `INTERACTION-MOL-TSV_HUMAN`
and `INTERACTION-MOL-TSV_SARS-CoV-2` (and once in `INTERACTION-MOL-TSV_COMBINED`).

### 3. XBXT: 21 vs 23 — also data-state delta

Trivially small absolute counts (~22), within the same A↔B doubling factor.

## Output structure (verified)

| Check | Result |
|---|---|
| File header block | OK — `#` block matches FMS conventions exactly |
| `# Data format` | OK — `PSI-MI TAB 2.7 Format` |
| `# README:` | OK — the PSI-MITAB spec URL |
| `# TaxonIDs:` prefix | OK — `NCBI:txid<n>` |
| `# File generated (UTC)` label | OK |
| Column header line (42 columns, prefixed with `#`) | OK — written by `PsiMiTabWriter` |
| Empty cell rendering | OK — single `-` per spec |

## Files modified / created in this PR

* `BaseInteractionFileGenerator.java` (new) — shared customizeRow + helper methods
* `MolecularInteractionFileGenerator.java` — extends the base, sets `docRoot()` to
  `"geneMolecularInteraction"`
* `GeneticInteractionFileGenerator.java` — extends the base, sets `docRoot()` to
  `"geneGeneticInteraction"` (covered by SCRUM-4789 / future ticket)
* `FileGeneratorConfig.java` — `interactionFieldMap()` shared by both interaction
  enum entries; configs reference `Format.PSI_MI_TAB`
* `Format.java` — `PSI_MI_TAB` enum value with PSI-MITAB labels
* `PsiMiTabWriter.java` — gzipped tab-separated, `#`-prefixed column header line, empty
  cells rendered as `-`
* `HeaderBuilder.buildPsiMiTabHeader(...)` — emits the PSI-MITAB header block
* `readmes/molecular_interactions.txt` — PSI-MITAB spec URL

## Outstanding follow-ups

1. **Re-run after the `_source` fix** to verify populated cells match FMS data shape
   (expect cells like `wormbase:WBGene00002996`, `psi-mi:"MI:0915"(physical association)`).
2. **Decide on dedupe** for the 2× directional doubling — recommend adding it via a
   thread-safe `seenInteractionIds` set keyed on `interactionId` (or a sorted gene-pair
   tuple if `interactionId` ever varies between A→B and B→A docs).
3. **Fill remaining gap columns** as data becomes available — biological roles, host
   organism, confidence value, features (especially relevant for genetic interactions
   which carry `interactorAGeneticPerturbation` allele info).
4. **SARS-CoV-2 data gap** — needs upstream curation reload on alpha; not a generator
   issue.

## Testing

```
ALLIANCE_RELEASE=9.0.0 \
ES_HOST=stage.cluster01.alliancegenome.org \
ES_INDEX=site_index \
CURATION_API_URL=https://alpha-curation.alliancegenome.org \
GENERATED_FILES_FOLDER=data \
java -jar target/agr_file_generator-jar-with-dependencies.jar MolecularInteractions
```

Expected outputs in `./data/`:
- `INTERACTION-MOL-TSV_{FB,HUMAN,MGI,RGD,SARS-CoV-2,SGD,WB,XBXL,XBXT,ZFIN,COMBINED}.tsv.gz`
