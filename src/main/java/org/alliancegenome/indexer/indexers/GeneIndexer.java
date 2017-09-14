package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.node.Gene;
import org.alliancegenome.indexer.repository.GeneRepository;
import org.alliancegenome.indexer.translators.GeneTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneIndexer extends Indexer<GeneDocument> {

	private Logger log = LogManager.getLogger(getClass());
	private GeneRepository repo2 = new GeneRepository();
	private GeneTranslator geneTrans = new GeneTranslator();

	public GeneIndexer(String currnetIndex, TypeConfig config) {
		super(currnetIndex, config);
	}

	@Override
	public void index() {
		try {
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<String>();
			List<String> fulllist = repo2.getAllGeneKeys();

			for(String s: fulllist) {
				queue.add(s);
			}

			List<WorkerThread> threads = new ArrayList<WorkerThread>();

			for(int i = 0; i < 10; i++) {
				WorkerThread thread = new WorkerThread(queue);
				threads.add(thread);
				thread.start();
			}

			int total = queue.size();
			startProcess(total);
			while(!queue.isEmpty()) {
				progress(queue.size(), total);
				Thread.sleep(60000);
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
		private GeneRepository repo = new GeneRepository();
		LinkedBlockingDeque<String> queue;
		public WorkerThread(LinkedBlockingDeque<String> queue) {
			this.queue = queue;
		}

		public void run() {
			ArrayList<Gene> list = new ArrayList<Gene>();
			while(true) {
				try {
					if(list.size() >= 100) {
						addDocuments(geneTrans.translateEntities(list));
						if(list != null) list.clear();
						list = new ArrayList<Gene>();
					}
					if(queue.isEmpty()) {
						if(list.size() > 0) {
							addDocuments(geneTrans.translateEntities(list));
							list.clear();
						}
						return;
					}

					String key = queue.takeFirst();
					Gene gene = repo.getOneGene(key);
					list.add(gene);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}



}