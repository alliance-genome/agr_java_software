package org.alliancegenome.es.index.site.document;

import java.util.List;
import java.util.Set;

import org.alliancegenome.es.index.site.doclet.PublicationDoclet;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;
import org.alliancegenome.es.index.site.doclet.SpeciesDoclet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

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
	private FeatureDocument featureDocument;
	private List<PublicationDoclet> publications;
	private List<String> parentTermIDs;
	private List<String> parentTermNames;
	private SourceDoclet source;
	private boolean searchable = true;

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
