package org.alliancegenome.indexer.variant.es.managers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.core.filedownload.model.DownloadFileSet;
import org.alliancegenome.core.filedownload.model.DownloadSource;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.repository.indexer.GeneIndexerRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SourceDocumentCreationManager extends Thread {

	private DownloadFileSet downloadSet;

	public SourceDocumentCreationManager(DownloadFileSet downloadSet) {
		this.downloadSet = downloadSet;
	}

	public void run() {

		try {

			ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getSourceDocumentCreatorThreads());

			GeneIndexerRepository geneRepo = new GeneIndexerRepository();
			GeneDocumentCache geneCache = geneRepo.getGeneCacheCrossReferencesSynonyms();
			geneRepo.close();

			for(DownloadSource source: downloadSet.getDownloadFileSet()) {
				SourceDocumentCreation creator = new SourceDocumentCreation(source, geneCache);
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
