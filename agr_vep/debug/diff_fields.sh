#!/bin/bash
# Field-by-field comparison of Perl vs Java VCF outputs.
# Aligns by chr:pos:transcript:allele.

DEBUG_DIR="$(cd "$(dirname "$0")" && pwd)"
PERL_VCF="$DEBUG_DIR/output/perl.vcf"
JAVA_VCF="$DEBUG_DIR/output/java.vcf"

# CSQ field order for Perl VEP (with our flags)
FIELDS=("Allele" "Consequence" "IMPACT" "SYMBOL" "Gene" "Feature_type" "Feature" "BIOTYPE" "EXON" "INTRON" "HGVSc" "HGVSp" "cDNA_position" "CDS_position" "Protein_position" "Amino_acids" "Codons" "Existing_variation" "DISTANCE" "STRAND" "FLAGS" "GeneLevelCons" "SYMBOL_SOURCE" "HGNC_ID" "GIVEN_REF" "USED_REF" "BAM_EDIT" "SOURCE" "HGVS_OFFSET" "HGVSg")

# Extract Perl entries: chr|pos|feature|allele → field array
extract_perl() {
    grep -v "^#" "$PERL_VCF" | awk -F'\t' '{
        chr=$1; pos=$2; ref=$4; alt=$5
        n=split($8, info_parts, "CSQ=")
        if (n < 2) next
        csq = info_parts[2]
        # CSQ ends at next semicolon
        sub(/;.*/, "", csq)
        # Multi-allelic: comma-separated
        m = split(csq, entries, ",")
        for (i=1; i<=m; i++) {
            split(entries[i], f, "|")
            feature = f[7]
            allele = f[1]
            print chr "|" pos "|" alt "|" feature "|" allele "\t" entries[i]
        }
    }' | sort
}

# Extract Java entries: same key
extract_java() {
    grep -v "^#" "$JAVA_VCF" | awk -F'\t' '{
        chr=$1; pos=$2; ref=$3; alt=$4
        split($5, f, "|")
        feature = f[7]
        allele = f[1]
        print chr "|" pos "|" alt "|" feature "|" allele "\t" $5
    }' | sort
}

PERL_TMP=$(mktemp)
JAVA_TMP=$(mktemp)
extract_perl > "$PERL_TMP"
extract_java > "$JAVA_TMP"

echo "Field-by-field diff (showing only mismatches):"
echo "==============================================="
echo ""

join -t $'\t' "$PERL_TMP" "$JAVA_TMP" | while IFS=$'\t' read -r key perl_csq java_csq; do
    IFS='|' read -ra P <<< "$perl_csq"
    IFS='|' read -ra J <<< "$java_csq"

    HAS_DIFF=0
    OUTPUT="--- $key ---"
    for ((i=0; i<${#FIELDS[@]}; i++)); do
        pf="${P[$i]:-}"
        jf="${J[$i]:-}"
        if [ "$pf" != "$jf" ]; then
            OUTPUT="$OUTPUT"$'\n'"  ${FIELDS[$i]}: perl='$pf' java='$jf'"
            HAS_DIFF=1
        fi
    done
    if [ $HAS_DIFF -eq 1 ]; then
        echo "$OUTPUT"
        echo ""
    fi
done

rm -f "$PERL_TMP" "$JAVA_TMP"
