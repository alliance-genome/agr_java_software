package org.alliancegenome.indexer.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class GenomeLocationDoclet {
    
    private Long start;
    private Long end;
    private String assembly;
    private String strand;
    private String chromosome;

}
