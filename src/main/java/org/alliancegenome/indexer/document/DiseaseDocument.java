package org.alliancegenome.indexer.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class DiseaseDocument {

    private String doId;
    private String primaryKey;
    private String primaryId;
    private String category = "disease";
    private String name;
    @JsonProperty("name_key")
    private String nameKey;
    private String definition;
    private List<String> definitionLinks;
    private List<DiseaseDocument> parents;
    private List<DiseaseDocument> children;
    private List<String> synonyms;
    private boolean searchable = true;
    @JsonProperty("disease_group")
    private Set<String> highLevelSlimTermNames = new HashSet<>();

}
