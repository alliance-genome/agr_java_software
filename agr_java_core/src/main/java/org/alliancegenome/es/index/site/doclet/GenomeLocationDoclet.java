package org.alliancegenome.es.index.site.doclet;

import org.alliancegenome.es.index.ESDoclet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class GenomeLocationDoclet extends ESDoclet {
	
	private Long start;
	private Long end;
	private String assembly;
	private String strand;
	private String chromosome;

}
