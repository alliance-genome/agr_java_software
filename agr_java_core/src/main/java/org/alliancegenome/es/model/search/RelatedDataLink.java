package org.alliancegenome.es.model.search;

import lombok.*;

@Getter @Setter
public class RelatedDataLink {

    private String category;
    private String targetField;
    private String sourceName;
    private Long count;
    private String label;

}
