package org.alliancegenome.indexer.kmeans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KMeans {
	
	private int clusterCount;
	private int iterations;
	
	private List<Integer> dataPoints;
	private List<Cluster> clusters;
	
	public KMeans(int clusterCount, int iterations, List<Integer> dataPoints) {
		this.clusterCount = clusterCount;
		this.iterations = iterations;
		this.dataPoints = dataPoints;
		clusters = new ArrayList<>();
	}
	
	public void run() {
        initializeClusters();
        for (int i = 0; i < iterations; i++) {
        	//log.info("Iterating cluster: " + i);
            assignDataPointsToClusters();
            recalculateClusterCentroids();
        }
    }
	
	private void initializeClusters() {
        for (int i = 0; i < clusterCount; i++) {
            Cluster cluster = new Cluster();
            int idx = (int)(Math.random() * dataPoints.size());
            cluster.setCentroid(dataPoints.get(idx));
            clusters.add(cluster);
        }
    }
	
	private void assignDataPointsToClusters() {
        for (Integer dataPoint : dataPoints) {
            double minDistance = Double.MAX_VALUE;
            Cluster closestCluster = null;
            for (Cluster cluster : clusters) {
                double distance = Math.abs(dataPoint - cluster.getCentroid());
                if (distance < minDistance) {
                    minDistance = distance;
                    closestCluster = cluster;
                }
            }
            closestCluster.addDataPoint(dataPoint);
        }
    }
	
	private void recalculateClusterCentroids() {
        for (Cluster cluster : clusters) {
            cluster.recalculateCentroid();
        }
    }
	
	public List<Integer> getCenters() {
		List<Integer> list = clusters.stream().map(c -> c.getCentroid()).collect(Collectors.toList());
		Collections.sort(list);
		return list;
    }
	
	@Data
	public class Cluster {
		private Integer centroid;
		private List<Integer> clusterDataPoints;
		
		public Cluster() {
			this.clusterDataPoints = new ArrayList<>();
		}
		
		public void addDataPoint(Integer dataPoint) {
			clusterDataPoints.add(dataPoint);
		}
		
		public void recalculateCentroid() {
			double sum = 0;
	        for (Integer dataPoint : clusterDataPoints) {
	            sum += dataPoint;
	        }
	        centroid = (int)(sum / clusterDataPoints.size());
	        clusterDataPoints.clear();
		}
	}
}
