package org.alliancegenome.es.model.search;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RelatedDataLink {

    private String category;
    private String targetField;
    private String sourceName;
    private Long count;

}
