package org.alliancegenome.indexer.indexers;


import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.DiseaseAnnotationDocument;
import org.alliancegenome.indexer.entity.node.DOTerm;
import org.alliancegenome.indexer.indexers.DiseaseIndexer.WorkerThread;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class DiseaseAnnotationIndexer extends Indexer<DiseaseAnnotationDocument> {

	private Logger log = LogManager.getLogger(getClass());

	private DiseaseRepository repo = new DiseaseRepository();
	private DiseaseTranslator diseaseTrans = new DiseaseTranslator();

	public DiseaseAnnotationIndexer(String currentIndex, TypeConfig config) {
		super(currentIndex, config);
	}

	@Override
	public void index() {

		//List<DOTerm> diseaseTermsWithAnnotations = repo.getDiseaseTermsWithAnnotations();
		//log.info("Disease Records with annotations: " + diseaseTermsWithAnnotations.size());
		//addDocuments(diseaseTrans.translateAnnotationEntities(diseaseTermsWithAnnotations, 1));
		//log.info("Finished indexing disease annotations: " );
		
		try {
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<String>();
			List<String> fulllist = repo.getAllDiseaseKeys();

			for(String s: fulllist) {
				queue.add(s);
			}

			List<WorkerThread> threads = new ArrayList<WorkerThread>();

			for(int i = 0; i < 4; i++) {
				WorkerThread thread = new WorkerThread(queue);
				threads.add(thread);
				thread.start();
			}

			int total = queue.size();
			startProcess(total);
			while(!queue.isEmpty()) {
				Thread.sleep(6000);
				progress(queue.size(), total);
			}
			finishProcess(total);

			for(WorkerThread t: threads) {
				t.join();
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	
	public class WorkerThread extends Thread {
		private DiseaseRepository repo2 = new DiseaseRepository();
		LinkedBlockingDeque<String> queue;
		public WorkerThread(LinkedBlockingDeque<String> queue) {
			this.queue = queue;
		}

		public void run() {
			ArrayList<DOTerm> list = new ArrayList<DOTerm>();
			while(true) {
				try {
					if(list.size() >= 100) {
						addDocuments(diseaseTrans.translateAnnotationEntities(list, 1));
						if(list != null) list.clear();
						list = new ArrayList<DOTerm>();
					}
					if(queue.isEmpty()) {
						if(list.size() > 0) {
							addDocuments(diseaseTrans.translateAnnotationEntities(list, 1));
							list.clear();
						}
						return;
					}

					String key = queue.takeFirst();
					DOTerm disease = repo2.getDiseaseTermWithAnnotations(key);
					if(disease != null) {
						list.add(disease);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
