package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AGMPhenotypeAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AGMPhenotypeAnnotationInterface;

import lombok.extern.log4j.Log4j2;
import si.mazi.rescu.RestProxyFactory;

@Log4j2
public class AGMPhenotypeAnnotationService extends BaseDiseaseAnnotationService {

	private final AGMPhenotypeAnnotationInterface agmApi = RestProxyFactory.createProxy(AGMPhenotypeAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final String cacheFileName = "agm_phenotype_annotation.json.gz";

	public List<AGMPhenotypeAnnotation> getFiltered(int threadCount, int bufferSize) {
		List<AGMPhenotypeAnnotation> ret = readFromCache(cacheFileName, List.class);
		if (ret != null && ret.size() > 0) {
			return ret;
		} else {
			ret = new ArrayList<>();
		}
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);

		
		LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
		LinkedBlockingDeque<AGMPhenotypeAnnotation> fullList = new LinkedBlockingDeque<>();
		
		SearchResponse<AGMPhenotypeAnnotation> response = agmApi.findForPublic(0, 0, null);

		int totalPages = (int) (response.getTotalResults() / bufferSize);
		
		display.startProcess("Pulling AGM PA's from curation", response.getTotalResults());
		
		for (int i = 0; i <= totalPages; i++) {
			//log.info("page: " + i + " limit: " + indexerConfig.getBufferSize());
			queue.add(String.valueOf(i));
		}

		List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < threadCount; i++) {
			WorkerThread thread = new WorkerThread(bufferSize, queue, fullList, display);
			threads.add(thread);
			thread.start();
		}

		try {
			while (queue.size() > 0) {
				TimeUnit.SECONDS.sleep(10);
			}
	
			for (Thread t : threads) {
				t.join();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		display.finishProcess();
		
		ret = new ArrayList<>(fullList);
		
		writeToCache(cacheFileName, ret);

		return ret;

	}

	public class WorkerThread extends Thread {
		private int bufferSize;
		private LinkedBlockingDeque<String> queue;
		private LinkedBlockingDeque<AGMPhenotypeAnnotation> fullList;
		private ProcessDisplayHelper display;
		
		public WorkerThread(int bufferSize, LinkedBlockingDeque<String> queue, LinkedBlockingDeque<AGMPhenotypeAnnotation> fullList, ProcessDisplayHelper display) {
			this.bufferSize = bufferSize;
			this.queue = queue;
			this.fullList = fullList;
			this.display = display;
		}

		@Override
		public void run() {

			HashMap<String, Object> params = new HashMap<>();
			params.put("internal", false);
			params.put("obsolete", false);
			//params.put("phenotypeAnnotationSubject.primaryExternalId", "SGD:S000001240");

			while(true) {
				if(queue.isEmpty()) {
					return;
				}

				try {
					int page = Integer.parseInt(queue.takeFirst());

					SearchResponse<AGMPhenotypeAnnotation> response = agmApi.findForPublic(page, bufferSize, params);
					for (AGMPhenotypeAnnotation pa : response.getResults()) {
						if (isValidNeoEntity(getAllNeoModelIDs(), pa.getPhenotypeAnnotationSubject().getIdentifier())) {
							fullList.offer(pa);
						}
						display.progressProcess();
					}

				} catch (NumberFormatException | InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
		
	}

}
