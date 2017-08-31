package org.alliancegenome.indexer.document;

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
	
	@Override
	public String getDocumentId() {
		return null;
	}
}
