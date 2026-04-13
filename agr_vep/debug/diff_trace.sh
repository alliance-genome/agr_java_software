#!/bin/bash
# Diff Perl and Java traces side-by-side.
# Filters to [TRACE] lines only and aligns them.

DEBUG_DIR="$(cd "$(dirname "$0")" && pwd)"
PERL_TRACE="$DEBUG_DIR/output/perl.trace"
JAVA_TRACE="$DEBUG_DIR/output/java.trace"

if [ ! -f "$PERL_TRACE" ] || [ ! -f "$JAVA_TRACE" ]; then
    echo "ERROR: Run run_perl.sh and run_java.sh first."
    exit 1
fi

PERL_FILTERED=$(mktemp)
JAVA_FILTERED=$(mktemp)
grep "^\[TRACE\]" "$PERL_TRACE" > "$PERL_FILTERED"
grep "^\[TRACE\]" "$JAVA_TRACE" > "$JAVA_FILTERED"

echo "Perl trace lines: $(wc -l < "$PERL_FILTERED")"
echo "Java trace lines: $(wc -l < "$JAVA_FILTERED")"
echo ""
echo "Side-by-side diff (perl | java):"
echo "================================"
diff -y --width=200 "$PERL_FILTERED" "$JAVA_FILTERED" | head -50

rm "$PERL_FILTERED" "$JAVA_FILTERED"
