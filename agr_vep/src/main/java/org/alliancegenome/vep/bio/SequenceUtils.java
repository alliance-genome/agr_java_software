package org.alliancegenome.vep.bio;

public class SequenceUtils {

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

	private SequenceUtils() {
	}
}
