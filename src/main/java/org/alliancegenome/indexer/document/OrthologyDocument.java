package org.alliancegenome.indexer.document;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class OrthologyDocument extends ESDocument {
	
	private String primaryKey;
	private boolean isBestScore;
	private boolean isBestRevScore;
	private String confidence;
	
	private String gene1Species;
	private String gene2Species;
	
	private String gene1SpeciesName;
	private String gene2SpeciesName;
	
	private String gene2Symbol;
	private String gene2AgrPrimaryId;

	private List<String> predictionMethodsNotCalled;
	private List<String> predictionMethodsMatched;
	private List<String> predictionMethodsNotMatched;
	
	@JsonIgnore
	public String getDocumentId() {
		return primaryKey;
	}
}
