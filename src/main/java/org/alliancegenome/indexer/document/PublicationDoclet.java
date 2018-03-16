package org.alliancegenome.indexer.document;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

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

    // retrieve PUBMED id if it is available
    // otherwise the mod id
    public String getPubId() {
        if (StringUtils.isNotEmpty(pubMedId))
            return pubMedId;
        return pubModId;

    }
}

