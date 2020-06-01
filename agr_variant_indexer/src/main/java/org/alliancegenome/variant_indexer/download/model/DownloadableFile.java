package org.alliancegenome.variant_indexer.download.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class DownloadableFile {
    private String chromosome;
    private String url;
    private String localGzipFilePath;
    private String localJsonFilePath;

}
