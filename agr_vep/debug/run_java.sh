#!/bin/bash
# Run Java VEP debug runner with tracing.
# Usage: ./run_java.sh [MOD] [input.vcf] [output.vcf]
#   MOD: SGD (default), WB, ZFIN, FB, HUMAN, MGI, RGD

set -e

DEBUG_DIR="$(cd "$(dirname "$0")" && pwd)"
MOD="${1:-SGD}"
INPUT_VCF="${2:-$DEBUG_DIR/input/sample.vcf}"
OUTPUT_VCF="${3:-$DEBUG_DIR/output/java.vcf}"
TRACE_LOG="$DEBUG_DIR/output/java.trace"

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

cd "$DEBUG_DIR/../.."

echo "Compiling..."
mvn -pl agr_vep clean compile -am -q 2>&1 | tail -3

CP_FILE=/tmp/agr_vep_cp.txt
mvn -pl agr_vep dependency:build-classpath -am -q -Dmdep.outputFile=$CP_FILE 2>/dev/null
DEP_CP=$(cat $CP_FILE)
CP="agr_vep/target/classes:agr_java_core/target/classes:$DEP_CP"

echo "Running Java VEP ($MOD)..."
echo "  Input:  $INPUT_VCF"
echo "  Output: $OUTPUT_VCF"
echo "  Trace:  $TRACE_LOG"

java -cp "$CP" -Dvep.trace=true \
    org.alliancegenome.vep.debug.DebugVep \
    "$INPUT_VCF" "$GFF" "$FASTA" "$OUTPUT_VCF" 2> "$TRACE_LOG"

echo "Done."
echo ""
echo "Java output VCF:"
grep -v "^#" "$OUTPUT_VCF" | head -5
echo ""
echo "Trace entries:"
grep -c "^\[TRACE" "$TRACE_LOG" || echo "0"
