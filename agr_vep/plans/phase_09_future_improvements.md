# Phase 9: Future Improvements

**Status**: Backlog

## Items

1. **Switch GFF3 parser to htsjdk Gff3Codec** — currently using a custom line-by-line parser for flexibility around MOD-specific attribute naming and malformed URL encoding. Could switch to htsjdk's `Gff3Codec` for better standards compliance and less code to maintain.

2. **Handle malformed VCF records** — HUMAN VCF has a malformed record at ~line 549K with trailing comma in ALT alleles (`AT,ATT,`). Need to catch and skip/log malformed records rather than crashing.

3. **HGVSg accession versioning** — FASTA headers have unversioned NC accessions (e.g., `NC_001133`) but VEP output uses versioned (e.g., `NC_001133.9`). Need to source versioned accessions, possibly from the RefSeq FASTA that RGD uses or from an NCBI assembly report file.

4. **Chromosome name case normalization** — SGD input VCF uses `chrMt` but GFF uses `chrmt`. Need case-insensitive matching or normalization.

5. **Transcript filtering** — Our GFF3 parser registers tRNA, snoRNA, ncRNA, etc. as transcripts, but VEP only annotates against protein_coding transcripts for SGD. Need to determine which transcript types to include per species.

6. **HGVSc transcript version** — VEP appends the version from its database/cache, NOT a default `.1`. Our current code appends `.1` when no version is in the ID. Need to source the actual transcript version from the GFF3 or a lookup table.

7. **`--flag_pick_allele_gene` PICK flag** — VEP sets `PICK=1` on the best transcript per allele+gene using criteria: MANE_Select > Canonical > TSL > protein_coding biotype > severity > transcript length. We don't implement PICK yet — we only need Gene_level_consequence (Phase 7).

8. **Intronic HGVS tie-breaking** — When equidistant from two exon boundaries, VEP favors upstream exon (positive strand) or downstream exon (negative strand). Need to verify our implementation matches.

9. **protein_altering_variant over-detection** — Our local codon allele comparison produces 303 protein_altering_variant vs VEP's 26. Need to exactly replicate VEP's `_get_codon_alleles` logic from `TranscriptVariationAllele.pm` lines 841-877, including proper codon boundary calculation via `translation_start`/`translation_end`.

10. **Compound consequence gaps** — Missing: `coding_sequence_variant&5_prime_UTR_variant` (15), `coding_sequence_variant&3_prime_UTR_variant` (11), `frameshift_variant&splice_region_variant` (6), `stop_gained&frameshift_variant` (14). These require variants that span multiple regions (CDS+UTR boundary, or frameshift near splice site).

11. **Frameshift compound over-detection** — `frameshift&start_lost` (47 vs 33 in ORIG), `frameshift&stop_lost` (39 vs 25). Need stricter check for whether the variant actually overlaps the start/stop codon region.
