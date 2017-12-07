package org.alliancegenome.indexer.document;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class PublicationDoclet implements Comparable<PublicationDoclet> {

    private String primaryKey;
    private String pubMedId;
    private String pubMedUrl;
    private String pubModId;
    private String pubModUrl;
    private Set<String> evidenceCodes;

    @Override
    public int compareTo(PublicationDoclet comp) {
        if (pubMedId != null && comp.pubMedId != null)
            return pubMedId.compareTo(comp.pubMedId);
        if (comp.pubModId == null)
            return -1;
        if (pubModId == null)
            return +1;
        return pubModId.compareToIgnoreCase(comp.pubModId);
    }
}
