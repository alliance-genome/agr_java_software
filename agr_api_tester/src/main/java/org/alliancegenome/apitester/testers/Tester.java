package org.alliancegenome.apitester.testers;

import java.util.Date;

import org.alliancegenome.es.util.ProcessDisplayHelper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class Tester extends Thread {
    protected abstract void test();
    
    private ProcessDisplayHelper display = new ProcessDisplayHelper();
    
    @Override
    public void run() {
        try {
            Date start = new Date();
            log.info(this.getClass().getSimpleName() + " started: " + start);
            test();
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

