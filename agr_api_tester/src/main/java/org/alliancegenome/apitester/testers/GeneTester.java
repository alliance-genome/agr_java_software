package org.alliancegenome.apitester.testers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.api.rest.interfaces.GeneRESTInterface;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

import lombok.extern.log4j.Log4j2;
import si.mazi.rescu.RestProxyFactory;

@Log4j2
public class GeneTester extends Tester {

	private GeneRepository repo = new GeneRepository();
	private GeneRESTInterface geneInt = RestProxyFactory.createProxy(GeneRESTInterface.class, ConfigHelper.getApiBaseUrl());
	private List<String> exceptionQueue = new ArrayList<String>();
	
	@Override
	protected void test() {
		
		List<String> geneIds = repo.getAllGeneKeys();
		
		startProcess("get All Gene Ids: " + ConfigHelper.getApiBaseUrl(), geneIds.size());

		ExecutorService executor = Executors.newFixedThreadPool(20);
		
		for(String id: geneIds) {
			Runnable worker = new WorkerThread(id);
			executor.execute(worker);
		
		}
		
		executor.shutdown();
		try {
			executor.awaitTermination(10,  TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		finishProcess();
		log.info("Exceptions: " + exceptionQueue.size());
		log.info(exceptionQueue);
	}
	
	class WorkerThread implements Runnable { 
		private String id;
		
		public WorkerThread(String id) {
			this.id = id;
		}
		
		public void run() {
			try {
				Gene g = geneInt.getGene(id);
				progressProcess();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
