package org.alliancegenome.shared.es.document.data_index;

import java.util.Date;

import org.alliancegenome.shared.es.document.site_index.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class DataFileDocument extends ESDocument {

	private String type = "data_file";

	private String schemaVersion;
	private String dataType;
	private String taxonId;
	private String path;
	private Date uploadDate = new Date();

	@Override
	public String getDocumentId() {
		return path;
	}

}
