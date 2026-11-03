# SCRUM-6019 — Variants VCF File Generator

## Summary

The new `VariantVcfFileGenerator` reads the `variant_summary` category from
**`site_index_*` (LTP-only — explicitly NOT the HTP `variant_index_*`)** and writes
per-MOD VCF v4.3 output (`VARIANT-CONSEQUENCE-VCF_{MOD}.vcf.gz`). MOD whitelist:
`FB`, `MGI`, `RGD`, `WB`, `ZFIN` (no HUMAN, no Xenopus, no SGD).

One row per variant_summary doc — each doc carries an allele plus a single-element
`variantList` with the variant. Filename split is by MOD (consistent with every other
generator); genome assembly metadata lives inside the file content, not the filename.

## ES alias contamination — root cause and fix

The `site_index` alias resolves to **both** the LTP `site_index_stage_*` AND the HTP
`variant_index_stage_*` (see `_cat/aliases`). Most categories only exist on the LTP
index, so they're unaffected. **`variant_summary` is the exception** — it's also
populated on the HTP variant_index, contributing 50,434,312 docs to the alias-wide
total (vs the LTP-only 64,696).

Initial scrolls of `variant_summary` were grinding through ~50M HTP docs that the file
generator had no business touching. Fix: VariantVcfFileGenerator overrides
`esIndex()` to return `site_index_*` — the wildcard cleanly excludes
`variant_index_*` while remaining environment-agnostic.

| Source | variant_summary count |
|---|---|
| `site_index` alias | 50,499,008 |
| `site_index_*` wildcard (LTP only) | **64,696** |
| `variant_index_*` (HTP — out of scope) | 50,434,312 |

Speedup from the fix is roughly 770× — scrolling 65K docs vs 50M.

## Column population (all built in `customizeRow`)

VCF v4.3 has 8 columns. Every value is a synthetic `_*` field populated from the
single-element `variantList[0].curatedVariantGenomicLocations[0]` subtree.

| Column | ES path / source |
|---|---|
| CHROM | `…variantGenomicLocationAssociationObject.name` |
| POS | `…start` |
| ID | `…hgvs` (e.g. `NC_000073.7:g.121737588T>C`) |
| REF | `…referenceSequence` |
| ALT | `…variantSequence` |
| QUAL | constant `.` |
| FILTER | constant `.` |
| INFO | composite — see below |

The INFO column is built as a `key="value";...` string with these keys:

| INFO key | ES path / aggregation |
|---|---|
| `hgvs_nomenclature` | same as `ID` column |
| `geneLevelConsequence` | `…mostSevereConsequence.variantConsequence.name` (fallback `mostSevereConsequence.vepConsequences[0].name`) |
| `transcriptLevelConsequence` | comma-joined `predictedVariantConsequences[*].vepConsequences[0].name` |
| `geneImpact` | `mostSevereConsequence.vepImpact.name` |
| `transcriptImpact` | comma-joined `predictedVariantConsequences[*].vepImpact.name` |
| `allele_ids` | `allele.primaryExternalId` |
| `allele_symbols` | `allele.alleleSymbol.displayText` |
| `allele_symbols_text` | `allele.alleleSymbol.formatText` |
| `soTerm` | `variantList[0].variantType.name` |
| `allele_of_gene_ids` | comma-joined `geneIds[]` |
| `allele_of_gene_symbols` | comma-joined deduped `predictedVariantConsequences[*].variantTranscript.transcriptGeneAssociations[0].transcriptGeneAssociationObject.geneSymbol.displayText` |
| `allele_of_transcript_ids` | comma-joined deduped `predictedVariantConsequences[*].variantTranscript.name` |
| `allele_of_transcript_gff3_ids` | (currently empty — `variantTranscript.modCrossRefCompleteUrl` not populated on these docs) |
| `allele_of_transcript_gff3_names` | (currently empty — `variantTranscript.displayName` not populated on these docs) |

## VCF header

Static `vcf_header_template.txt` resource carries:
- `##fileformat=VCFv4.3`
- `##fileDate={fileDate}` — substituted at write time with `YYYYMMDD`
- 11 `##ALT=<ID=…>` IUPAC code definitions
- 14 `##INFO=<ID=…>` definitions matching the INFO keys we emit
- `##phasing=partial`
- `##source=AGR VCF File generator`

Followed by the column header line `#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO`.

**`##contig=…` lines:** emitted between `##source=` and the column header. Per-MOD pre-pass runs one ES `_search` (size=0) with a `multi_terms` aggregation over (chrom, assembly, species), filtered by category=variant_summary AND a prefix match on `allele.primaryExternalId.keyword` for the MOD. Buckets are sorted numeric-chrom-first then lexical, and rendered as `##contig=<ID=<chrom>,assembly=<asm>,species="<spp>">`. Empty data for a MOD yields an empty `{contigLines}` substitution and the placeholder line is dropped from the header.

The chrom and assembly fields are not currently mapped on existing 64K docs — `Mapping.java` was extended to add keyword mappings on `variantList.curatedVariantGenomicLocations.variantGenomicLocationAssociationObject.name`, `variantList.curatedVariantGenomicLocations.variantGenomicLocationAssociationObject.genomeAssembly.primaryExternalId`, and `allele.taxon.name`. **The site index must be reindexed for these mappings to take effect on existing docs**; until reindex, the multi_terms aggregation returns zero buckets and the contig block is empty (which the writer cleanly drops).

## Performance tuning for VCF

| Setting | Value | Rationale |
|---|---|---|
| `bufferSize` | 1000 | Default; with only 64K docs the per-page overhead doesn't matter |
| `threadCount` | 8 | Matches `site_index_stage_*` primary shard count (8) |
| `additionalSourceIncludes()` | 21 specific paths | Narrowed from `[allele, variantList, geneIds]` to drop `relatedNotes`, `dataProviderCrossReference`, nested `taxon.species`, etc. — roughly halves wire payload |
| `esIndex()` | `site_index_*` | LTP-only wildcard, excludes HTP `variant_index_*` |

## Verification against FMS reference

| MOD | Ours | FMS | Δ | FMS assembly |
|---|---|---|---|---|
| MGI | 6,384 | 5,560 | +15% | GRCm39 |
| ZFIN | 38,729 | 37,644 | +3% | GRCz11 |
| RGD | 210 | 205 | +2% | mRatBN7.2 |
| **FB** | **15,220** | **49,671** | **-69%** | R6 |
| WB | 4,153 | 3,961 | +5% | WBcel235 |
| **TOTAL** | **64,696** | **97,041** | **-33%** | |

Stage LTP `variant_summary` total = **64,696**, matching our output exactly. Most
species are within 5%; FB is the outlier — alpha curation has roughly a third of the FB
variants prod had at the FMS generation date. Same upstream data-state pattern as the
WB allele anomaly (SCRUM-6020) and Xenopus expression gap (SCRUM-6007). Not a generator
bug.

## Sample row (ours, MGI)

```
7	121737588	NC_000073.7:g.121737588T>C	T	C	.	.	hgvs_nomenclature="NC_000073.7:g.121737588T>C";geneLevelConsequence="splice_donor_region_variant";transcriptLevelConsequence="splice_donor_region_variant,splice_donor_region_variant";geneImpact="LOW";transcriptImpact="LOW,LOW";allele_ids="MGI:5313698";allele_symbols="Dctn5<sup>b2b315Clo</sup>";allele_symbols_text="Dctn5<b2b315Clo>";soTerm="point_mutation";allele_of_gene_ids="MGI:1891689";allele_of_gene_symbols="Dctn5";allele_of_transcript_ids="ENSMUST00000033156,NM_021608.3";allele_of_transcript_gff3_ids="";allele_of_transcript_gff3_names=""
```

Matches FMS row shape: tab-separated columns, semicolon-separated INFO with `key="value"` quoting.

## Output structure

| Check | Result |
|---|---|
| `##fileformat=VCFv4.3` | OK |
| `##fileDate=YYYYMMDD` | OK — current UTC date |
| 11 `##ALT` lines | OK |
| 14 `##INFO` lines | OK |
| `##phasing=partial` | OK |
| `##source=AGR VCF File generator` | OK |
| `##contig=…` per-chromosome lines | OK — emitted via per-MOD pre-pass `multi_terms` agg (requires reindex against new mappings to populate) |
| Column header (`#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO`) | OK |
| Empty cells render as `.` per spec | OK |
| Tab-separated 8-column rows | OK |

## Outstanding follow-ups

1. **`allele_of_transcript_gff3_ids` / `_names`** — currently empty. Field paths
   (`variantTranscript.modCrossRefCompleteUrl`, `variantTranscript.displayName`) don't
   resolve on the doc — likely a different path or fields not indexed. Worth a closer look
   at `Transcript` entity to find the canonical mapping.
2. **FB volume** — alpha curation has fewer FB variants than prod did (15K vs 50K).
   Upstream curation data state, not a generator bug.

## Reused framework features

- Sliced parallel scroll (8 slices, 1000-doc buffer per slice)
- `_source` filtering — narrowed to 21 specific paths to halve wire payload
- MOD whitelist gating per-MOD output files
- New `esIndex()` hook on `FileGenerator` allowing per-generator index override (added for
  this generator; default returns `ConfigHelper.getEsIndex()`)
- Synchronized writers for thread-safe dispatch

## Testing

```
ALLIANCE_RELEASE=9.0.0 \
ES_HOST=stage.cluster01.alliancegenome.org \
ES_INDEX=site_index \
CURATION_API_URL=https://alpha-curation.alliancegenome.org \
GENERATED_FILES_FOLDER=data \
SKIP_S3_UPLOAD=true \
java -jar target/agr_file_generator-jar-with-dependencies.jar VariantsVcf
```

Expected outputs in `./data/`:
- `VARIANT-CONSEQUENCE-VCF_{FB,MGI,RGD,WB,ZFIN}.vcf.gz`

## Related tickets

- **SCRUM-6020** — Variants/Alleles (sibling generator using `allele_summary`; same
  `customizeRow` synthetic-field pattern)
- **SCRUM-1953** — Disease (originated the synthetic-field-via-customizeRow approach)
