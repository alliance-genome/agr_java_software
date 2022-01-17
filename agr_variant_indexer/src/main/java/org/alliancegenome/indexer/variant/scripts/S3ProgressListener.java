package org.alliancegenome.indexer.variant.scripts;

import org.alliancegenome.es.util.ProcessDisplayHelper;

import com.amazonaws.event.*;
import com.amazonaws.services.s3.transfer.Upload;

public class S3ProgressListener implements ProgressListener {

    private ProcessDisplayHelper pdh = new ProcessDisplayHelper(10000);
    private boolean started = false;
    
    public void setUpload(Upload upload) {
        pdh.startProcess(upload.getDescription(), upload.getProgress().getTotalBytesToTransfer());
        started = true;
    }

    @Override
    public void progressChanged(ProgressEvent progressEvent) {
        if(started) {
            pdh.progressProcess(progressEvent.getBytesTransferred());
        }
    }
    
}
