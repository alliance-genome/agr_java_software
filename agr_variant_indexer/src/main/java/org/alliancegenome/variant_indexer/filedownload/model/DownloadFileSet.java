package org.alliancegenome.variant_indexer.filedownload.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class DownloadFileSet {
    private List<DownloadSource> downloadFileSet;
    private String downloadPath;

    public List<DownloadableFile> getFilesToDownload() {
        List<DownloadableFile> ret = new ArrayList<DownloadableFile>();
        for(DownloadSource source: downloadFileSet) {
            ret.addAll(source.getFileList());
        }
        return ret;
    }

}
