package org.alliancegenome.indexer.document.go;

import java.util.List;

import org.alliancegenome.indexer.document.ESDocument;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class GoDocument extends ESDocument {

	private List<String> go_synonyms;
	private List<String> go_genes;
	private String name;
	private List<String> go_species;
	private String href;
	private String id;
	private String name_key;
	private String go_type;
	private String description;

	@JsonIgnore
	public String getDocumentId() {
		return id;
	}
}
