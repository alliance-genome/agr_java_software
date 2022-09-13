package org.alliancegenome.indexer.variant.es.managers;

import java.util.concurrent.*;

import org.alliancegenome.core.filedownload.model.*;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.repository.indexer.GeneIndexerRepository;
import org.elasticsearch.client.RestHighLevelClient;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class SourceDocumentCreationManager extends Thread {

	private DownloadFileSet downloadSet;
	private RestHighLevelClient client;

	public SourceDocumentCreationManager(RestHighLevelClient client, DownloadFileSet downloadSet) {
		this.client = client;
		this.downloadSet = downloadSet;
	}

	public void run() {

		try {

			ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getSourceDocumentCreatorThreads());

			GeneIndexerRepository geneRepo = new GeneIndexerRepository();
			GeneDocumentCache geneCache = geneRepo.getGeneCacheCrossReferencesSynonyms();
			geneRepo.close();

			for(DownloadSource source: downloadSet.getDownloadFileSet()) {
				SourceDocumentCreation creator = new SourceDocumentCreation(client, source, geneCache);
				executor.execute(creator);
			}

			log.info("SourceDocumentCreationManager shuting down executor: ");
			executor.shutdown();
			while (!executor.isTerminated()) {
				Thread.sleep(1000);
			}
			log.info("SourceDocumentCreationManager executor shut down: ");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
