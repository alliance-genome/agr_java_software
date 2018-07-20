package org.alliancegenome.es.index.site.document;

import org.alliancegenome.es.index.ESDocument;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class SearchableItemDocument extends ESDocument {

    protected String category;

    private String primaryId;
    private String name;
    @JsonProperty("name_key")
    private String nameKey;
    private String description;

    private List<String> phenotypeStatements = new ArrayList<>();

    private boolean searchable = true;

    @Override
    @JsonIgnore
    public String getDocumentId() {
        return primaryId;
    }
    
    @Override
    @JsonIgnore
    public String getType() {
        return category;
    }

    public void setNameKeyWithSpecies(String nameKey, String species) {
        this.nameKey = nameKey;
        if (species != null) {
            this.nameKey += " (" + species + ")";
        }
    }
}
