package org.alliancegenome.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DownloadFile {

	private String filename;
	private String s3Path;
	private String s3Url;
	private String stableURL;
	private String releaseVersion;
	private long size;
	private String lastModified;
	private String dataType;
	private String dataSubType;
	private String fileExtension;

}
