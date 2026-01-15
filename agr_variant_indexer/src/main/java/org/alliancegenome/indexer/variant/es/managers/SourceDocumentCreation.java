package org.alliancegenome.indexer.variant.es.managers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.api.entity.VariantSummaryDocument;
import org.alliancegenome.core.filedownload.model.DownloadSource;
import org.alliancegenome.core.util.StatsCollector;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.core.variant.converters.AlleleVariantSequenceConverter;
import org.alliancegenome.core.variant.converters.AlleleVariantSequenceCurationConverter;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SourceDocumentCreation extends Thread {

	private final GeneDocumentCache geneCache;
	private String downloadPath;
	private DownloadSource source;
	private SpeciesType speciesType;
	private String[] header;
	public static String indexName;

	private BulkProcessor.Builder builder1;
	private BulkProcessor.Builder builder2;
	private BulkProcessor.Builder builder3;
	private BulkProcessor.Builder builder4;
	private BulkProcessor.Builder builder5;
	private BulkProcessor.Builder builder6;
	private BulkProcessor.Builder builder7;
	private BulkProcessor.Builder builder8;

	private BulkProcessor bulkProcessor1;
	private BulkProcessor bulkProcessor2;
	private BulkProcessor bulkProcessor3;
	private BulkProcessor bulkProcessor4;
	private BulkProcessor bulkProcessor5;
	private BulkProcessor bulkProcessor6;
	private BulkProcessor bulkProcessor7;
	private BulkProcessor bulkProcessor8;

	// public AlleleRepository repo = new AlleleRepository();

	private boolean indexing = VariantConfigHelper.isIndexing();
	private boolean gatherStats = VariantConfigHelper.isGatherStats();

	private LinkedBlockingDeque<List<VariantContext>> vcQueue = new LinkedBlockingDeque<List<VariantContext>>(VariantConfigHelper.getSourceDocumentCreatorVCQueueSize());
	private LinkedBlockingDeque<List<Object>> objectQueue = new LinkedBlockingDeque<>(VariantConfigHelper.getSourceDocumentCreatorObjectQueueSize());

	private AlleleVariantSequenceConverter aVSConverter;

	private LinkedBlockingDeque<List<String>> jsonQueue1;
	private LinkedBlockingDeque<List<String>> jsonQueue2;
	private LinkedBlockingDeque<List<String>> jsonQueue3;
	private LinkedBlockingDeque<List<String>> jsonQueue4;
	private LinkedBlockingDeque<List<String>> jsonQueue5;
	private LinkedBlockingDeque<List<String>> jsonQueue6;
	private LinkedBlockingDeque<List<String>> jsonQueue7;
	private LinkedBlockingDeque<List<String>> jsonQueue8;

	private long[][] jqs = new long[8][3]; // Json Queue Stats

	private ProcessDisplayHelper ph1 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph2 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph4 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph5 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());

	private AlleleVariantSequenceCurationConverter converter;

	private StatsCollector statsCollector = new StatsCollector();
	private String messageHeader = "";

	private RestHighLevelClient client1 = EsClientFactory.getMustCloseSearchClient();
	private RestHighLevelClient client2 = EsClientFactory.getMustCloseSearchClient();
	private RestHighLevelClient client3 = EsClientFactory.getMustCloseSearchClient();
	private RestHighLevelClient client4 = EsClientFactory.getMustCloseSearchClient();
	private RestHighLevelClient client5 = EsClientFactory.getMustCloseSearchClient();
	private RestHighLevelClient client6 = EsClientFactory.getMustCloseSearchClient();
	private RestHighLevelClient client7 = EsClientFactory.getMustCloseSearchClient();
	private RestHighLevelClient client8 = EsClientFactory.getMustCloseSearchClient();

	public SourceDocumentCreation(String downloadPath, DownloadSource source, GeneDocumentCache geneCache) {
		this.downloadPath = downloadPath;
		this.source = source;
		this.geneCache = geneCache;
		speciesType = SpeciesType.getTypeByID(source.getTaxonId());
		aVSConverter = new AlleleVariantSequenceConverter();
		converter = new AlleleVariantSequenceCurationConverter();
		messageHeader = speciesType.getModName() + " ";
	}

	@Override
	public void run() {

		jsonQueue1 = new LinkedBlockingDeque<>(250);
		jsonQueue2 = new LinkedBlockingDeque<>(250);
		jsonQueue3 = new LinkedBlockingDeque<>(250);
		jsonQueue4 = new LinkedBlockingDeque<>(250);
		jsonQueue5 = new LinkedBlockingDeque<>(250);
		jsonQueue6 = new LinkedBlockingDeque<>(250);
		jsonQueue7 = new LinkedBlockingDeque<>(250);
		jsonQueue8 = new LinkedBlockingDeque<>(250);

		if (indexing) {
			builder1 = BulkProcessor.builder((request, bulkListener) -> client1.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(messageHeader + "BulkProcessor1 Request Failure: " + failure.getMessage());
					failure.printStackTrace();
					System.exit(-1);
				}
			});

			builder2 = BulkProcessor.builder((request, bulkListener) -> client2.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(messageHeader + "BulkProcessor2 Request Failure: " + failure.getMessage());
					failure.printStackTrace();
					System.exit(-1);
				}
			});

			builder3 = BulkProcessor.builder((request, bulkListener) -> client3.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(messageHeader + "BulkProcessor3 Request Failure: " + failure.getMessage());
					failure.printStackTrace();
					System.exit(-1);
				}
			});

			builder4 = BulkProcessor.builder((request, bulkListener) -> client4.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(messageHeader + "BulkProcessor4 Request Failure: " + failure.getMessage());
					failure.printStackTrace();
					System.exit(-1);
				}
			});

			builder5 = BulkProcessor.builder((request, bulkListener) -> client5.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(messageHeader + "BulkProcessor4 Request Failure: " + failure.getMessage());
					failure.printStackTrace();
					System.exit(-1);
				}
			});

			builder6 = BulkProcessor.builder((request, bulkListener) -> client6.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(messageHeader + "BulkProcessor4 Request Failure: " + failure.getMessage());
					failure.printStackTrace();
					System.exit(-1);
				}
			});

			builder7 = BulkProcessor.builder((request, bulkListener) -> client7.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(messageHeader + "BulkProcessor4 Request Failure: " + failure.getMessage());
					failure.printStackTrace();
					System.exit(-1);
				}
			});

			builder8 = BulkProcessor.builder((request, bulkListener) -> client8.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() {
				@Override
				public void beforeBulk(long executionId, BulkRequest request) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				}

				@Override
				public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
					log.error(messageHeader + "BulkProcessor4 Request Failure: " + failure.getMessage());
					failure.printStackTrace();
					System.exit(-1);
				}
			});

			bulkProcessor1 = builder1.build();
			bulkProcessor2 = builder2.build();
			bulkProcessor3 = builder3.build();
			bulkProcessor4 = builder4.build();
			bulkProcessor5 = builder5.build();
			bulkProcessor6 = builder6.build();
			bulkProcessor7 = builder7.build();
			bulkProcessor8 = builder8.build();

		}

		ph1.startProcess(messageHeader + "VCFReader");
		List<VCFReader> readers = new ArrayList<VCFReader>();
		for (String filePath : source.getGenerateFilePaths()) {
			VCFReader reader = new VCFReader(downloadPath + "/" + filePath);
			reader.start();
			readers.add(reader);
		}

		List<DocumentTransformer> transformers = new ArrayList<>();
		ph2.startProcess(messageHeader + "VCFTransformers");
		for (int i = 0; i < VariantConfigHelper.getTransformerThreads(); i++) {
			DocumentTransformer transformer = new DocumentTransformer();
			transformer.start();
			transformers.add(transformer);
		}

		List<JSONProducer> producers = new ArrayList<>();
		ph5.startProcess(messageHeader + "JSONProducers");
		for (int i = 0; i < VariantConfigHelper.getProducerThreads(); i++) {
			JSONProducer producer = new JSONProducer();
			producer.start();
			producers.add(producer);
		}

		ArrayList<VCFJsonBulkIndexer> indexers = new ArrayList<>();

		if (!indexing) {
			indexName = "no_index";
		}

		ph3.startProcess(messageHeader + "VCFJsonIndexer BulkProcessor");
		ph4.startProcess(messageHeader + "VCFJsonIndexer Buckets");
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
			VCFJsonBulkIndexer indexer5 = new VCFJsonBulkIndexer(jsonQueue5, bulkProcessor5);
			indexer5.start();
			indexers.add(indexer5);
			VCFJsonBulkIndexer indexer6 = new VCFJsonBulkIndexer(jsonQueue6, bulkProcessor6);
			indexer6.start();
			indexers.add(indexer6);
			VCFJsonBulkIndexer indexer7 = new VCFJsonBulkIndexer(jsonQueue7, bulkProcessor7);
			indexer7.start();
			indexers.add(indexer7);
			VCFJsonBulkIndexer indexer8 = new VCFJsonBulkIndexer(jsonQueue8, bulkProcessor8);
			indexer8.start();
			indexers.add(indexer8);
		}

		try {

			log.info(messageHeader + "Waiting for VCFReader's to finish");
			for (VCFReader r : readers) {
				r.join();
			}
			ph1.finishProcess();

			log.info(messageHeader + "Waiting for VC Queue to empty");
			while (!vcQueue.isEmpty()) {
				Thread.sleep(15000);
			}
			TimeUnit.MILLISECONDS.sleep(15000);
			log.info(messageHeader + "VC Queue Empty shutting down transformers");

			log.info(messageHeader + "Shutting down transformers");
			for (DocumentTransformer t : transformers) {
				t.interrupt();
				t.join();
			}
			log.info(messageHeader + "Transformers shutdown");
			ph2.finishProcess();

			log.info(messageHeader + "Waiting for Object Queue to empty");
			while (!objectQueue.isEmpty()) {
				Thread.sleep(15000);
			}
			TimeUnit.MILLISECONDS.sleep(15000);
			log.info(messageHeader + "Object Empty shuting down producers");

			log.info(messageHeader + "Shutting down producers");
			for (JSONProducer p : producers) {
				p.interrupt();
				p.join();
			}
			log.info(messageHeader + "JSONProducers shutdown");
			ph5.finishProcess();

			log.info(messageHeader + "Waiting for jsonQueue to empty");
			while (
				!jsonQueue1.isEmpty() || !jsonQueue2.isEmpty() || !jsonQueue3.isEmpty() || !jsonQueue4.isEmpty() ||
					!jsonQueue5.isEmpty() || !jsonQueue6.isEmpty() || !jsonQueue7.isEmpty() || !jsonQueue8.isEmpty()
			) {
				Thread.sleep(1000);
			}

			log.info(messageHeader + "Waiting for bulk processors to finish");

			Thread.sleep(60000);

			log.info(messageHeader + "JSon Queue Empty shuting down bulk indexers");
			for (VCFJsonBulkIndexer indexer : indexers) {
				indexer.interrupt();
				indexer.join();
			}
			log.info(messageHeader + "Bulk Indexers shutdown");
			ph3.finishProcess();
			ph4.finishProcess();


			if (gatherStats) {
				statsCollector.printOutput(speciesType.getModName());
			}

			if (indexing) {
				bulkProcessor1.flush();
				bulkProcessor2.flush();
				bulkProcessor3.flush();
				bulkProcessor4.flush();
				bulkProcessor5.flush();
				bulkProcessor6.flush();
				bulkProcessor7.flush();
				bulkProcessor8.flush();

				bulkProcessor1.awaitClose(10, TimeUnit.DAYS);
				bulkProcessor2.awaitClose(10, TimeUnit.DAYS);
				bulkProcessor3.awaitClose(10, TimeUnit.DAYS);
				bulkProcessor4.awaitClose(10, TimeUnit.DAYS);
				bulkProcessor5.awaitClose(10, TimeUnit.DAYS);
				bulkProcessor6.awaitClose(10, TimeUnit.DAYS);
				bulkProcessor7.awaitClose(10, TimeUnit.DAYS);
				bulkProcessor8.awaitClose(10, TimeUnit.DAYS);

				client1.close();
				client2.close();
				client3.close();
				client4.close();
				client5.close();
				client6.close();
				client7.close();
				client8.close();

			}

			log.info(messageHeader + "Threads finished: ");

		} catch (Exception e) {
			e.printStackTrace();
		}

		log.info(messageHeader + "Bulk Processors finished");
	}

	private class VCFReader extends Thread {

		private String filePath;
		private int workBucketSize = VariantConfigHelper.getSourceDocumentCreatorVCQueueBucketSize();

		public VCFReader(String filePath) {
			this.filePath = filePath;
		}

		@Override
		public void run() {

			VCFFileReader reader = new VCFFileReader(new File(filePath), false);
			CloseableIterator<VariantContext> iter1 = reader.iterator();
			if (header == null) {
				log.info(messageHeader + "Setting VCF File Header: " + filePath);
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

		@Override
		public void run() {
			List<Object> workBucket = new ArrayList<>();
			while (!(Thread.currentThread().isInterrupted())) {
				try {
					List<VariantContext> ctxList = vcQueue.take();

					for (VariantContext ctx : ctxList) {
						try {
							List<AlleleVariantSequence> avsList = aVSConverter.convertContextToAlleleVariantSequence(ctx, header, speciesType, geneCache);
							List<VariantSummaryDocument> variantSummaryDocuments = converter.convertContextToDocument(ctx, header, speciesType, geneCache);
							for (AlleleVariantSequence avs : avsList) {
								workBucket.add(avs);
								ph2.progressProcess("objectQueue: " + objectQueue.size());
							}
							for (VariantSummaryDocument variantSummaryDocument : variantSummaryDocuments) {
								workBucket.add(variantSummaryDocument);
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
		private final ObjectMapper variantSummaryMapper = new ObjectMapper();

		//private SummaryStatistics stats = new SummaryStatistics();
		private DescriptiveStatistics stats = new DescriptiveStatistics(100000);

		@Override
		public void run() {
			mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
			mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
			// Separate mapper for VariantSummaryDocument - no JsonView restriction
			variantSummaryMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
			while (!(Thread.currentThread().isInterrupted())) {
				try {
					List<Object> docList = objectQueue.take();

					List<String> docs1 = new ArrayList<>();
					List<String> docs2 = new ArrayList<>();
					List<String> docs3 = new ArrayList<>();
					List<String> docs4 = new ArrayList<>();
					List<String> docs5 = new ArrayList<>();
					List<String> docs6 = new ArrayList<>();
					List<String> docs7 = new ArrayList<>();
					List<String> docs8 = new ArrayList<>();

					if (docList.size() > 0) {
						for (Object doc : docList) {
							try {
								String jsonDoc;
								if (doc instanceof VariantSummaryDocument) {
									jsonDoc = variantSummaryMapper.writeValueAsString(doc);
								} else {
									jsonDoc = mapper.writerWithView(View.AlleleVariantSequenceConverterForES.class).writeValueAsString(doc);
								}
								int len = jsonDoc.length();
								stats.addValue(len);

//								double z = (jsonDoc.length() - stats.getMean()) / stats.getStandardDeviation();
//								
//								if (z < -norm) {
//									docs1.add(jsonDoc);
//								} else if (z > norm) {
//									docs4.add(jsonDoc);
//								} else if (z < 0) {
//									docs2.add(jsonDoc);
//								} else if (z > 0) {
//									docs3.add(jsonDoc);
//								} else {
//									// Should never hit this condition
//								}

								double skew = stats.getSkewness();
								double sd = stats.getStandardDeviation();

								int lowerWidth = (int) (sd / skew);
								int upperWidth = (int) sd;

								double mean = stats.getMean();

								int t1 = (int) (mean - (1.5 * lowerWidth));
								int t2 = (int) (mean - (1 * lowerWidth));
								int t3 = (int) (mean - (0.5 * lowerWidth));
								int t4 = (int) mean;
								int t5 = (int) (mean + (0.5 * upperWidth));
								int t6 = (int) (mean + (1 * upperWidth));
								int t7 = (int) (mean + (2 * upperWidth));

								if (len < t1) {
									docs1.add(jsonDoc);
									jqs[0][2] += len;
								} else if (len < t2) {
									docs2.add(jsonDoc);
									jqs[1][2] += len;
								} else if (len < t3) {
									docs3.add(jsonDoc);
									jqs[2][2] += len;
								} else if (len < t4) {
									docs4.add(jsonDoc);
									jqs[3][2] += len;
								} else if (len < t5) {
									docs5.add(jsonDoc);
									jqs[4][2] += len;
								} else if (len < t6) {
									docs6.add(jsonDoc);
									jqs[5][2] += len;
								} else if (len < t7) {
									docs7.add(jsonDoc);
									jqs[6][2] += len;
								} else {
									docs8.add(jsonDoc);
									jqs[7][2] += len;
								}

								ph5.progressProcess("M: " + (int) mean + " SD: " + (int) sd + " SK: " + skew
									//+ " lw: " + lowerWidth + " uw: " + upperWidth + " t1: " + t1 + " t2: " + t2 + " t3: " + t3 + " t4: " + t4 + " t5: " + t5 + " t6: " + t6 + " t7: " + t7
									+ " jsonQueue1(" + jqs[0][0] + "," + jqs[0][1] + "," + jqs[0][2] + "): " + jsonQueue1.size()
									+ " jsonQueue2(" + jqs[1][0] + "," + jqs[1][1] + "," + jqs[1][2] + "): " + jsonQueue2.size()
									+ " jsonQueue3(" + jqs[2][0] + "," + jqs[2][1] + "," + jqs[2][2] + "): " + jsonQueue3.size()
									+ " jsonQueue4(" + jqs[3][0] + "," + jqs[3][1] + "," + jqs[3][2] + "): " + jsonQueue4.size()
									+ " jsonQueue5(" + jqs[4][0] + "," + jqs[4][1] + "," + jqs[4][2] + "): " + jsonQueue5.size()
									+ " jsonQueue6(" + jqs[5][0] + "," + jqs[5][1] + "," + jqs[5][2] + "): " + jsonQueue6.size()
									+ " jsonQueue7(" + jqs[6][0] + "," + jqs[6][1] + "," + jqs[6][2] + "): " + jsonQueue7.size()
									+ " jsonQueue8(" + jqs[7][0] + "," + jqs[7][1] + "," + jqs[7][2] + "): " + jsonQueue8.size()
								);

							} catch (Exception e) {
								e.printStackTrace();
							}
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
							if (docs5.size() > 0) {
								jsonQueue5.put(docs5);
								jqs[4][0]++;
								jqs[4][1] += docs5.size();
							}
							if (docs6.size() > 0) {
								jsonQueue6.put(docs6);
								jqs[5][0]++;
								jqs[5][1] += docs6.size();
							}
							if (docs7.size() > 0) {
								jsonQueue7.put(docs7);
								jqs[6][0]++;
								jqs[6][1] += docs7.size();
							}
							if (docs8.size() > 0) {
								jsonQueue8.put(docs8);
								jqs[7][0]++;
								jqs[7][1] += docs8.size();
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

		@Override
		public void run() {
			while (!(Thread.currentThread().isInterrupted())) {
				try {
					List<String> docs = jsonQueue.take();

					for (String doc : docs) {
						if (gatherStats) {
							statsCollector.addDocument(doc);
						}
						if (indexing) {
							bulkProcessor.add(new IndexRequest(indexName).source(doc, XContentType.JSON));
						}
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
