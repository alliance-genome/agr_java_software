package org.alliancegenome.core.filedownload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.google.common.base.Joiner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileDownload extends Thread {

	private String allianceRelease;
	private String source;
	private String chromosome;
	private String downloadPath;
	private String s3RootUrl;
	private URL downloadUrl;

	public FileDownload(String allianceRelease, String source, String chromosome, String downloadPath, String s3RootUrl) {
		this.allianceRelease = allianceRelease;
		this.source = source;
		this.chromosome = chromosome;
		this.downloadPath = downloadPath;
		this.s3RootUrl = s3RootUrl;
	}

	private URL verifyUrl(String url) {
		// Only allow these URLs.
		if (!url.toLowerCase().startsWith("http://")
			&& !url.toLowerCase().startsWith("https://")
			&& !url.toLowerCase().startsWith("ftp://")) {
			return null;
		}

		URL verifiedUrl = null;
		try {
			verifiedUrl = new URL(url);
		} catch (Exception e) {
			return null;
		}

		if (verifiedUrl.getFile().length() < 2) {
			return null;
		}

		return verifiedUrl;
	}

	private String getFilePath(URL url) {
		String fileName = url.getFile();
		return fileName.substring(fileName.lastIndexOf('/') + 1);
	}

	@Override
	public void run() {
		try {
			String file = Joiner.on(".").join(List.of(source, "vep", chromosome, "vcf.gz"));
			String url = Joiner.on("/").join(List.of(s3RootUrl, allianceRelease, source, file));
			
			downloadUrl = verifyUrl(url);
			if (downloadUrl == null) {
				log.warn("Unable to verify file: " + url);
				return;
			}
			log.info("Downloading: " + downloadUrl + " -> " + downloadPath);
			File dir = new File(downloadPath);
			if (!dir.exists()) {
				Files.createDirectories(Paths.get(downloadPath));
			}

			File localFile = new File(downloadPath + "/" + getFilePath(downloadUrl));

			if (localFile.exists()) {
				log.warn("Local File: " + localFile.getAbsolutePath() + " already exists: skipping");
				return;
			}

			InputStream in = downloadUrl.openStream();
			Files.copy(in, Paths.get(localFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
			log.info("Finished Downloading: " + downloadUrl + " -> " + localFile.getAbsolutePath());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
