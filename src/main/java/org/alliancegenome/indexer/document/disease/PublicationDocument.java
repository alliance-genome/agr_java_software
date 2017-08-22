package org.alliancegenome.indexer.document.disease;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.indexer.document.ESDocument;

import java.util.List;

@Getter
@Setter
public class PublicationDocument extends ESDocument {

    private String primaryKey;
    private String pubMedId;
    private String pubModId;
    private String pubModUrl;
    private List<String> evidenceCodes;

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

}
