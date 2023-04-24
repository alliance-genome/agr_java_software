package org.alliancegenome.core.filedownload.process;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.core.filedownload.FileDownload;
import org.alliancegenome.core.filedownload.model.DownloadFileSet;
import org.alliancegenome.core.filedownload.model.DownloadSource;
import org.alliancegenome.core.filedownload.model.DownloadableFile;
import org.alliancegenome.core.variant.config.VariantConfigHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileDownloadManager extends Thread {

	private DownloadFileSet downloadSet;

	public FileDownloadManager(DownloadFileSet downloadSet) {
		this.downloadSet = downloadSet;
	}

	public void run() {
		
		if(downloadSet == null || downloadSet.getDownloadFileSet() == null)
			return;

		log.info("Starting downloading variant Files");
		
		ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getFileDownloadThreads());

		for(DownloadSource source: downloadSet.getDownloadFileSet()) {
			for(DownloadableFile df: source.getFileList()) {
				FileDownload fd = new FileDownload(df, downloadSet.getDownloadPath());
				executor.execute(fd);
			}
		}
		
		executor.shutdown();  
		while (!executor.isTerminated()) {	 }	

		log.info("Finished downloading Files");
	}


}
