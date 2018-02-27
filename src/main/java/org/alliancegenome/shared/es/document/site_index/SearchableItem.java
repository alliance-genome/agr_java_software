package org.alliancegenome.shared.es.document.site_index;

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

	public void setNameKeyWithSpecies(String species) {
		if(name == null)
			throw new RuntimeException("You first have to populate the name attribute before calling this method!");
		nameKey = name;
		if (species != null) {
			nameKey += " (" + species + ")";
		}
	}
}
