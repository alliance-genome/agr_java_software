package org.alliancegenome.core.filedownload.model;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class DownloadSource {
	private String source;
	private String species;
	private String taxonId;
	private List<String> chromosomeList;
	
	public List<String> getGenerateFilePaths() {
		ArrayList<String> ret = new ArrayList<String>();
		for(String chromosome: chromosomeList) {
			ret.add(Joiner.on(".").join(List.of(source, "vep", chromosome, "vcf.gz")));
		}
		return ret;
	}
}
