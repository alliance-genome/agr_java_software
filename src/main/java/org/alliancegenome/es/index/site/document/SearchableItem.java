package org.alliancegenome.es.index.site.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class SearchableItem extends ESDocument {

	protected String category;

	private String primaryId;
	private String name;
	@JsonProperty("name_key")
	private String nameKey;
	private String description;
	private boolean searchable = true;

	@Override
	@JsonIgnore
	public String getDocumentId() {
		return primaryId;
	}

	public void setNameKeyWithSpecies(String nameKey, String species) {
		this.nameKey = nameKey;
		if (species != null) {
			this.nameKey += " (" + species + ")";
		}
	}
}
