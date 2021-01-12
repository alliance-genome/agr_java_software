package org.alliancegenome.core.filedownload.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class DownloadableFile {
    private String chromosome;
    private String url;
    private String localGzipFilePath;
    private List<String> jsonFileParts;
}
