package org.alliancegenome.indexer.document;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PublicationDoclet {

    private String primaryKey;
    private String pubMedId;
    private String pubMedUrl;
    private String pubModId;
    private String pubModUrl;
    private List<String> evidenceCodes;

}

