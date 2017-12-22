package org.alliancegenome.api.model.esdata;

import org.alliancegenome.indexer.document.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class DataFileDocument extends ESDocument {

    private String type = "data_file";

    private String schemaVersion;
    private String dataType;
    private String mod;
    private String path;

    @Override
    public String getDocumentId() {
        return path;
    }

}
