package org.alliancegenome.es.index.data.document;

import java.util.Date;

import org.alliancegenome.es.index.document.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class DataFileDocument extends ESDocument {

	private String schemaVersion;
	private String dataType;
	private String taxonIDPart;
	private String path;
	private Date uploadDate = new Date();

	@Override
	public String getDocumentId() {
		return path;
	}

	@Override
	public String getType() {
		return "data_file";
	}

}
