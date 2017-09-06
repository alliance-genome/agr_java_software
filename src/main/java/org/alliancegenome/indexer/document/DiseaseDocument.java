package org.alliancegenome.indexer.document;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiseaseDocument extends ESDocument {

	private String category;
	private String doId;
	private String primaryKey;
	private String name;
	private String definition;
	private String species;
	private List<AnnotationDocument> annotations;
	private List<DiseaseDocument> parents;
	private List<DiseaseDocument> children;
	private List<String> synonyms;
	private List<String> external_ids;

	@JsonIgnore
	public String getDocumentId() {
		return primaryKey;
	}
}
