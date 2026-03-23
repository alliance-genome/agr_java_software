package org.alliancegenome.indexer.variant.es.managers;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.filedownload.model.DownloadFileSet;
import org.alliancegenome.core.filedownload.model.DownloadSource;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.VariantDocumentInterface;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.neo4j.repository.indexer.GeneIndexerRepository;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class SourceDocumentCreationManager extends Thread {

	private DownloadFileSet downloadSet;

	private final VariantDocumentInterface variantApi = RestProxyFactory.createProxy(VariantDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public SourceDocumentCreationManager(DownloadFileSet downloadSet) {
		this.downloadSet = downloadSet;
	}

	@Override
	public void run() {

		try {

			ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getSourceDocumentCreatorThreads());

			GeneIndexerRepository geneRepo = new GeneIndexerRepository();
			GeneDocumentCache geneCache = geneRepo.getGeneCacheCrossReferencesSynonyms();

			HashSet<String> variantsCache = new HashSet<>(variantApi.getAllVariantNames().getEntities());
			geneRepo.close();

			for (DownloadSource source : downloadSet.getDownloadFileSources()) {
				if (source.getActive()) {
					SourceDocumentCreation creator = new SourceDocumentCreation(downloadSet.getDownloadPath(), source, geneCache, variantsCache);
					executor.execute(creator);
				}
			}
			log.info("SourceDocumentCreationManager shutting down executor... ");
			executor.shutdown();
			while (!executor.isTerminated()) {
				Thread.sleep(1000);
			}
			log.info("SourceDocumentCreationManager executor shut down: ");

		} catch (Exception e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
