#!/bin/bash
# Run Perl VEP on WB data with vendored PMs for tracing.

set -e

DEBUG_DIR="$(cd "$(dirname "$0")" && pwd)"
INPUT_VCF="${1:-$DEBUG_DIR/input/sample.vcf}"
OUTPUT_VCF="${2:-$DEBUG_DIR/output/perl.vcf}"
TRACE_LOG="$DEBUG_DIR/output/perl.trace"

GFF=~/Desktop/FMS/HTP/GFF/WB_4.gff.gz
FASTA=~/Desktop/FMS/HTP/FASTA/WB_WBcel235.fa

export PERL5LIB="$DEBUG_DIR/vendored_pm:$DEBUG_DIR/vendored_pm/Bio-DB-HTS/blib/lib:$DEBUG_DIR/vendored_pm/Bio-DB-HTS/blib/arch:/usr/local/Cellar/bioperl/1.7.8_5/libexec/lib/perl5:$HOME/git/ensembl/modules:$HOME/git/ensembl-variation/modules:$HOME/git/ensembl-io/modules:$HOME/git/ensembl-funcgen/modules:$HOME/git/ensembl-vep/modules"

echo "Running Perl VEP (WB)..."
echo "  Input:  $INPUT_VCF"
echo "  Output: $OUTPUT_VCF"
echo "  Trace:  $TRACE_LOG"

perl "$HOME/git/ensembl-vep/vep" \
    -i "$INPUT_VCF" \
    -gff "$GFF" \
    --format vcf \
    -fasta "$FASTA" \
    --vcf --hgvs --hgvsg -shift_hgvs=0 \
    --symbol --numbers --distance 0 \
    --flag_pick_allele_gene --remove_hgvsp_version \
    --output_file "$OUTPUT_VCF" \
    --force_overwrite --safe --no_stats \
    2> "$TRACE_LOG"

echo "Done."
echo ""
grep -v "^#" "$OUTPUT_VCF" | head -3
echo ""
echo "Trace entries:"
grep -c "^\[TRACE" "$TRACE_LOG" || echo 0
