package org.alliancegenome.es.index.site.document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelDocument extends SearchableItemDocument {
    public static final String CATEGORY = "model";

    {
        category = CATEGORY;
    }


}
