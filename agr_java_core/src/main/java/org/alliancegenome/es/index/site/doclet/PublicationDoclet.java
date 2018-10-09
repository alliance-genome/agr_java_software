package org.alliancegenome.es.index.site.doclet;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.alliancegenome.es.index.ESDoclet;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicationDoclet extends ESDoclet implements Comparable<PublicationDoclet> {

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

    // retrieve PUBMED id if it is available
    // otherwise the mod id
    @JsonIgnore
    public String getPubId() {
        if (StringUtils.isNotEmpty(pubMedId))
            return pubMedId;
        return pubModId;

    }

    @Override
    public String toString() {
        return getPubId() + ':' + evidenceCodes;
    }
}
