package org.alliancegenome.es.util;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ProcessDisplayHelper {

    private Runtime runtime = Runtime.getRuntime();
    private DecimalFormat df = new DecimalFormat("#");

    private Date startTime = new Date();
    private Date lastTime = new Date();
    private String message;
    private long lastSizeCounter = 0;
    private long totalSize;
    private long sizeCounter = 0;
    private long displayTimeout = 30000; // How often to display to the console
    
    public ProcessDisplayHelper() { }
    
    public ProcessDisplayHelper(int displayTimeout) {
        this.displayTimeout = displayTimeout;
    }

    public void startProcess(String message) {
        startProcess(message, 0);
    }
    
    public void startProcess(String message, int totalSize) {
        this.message = message + ": ";
        this.totalSize = totalSize;
        lastSizeCounter = 0;
        startTime = new Date();
        sizeCounter = 0;
        if (totalSize > 0)
            log.info(this.message + "Starting Process [total =  " + getBigNumber(totalSize) + "] ");
        else
            log.info(this.message + "Starting Process at: ");

        lastTime = new Date();
    }

    public void progressProcess() {
        progressProcess(null);
    }
    
    public void progressProcess(String data) {
        Date now = new Date();
        long diff = now.getTime() - startTime.getTime();
        long time = now.getTime() - lastTime.getTime();
        //log.info(this.message + "diff: " + diff + " time: " + time + " now: " + now + " startTime: " + startTime + " lastTime: " + lastTime);
        sizeCounter++;
        if (time < displayTimeout) return; // report every 30 seconds
        checkMemory();
        
        double percent = 0;
        if (totalSize > 0) {
            percent = ((double) (sizeCounter) / totalSize);
        }
        long processedAmount = (sizeCounter - lastSizeCounter);
        String localMessage = "" + getBigNumber(sizeCounter);
        if(totalSize > 0) {
            localMessage += " of [" + getBigNumber(totalSize) + "] " + (int) (percent * 100) + "%";
        }
        localMessage += ", " + (time / 1000) + "s to process " + getBigNumber(processedAmount) + " records at " + getBigNumber((processedAmount * 1000) / time) + "r/s";
        if(data != null) {
            localMessage += " " + data;
        }
        
        if (percent > 0) {
            int perms = (int) (diff / percent);
            Date end = new Date(startTime.getTime() + perms);
            String expectedDuration = getHumanReadableTimeDisplay(end.getTime() - (new Date()).getTime());
            localMessage += ", Mem: " + df.format(memoryPercent() * 100) + "%, ETA: " + expectedDuration + " [" + end + "]";
        }
        log.info(this.message + localMessage);
        lastSizeCounter = sizeCounter;
        lastTime = now;
    }

    public void finishProcess() {
        finishProcess(null);
    }
    
    public void finishProcess(String data) {
        Date now = new Date();
        long duration = now.getTime() - startTime.getTime();
        String result = getHumanReadableTimeDisplay(duration);
        String localMessage = message + "Finished: took: " + result + " to process " + getBigNumber(sizeCounter);
        if (duration != 0) {
            localMessage += " records at a rate of: " + ((sizeCounter * 1000) / duration) + "r/s";
        } else {
            localMessage += " records";
        }
        
        if(data != null) {
            localMessage += " " + data;
        }
        log.info(localMessage);
    }

    private static String getBigNumber(long number) {
        return String.format("%,d", number);
    }

    public static String getHumanReadableTimeDisplay(long duration) {
        long hours = TimeUnit.MILLISECONDS.toHours(duration) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }


    private void checkMemory() {
        if (memoryPercent() > 0.95) {
            log.warn(message + "Memory Warning: " + df.format(memoryPercent() * 100) + "%");
            log.warn(message + "Used Mem: " + (runtime.totalMemory() - runtime.freeMemory()));
            log.warn(message + "Free Mem: " + runtime.freeMemory());
            log.warn(message + "Total Mem: " + runtime.totalMemory());
            log.warn(message + "Max Memory: " + runtime.maxMemory());
        }
    }

    private double memoryPercent() {
        return ((double) runtime.totalMemory() - (double) runtime.freeMemory()) / (double) runtime.maxMemory();
    }
}
