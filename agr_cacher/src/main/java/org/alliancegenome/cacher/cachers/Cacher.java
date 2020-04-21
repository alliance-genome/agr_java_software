package org.alliancegenome.cacher.cachers;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.view.View;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Log4j2
@Setter
@Getter
public abstract class Cacher extends Thread {

    protected abstract void cache();

    protected boolean useCache;

    private ProcessDisplayHelper display = new ProcessDisplayHelper();

    private BasicCachingManager basicManager = new BasicCachingManager();

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
        display = new ProcessDisplayHelper();
        display.startProcess(message, totalSize);
    }

    protected void progressProcess() {
        display.progressProcess();
    }

    protected void finishProcess() {
        display.finishProcess();
    }

    public void setCacheStatus(CacheStatus status) {
        basicManager.setCache(status.getName(), status, View.CacherDetail.class, CacheAlliance.CACHING_STATS);
    }

    public void setCacheStatus(int size, CacheAlliance cache) {
        CacheStatus status = new CacheStatus(cache);
        status.setNumberOfEntities(size);
        setCacheStatus(status);
    }

    void populateCacheFromMap(Map<String, List<Allele>> map, Class view, CacheAlliance cacheAlliance) {
        startProcess(cacheAlliance.name() + " into cache", map.size());
        BasicCachingManager manager = new BasicCachingManager();
        for (Map.Entry<String, List<Allele>> entry : map.entrySet()) {
            manager.setCache(entry.getKey(), entry.getValue(), view, cacheAlliance);
        }

        finishProcess();
    }

}
