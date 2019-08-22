package org.alliancegenome.cacher.cachers;

import java.util.Date;

import org.alliancegenome.es.util.ProcessDisplayHelper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class Cacher extends Thread {

    protected abstract void cache();
    private ProcessDisplayHelper display = new ProcessDisplayHelper();

    @Override
    public void run() {
        try {
            Date start = new Date();
            log.info(this.getClass().getSimpleName() + " started: " + start);
            cache();
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
        display.startProcess(message, totalSize);
    }

    protected void progressProcess() {
        display.progressProcess();
    }

    protected void finishProcess() {
        display.finishProcess();
    }

}
