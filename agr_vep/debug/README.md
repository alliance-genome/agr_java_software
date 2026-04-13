# VEP Debug Trace Infrastructure

Side-by-side trace of Perl VEP vs our Java VEP. Add `[TRACE] ...` log lines to matching code paths in both sides, run on a small VCF, then diff to find divergences.

## Layout

```
debug/
  input/sample.vcf           — small VCF with issue variants
  vendored_pm/Bio/EnsEMBL/   — patched copies of Perl PMs (PERL5LIB takes precedence)
  output/
    perl.vcf, perl.trace
    java.vcf, java.trace
  run_perl.sh                — runs Perl VEP with vendored PMs
  run_java.sh                — runs Java DebugVep with -Dvep.trace=true
  diff_trace.sh              — diffs the two trace files
```

## Setup (one-time)

- Ensembl repos must all be on **release/111**:
  - `~/git/ensembl-vep`
  - `~/git/ensembl-variation`
  - `~/git/ensembl`
- BioPerl from Homebrew: `/usr/local/Cellar/bioperl/1.7.8_5`
- Bio::DB::HTS built at `/tmp/Bio-DB-HTS` (run `cd /tmp/Bio-DB-HTS && HTSLIB_DIR=/usr/local/opt/htslib ./Build` if needed)

## Trace format

Both sides write to stderr:

```
[TRACE] Module.method key1=val1 key2=val2 ...
```

Examples:
```
[TRACE] Mapper.map_coordinates id=genome start=100273 end=100273 strand=-1 type=genomic from=cdna to=genomic
[TRACE]   -> Coord id=cdna start=873 end=873 strand=1
[TRACE] BTV.cdna_start tr=SGD:S000287705 vf=100273:100273 cdna_start=873 cdna_end=873
[TRACE] TVA.codon tr=SGD:S000287705 allele=A codon=TAT
```

## Adding new trace points

1. **Perl side**: edit the relevant PM in `debug/vendored_pm/`, add `warn "[TRACE] Module.method key=$value\n";`
2. **Java side**: in the matching method, add `Trace.log("Module.method", "key=%s", value);`

Use the same tag names and key names so diff aligns.

## Workflow

```bash
./run_perl.sh   # produces perl.vcf + perl.trace
./run_java.sh   # produces java.vcf + java.trace
./diff_trace.sh # side-by-side comparison
```

## Currently instrumented

| Method | Perl PM | Java class |
|---|---|---|
| `Mapper.map_coordinates` | `Bio::EnsEMBL::Mapper` | `Mapper.java` |
| `BTV.cdna_start` | `BaseTranscriptVariation` | `BaseTranscriptVariation.java` |
| `BTV.cds_start` | `BaseTranscriptVariation` | `BaseTranscriptVariation.java` |
| `BTV.translation_start` | `BaseTranscriptVariation` | `BaseTranscriptVariation.java` |
| `TVA.peptide` | `TranscriptVariationAllele` | `TranscriptVariationAllele.java` |
| `TVA.codon` | `TranscriptVariationAllele` | `TranscriptVariationAllele.java` |
