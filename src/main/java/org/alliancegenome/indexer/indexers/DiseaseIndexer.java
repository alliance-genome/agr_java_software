package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.entity.node.DOTerm;
import org.alliancegenome.indexer.entity.node.Gene;
import org.alliancegenome.indexer.indexers.GeneIndexer.WorkerThread;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.repository.GeneRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {

	private Logger log = LogManager.getLogger(getClass());

	private DiseaseRepository repo = new DiseaseRepository();
	private DiseaseTranslator diseaseTrans = new DiseaseTranslator();

	public DiseaseIndexer(String currnetIndex, TypeConfig config) {
		super(currnetIndex, config);
	}

	@Override
	public void index() {


		try {
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<String>();
			List<String> fulllist = repo.getAllDiseaseKeys();

			for(String s: fulllist) {
				queue.add(s);
			}

			List<WorkerThread> threads = new ArrayList<WorkerThread>();

			for(int i = 0; i < typeConfig.getThreadCount(); i++) {
				WorkerThread thread = new WorkerThread(queue);
				threads.add(thread);
				thread.start();
			}

			int total = queue.size();
			startProcess(total);
			while(!queue.isEmpty()) {
				Thread.sleep(60000);
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
					if(list.size() >= typeConfig.getBufferSize()) {
						addDocuments(diseaseTrans.translateEntities(list));
						if(list != null) list.clear();
						list = new ArrayList<DOTerm>();
					}
					if(queue.isEmpty()) {
						if(list.size() > 0) {
							addDocuments(diseaseTrans.translateEntities(list));
							list.clear();
						}
						return;
					}

					String key = queue.takeFirst();
					DOTerm disease = repo2.getDiseaseTerm(key);
					list.add(disease);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
