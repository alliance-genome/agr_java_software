package org.alliancegenome.indexer.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvidenceCodeDocument extends ESDocument {

    private String primaryKey;

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

}
