package org.alliancegenome.es.index.site.document;

import java.util.HashSet;
import java.util.Set;

import org.alliancegenome.es.index.ESDocument;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class SearchableItemDocument extends ESDocument {

    protected String category;

    String primaryId;
    String name;
    @JsonProperty("name_key")
    String nameKey;
    String description;

    Set<String> diseases = new HashSet<>();
    Set<String> alleles = new HashSet<>();
    Set<String> genes = new HashSet<>();
    Set<String> phenotypeStatements = new HashSet<>();

    boolean searchable = true;

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
