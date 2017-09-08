package org.alliancegenome.indexer.document;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DiseaseAnnotationDocument extends ESDocument implements Comparable<DiseaseAnnotationDocument> {
	
	private String primaryKey;
	private String diseaseID;
	private List<String> parentDiseaseIDs;
	private String diseaseName;
	private SpeciesDoclet species;
	private String associationType;
	private GeneDocument geneDocument;
	private List<PublicationDoclet> publications;
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
