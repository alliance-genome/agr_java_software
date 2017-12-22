package org.alliancegenome.api.model.esdata;

import org.alliancegenome.indexer.document.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class DataTypeDocument extends ESDocument {

    private String type = "data_type";

    private String name;
    private String fileExtension;
    private String description;
    private boolean modRequired;
    private boolean validationRequired;

    @Override
    public String getDocumentId() {
        return name;
    }

}
