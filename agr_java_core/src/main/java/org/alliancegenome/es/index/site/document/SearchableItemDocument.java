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

    String primaryKey;
    String name;
    @JsonProperty("name_key")
    String nameKey;
    String nameText;
    String description;
    String localId;
    String globalId;
    String modCrossRefCompleteUrl;
    String species;


    Set<String> diseases = new HashSet<>();
    Set<String> diseasesAgrSlim = new HashSet<>();
    Set<String> diseasesWithParents = new HashSet<>();
    Set<String> alleles = new HashSet<>();
    Set<String> genes = new HashSet<>();
    Set<String> models = new HashSet<>();
    Set<String> phenotypeStatements = new HashSet<>();
    Set<String> secondaryIds = new HashSet<>();
    Set<String> synonyms = new HashSet<>();

    boolean searchable = true;

    @Override
    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

    @Override
    @JsonIgnore
    public String getType() {
        return "searchable_item";
    }

    public void setNameKeyWithSpecies(String nameKey, String species) {
        this.nameKey = nameKey;
        if (species != null) {
            this.nameKey += " (" + species + ")";
        }
    }

}
