# Prepare UploadVEP Files for Comparison

Process to generate per-MOD comparison bundles for Jeff from the latest benchmark run.

## Prerequisites

- Benchmark has been run, producing:
  - `agr_vep/src/test/resources/{MOD}/diff/input.vcf` — diff variants (lines that differ between Java and Perl)
  - `agr_vep/src/test/resources/{MOD}/diff/expected.vcf` — Perl output filtered to those diff variants
- Java pipeline output files available at: `~/Desktop/FMS/HTP/OUTPUT/{MOD}.vep.vcf.gz`

## Output

Files placed in `~/Desktop/UploadVEP/{MOD}/`:

```
{MOD}/
  input.vcf         # variants that differ (from benchmark diff)
  perl_output.vcf   # Perl VEP output for those variants (copied from expected.vcf)
  java_output.vcf   # Java VEP output filtered to those same variants
```

MODs: `FB`, `HUMAN`, `MGI`, `RGD`, `SGD`, `WB`, `ZFIN`

## Process

```bash
DIFF_BASE="$HOME/git/agr_java_software/agr_vep/src/test/resources"
JAVA_BASE="$HOME/Desktop/FMS/HTP/OUTPUT"
OUT_BASE="$HOME/Desktop/UploadVEP"

for MOD in FB HUMAN MGI RGD SGD WB ZFIN; do
  mkdir -p "$OUT_BASE/$MOD"

  # Copy input (diff variants) and perl output (already filtered)
  cp "$DIFF_BASE/$MOD/diff/input.vcf" "$OUT_BASE/$MOD/input.vcf"
  cp "$DIFF_BASE/$MOD/diff/expected.vcf" "$OUT_BASE/$MOD/perl_output.vcf"

  # Filter Java output to only the diff variants (match on CHROM+POS+REF+ALT)
  awk -F'\t' '
    NR==FNR { if ($0 !~ /^#/) keys[$1"\t"$2"\t"$4"\t"$5]=1; next }
    /^#/ { print; next }
    { if (keys[$1"\t"$2"\t"$4"\t"$5]) print }
  ' "$DIFF_BASE/$MOD/diff/input.vcf" <(gzcat "$JAVA_BASE/$MOD.vep.vcf.gz") \
    > "$OUT_BASE/$MOD/java_output.vcf"

  # Verify counts
  INPUT=$(grep -c -v '^#' "$OUT_BASE/$MOD/input.vcf")
  PERL=$(grep -c -v '^#' "$OUT_BASE/$MOD/perl_output.vcf")
  JAVA=$(grep -c -v '^#' "$OUT_BASE/$MOD/java_output.vcf")
  echo "$MOD: input=$INPUT perl=$PERL java=$JAVA"
done
```

## Upload to S3

```bash
AWS_PROFILE=agr aws s3 sync ~/Desktop/UploadVEP/ s3://mod-datadumps/java_vep/ --exclude ".DS_Store" --exclude "*.swp"
```

## Notes

- HUMAN and MGI are large — the Java .gz files are 2.2GB and 6.3GB respectively. Filtering takes several minutes.
- Key matching is CHROM (col1) + POS (col2) + REF (col4) + ALT (col5).
- A small number of Java output lines may not match if the Java pipeline didn't emit a variant that Perl did (e.g., SGD had 5 fewer Java lines than input).
- The `expected.vcf` files are already filtered to match `input.vcf` (same line counts), so they can be copied directly as `perl_output.vcf`.
