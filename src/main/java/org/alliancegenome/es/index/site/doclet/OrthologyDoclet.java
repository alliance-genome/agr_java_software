package org.alliancegenome.es.index.site.doclet;

import java.util.List;

import org.alliancegenome.es.index.doclet.Doclet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class OrthologyDoclet extends Doclet {
	
	private String primaryKey;
	private Boolean isBestScore;
	private Boolean isBestRevScore;
	private String confidence;
	
	private String gene1Species;
	private String gene2Species;
	
	private String gene1SpeciesName;
	private String gene2SpeciesName;
	
	private String gene1Symbol;
	private String gene2Symbol;
	
	private String gene1AgrPrimaryId;
	private String gene2AgrPrimaryId;

	private List<String> predictionMethodsNotCalled;
	private List<String> predictionMethodsMatched;
	private List<String> predictionMethodsNotMatched;

}
