package org.alliancegenome.indexer.document.disease;

import org.alliancegenome.indexer.document.ESDocument;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.indexer.document.gene.GeneDocument;

import java.util.List;

@Getter @Setter
public class DiseaseDocument extends ESDocument {
	
	private String primaryKey;
	private String name;
	private List<GeneDocument> geneDocuments;

	@JsonIgnore
	public String getDocumentId() {
		return primaryKey;
	}
}
