package org.alliancegenome.indexer.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import org.alliancegenome.indexer.document.GeneDocument;

import java.util.List;

@Getter @Setter
public class DiseaseAnnotationDocument extends ESDocument implements Comparable<DiseaseAnnotationDocument> {
	
	private String primaryKey;
	private String diseaseName;
	private String species;
	private String associationType;
	private GeneDocument geneDocument;
	private List<PublicationDocument> publications;
	private String source;
	private List<String> parentTermIDs;
	private List<String> parentTermNames;

	@JsonIgnore
	public String getDocumentId() {
		return primaryKey;
	}

	@Override
	public int compareTo(DiseaseAnnotationDocument doc) {
		return 0;
	}
}
