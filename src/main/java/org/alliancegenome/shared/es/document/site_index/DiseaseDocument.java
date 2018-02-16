package org.alliancegenome.shared.es.document.site_index;


import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiseaseDocument extends SearchableItem {

	private String doId;
	private String primaryKey;
	private String primaryId;
	private String category = "disease";
	private String name;
	@JsonProperty("name_key")
	private String nameKey;
	private Set<String> parentDiseaseNames;
	private String definition;
	private Date dateProduced;
	private List<String> definitionLinks;
	private List<AnnotationDocument> annotations;
	private List<DiseaseDocument> parents;
	private List<DiseaseDocument> children;
	private List<String> synonyms;
	private List<CrossReferenceDoclet> crossReferences;
	private List<SourceDoclet> sourceList;
	@JsonProperty("disease_group")
	private Set<String> highLevelSlimTermNames = new HashSet<>();

	@JsonIgnore
	public String getDocumentId() {
		return primaryKey;
	}
	
	@Override
	@JsonIgnore
	public String getType() {
		return category;
	}
}
