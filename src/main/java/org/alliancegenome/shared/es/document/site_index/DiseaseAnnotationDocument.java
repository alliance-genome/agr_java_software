package org.alliancegenome.shared.es.document.site_index;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class DiseaseAnnotationDocument extends ESDocument implements Comparable<DiseaseAnnotationDocument> {

	private String category = "diseaseAnnotation";
	private String primaryKey;
	private String diseaseID;
	private Set<String> parentDiseaseIDs;
	private String diseaseName;
	@JsonProperty("disease_species")
	private SpeciesDoclet species;
	private String associationType;
	private GeneDocument geneDocument;
	private List<PublicationDoclet> publications;
	private List<String> parentTermIDs;
	private List<String> parentTermNames;
	private SourceDoclet source;

	@Override
	@JsonIgnore
	public String getDocumentId() {
		return primaryKey;
	}
	
	@Override
	@JsonIgnore
	public String getType() {
		return category;
	}

	@Override
	public int compareTo(DiseaseAnnotationDocument doc) {
		return 0;
	}
	
}
