package org.alliancegenome.indexer;

import java.util.ArrayList;

import org.alliancegenome.indexer.enums.DocumentEntityType;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.util.ConfigHelper;
import org.alliancegenome.indexer.util.IndexManager;

public class Main {

	public static void main(String[] args) {
		ConfigHelper.init();

		IndexManager manager = new IndexManager();
		manager.createIndexes();
		
		ArrayList<Indexer> indexers = new ArrayList<Indexer>();

		for(DocumentEntityType det: DocumentEntityType.values()) {

			try {
				Indexer indexer = (Indexer)det.getIndexerClass().newInstance();
				indexer.init();
				indexer.start();
				indexer.finish();
				indexers.add(indexer);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		System.out.println("Waiting for Indexers to finish");
		for(Indexer i: indexers) {
			try {
				i.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
}
