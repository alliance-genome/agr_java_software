package org.alliancegenome.indexer.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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

}
