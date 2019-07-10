package org.alliancegenome.cacher.cachers;

import java.util.concurrent.TimeUnit;

import org.alliancegenome.cacher.config.Caches;
import org.alliancegenome.cacher.config.IOCacherConfig;
import org.alliancegenome.core.config.ConfigHelper;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.CacheMode;

import lombok.extern.log4j.Log4j2;

public abstract class Cacher extends Thread {

    protected abstract void cache();

    public void runCache() {
        try {
            cache();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void run() {
        super.run();
        try {
            cache();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    public <T> RemoteCache<String, T> setupCache(String cacheName) {
        
        ConfigurationBuilder cb = new ConfigurationBuilder();

        cb.addServer()
        .host(ConfigHelper.getCacheHost())
        .port(ConfigHelper.getCachePort())
        .socketTimeout(500000)
        .connectionTimeout(500000)
        .tcpNoDelay(true);

        RemoteCacheManager rmc = new RemoteCacheManager(cb.build());
        
        //cache = rmc.administration().withFlags(AdminFlag.PERMANENT).getOrCreateCache(cacherConfig.getCacheName(), cacherConfig.getCacheTemplate());

        org.infinispan.configuration.cache.ConfigurationBuilder cb2 = new org.infinispan.configuration.cache.ConfigurationBuilder();
        
//        cb2.persistence()
//        .passivation(false)
//        .addSingleFileStore()
//            .shared(false)
//            .preload(true)
//            .fetchPersistentState(true)
//            .purgeOnStartup(false)
//            .location("/tmp/" + cacheName)
//            .async()
//               .enabled(true)
//               .threadPoolSize(5);
        
        cb2.jmxStatistics();
        cb2.clustering().cacheMode(CacheMode.LOCAL).create();

        return rmc.administration().getOrCreateCache(cacheName, cb2.build());
    }

    

//  protected Runtime runtime = Runtime.getRuntime();
//  protected DecimalFormat df = new DecimalFormat("#");
//
//  // Used for showing progress
//  private Date startTime = new Date();
//  private Date lastTime = new Date();
//  private int lastSize;
//  private long batchTotalSize = 0;
//  private long batchCount = 0;
    
//  protected void startProcess(int amountBatches, int batchSize, int totalDocAmount) {
//      log.info("Starting Processing: batches: " + amountBatches + " size: " + batchSize + " total: " + getBigNumber(totalDocAmount) + " at: " + startTime);
//      lastTime = new Date();
//  }
//
//  private void progress(int currentSize, int totalDocAmount) {
//      double percent = ((double) (totalDocAmount - currentSize) / (double) totalDocAmount);
//      Date now = new Date();
//      long diff = now.getTime() - startTime.getTime();
//      long time = (now.getTime() - lastTime.getTime());
//      int processedAmount = (lastSize - currentSize);
//      String message = "" + getBigNumber(totalDocAmount - currentSize) + " records [" + getBigNumber(totalDocAmount) + "] ";
//      message += (int) (percent * 100) + "% took: " + (time / 1000) + "s to process " + processedAmount;
//
//      int batchAvg = 0;
//      if (batchCount > 0) {
//          batchAvg = (int) (batchTotalSize / batchCount);
//      }
//      message += " rate: " + ((processedAmount * 1000) / time) + "r/s ABS: " + batchAvg;
//
//      if (percent > 0) {
//          int perms = (int) (diff / percent);
//          Date end = new Date(startTime.getTime() + perms);
//          String expectedDuration = getHumanReadableTimeDisplay(end.getTime() - (new Date()).getTime());
//          message += ", Memory: " + df.format(memoryPercent() * 100) + "%, ETA: " + expectedDuration + " [" + end + "]";
//      }
//      log.info(message);
//      lastSize = currentSize;
//      lastTime = now;
//      batchCount = 0;
//      batchTotalSize = 0;
//  }
//
//  private void finishProcess(int totalDocAmount) {
//      Date now = new Date();
//      long duration = now.getTime() - startTime.getTime();
//      String result = getHumanReadableTimeDisplay(duration);
//      log.info("Finished: took: " + result + " to process " + getBigNumber(totalDocAmount) + " records at a rate of: " + ((totalDocAmount * 1000) / duration) + "r/s");
//  }
//
//  public static String getBigNumber(int number) {
//      return String.format("%,d", number);
//  }
//
    public static String getHumanReadableTimeDisplay(long duration) {
        long hours = TimeUnit.MILLISECONDS.toHours(duration) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
//
//  private void checkMemory() {
//      if (memoryPercent() > 0.95) {
//          log.warn("Memory Warning: " + df.format(memoryPercent() * 100) + "%");
//          log.warn("Used Mem: " + (runtime.totalMemory() - runtime.freeMemory()));
//          log.warn("Free Mem: " + runtime.freeMemory());
//          log.warn("Total Mem: " + runtime.totalMemory());
//          log.warn("Max Memory: " + runtime.maxMemory());
//      }
//  }
//
//  private double memoryPercent() {
//      return ((double) runtime.totalMemory() - (double) runtime.freeMemory()) / (double) runtime.maxMemory();
//  }

}
