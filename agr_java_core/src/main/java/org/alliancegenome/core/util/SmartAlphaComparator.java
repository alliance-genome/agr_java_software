package org.alliancegenome.core.util;

import java.util.Comparator;

/**
 * Smart alpha string comparator (a.k.a. natural sort): walks two strings in alternating digit / non-digit chunks, compares digit chunks as integers (leading zeros ignored) and non-digit chunks case-insensitively.
 *
 * Designed for chromosome / contig sorting where lexical and pure-numeric comparators both fall short. Examples (each line ordered ascending):
 *   - 2L, 2R, 3L, 3R, 4, X, Y                  (Drosophila — letter-suffixed numerics come before pure numerics with the same prefix, then non-numeric)
 *   - 1, 2, ..., 9, 10, 11, ..., 19, MT, X, Y  (Mouse — pure numerics in numeric order, then non-numeric lexical)
 *   - I, II, III, IV, MtDNA, V, X              (Worm — purely non-numeric chunks fall back to case-insensitive lexical)
 *
 * Null and empty strings sort last. Mirrors the spirit of the ES {@code smart_alpha_sort} normalizer (zero-pad digit runs, lowercase) but without the analyzer dependency, so it works wherever a Java {@code Comparator} is needed (e.g. the VCF file generator's contig pre-pass and CHROM-then-POS row sort).
 */
public final class SmartAlphaComparator implements Comparator<String> {

	public static final SmartAlphaComparator INSTANCE = new SmartAlphaComparator();

	private SmartAlphaComparator() {
	}

	@Override
	public int compare(String a, String b) {
		if (a == null && b == null) {
			return 0;
		}
		if (a == null || a.isEmpty()) {
			return 1;
		}
		if (b == null || b.isEmpty()) {
			return -1;
		}

		int ai = 0;
		int bi = 0;
		int alen = a.length();
		int blen = b.length();

		while (ai < alen && bi < blen) {
			char ac = a.charAt(ai);
			char bc = b.charAt(bi);
			boolean ad = Character.isDigit(ac);
			boolean bd = Character.isDigit(bc);

			if (ad && bd) {
				int aStart = ai;
				while (ai < alen && Character.isDigit(a.charAt(ai))) {
					ai++;
				}
				int bStart = bi;
				while (bi < blen && Character.isDigit(b.charAt(bi))) {
					bi++;
				}
				// Strip leading zeros for value comparison, fall back to length-then-lex.
				String aNum = stripLeadingZeros(a, aStart, ai);
				String bNum = stripLeadingZeros(b, bStart, bi);
				if (aNum.length() != bNum.length()) {
					return Integer.compare(aNum.length(), bNum.length());
				}
				int cmp = aNum.compareTo(bNum);
				if (cmp != 0) {
					return cmp;
				}
				continue;
			}

			if (ad != bd) {
				// Digit vs non-digit at the same position — digits sort first (so "4 < X", and earlier-shared chunks have already settled "2L < 4" by the time we get here).
				return ad ? -1 : 1;
			}

			int cmp = Character.compare(Character.toLowerCase(ac), Character.toLowerCase(bc));
			if (cmp != 0) {
				return cmp;
			}
			ai++;
			bi++;
		}

		return Integer.compare(alen - ai, blen - bi);
	}

	private static String stripLeadingZeros(String s, int from, int to) {
		int i = from;
		while (i < to - 1 && s.charAt(i) == '0') {
			i++;
		}
		return s.substring(i, to);
	}
}
