package org.alliancegenome.core.util;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jfree.chart.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class StatsCollector {

    private int histSize = 1_000_000;
    private double[] histogramData = new double[histSize];

    private SummaryStatistics stats = new SummaryStatistics();

    public void addDocument(String jsonDoc) {
        synchronized (jsonDoc) {
            int len = jsonDoc.length();
            stats.addValue(len);
            if(len >= 1_000_000) {
                len = 1_000_000 - 1;
            }
            histogramData[len]++;
        }
    }
    
    public void printOutput() {
        log.info("Document Stats: " + stats);
    }

    public void printOutput(String prefix) {
        log.info("Document Stats: " + stats);
        saveHistogram(prefix);
    }

    private void saveHistogram(String savePrefix) {


//
//      HashMap<Integer, Double> distributionMap  = new HashMap<Integer, Double>();
//      
//      for(int i = 0; i < histogramData.length; i++) {
//          if(histogramData[i] > 0) {
//              distributionMap.put(i, histogramData[i]);
//          }
//      }
//      
//      
//
//      CategoryChart chart = new CategoryChartBuilder().width(1024).height(400)
//          .title("Age Distribution")
//          .xAxisTitle("Age Group")
//          .yAxisTitle("Frequency")
//          .build();
//
//
//      chart.getStyler().setAvailableSpaceFill(0.99);
//      chart.getStyler().setOverlapped(true);
//
//      List yData = new ArrayList();
//      yData.addAll(distributionMap.values());
//      List xData = Arrays.asList(distributionMap.keySet().toArray());
//      chart.addSeries("age group", xData, yData);
//
//      try {
//          BitmapEncoder.saveBitmap(chart, "histograms/" + savePrefix + "_histogram.png", BitmapFormat.PNG);
//      } catch (IOException e) {
//          e.printStackTrace();
//      }
//
//      
        
        
        
        
        
        int binCount = 1000;

        //if((stats.getMax() - stats.getMin()) / 1000 <= 1.0) {
        //  binCount = (int)(stats.getMax() - stats.getMin());
        //}

        ArrayList<Integer> list = new ArrayList<>();

        for(int i = 0; i < stats.getMean() + (stats.getStandardDeviation() * 3) && i < histSize; i++) {
            for(int k = 0; k < histogramData[i]; k++) {
                list.add(i);
            }
        }

        log.info("Hist List Size: " + list.size());

        double[] array = new double[list.size()];
        int c = 0;
        for(Integer i: list) {
            array[c++] = i;
        }

        //      SimpleHistogramDataset dataset = new SimpleHistogramDataset("Size");
        //      for(int i = 1280; i < 1524; i+=2) {
        //          dataset.addBin(new SimpleHistogramBin(i, i+1));
        //      }
        //      dataset.addObservations(histogramData);
        
        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("key", array, binCount);

        JFreeChart histogram = ChartFactory.createHistogram("JFreeChart Histogram " + savePrefix, "Data Size", "Frequency", dataset, PlotOrientation.VERTICAL, false, false, false);

        try {
            ChartUtilities.saveChartAsPNG(new File("histograms/" + savePrefix + "_histogram.png"), histogram, 1024, 400);
            //TimeUnit.SECONDS.sleep(600);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
