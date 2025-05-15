package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.curation.LitDocument;
import org.alliancegenome.indexer.indexers.curation.interfaces.LiteratureEaslticSearchInterface;

import si.mazi.rescu.RestProxyFactory;

public class LitIndexer extends Indexer {

	public LitIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	private LiteratureEaslticSearchInterface literatureESApi = RestProxyFactory.createProxy(LiteratureEaslticSearchInterface.class, "");

	
	@Override
	protected void index() {
		
		Map<String, Object> countObject = literatureESApi.count();
		
		try {
		
			int totalPages = (int)countObject.get("count") / indexerConfig.getBufferSize();
			
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
	
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}
			
			System.out.println(totalPages);
	
			initiateThreading(queue);
		
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		while (true) {
			try {
				if (queue.isEmpty()) {
					return;
				}
				String page = queue.takeFirst();
				Map<String, Object> object = literatureESApi.search(Integer.parseInt(page), indexerConfig.getBufferSize());
				
				Map<String, Object> hitsMap = (Map<String, Object>) object.get("hits");
				
				List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsMap.get("hits");

				List<LitDocument> list = new ArrayList<>();
				
				for(Map<String, Object> map: hits) {
					LitDocument doc = new LitDocument();
					list.add(doc);
				}
				
				indexDocuments(list);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
				return;
			}
		}
		
	}

}
