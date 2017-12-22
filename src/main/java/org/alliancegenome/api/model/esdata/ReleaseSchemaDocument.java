package org.alliancegenome.api.model.esdata;

import org.alliancegenome.indexer.document.ESDocument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReleaseSchemaDocument extends ESDocument {

    private String type = "release_schema";

    private String id;
    private String relaeseVersion;
    private String schemaVersion;

    @Override
    public String getDocumentId() {
        return id;
    }

}
