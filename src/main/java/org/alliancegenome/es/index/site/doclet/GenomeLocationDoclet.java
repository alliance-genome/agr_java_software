package org.alliancegenome.es.index.site.doclet;

import org.alliancegenome.es.index.doclet.Doclet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class GenomeLocationDoclet extends Doclet {
    
    private Long start;
    private Long end;
    private String assembly;
    private String strand;
    private String chromosome;

}
