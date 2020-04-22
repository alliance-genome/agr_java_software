package org.alliancegenome.cacher.cachers;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.view.View;

import java.util.*;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

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

    void populateCacheFromMap(Map<String, ? extends Object> map, Class view, CacheAlliance cacheAlliance) {
        startProcess(cacheAlliance.name() + " into cache", map.size());
        BasicCachingManager manager = new BasicCachingManager();
        for (Map.Entry<String, ? extends Object> entry : map.entrySet()) {
            manager.setCache(entry.getKey(), entry.getValue(), view, cacheAlliance);
            progressProcess();
        }
        finishProcess();
    }

    public void storeStatistics(CacheStatus status, Map<String, Integer> entityStats, Map<String, List<Species>> speciesStatistics) {
        Map<String, Integer> speciesStats = new HashMap<>();
        speciesStatistics.forEach((species, speciesList) -> {
            speciesStats.put(species, speciesList.size());
        });

        Arrays.stream(SpeciesType.values())
                .filter(speciesType -> !speciesStats.keySet().contains(speciesType.getName()))
                .forEach(speciesType -> speciesStats.put(speciesType.getName(), 0));


        Map<String, Integer> sortedMap = speciesStats
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        status.setEntityStats(entityStats);
        status.setSpeciesStats(sortedMap);
    }


}
