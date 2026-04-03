package org.alliancegenome.indexer.variant.es.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.filedownload.model.DownloadFileSet;
import org.alliancegenome.core.filedownload.model.DownloadSource;
import org.alliancegenome.curation_api.interfaces.document.VariantDocumentInterface;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.neo4j.repository.indexer.GeneIndexerRepository;

import lombok.extern.slf4j.Slf4j;
import net.nilosplace.process_display.util.ObjectFileStorage;
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

			GeneIndexerRepository geneRepo = new GeneIndexerRepository();
			GeneDocumentCache geneCache = geneRepo.getGeneCacheCrossReferencesSynonyms();
			geneRepo.close();

			ObjectFileStorage<HashSet<String>> variantsCacheFileStorage = new ObjectFileStorage<>();

			HashSet<String> variantsCache = null;
			File cacheFile = new File("data/variantCache.data");
			try {
				if (!cacheFile.exists()) {
					log.info("Pulling Variant Data from API");
					variantsCache = new HashSet<>(variantApi.getAllVariantNames().getEntities());
					log.info("Caching Variant Data to file: ");
					variantsCacheFileStorage.writeObjectToFile(variantsCache, cacheFile);
				} else {
					log.info("Reading Varinat Cache from file:");
					variantsCache = variantsCacheFileStorage.readObjectFromFile(cacheFile);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			List<SourceDocumentCreation> creators = new ArrayList<>();
			for (DownloadSource source : downloadSet.getDownloadFileSources()) {
				if (source.getActive()) {
					SourceDocumentCreation creator = new SourceDocumentCreation(downloadSet.getDownloadPath(), source, geneCache, variantsCache);
					creator.start();
					creators.add(creator);
				}
			}
			for (SourceDocumentCreation creator : creators) {
				creator.join();
			}
			log.info("SourceDocumentCreationManager all species finished");

		} catch (Exception e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
