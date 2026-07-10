package org.alliancegenome.indexer.variant.filedownload.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DownloadFileSet {
	private List<DownloadSource> downloadFileSources;
	private String downloadPath;
	private String s3RootUrl;

	public List<String> getChromosomesToDownload() {
		List<String> ret = new ArrayList<String>();
		for (DownloadSource source: downloadFileSources) {
			ret.addAll(source.getChromosomeList());
		}
		return ret;
	}

}
