package org.alliancegenome.indexer.variant.es.stats;

import java.io.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jfree.chart.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.*;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class StatsCollector {

    private double[] histogramData = new double[1_000_000];

    private SummaryStatistics stats = new SummaryStatistics();

    public void addDocument(String jsonDoc) {
        synchronized (jsonDoc) {
            int len = jsonDoc.length();
            stats.addValue(len);
            if(len >= 1_000_000) {
                len = 1_000_000;
            }
            histogramData[len]++;
        }
    }

    public void printOutput() {
        log.info("Document Stats: " + stats);
        saveHistogram();
    }

    private void saveHistogram() {
        int binCount = 1000;
        
        if((stats.getMax() - stats.getMin()) / 1000 <= 1.0) {
            binCount = (int)(stats.getMax() - stats.getMin());
        }
        
//      SimpleHistogramDataset dataset = new SimpleHistogramDataset("Size");
//      for(int i = 1280; i < 1524; i+=2) {
//          dataset.addBin(new SimpleHistogramBin(i, i+1));
//      }
//      dataset.addObservations(histogramData);

        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("key", histogramData, binCount);
        

        JFreeChart histogram = ChartFactory.createHistogram("JFreeChart Histogram", "Data", "Frequency", dataset, PlotOrientation.VERTICAL, false, false, false);
        
        try {
            ChartUtilities.saveChartAsPNG(new File("histogram.png"), histogram, 1024, 400);
            //TimeUnit.SECONDS.sleep(600);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        

    }

}
