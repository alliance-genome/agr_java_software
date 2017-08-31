package org.alliancegenome.indexer.document;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class OrthologyDocument extends ESDocument {
	
	private String uuid;
	private boolean isBestScore;
	private boolean isBestRevScore;
	private String confidence;
	
	private int gene1Species;
	private int gene2Species;
	
	private String gene1SpeciesName;
	private String gene2SpeciesName;
	
	private String gene2Symbol;
	private String gene2AgrPrimaryId;

	private List<String> predictionMethodsNotCalled;
	private List<String> predictionMethodsMatched;
	private List<String> predictionMethodsNotMatched;
	
	@Override
	public String getDocumentId() {
		return uuid;
	}
}
