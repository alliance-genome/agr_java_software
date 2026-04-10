package org.alliancegenome.vep.bio;

import java.util.HashMap;
import java.util.Map;

public class AminoAcid {

	private static final Map<Character, String> ONE_TO_THREE = new HashMap<>();

	static {
		ONE_TO_THREE.put('A', "Ala"); ONE_TO_THREE.put('R', "Arg");
		ONE_TO_THREE.put('N', "Asn"); ONE_TO_THREE.put('D', "Asp");
		ONE_TO_THREE.put('C', "Cys"); ONE_TO_THREE.put('E', "Glu");
		ONE_TO_THREE.put('Q', "Gln"); ONE_TO_THREE.put('G', "Gly");
		ONE_TO_THREE.put('H', "His"); ONE_TO_THREE.put('I', "Ile");
		ONE_TO_THREE.put('L', "Leu"); ONE_TO_THREE.put('K', "Lys");
		ONE_TO_THREE.put('M', "Met"); ONE_TO_THREE.put('F', "Phe");
		ONE_TO_THREE.put('P', "Pro"); ONE_TO_THREE.put('S', "Ser");
		ONE_TO_THREE.put('T', "Thr"); ONE_TO_THREE.put('W', "Trp");
		ONE_TO_THREE.put('Y', "Tyr"); ONE_TO_THREE.put('V', "Val");
		ONE_TO_THREE.put('U', "Sec"); ONE_TO_THREE.put('O', "Pyl");
		ONE_TO_THREE.put('*', "Ter");
	}

	public static String threeLetterCode(char aa) {
		String code = ONE_TO_THREE.get(aa);
		return code != null ? code : "?";
	}

	private AminoAcid() {
	}
}
