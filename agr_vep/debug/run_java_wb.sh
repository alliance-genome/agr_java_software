#!/bin/bash
# Run Java VEP debug runner with tracing.
# Usage: ./run_java.sh [input.vcf] [output.vcf]

set -e

DEBUG_DIR="$(cd "$(dirname "$0")" && pwd)"
INPUT_VCF="${1:-$DEBUG_DIR/input/sample.vcf}"
OUTPUT_VCF="${2:-$DEBUG_DIR/output/java.vcf}"
TRACE_LOG="$DEBUG_DIR/output/java.trace"

GFF=~/Desktop/FMS/HTP/GFF/WB_4.gff.gz
FASTA=~/Desktop/FMS/HTP/FASTA/WB_WBcel235.fa

cd "$DEBUG_DIR/../.."

echo "Compiling..."
mvn -pl agr_vep compile -am -q 2>&1 | tail -3

# Build full classpath including dependencies
CP_FILE=/tmp/agr_vep_cp.txt
mvn -pl agr_vep dependency:build-classpath -am -q -Dmdep.outputFile=$CP_FILE 2>/dev/null
DEP_CP=$(cat $CP_FILE)
CP="agr_vep/target/classes:agr_java_core/target/classes:$DEP_CP"

echo "Running Java VEP..."
echo "  Input:  $INPUT_VCF"
echo "  Output: $OUTPUT_VCF"
echo "  Trace:  $TRACE_LOG"

java -cp "$CP" -Dvep.trace=true \
    org.alliancegenome.vep.debug.DebugVep \
    "$INPUT_VCF" "$GFF" "$FASTA" "$OUTPUT_VCF" 2> "$TRACE_LOG"

echo "Done."
echo ""
echo "Java output VCF:"
head -5 "$OUTPUT_VCF"
echo ""
echo "Trace entries:"
grep -c "^\[TRACE" "$TRACE_LOG" || echo "0"
