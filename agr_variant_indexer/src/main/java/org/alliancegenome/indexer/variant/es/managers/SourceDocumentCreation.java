package org.alliancegenome.indexer.variant.es.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.core.filedownload.model.DownloadSource;
import org.alliancegenome.core.filedownload.model.DownloadableFile;
import org.alliancegenome.core.util.StatsCollector;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.core.variant.converters.AlleleVariantSequenceConverter;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.view.View;
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
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.core.TimeValue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SourceDocumentCreation extends Thread {

	private final GeneDocumentCache geneCache;
	private DownloadSource source;
	private SpeciesType speciesType;
	private String[] header = null;
	public static String indexName;
	
	private BulkProcessor.Builder builder1;
	private BulkProcessor.Builder builder2;
	private BulkProcessor.Builder builder3;
	private BulkProcessor.Builder builder4;

	private BulkProcessor bulkProcessor1;
	private BulkProcessor bulkProcessor2;
	private BulkProcessor bulkProcessor3;
	private BulkProcessor bulkProcessor4;

	//public AlleleRepository repo = new AlleleRepository();

	private boolean indexing = VariantConfigHelper.isIndexing();
	private boolean gatherStats = VariantConfigHelper.isGatherStats();

	private LinkedBlockingDeque<List<VariantContext>> vcQueue = new LinkedBlockingDeque<List<VariantContext>>(VariantConfigHelper.getSourceDocumentCreatorVCQueueSize());
	private LinkedBlockingDeque<List<AlleleVariantSequence>> objectQueue = new LinkedBlockingDeque<List<AlleleVariantSequence>>(VariantConfigHelper.getSourceDocumentCreatorObjectQueueSize());

	private AlleleVariantSequenceConverter aVSConverter;
	
	private LinkedBlockingDeque<List<String>> jsonQueue1;
	private LinkedBlockingDeque<List<String>> jsonQueue2;
	private LinkedBlockingDeque<List<String>> jsonQueue3;
	private LinkedBlockingDeque<List<String>> jsonQueue4;
	
	private int[][] jqs = new int[4][2]; // Json Queue Stats

	private ProcessDisplayHelper ph1 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph2 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph4 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph5 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	
	private int[][] config_settings = VariantConfigHelper.getBulkProcessorSettingsArray();

	AlleleVariantSequenceConverter converter = new AlleleVariantSequenceConverter();

	private StatsCollector statsCollector = new StatsCollector();
	private String message_header = "";
	
	private RestHighLevelClient client1 = EsClientFactory.getMustCloseSearchClient();
	private RestHighLevelClient client2 = EsClientFactory.getMustCloseSearchClient();
	private RestHighLevelClient client3 = EsClientFactory.getMustCloseSearchClient();
	private RestHighLevelClient client4 = EsClientFactory.getMustCloseSearchClient();
	
	public SourceDocumentCreation(DownloadSource source, GeneDocumentCache geneCache) {
		this.source = source;
		this.geneCache = geneCache;
		speciesType = SpeciesType.getTypeByID(source.getTaxonId());
		aVSConverter = new AlleleVariantSequenceConverter();
		message_header = speciesType.getModName() + " ";
	}

	public void run() {

		jsonQueue1 = new LinkedBlockingDeque<>(config_settings[0][3]); // Max 10K * 10K = 100M
		jsonQueue2 = new LinkedBlockingDeque<>(config_settings[1][3]); // Max 75K * 1333 = 100M
		jsonQueue3 = new LinkedBlockingDeque<>(config_settings[2][3]); // Max 100K * 1000 = 100M
		jsonQueue4 = new LinkedBlockingDeque<>(config_settings[3][3]); // Max 200K * 500 = 100M if documents are larger then we might need to split this down more

		if(indexing) {
			log.info(message_header + "Creating Bulk Processor 0 - 10K");
			builder1 = BulkProcessor.builder((request, bulkListener) -> client1.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}
	
				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}
	
				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(message_header + "BulkProcessor1 Request Failure: " + failure.getMessage());
					for (DocWriteRequest<?> req : request.requests()) {
						IndexRequest idxreq = (IndexRequest) req;
						bulkProcessor1.add(idxreq);
					}
					log.error(message_header + "Finished Adding requests to Queue:");
				}
			});
	
			log.info(message_header + "Creating Bulk Processor 10K - 75K");
			builder2 = BulkProcessor.builder((request, bulkListener) -> client2.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}
	
				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}
	
				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(message_header + "BulkProcessor2 Request Failure: " + failure.getMessage());
					for (DocWriteRequest<?> req : request.requests()) {
						IndexRequest idxreq = (IndexRequest) req;
						bulkProcessor2.add(idxreq);
					}
					log.error(message_header + "Finished Adding requests to Queue:");
				}
			});
	
			log.info(message_header + "Creating Bulk Processor 75K - 100K");
			builder3 = BulkProcessor.builder((request, bulkListener) -> client3.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}
	
				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}
	
				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(message_header + "BulkProcessor3 Request Failure: " + failure.getMessage());
					for (DocWriteRequest<?> req : request.requests()) {
						IndexRequest idxreq = (IndexRequest) req;
						bulkProcessor3.add(idxreq);
					}
					log.error(message_header + "Finished Adding requests to Queue:");
				}
			});
	
			log.info(message_header + "Creating Bulk Processor 100K - 200K");
			builder4 = BulkProcessor.builder((request, bulkListener) -> client4.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}
	
				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}
	
				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(message_header + "BulkProcessor4 Request Failure: " + failure.getMessage());
					for (DocWriteRequest<?> req : request.requests()) {
						IndexRequest idxreq = (IndexRequest) req;
						bulkProcessor4.add(idxreq);
					}
					log.error(message_header + "Finished Adding requests to Queue:");
				}
			});
	
			builder1.setBulkActions(config_settings[0][0]); // 1000
			builder1.setConcurrentRequests(config_settings[0][1]); // 10
			builder1.setBulkSize(new ByteSizeValue(config_settings[0][2], ByteSizeUnit.MB)); // 10
			builder1.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
			bulkProcessor1 = builder1.build();
			log.info(message_header + "BP1: BA: " + config_settings[0][0] + " CR: " + config_settings[0][1] + " BS: " + config_settings[0][2]);

			builder2.setBulkActions(config_settings[1][0]); // 133
			builder2.setConcurrentRequests(config_settings[1][1]); // 10
			builder2.setBulkSize(new ByteSizeValue(config_settings[1][2], ByteSizeUnit.MB)); // 10
			builder2.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
			bulkProcessor2 = builder2.build();
			log.info(message_header + "BP2: BA: " + config_settings[1][0] + " CR: " + config_settings[1][1] + " BS: " + config_settings[1][2]);
	
			builder3.setBulkActions(config_settings[2][0]); // 100 
			builder3.setConcurrentRequests(config_settings[2][1]); // 10
			builder3.setBulkSize(new ByteSizeValue(config_settings[2][2], ByteSizeUnit.MB)); // 10
			builder3.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
			bulkProcessor3 = builder3.build();
			log.info(message_header + "BP3: BA: " + config_settings[2][0] + " CR: " + config_settings[2][1] + " BS: " + config_settings[2][2]);
	
			builder4.setBulkActions(config_settings[3][0]); // 50
			builder4.setConcurrentRequests(config_settings[3][1]); // 10
			builder4.setBulkSize(new ByteSizeValue(config_settings[3][2], ByteSizeUnit.MB)); // 10
			builder4.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
			bulkProcessor4 = builder4.build();
			log.info(message_header + "BP4: BA: " + config_settings[3][0] + " CR: " + config_settings[3][1] + " BS: " + config_settings[3][2]);

		}
		
		ph1.startProcess(message_header + "VCFReader");
		List<VCFReader> readers = new ArrayList<VCFReader>();
		for (DownloadableFile df : source.getFileList()) {
			VCFReader reader = new VCFReader(df);
			reader.start();
			readers.add(reader);
		}

		List<DocumentTransformer> transformers = new ArrayList<>();
		ph2.startProcess(message_header + "VCFTransformers");
		for (int i = 0; i < VariantConfigHelper.getTransformerThreads(); i++) {
			DocumentTransformer transformer = new DocumentTransformer();
			transformer.start();
			transformers.add(transformer);
		}

		List<JSONProducer> producers = new ArrayList<>();
		ph5.startProcess(message_header + "JSONProducers");
		for (int i = 0; i < VariantConfigHelper.getProducerThreads(); i++) {
			JSONProducer producer = new JSONProducer();
			producer.start();
			producers.add(producer);
		}

		ArrayList<VCFJsonBulkIndexer> indexers = new ArrayList<>();
		
		if(!indexing) indexName = "no_index";
		
		ph3.startProcess(message_header + "VCFJsonIndexer BulkProcessor");
		ph4.startProcess(message_header + "VCFJsonIndexer Buckets");
		for (int i = 0; i < VariantConfigHelper.getIndexerBulkProcessorThreads(); i++) {
			VCFJsonBulkIndexer indexer1 = new VCFJsonBulkIndexer(jsonQueue1, bulkProcessor1);
			indexer1.start();
			indexers.add(indexer1);
			VCFJsonBulkIndexer indexer2 = new VCFJsonBulkIndexer(jsonQueue2, bulkProcessor2);
			indexer2.start();
			indexers.add(indexer2);
			VCFJsonBulkIndexer indexer3 = new VCFJsonBulkIndexer(jsonQueue3, bulkProcessor3);
			indexer3.start();
			indexers.add(indexer3);
			VCFJsonBulkIndexer indexer4 = new VCFJsonBulkIndexer(jsonQueue4, bulkProcessor4);
			indexer4.start();
			indexers.add(indexer4);
		}

		try {

			log.info(message_header + "Waiting for VCFReader's to finish");
			for (VCFReader r : readers) {
				r.join();
			}
			ph1.finishProcess();

			log.info(message_header + "Waiting for VC Queue to empty");
			while (!vcQueue.isEmpty()) {
				Thread.sleep(15000);
			}
			TimeUnit.MILLISECONDS.sleep(15000);
			log.info(message_header + "VC Queue Empty shutting down transformers");

			log.info(message_header + "Shutting down transformers");
			for (DocumentTransformer t : transformers) {
				t.interrupt();
				t.join();
			}
			log.info(message_header + "Transformers shutdown");
			ph2.finishProcess();

			log.info(message_header + "Waiting for Object Queue to empty");
			while (!objectQueue.isEmpty()) {
				Thread.sleep(15000);
			}
			TimeUnit.MILLISECONDS.sleep(15000);
			log.info(message_header + "Object Empty shuting down producers");

			log.info(message_header + "Shutting down producers");
			for (JSONProducer p : producers) {
				p.interrupt();
				p.join();
			}
			log.info(message_header + "JSONProducers shutdown");
			ph5.finishProcess();


			log.info(message_header + "Waiting for jsonQueue to empty");
			while (!jsonQueue1.isEmpty() || !jsonQueue2.isEmpty() || !jsonQueue3.isEmpty() || !jsonQueue4.isEmpty()) {
				Thread.sleep(1000);
			}

			log.info(message_header + "Waiting for bulk processors to finish");


			log.info(message_header + "JSon Queue Empty shuting down bulk indexers");
			for (VCFJsonBulkIndexer indexer : indexers) {
				indexer.interrupt();
				indexer.join();
			}
			log.info(message_header + "Bulk Indexers shutdown");
			ph3.finishProcess();
			ph4.finishProcess();

			log.info(message_header + "Threads finished: ");

			//log.info("Shutdown Neo Repo: ");
			//repo.clearCache();
			
			if(gatherStats) statsCollector.printOutput(speciesType.getModName());

			if(indexing) {
				bulkProcessor1.flush();
				bulkProcessor2.flush();
				bulkProcessor3.flush();
				bulkProcessor4.flush();

				bulkProcessor1.awaitClose(10, TimeUnit.DAYS);
				bulkProcessor2.awaitClose(10, TimeUnit.DAYS);
				bulkProcessor3.awaitClose(10, TimeUnit.DAYS);
				bulkProcessor4.awaitClose(10, TimeUnit.DAYS);
				
				client1.close();
				client2.close();
				client3.close();
				client4.close();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.info(message_header + "Bulk Processors finished");
	}

	private class VCFReader extends Thread {

		private DownloadableFile df;
		private int workBucketSize = VariantConfigHelper.getSourceDocumentCreatorVCQueueBucketSize();

		public VCFReader(DownloadableFile df) {
			this.df = df;
		}

		public void run() {

			VCFFileReader reader = new VCFFileReader(new File(df.getLocalGzipFilePath()), false);
			CloseableIterator<VariantContext> iter1 = reader.iterator();
			if (header == null) {
				log.info(message_header + "Setting VCF File Header: " + df.getLocalGzipFilePath());
				VCFInfoHeaderLine fileHeader = reader.getFileHeader().getInfoHeaderLine("CSQ");
				header = fileHeader.getDescription().split("Format: ")[1].split("\\|");
				try {
					TimeUnit.MILLISECONDS.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			try {
				List<VariantContext> workBucket = new ArrayList<>();
				while (iter1.hasNext()) {
					VariantContext vc = iter1.next();
					workBucket.add(vc);

					if (workBucket.size() >= workBucketSize) {
						vcQueue.put(workBucket);
						workBucket = new ArrayList<>();
					}
					ph1.progressProcess("vcQueue: " + vcQueue.size());
				}
				if (workBucket.size() > 0) {
					vcQueue.put(workBucket);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			reader.close();
		}
	}


	private class DocumentTransformer extends Thread {

		private final int workBucketSize = VariantConfigHelper.getSourceDocumentCreatorObjectQueueBucketSize();

		public void run() {
			List<AlleleVariantSequence> workBucket = new ArrayList<>();
			while (!(Thread.currentThread().isInterrupted())) {
				try {
					List<VariantContext> ctxList = vcQueue.take();

					for (VariantContext ctx : ctxList) {
						try {
							List<AlleleVariantSequence> avsList = aVSConverter.convertContextToAlleleVariantSequence(ctx, header, speciesType, geneCache);

							for(AlleleVariantSequence sequence: avsList) {
								workBucket.add(sequence);
								ph2.progressProcess("objectQueue: " + objectQueue.size());
							}
						} catch (Exception e) {
							e.printStackTrace();
							System.exit(-1);
						}
					}

					if (workBucket.size() >= workBucketSize) {
						objectQueue.put(workBucket);
						workBucket = new ArrayList<>();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			try {
				if (workBucket.size() > 0) {
					objectQueue.put(workBucket);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private class JSONProducer extends Thread {

		private final ObjectMapper mapper = new ObjectMapper();

		public void run() {
			mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
			mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
			while (!(Thread.currentThread().isInterrupted())) {
				try {
					List<AlleleVariantSequence> docList = objectQueue.take();

					List<String> docs1 = new ArrayList<>();
					List<String> docs2 = new ArrayList<>();
					List<String> docs3 = new ArrayList<>();
					List<String> docs4 = new ArrayList<>();

					if (docList.size() > 0) {
						for (AlleleVariantSequence doc : docList) {
							//if(!repo.getAllAllelicHgvsGNameCache().contains(doc.getVariant().getHgvsNomenclature())) {
								try {
									String jsonDoc = mapper.writerWithView(View.AlleleVariantSequenceConverterForES.class).writeValueAsString(doc);
									if (jsonDoc.length() < config_settings[0][4]) {
										docs1.add(jsonDoc);
									} else if (jsonDoc.length() < config_settings[1][4]) {
										docs2.add(jsonDoc);
									} else if (jsonDoc.length() < config_settings[2][4]) {
										docs3.add(jsonDoc);
									} else {
										docs4.add(jsonDoc);
									}
									ph5.progressProcess(
											"jsonQueue1(" + jqs[0][0] + "," + jqs[0][1] + "): " + jsonQueue1.size() + 
											" jsonQueue2(" + jqs[1][0] + "," + jqs[1][1] + "): " + jsonQueue2.size() + 
											" jsonQueue3(" + jqs[2][0] + "," + jqs[2][1] + "): " + jsonQueue3.size() + 
											" jsonQueue4(" + jqs[3][0] + "," + jqs[3][1] + "): " + jsonQueue4.size());
								} catch (Exception e) {
									e.printStackTrace();
								}
							//}
						}

						try {
							if (docs1.size() > 0) {
								jsonQueue1.put(docs1);
								jqs[0][0]++;
								jqs[0][1] += docs1.size();
							}
							if (docs2.size() > 0) {
								jsonQueue2.put(docs2);
								jqs[1][0]++;
								jqs[1][1] += docs2.size();
							}
							if (docs3.size() > 0) {
								jsonQueue3.put(docs3);
								jqs[2][0]++;
								jqs[2][1] += docs3.size();
							}
							if (docs4.size() > 0) {
								jsonQueue4.put(docs4);
								jqs[3][0]++;
								jqs[3][1] += docs4.size();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}

			}
		}
	}

	private class VCFJsonBulkIndexer extends Thread {
		private LinkedBlockingDeque<List<String>> jsonQueue;
		private BulkProcessor bulkProcessor;

		public VCFJsonBulkIndexer(LinkedBlockingDeque<List<String>> jsonQueue, BulkProcessor bulkProcessor) {
			this.jsonQueue = jsonQueue;
			this.bulkProcessor = bulkProcessor;
		}

		public void run() {
			while (!(Thread.currentThread().isInterrupted())) {
				try {
					List<String> docs = jsonQueue.take();
					
					for (String doc : docs) {
						if(gatherStats) statsCollector.addDocument(doc);
						if(indexing) bulkProcessor.add(new IndexRequest(indexName).source(doc, XContentType.JSON));
						ph3.progressProcess();
					}
					ph4.progressProcess("JSon Queue: " + jsonQueue.size());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

}
