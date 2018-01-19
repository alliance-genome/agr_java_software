package org.alliancegenome.api.model.esdata;

import org.alliancegenome.indexer.document.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class TaxonIdDocument extends ESDocument {

    private String type = "taxonid";

    private String name;
    private String description;

    @Override
    public String getDocumentId() {
        return name;
    }

}
