#!/bin/bash
# Run Perl VEP with vendored (patched) PM files for tracing.
# Usage: ./run_perl.sh [MOD] [input.vcf] [output.vcf]
#   MOD: SGD (default), WB, ZFIN, FB, HUMAN, MGI, RGD

set -e

DEBUG_DIR="$(cd "$(dirname "$0")" && pwd)"
MOD="${1:-SGD}"
INPUT_VCF="${2:-$DEBUG_DIR/input/sample.vcf}"
OUTPUT_VCF="${3:-$DEBUG_DIR/output/perl.vcf}"
TRACE_LOG="$DEBUG_DIR/output/perl.trace"

DATA_DIR=~/Desktop/FMS/HTP
case "$MOD" in
	SGD)   GFF="$DATA_DIR/GFF/SGD_0.gff.gz";   FASTA="$DATA_DIR/FASTA/SGD_SGDr64.fa" ;;
	WB)    GFF="$DATA_DIR/GFF/WB_4.gff.gz";     FASTA="$DATA_DIR/FASTA/WB_WBcel235.fa" ;;
	ZFIN)  GFF="$DATA_DIR/GFF/ZFIN_0.gff.gz";   FASTA="$DATA_DIR/FASTA/ZFIN_GRCz11.fa" ;;
	FB)    GFF="$DATA_DIR/GFF/FB_0.gff.gz";      FASTA="$DATA_DIR/FASTA/FB_R627.fa" ;;
	HUMAN) GFF="$DATA_DIR/GFF/HUMAN_0.gff.gz";   FASTA="$DATA_DIR/FASTA/HUMAN.fa" ;;
	MGI)   GFF="$DATA_DIR/GFF/MGI_2.gff.gz";     FASTA="$DATA_DIR/FASTA/MGI_GRCm39.fa" ;;
	RGD)   GFF="$DATA_DIR/GFF/RGD_0.gff.gz";     FASTA="$DATA_DIR/FASTA/RGD_mRatBN7.2.fa" ;;
	*)     echo "Unknown MOD: $MOD"; exit 1 ;;
esac

export PERL5LIB="$DEBUG_DIR/vendored_pm:$DEBUG_DIR/vendored_pm/Bio-DB-HTS/blib/lib:$DEBUG_DIR/vendored_pm/Bio-DB-HTS/blib/arch:/usr/local/Cellar/bioperl/1.7.8_5/libexec/lib/perl5:$HOME/git/ensembl/modules:$HOME/git/ensembl-variation/modules:$HOME/git/ensembl-io/modules:$HOME/git/ensembl-funcgen/modules:$HOME/git/ensembl-vep/modules"

echo "Running Perl VEP ($MOD)..."
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
echo "Perl output VCF:"
grep -v "^#" "$OUTPUT_VCF" | head -5
echo ""
echo "Trace entries:"
grep -c "^\[TRACE" "$TRACE_LOG" || echo "0 (no trace lines — add warn statements to PMs)"
