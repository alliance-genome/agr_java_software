package org.alliancegenome.indexer.variant.es.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.crud.ontology.SoTermCrudInterface;
import org.alliancegenome.curation_api.interfaces.document.VariantDocumentInterface;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.Species;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.core.es.util.ElasticSearchInterface;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.variant.config.VariantConfigHelper;
import org.alliancegenome.indexer.variant.filedownload.model.DownloadFileSet;
import org.alliancegenome.indexer.variant.filedownload.model.DownloadSource;
import org.alliancegenome.indexer.variant.interfaces.SpeciesInterface;

import lombok.extern.slf4j.Slf4j;
import net.nilosplace.process_display.util.ObjectFileStorage;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class SourceDocumentCreationManager extends Thread {

	private DownloadFileSet downloadSet;

	private final VariantDocumentInterface variantApi = RestProxyFactory.createProxy(VariantDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final SoTermCrudInterface soTermApi = RestProxyFactory.createProxy(SoTermCrudInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final SpeciesInterface speciesApi = RestProxyFactory.createProxy(SpeciesInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public SourceDocumentCreationManager(DownloadFileSet downloadSet) {
		this.downloadSet = downloadSet;
	}

	@Override
	public void run() {

		try {

			GeneCacheService geneCacheService = new GeneCacheService();
			Map<String, Gene> geneCacheMap = geneCacheService.loadGeneCache();

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

			log.info("Fetching SO term severity ranking from curation API...");
			Map<String, Integer> severityRanking = soTermApi.getSeverityRanking();
			log.info("Fetched severity ranking for {} SO terms", severityRanking.size());

			log.info("Fetching species list from curation API...");
			Map<String, Species> speciesByTaxon = new HashMap<>();
			for (Species s : speciesApi.findForPublic(0, 100, "FieldsOnly", new HashMap<>()).getResults()) {
				if (s.getTaxon() != null) {
					speciesByTaxon.put(s.getTaxon().getCurie(), s);
				}
			}
			log.info("Fetched {} species", speciesByTaxon.size());

			// Count active species and create shared jsonQueue
			long activeCount = downloadSet.getDownloadFileSources().stream().filter(DownloadSource::getActive).count();
			LinkedBlockingDeque<List<byte[]>> jsonQueue = new LinkedBlockingDeque<>((int) (250 * activeCount));

			// Query cluster CPU count and create shared bulk indexer pool
			int totalCpus = getClusterCpuCount();
			int poolSize = totalCpus * 2;
			log.info("ES cluster total CPUs: {}, RoutedBulkIndexer pool size: {}, active species: {}, jsonQueue capacity: {}", totalCpus, poolSize, activeCount, 250 * activeCount);

			// Start single-threaded retry worker with bounded queue (backpressure on producers)
			int retryQueueCapacity = 100;
			log.info("Starting RetryWorker with queue capacity: {}", retryQueueCapacity);
			RetryWorker retryWorker = new RetryWorker(retryQueueCapacity, SourceDocumentCreation.indexName);
			retryWorker.start();

			// Start shared RoutedBulkIndexer pool
			ProcessDisplayHelper phPool = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
			phPool.startProcess("SharedRoutedBulkIndexers");
			ArrayList<RoutedBulkIndexer> indexers = new ArrayList<>();
			for (int i = 0; i < poolSize; i++) {
				RoutedBulkIndexer indexer = new RoutedBulkIndexer(jsonQueue, SourceDocumentCreation.indexName, "SharedBP(" + (i + 1) + ")", phPool, retryWorker);
				indexer.start();
				indexers.add(indexer);
			}

			// Build and start active species creators
			List<SourceDocumentCreation> creators = new ArrayList<>();
			for (DownloadSource source : downloadSet.getDownloadFileSources()) {
				if (source.getActive()) {
					Species species = speciesByTaxon.get(source.getTaxonId());
					if (species == null) {
						throw new RuntimeException("Species not found in curation API for taxon: " + source.getTaxonId());
					}
					SourceDocumentCreation creator = new SourceDocumentCreation(downloadSet.getDownloadPath(), source, species, geneCacheMap, variantsCache, severityRanking, jsonQueue);
					creator.start();
					creators.add(creator);
				}
			}

			// Wait for all species threads to complete
			for (SourceDocumentCreation creator : creators) {
				creator.join();
			}

			// Wait for shared jsonQueue to drain
			log.info("All species threads finished, waiting for jsonQueue to drain");
			while (!jsonQueue.isEmpty()) {
				Thread.sleep(1000);
			}

			// Shut down shared bulk indexer pool
			log.info("Shutting down shared RoutedBulkIndexer pool");
			for (RoutedBulkIndexer indexer : indexers) {
				indexer.interrupt();
				indexer.join();
			}
			phPool.finishProcess();
			log.info("Shared RoutedBulkIndexer pool shutdown");

			// Shut down retry worker (finishes draining queue + internal deque before returning)
			log.info("Shutting down RetryWorker");
			retryWorker.shutdown();
			retryWorker.join();
			log.info("RetryWorker shutdown");

			log.info("SourceDocumentCreationManager all species finished");

		} catch (Exception e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private int getClusterCpuCount() {
		int defaultCpus = VariantConfigHelper.getIndexerShards() / 2;
		try {
			String firstHost = ConfigHelper.getEsHost().split(",")[0];
			String esHost;
			String esPort;
			if (firstHost.contains(":")) {
				esHost = firstHost.split(":")[0];
				esPort = firstHost.split(":")[1];
			} else {
				esHost = firstHost;
				esPort = String.valueOf(ConfigHelper.getEsPort());
			}
			String esUrl = "http://" + esHost + ":" + esPort;
			ElasticSearchInterface esApi = RestProxyFactory.createProxy(ElasticSearchInterface.class, esUrl);
			Map<String, Object> response = esApi.getNodesOs();
			Map<String, Object> nodes = (Map<String, Object>) response.get("nodes");
			int totalCpus = 0;
			for (Object nodeObj : nodes.values()) {
				Map<String, Object> node = (Map<String, Object>) nodeObj;
				Map<String, Object> os = (Map<String, Object>) node.get("os");
				if (os != null && os.containsKey("available_processors")) {
					totalCpus += ((Number) os.get("available_processors")).intValue();
				}
			}
			if (totalCpus > 0) {
				return totalCpus;
			}
		} catch (Exception e) {
			log.warn("Failed to query ES cluster CPU count, using default: {}", defaultCpus, e);
		}
		return defaultCpus;
	}
}
