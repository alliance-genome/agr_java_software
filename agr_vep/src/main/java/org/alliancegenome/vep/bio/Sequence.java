package org.alliancegenome.vep.bio;

public class Sequence {

	private static final char[] COMPLEMENT = new char[128];

	static {
		COMPLEMENT['A'] = 'T'; COMPLEMENT['T'] = 'A';
		COMPLEMENT['G'] = 'C'; COMPLEMENT['C'] = 'G';
		COMPLEMENT['a'] = 't'; COMPLEMENT['t'] = 'a';
		COMPLEMENT['g'] = 'c'; COMPLEMENT['c'] = 'g';
		COMPLEMENT['N'] = 'N'; COMPLEMENT['n'] = 'n';
	}

	public static String reverseComplement(String seq) {
		char[] rc = new char[seq.length()];
		for (int i = 0, j = seq.length() - 1; j >= 0; i++, j--) {
			char c = seq.charAt(j);
			rc[i] = c < 128 ? COMPLEMENT[c] : 'N';
		}
		return new String(rc);
	}

	private Sequence() {
	}

	// ===================================================================
	// Ports from Bio::EnsEMBL::Variation::Utils::Sequence
	// ===================================================================

	/** Result of hgvs_variant_notation or trim_sequences. */
	public static class HgvsNotation {
		public String ref;
		public String alt;
		public int start;
		public int end;
		public String type;
	}

	/**
	 * Sequence.pm line 493-619: hgvs_variant_notation.
	 * Determines HGVS variant type from ref/alt alleles and coordinates.
	 * Used for HGVSg and HGVSc (genomic and coding HGVS).
	 *
	 * @param altAllele The alt allele sequence (empty string for deletion)
	 * @param refSequence The full reference sequence
	 * @param refStart 1-based start in refSequence
	 * @param refEnd 1-based end in refSequence
	 * @param displayStart Display coordinate start
	 * @param displayEnd Display coordinate end
	 * @param dupLookupDirection -1 for preceding (default), +1 for following
	 */
	public static HgvsNotation hgvsVariantNotation(String altAllele, String refSequence,
			int refStart, int refEnd, int displayStart, int displayEnd, int dupLookupDirection) {

		// Line 503-505
		if (displayStart == 0) displayStart = refStart;
		if (displayEnd == 0) displayEnd = refEnd;

		// Line 514-517: ref length
		int refLength = refEnd - refStart + 1;
		if (refLength < 0) refLength = 0;

		// Line 520: remove gap chars from alt
		String alt = altAllele.replace("-", "");
		int altLength = alt.length();

		// Line 526: get ref allele
		String refAllele = (refStart >= 1 && refStart <= refSequence.length())
			? refSequence.substring(refStart - 1, Math.min(refStart - 1 + refLength, refSequence.length()))
			: "";

		// Line 529-532: check alleles are different
		if (refAllele.equals(alt)) return null;

		HgvsNotation n = new HgvsNotation();
		n.start = displayStart;
		n.end = displayEnd;
		n.ref = refAllele;
		n.alt = alt;

		// Line 541-546: deletion
		if (altLength == 0) {
			n.type = "del";
			return n;
		}

		// Line 549-567: equal lengths
		if (refLength == altLength) {
			if (refLength == 1) {
				n.type = ">";
				return n;
			}
			// Check inversion
			String revRef = reverseComplement(refAllele);
			if (alt.equals(revRef)) {
				n.type = "inv";
				return n;
			}
			n.type = "delins";
			return n;
		}

		// Line 570-597: insertion (ref length = 0)
		if (refLength == 0) {
			// Check for duplication
			String prevStr;
			if (dupLookupDirection == -1) {
				int prevStart = refEnd - altLength;
				prevStr = (prevStart >= 0 && prevStart + altLength <= refSequence.length())
					? refSequence.substring(prevStart, prevStart + altLength) : "";
			} else {
				prevStr = (refEnd >= 0 && refEnd + altLength <= refSequence.length())
					? refSequence.substring(refEnd, refEnd + altLength) : "";
			}
			if (prevStr.equals(alt)) {
				if (dupLookupDirection == -1) {
					n.start = displayEnd - altLength + 1;
				} else {
					n.end = displayStart + altLength - 1;
				}
				n.type = "dup";
				return n;
			}
			// Plain insertion
			n.start = displayEnd;
			n.end = displayStart;
			n.type = "ins";
			return n;
		}

		// Line 602-613: check for repeat/multiplication
		if (altLength % refLength == 0) {
			int multiple = altLength / refLength;
			StringBuilder repeated = new StringBuilder();
			for (int i = 0; i < multiple; i++) repeated.append(refAllele);
			if (alt.equals(repeated.toString())) {
				if (multiple == 2) {
					n.type = "dup";
				} else {
					n.type = "[" + multiple + "]";
				}
				return n;
			}
		}

		// Line 616-618: default delins
		n.type = "delins";
		return n;
	}

	/**
	 * Sequence.pm line 635-685: format_hgvs_string.
	 * Formats HGVS notation hash to string. Used for HGVSg and HGVSc.
	 *
	 * @param refName Reference accession (e.g. NC_001133.9)
	 * @param numbering Numbering type ("g" for genomic, "c" for coding)
	 * @param n The notation with type, ref, alt, start, end
	 */
	public static String formatHgvsString(String refName, String numbering, HgvsNotation n) {
		String prefix = refName + ":" + numbering + ".";

		// Line 644-650: coordinates
		String coordinates;
		if (String.valueOf(n.start).equals(String.valueOf(n.end))) {
			coordinates = String.valueOf(n.start);
		} else {
			coordinates = n.start + "_" + n.end;
		}

		// Line 654-657: substitution >
		if (">".equals(n.type) || ("inv".equals(n.type) && n.ref.length() == 1)) {
			return prefix + n.start + n.ref + ">" + n.alt;
		}

		// Line 659-664: del, inv, dup
		if ("del".equals(n.type) || "inv".equals(n.type) || "dup".equals(n.type)) {
			return prefix + coordinates + n.type;
		}

		// Line 666-668: delins
		if ("delins".equals(n.type)) {
			return prefix + coordinates + "delins" + n.alt;
		}

		// Line 670-673: ins
		if ("ins".equals(n.type)) {
			return prefix + coordinates + "ins" + n.alt;
		}

		// Line 675-678: repeat notation [N]
		if (n.type.matches("\\[\\d+\\]")) {
			return prefix + coordinates + n.type;
		}

		return null;
	}

	/**
	 * Sequence.pm line 964-1016: trim_sequences.
	 * Trims common prefix and/or suffix from ref and alt sequences.
	 *
	 * @param ref Reference allele
	 * @param alt Alternative allele
	 * @param start Start coordinate
	 * @param end End coordinate
	 * @param emptyToDash Replace empty strings with "-"
	 * @param endFirst Trim from right first if true
	 * @return [ref, alt, start, end, changed]
	 */
	public static Object[] trimSequences(String ref, String alt, int start, int end,
			boolean emptyToDash, boolean endFirst) {
		boolean changed = false;
		String r = ref;
		String a = alt;
		int s = start;
		int e = end;

		if (endFirst) {
			// Line 976-981: trim from right first
			while (r.length() > 0 && a.length() > 0
					&& r.charAt(r.length() - 1) == a.charAt(a.length() - 1)) {
				r = r.substring(0, r.length() - 1);
				a = a.substring(0, a.length() - 1);
				e--;
				changed = true;
			}
			// Line 984-989: then trim from left
			while (r.length() > 0 && a.length() > 0
					&& r.charAt(0) == a.charAt(0)) {
				r = r.substring(1);
				a = a.substring(1);
				s++;
				changed = true;
			}
		} else {
			// Line 994-999: trim from left first (default)
			while (r.length() > 0 && a.length() > 0
					&& r.charAt(0) == a.charAt(0)) {
				r = r.substring(1);
				a = a.substring(1);
				s++;
				changed = true;
			}
			// Line 1002-1007: then trim from right
			while (r.length() > 0 && a.length() > 0
					&& r.charAt(r.length() - 1) == a.charAt(a.length() - 1)) {
				r = r.substring(0, r.length() - 1);
				a = a.substring(0, a.length() - 1);
				e--;
				changed = true;
			}
		}

		// Line 1010-1013
		if (emptyToDash) {
			if (r.isEmpty()) r = "-";
			if (a.isEmpty()) a = "-";
		}

		return new Object[]{r, a, s, e, changed};
	}
}
