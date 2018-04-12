package org.alliancegenome.es.index.site.document;


import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiseaseDocument extends SearchableItem {

	{ category = "disease"; }
	
	private String doId;
	private String primaryKey;

	private Set<String> parentDiseaseNames;
	private String definition;
	private Date dateProduced;
	private List<String> definitionLinks;
	private List<AnnotationDocument> annotations;
	private List<DiseaseDocument> parents;
	private List<DiseaseDocument> children;
	private List<String> synonyms;
	@JsonProperty("crossReferences")
	private Map<String, List<CrossReferenceDoclet>> crossReferencesMap;
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
