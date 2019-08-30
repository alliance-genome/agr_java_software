package org.alliancegenome.cacher.cachers;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCacheManager;
import org.alliancegenome.es.util.ProcessDisplayHelper;

import java.util.Date;

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

    protected void setCacheStatus(int size, String name) {
        BasicCacheManager<CacheStatus> basicManager = new BasicCacheManager<>();
        CacheStatus status = new CacheStatus(name);
        status.setNumberOfEntities(size);
        try {
            basicManager.putCache(name, status, CacheAlliance.CACHING_STATS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
