package org.alliancegenome.indexer.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DiseaseDocument extends ESDocument {

    private String doId;
    private String primaryKey;
    private String name;
    private String definition;
    private String species;
    private List<AnnotationDocument> annotations;
    private List<DiseaseDocument> parents;
    private List<DiseaseDocument> children;
    private List<String> synonyms;
    private List<String> crossReferences;

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }
}
