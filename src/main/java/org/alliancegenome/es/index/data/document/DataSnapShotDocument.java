package org.alliancegenome.es.index.data.document;

import java.util.Date;
import java.util.HashMap;

import org.alliancegenome.es.index.document.ESDocument;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString @NoArgsConstructor
public class DataSnapShotDocument extends ESDocument {

    private String system;
    private HashMap<String, Date> releaseSnapShotMap = new HashMap<>();
    
    public DataSnapShotDocument(String system) {
        this.system = system;
    }

    @Override
    public String getDocumentId() {
        return system;
    }

    @Override
    public String getType() {
        return "data_snapshot";
    }

}
