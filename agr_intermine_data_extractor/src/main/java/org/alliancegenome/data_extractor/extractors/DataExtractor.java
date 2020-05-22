package org.alliancegenome.data_extractor.extractors;

import java.io.File;
import java.io.PrintWriter;
import java.util.Date;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.ProcessDisplayHelper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Setter
@Getter
public abstract class DataExtractor extends Thread {

    protected abstract void extract(PrintWriter writer);
    protected abstract String getFileName();

    private ProcessDisplayHelper display = new ProcessDisplayHelper();
    
    @Override
    public void run() {
        try {
            Date start = new Date();
            log.info(this.getClass().getSimpleName() + " started: " + start);
            PrintWriter output_writer = new PrintWriter(new File(ConfigHelper.getDataExtractorDirectory() + "/" + getFileName()));
            extract(output_writer);
            output_writer.close();
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
}