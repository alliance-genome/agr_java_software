package org.alliancegenome.es.index.data.doclet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alliancegenome.es.index.data.document.DataFileDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class SnapShotDoclet {

    private String releaseVersion;
    private String schemaVersion;
    private Date snapShotDate;
    private List<DataFileDocument> dataFiles = new ArrayList<DataFileDocument>();
}
