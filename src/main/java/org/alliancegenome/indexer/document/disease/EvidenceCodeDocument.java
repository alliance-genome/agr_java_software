package org.alliancegenome.indexer.document.disease;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.indexer.document.ESDocument;

@Getter
@Setter
public class EvidenceCodeDocument extends ESDocument {

    private String primaryKey;

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

}
