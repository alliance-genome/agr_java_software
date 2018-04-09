package org.alliancegenome.es.index.data.document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SnapShotDoclet {

	private String releaseVersion;
	private String schemaVersion;
	private Date snapShotDate;
	private List<DataFileDocument> dataFiles = new ArrayList<DataFileDocument>();
}
