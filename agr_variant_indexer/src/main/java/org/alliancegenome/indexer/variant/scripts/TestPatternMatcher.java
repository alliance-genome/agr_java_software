package org.alliancegenome.indexer.variant.scripts;

import java.util.regex.Pattern;

public class TestPatternMatcher {

    public static void main(String[] args) {
        Pattern validAlleles = Pattern.compile("[ACGTN\\-]+");
        
        String s = "CGTATAATTNTN-";
        
        System.out.println(validAlleles.matcher(s).matches());

    }

}
