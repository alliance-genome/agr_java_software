package org.alliancegenome.indexer.indexers;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.Note;
import org.alliancegenome.curation_api.model.entities.Species;
import org.alliancegenome.curation_api.model.entities.base.AuditedObject;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.core.es.util.EsClientFactory;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.AuditedObjectIndexerMixin;
import org.alliancegenome.indexer.interfaces.SpeciesInterface;
import org.alliancegenome.indexer.util.StatsCollector;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.xcontent.XContentType;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public abstract class Indexer extends Thread {

	public static String indexName;
	protected IndexerConfig indexerConfig;
	private RestHighLevelClient searchClient;

	private final SpeciesInterface speciesApi = RestProxyFactory.createProxy(SpeciesInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	// taxonIdPart (e.g. "9606") -> phylogeneticOrder
	protected Map<String, Integer> speciesOrderLookup;
	protected Runtime runtime = Runtime.getRuntime();
	protected DecimalFormat df = new DecimalFormat("#");
	protected ObjectMapper om = new ObjectMapper();

	private ProcessDisplayHelper display = new ProcessDisplayHelper();
	private StatsCollector stats = new StatsCollector();

	protected Map<String, Double> popularityScore = new HashMap<>();

	@Getter
	private Duration duration = Duration.ZERO;

	protected BulkProcessor bulkProcessor;

	public Indexer(IndexerConfig indexerConfig) {
		this.indexerConfig = indexerConfig;

		om.setSerializationInclusion(Include.NON_NULL);
		om = customizeObjectMapper(om);
		om.addMixIn(AuditedObject.class, AuditedObjectIndexerMixin.class);

		searchClient = EsClientFactory.getMustCloseSearchClient();
		log.info(getClass().getSimpleName() + " ES client created: " + System.identityHashCode(searchClient));

		BulkProcessor.Listener listener = new BulkProcessor.Listener() {
			@Override
			public void beforeBulk(long executionId, BulkRequest request) {
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				if (response.hasFailures()) {
					log.info("Size: " + request.requests().size() + " MB: " + request.estimatedSizeInBytes() + " Time: " + response.getTook() + " Bulk Request Finished");
					log.info(response.buildFailureMessage());
				}
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
				log.error("Bulk Request Failure: " + failure.getMessage());
				for (DocWriteRequest<?> req : request.requests()) {
					IndexRequest idxreq = (IndexRequest) req;
					bulkProcessor.add(idxreq);
				}
				log.error("Finished Adding failed requests to bulkProcessor: ");
			}
		};

		BulkProcessor.Builder builder = BulkProcessor.builder((request, bulkListener) -> searchClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener);
		builder.setBulkActions(indexerConfig.getBulkActions());
		builder.setBulkSize(new ByteSizeValue(indexerConfig.getBulkSize(), ByteSizeUnit.MB));
		builder.setConcurrentRequests(indexerConfig.getConcurrentRequests());
		builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
		bulkProcessor = builder.build();
		// bulkProcessor = BulkProcessor.builder((request, bulkListener) ->
		// searchClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
		// listener).build();

	}

	public void runIndex() {
		try {
			Instant start = Instant.now();
			display.startProcess(getClass().getSimpleName());
			index(display);
			log.info("Waiting for bulkProcessor to finish");
			bulkProcessor.flush();
			bulkProcessor.awaitClose(30L, TimeUnit.DAYS);
			display.finishProcess();
			stats.printOutput();
			Instant end = Instant.now();
			duration = Duration.between(start, end);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			ExceptionCatcher.report(e);
			System.exit(-1);
		} finally {
			closeSearchClient();
		}
	}

	@Override
	public void run() {
		super.run();
		try {
			Instant start = Instant.now();
			display.startProcess(getClass().getSimpleName());
			index(display);
			log.info("Waiting for bulkProcessor to finish");
			bulkProcessor.flush();
			bulkProcessor.awaitClose(30L, TimeUnit.DAYS);
			display.finishProcess();
			stats.printOutput();
			Instant end = Instant.now();
			duration = Duration.between(start, end);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			ExceptionCatcher.report(e);
			System.exit(-1);
		} finally {
			closeSearchClient();
		}
	}

	private void closeSearchClient() {
		if (searchClient != null) {
			try {
				log.info(getClass().getSimpleName() + " closing ES client: " + System.identityHashCode(searchClient));
				searchClient.close();
			} catch (IOException e) {
				log.warn(getClass().getSimpleName() + " error closing ES client: " + e.getMessage());
			}
		}
	}

	public <D extends ESDocument> void indexDocuments(Iterable<D> docs) {
		indexDocuments(docs, null);
	}

	public <D extends ESDocument> void indexDocuments(Iterable<D> docs, Class<?> view) {
		for (D doc : docs) {
			indexDocument(doc, view);
		}
	}

	public <D extends ESDocument> void indexDocument(D doc) {
		indexDocument(doc, null);
	}

	public <D extends ESDocument> void indexDocument(D doc, Class<?> view) {
		try {
			String json = "";
			if (view != null) {
				json = om.writerWithView(view).writeValueAsString(doc);
			} else {
				json = om.writeValueAsString(doc);
			}
			if (json.length() > 19_000_000) {
				log.error("Document is too large for ES skipping: " + json.length());
				return;
			}
			stats.addDocument(json);
			bulkProcessor.add(new IndexRequest(indexName).source(json, XContentType.JSON));
			display.progressProcess();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			log.error(e.getMessage());
			ExceptionCatcher.report(e);
			System.exit(-1);
		}
	}

	public void initiateThreading(LinkedBlockingDeque<String> queue) throws InterruptedException {
		Integer numberOfThreads = indexerConfig.getThreadCount();

		List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < numberOfThreads; i++) {
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					startSingleThread(queue);
				}
			});
			threads.add(t);
			t.start();
		}

		while (queue.size() > 0) {
			TimeUnit.SECONDS.sleep(10);
		}

		for (Thread t : threads) {
			t.join();
		}
	}

	protected void loadSpeciesOrderLookup() {
		speciesOrderLookup = new HashMap<>();
		List<Species> allSpecies = speciesApi.findForPublic(0, 100, "FieldsOnly", new HashMap<>()).getResults();
		for (Species species : allSpecies) {
			if (species.getTaxon() != null && species.getPhylogeneticOrder() != null) {
				String taxonIdPart = species.getTaxon().getCurie().replace("NCBITaxon:", "");
				speciesOrderLookup.put(taxonIdPart, species.getPhylogeneticOrder());
			}
		}
		log.info("Loaded " + speciesOrderLookup.size() + " species for speciesOrder lookup");
	}

	/**
	 * Builds a speciesOrder map for an ES document based on its subject's taxon.
	 *
	 * The returned map has one entry per species, keyed by NCBI taxon ID part (e.g. "9606").
	 * The subject's own species is set to 0; all other species are set to the subject's
	 * phylogenetic order value. This allows the API to sort by speciesOrder.<focusTaxonId>
	 * and get the focus species first (0), with all other species sorted by their own
	 * phylogenetic position — because each species' documents carry that species' own
	 * phylogenetic order as the non-self value.
	 *
	 * Example for a Rat document (phylogeneticOrder=20):
	 *   { "9606": 20, "10116": 0, "10090": 20, "7955": 20, ... }
	 *
	 * Example for a Human document (phylogeneticOrder=10):
	 *   { "9606": 0, "10116": 10, "10090": 10, "7955": 10, ... }
	 *
	 * When the API sorts by speciesOrder.10090 (mouse gene page), documents sort as:
	 *   Mouse=0, Human=10, Rat=20, Zebrafish=40, ... (each species has a unique value)
	 */
	protected HashMap<String, Integer> buildSpeciesOrder(String taxonCurie) {
		if (speciesOrderLookup == null) {
			loadSpeciesOrderLookup();
		}
		HashMap<String, Integer> order = new HashMap<>();
		String subjectTaxonIdPart = taxonCurie.replace("NCBITaxon:", "");
		Integer subjectOrder = speciesOrderLookup.getOrDefault(subjectTaxonIdPart, 0);
		for (String key : speciesOrderLookup.keySet()) {
			order.put(key, subjectOrder);
		}
		order.put(subjectTaxonIdPart, 0);
		return order;
	}

	/**
	 * Removes notes marked internal=true or obsolete=true from a relatedNotes list before it's
	 * embedded in a public ES document. Curation entities carry internal/obsolete notes (e.g.
	 * private_comment) alongside public ones with no server-side view filtering applied during
	 * indexing, so this must run on every entity's relatedNotes before it reaches
	 * indexDocument(s) — see SCRUM-6327.
	 */
	protected static void stripInternalOrObsoleteNotes(List<Note> notes) {
		if (CollectionUtils.isNotEmpty(notes)) {
			notes.removeIf(note -> !note.isNotInternalOrObsolete());
		}
	}

	protected abstract void index(ProcessDisplayHelper display);

	protected abstract void startSingleThread(LinkedBlockingDeque<String> queue);

	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return objectMapper;
	}
}
