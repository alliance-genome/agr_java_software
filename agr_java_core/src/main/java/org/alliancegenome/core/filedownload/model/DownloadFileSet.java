package org.alliancegenome.core.filedownload.model;

import java.util.*;

import lombok.*;

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
