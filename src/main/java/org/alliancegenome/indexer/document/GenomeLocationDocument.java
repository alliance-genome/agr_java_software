package org.alliancegenome.indexer.document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class GenomeLocationDocument extends ESDocument {
	
	private Long start;
	private Long end;
	private String assembly;
	private String strand;
	private String chromosome;
	
	@JsonIgnore
	public String getDocumentId() {
		return null;
	}
}
