package org.alliancegenome.core.filedownload.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class DownloadSource {
    private String source;
    private String species;
    private String taxonId;
    private List<DownloadableFile> fileList;
}
