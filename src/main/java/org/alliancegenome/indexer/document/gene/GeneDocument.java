package org.alliancegenome.indexer.document.gene;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.indexer.document.ESDocument;

@Getter
@Setter
public class GeneDocument extends ESDocument {

    private String primaryId;
    private String symbol;
    private String species;
    private String taxonId;

    @JsonIgnore
    public String getDocumentId() {
        return primaryId;
    }
}
