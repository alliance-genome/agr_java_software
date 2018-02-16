package org.alliancegenome.shared.es.document.site_index;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class SearchableItem extends ESDocument {

	protected String category;

	private String primaryId;
	private String name;
	private String name_key;
	private String description;
	private boolean searchable = true;

	@Override
	@JsonIgnore
	public String getDocumentId() {
		return primaryId;
	}

}
