package org.alliancegenome.data_extractor.extractors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.ProcessDisplayHelper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public abstract class DataExtractor extends Thread {

	protected abstract void extract(PrintWriter writer);
	protected abstract String getFileName();
	protected abstract String getDirName();

	private ProcessDisplayHelper display = new ProcessDisplayHelper();
	
	@Override
	public void run() {
		try {
			Date start = new Date();
			log.info(this.getClass().getSimpleName() + " started: " + start);
			if(getFileName() != null) {
				File directory = new File(ConfigHelper.getDataExtractorDirectory() + "/" + getDirName());
				if (! directory.exists()){
					directory.mkdir();
				}
				PrintWriter output_writer = new PrintWriter(new File(ConfigHelper.getDataExtractorDirectory() + "/" + getDirName() + "/" + getFileName()));
				extract(output_writer);
				output_writer.close();
			} else {
				extract(null);
			}
			
			Date end = new Date();
			log.info(this.getClass().getSimpleName() + " finished: " + ProcessDisplayHelper.getHumanReadableTimeDisplay(end.getTime() - start.getTime()));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	protected void startProcess(String message) {
		startProcess(message, 0);
	}

	protected void startProcess(String message, int totalSize) {
		display = new ProcessDisplayHelper();
		display.startProcess(message, totalSize);
	}

	protected void progressProcess() {
		display.progressProcess();
	}

	protected void finishProcess() {
		display.finishProcess();
	}
	
	protected void decompressGzip(File source, File target) throws IOException {
		log.debug("decompressing file  " + source.getAbsolutePath() + "	 to " + target.getAbsolutePath());
		try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(source));
			 FileOutputStream fos = new FileOutputStream(target)) {
			byte[] buffer = new byte[1024];
			int len;
			while ((len = gis.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
		}
	}

}
