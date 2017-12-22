package org.alliancegenome.api.model.esdata;

import org.alliancegenome.indexer.document.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class ReleaseDocument extends ESDocument {

    private String type = "schema";

    private String name;

    @Override
    public String getDocumentId() {
        return name;
    }

}
