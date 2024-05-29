package org.alliancegenome.core.filedownload.process;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.filedownload.FileDownload;
import org.alliancegenome.core.filedownload.model.DownloadFileSet;
import org.alliancegenome.core.filedownload.model.DownloadSource;
import org.alliancegenome.core.variant.config.VariantConfigHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileDownloadManager extends Thread {

	private DownloadFileSet downloadSet;

	public FileDownloadManager(DownloadFileSet downloadSet) {
		this.downloadSet = downloadSet;
	}

	@Override
	public void run() {
		
		if(downloadSet == null || downloadSet.getChromosomesToDownload() == null) {
			return;
		}

		log.info("Starting downloading variant Files");

		ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getFileDownloadThreads());

		for(DownloadSource source: downloadSet.getDownloadFileSources()) {
			for(String chromosome: source.getChromosomeList()) {
				FileDownload fd = new FileDownload(ConfigHelper.getAllianceRelease(), source.getSource(), chromosome, downloadSet.getDownloadPath(), downloadSet.getS3RootUrl());
				executor.execute(fd);
			}
		}

		executor.shutdown();
		while (!executor.isTerminated()) {
			//
		}

		log.info("Finished downloading Files");
	}

}
