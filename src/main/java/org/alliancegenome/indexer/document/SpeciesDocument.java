package org.alliancegenome.indexer.document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpeciesDocument extends ESDocument {

    private String name;
    private String taxonID;
    private int orderID;
	
    @JsonIgnore
	public String getDocumentId() {
		return taxonID;
	}
}
