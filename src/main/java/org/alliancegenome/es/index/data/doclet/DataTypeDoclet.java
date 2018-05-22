package org.alliancegenome.es.index.data.doclet;

import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class DataTypeDoclet {

    private String name;
    private String fileExtension;
    private String description;
    private boolean taxonIdRequired;
    private boolean validationRequired;
    private boolean modVersionStored;
    private HashMap<String, String> schemaFiles = new HashMap<String, String>();


}
