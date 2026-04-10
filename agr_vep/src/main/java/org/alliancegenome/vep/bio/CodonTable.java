package org.alliancegenome.vep.bio;

import java.util.HashMap;
import java.util.Map;

public class CodonTable {

	/** Standard genetic code (NCBI translation table 1) */
	private static final Map<String, Character> CODON_TO_AA = new HashMap<>();

	/** Vertebrate mitochondrial code (NCBI translation table 2) */
	private static final Map<String, Character> MITO_CODON_TO_AA = new HashMap<>();

	static {
		String[] codons = {
			"TTT", "F", "TTC", "F", "TTA", "L", "TTG", "L",
			"CTT", "L", "CTC", "L", "CTA", "L", "CTG", "L",
			"ATT", "I", "ATC", "I", "ATA", "I", "ATG", "M",
			"GTT", "V", "GTC", "V", "GTA", "V", "GTG", "V",
			"TCT", "S", "TCC", "S", "TCA", "S", "TCG", "S",
			"CCT", "P", "CCC", "P", "CCA", "P", "CCG", "P",
			"ACT", "T", "ACC", "T", "ACA", "T", "ACG", "T",
			"GCT", "A", "GCC", "A", "GCA", "A", "GCG", "A",
			"TAT", "Y", "TAC", "Y", "TAA", "*", "TAG", "*",
			"CAT", "H", "CAC", "H", "CAA", "Q", "CAG", "Q",
			"AAT", "N", "AAC", "N", "AAA", "K", "AAG", "K",
			"GAT", "D", "GAC", "D", "GAA", "E", "GAG", "E",
			"TGT", "C", "TGC", "C", "TGA", "*", "TGG", "W",
			"CGT", "R", "CGC", "R", "CGA", "R", "CGG", "R",
			"AGT", "S", "AGC", "S", "AGA", "R", "AGG", "R",
			"GGT", "G", "GGC", "G", "GGA", "G", "GGG", "G"
		};
		for (int i = 0; i < codons.length; i += 2) {
			CODON_TO_AA.put(codons[i], codons[i + 1].charAt(0));
		}

		// Vertebrate mitochondrial: same as standard except:
		// TGA=W (not stop), AGA=* (stop), AGG=* (stop), ATA=M (not I)
		MITO_CODON_TO_AA.putAll(CODON_TO_AA);
		MITO_CODON_TO_AA.put("TGA", 'W');
		MITO_CODON_TO_AA.put("AGA", '*');
		MITO_CODON_TO_AA.put("AGG", '*');
		MITO_CODON_TO_AA.put("ATA", 'M');
	}

	public static char translate(String codon) {
		return translate(codon, 1);
	}

	public static char translate(String codon, int table) {
		if (codon == null || codon.length() != 3) return '?';
		Map<String, Character> map = (table == 2) ? MITO_CODON_TO_AA : CODON_TO_AA;
		Character aa = map.get(codon.toUpperCase());
		return aa != null ? aa : '?';
	}

	public static boolean isStop(String codon) {
		return translate(codon) == '*';
	}

	public static boolean isStop(String codon, int table) {
		return translate(codon, table) == '*';
	}

	public static boolean isStart(String codon) {
		return "ATG".equalsIgnoreCase(codon);
	}

	private CodonTable() {
	}
}
