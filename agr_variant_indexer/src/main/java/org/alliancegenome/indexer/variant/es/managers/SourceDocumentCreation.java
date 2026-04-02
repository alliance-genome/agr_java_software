package org.alliancegenome.indexer.variant.es.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.core.filedownload.model.DownloadSource;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.core.variant.converters.SequenceSummaryConverter;
import org.alliancegenome.core.variant.converters.VariantSearchResultConverter;
import org.alliancegenome.core.variant.converters.VariantSummaryConverter;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.document.es.SequenceSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.es.model.VariantSearchResultDocument;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.neo4j.entity.SpeciesType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SourceDocumentCreation extends Thread {

	private final GeneDocumentCache geneCache;
	private final HashSet<String> variantsCache;
	private String downloadPath;
	private DownloadSource source;
	private SpeciesType speciesType;
	private String[] header;
	public static String indexName;

	private LinkedBlockingDeque<List<VariantContext>> vcQueue;
	private LinkedBlockingDeque<List<ESDocument>> objectQueue;

	private LinkedBlockingDeque<List<byte[]>> jsonQueue1;
	private LinkedBlockingDeque<List<byte[]>> jsonQueue2;
	private LinkedBlockingDeque<List<byte[]>> jsonQueue3;
	private LinkedBlockingDeque<List<byte[]>> jsonQueue4;
	private LinkedBlockingDeque<List<byte[]>> jsonQueue5;
	private LinkedBlockingDeque<List<byte[]>> jsonQueue6;
	private LinkedBlockingDeque<List<byte[]>> jsonQueue7;
	private LinkedBlockingDeque<List<byte[]>> jsonQueue8;

	private long[][] jqs = new long[8][3]; // Json Queue Stats

	private ProcessDisplayHelper ph1 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph2 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph4 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());

	private VariantSummaryConverter variantSummaryConverter;
	private SequenceSummaryConverter sequenceSummaryConverter;
	private VariantSearchResultConverter variantSearchResultConverter;

	private String messageHeader = "";

	public SourceDocumentCreation(String downloadPath, DownloadSource source, GeneDocumentCache geneCache, HashSet<String> variantsCache) {
		this.downloadPath = downloadPath;
		this.source = source;
		this.geneCache = geneCache;
		this.variantsCache = variantsCache;
		speciesType = SpeciesType.getTypeByID(source.getTaxonId());
		messageHeader = speciesType.getModName() + " ";
		int vcQueueSize = source.getVcQueueSize() != null ? source.getVcQueueSize() : VariantConfigHelper.getSourceDocumentCreatorVCQueueSize();
		int objectQueueSize = source.getObjectQueueSize() != null ? source.getObjectQueueSize() : VariantConfigHelper.getSourceDocumentCreatorObjectQueueSize();
		vcQueue = new LinkedBlockingDeque<>(vcQueueSize);
		objectQueue = new LinkedBlockingDeque<>(objectQueueSize);
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

		ph1.startProcess(messageHeader + "VCFReader");
		List<VCFReader> readers = new ArrayList<VCFReader>();
		for (String filePath : source.getGenerateFilePaths()) {
			VCFReader reader = new VCFReader(downloadPath + "/" + filePath);
			reader.start();
			readers.add(reader);
		}

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}

		List<DocumentTransformer> transformers = new ArrayList<>();
		ph2.startProcess(messageHeader + "VCFTransformers");
		int transformerThreadCount = source.getTransformerThreads() != null ? source.getTransformerThreads() : VariantConfigHelper.getTransformerThreads();
		for (int i = 0; i < transformerThreadCount; i++) {
			DocumentTransformer transformer = new DocumentTransformer();
			transformer.start();
			transformers.add(transformer);
		}

		List<JSONProducer> producers = new ArrayList<>();
		ph3.startProcess(messageHeader + "JSONProducers");
		int producerThreadCount = source.getProducerThreads() != null ? source.getProducerThreads() : VariantConfigHelper.getProducerThreads();
		for (int i = 0; i < producerThreadCount; i++) {
			JSONProducer producer = new JSONProducer();
			producer.start();
			producers.add(producer);
		}

		int shardCount = VariantConfigHelper.getIndexerShards();
		LinkedBlockingDeque<List<byte[]>>[] jsonQueues = new LinkedBlockingDeque[] {
			jsonQueue1, jsonQueue2, jsonQueue3, jsonQueue4,
			jsonQueue5, jsonQueue6, jsonQueue7, jsonQueue8
		};

		ph4.startProcess(messageHeader + "RoutedBulkIndexers");
		ArrayList<RoutedBulkIndexer> indexers = new ArrayList<>();
		for (int i = 0; i < jsonQueues.length; i++) {
			RoutedBulkIndexer indexer = new RoutedBulkIndexer(
				jsonQueues[i], indexName, shardCount, 100,
				messageHeader + "BP(" + (i + 1) + ")", ph4
			);
			indexer.start();
			indexers.add(indexer);
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
			ph3.finishProcess();

			log.info(messageHeader + "Waiting for jsonQueues to empty");
			while (
				!jsonQueue1.isEmpty() || !jsonQueue2.isEmpty() || !jsonQueue3.isEmpty() || !jsonQueue4.isEmpty() ||
					!jsonQueue5.isEmpty() || !jsonQueue6.isEmpty() || !jsonQueue7.isEmpty() || !jsonQueue8.isEmpty()
			) {
				Thread.sleep(1000);
			}

			log.info(messageHeader + "Shutting down bulk indexers");
			for (RoutedBulkIndexer indexer : indexers) {
				indexer.interrupt();
				indexer.join();
			}
			ph4.finishProcess();
			log.info(messageHeader + "Bulk Indexers shutdown");
			
		} catch (Exception e) {
			ExceptionCatcher.report(e);
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
				// All files for a Mod have the same header so we only need one of them
				variantSummaryConverter = new VariantSummaryConverter(header, geneCache);
				sequenceSummaryConverter = new SequenceSummaryConverter();
				variantSearchResultConverter = new VariantSearchResultConverter();
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
				ExceptionCatcher.report(e);
				e.printStackTrace();
			}
			reader.close();
		}
	}

	private class DocumentTransformer extends Thread {

		private final int workBucketSize = VariantConfigHelper.getSourceDocumentCreatorObjectQueueBucketSize();

		@Override
		public void run() {
			List<ESDocument> workBucket = new ArrayList<>();
			while (!(Thread.currentThread().isInterrupted())) {
				try {
					List<VariantContext> ctxList = vcQueue.take();

					for (VariantContext ctx : ctxList) {
						try {
							List<VariantSummaryDocument> variantSummaryDocuments = variantSummaryConverter.convertContextToDocument(ctx, speciesType);
							variantSummaryDocuments.removeIf(doc -> doc.getSymbol() != null && variantsCache.contains(doc.getSymbol()));
							for (VariantSummaryDocument variantSummaryDocument : variantSummaryDocuments) {
								workBucket.add(variantSummaryDocument);
								if (workBucket.size() >= workBucketSize) {
									objectQueue.put(workBucket);
									workBucket = new ArrayList<>();
								}
								ph2.progressProcess("objectQueue: " + objectQueue.size());
							}
							List<SequenceSummaryDocument> sequenceSummaryDocuments = sequenceSummaryConverter.convertToSequenceSummary(variantSummaryDocuments);
							for (SequenceSummaryDocument sequenceSummaryDocument : sequenceSummaryDocuments) {
								workBucket.add(sequenceSummaryDocument);
								if (workBucket.size() >= workBucketSize) {
									objectQueue.put(workBucket);
									workBucket = new ArrayList<>();
								}
								ph2.progressProcess("objectQueue: " + objectQueue.size());
							}
							List<VariantSearchResultDocument> variantSearchResultDocuments = variantSearchResultConverter.convertToVariantSearchDocument(variantSummaryDocuments);
							for (VariantSearchResultDocument variantSearchResultDocument : variantSearchResultDocuments) {
								workBucket.add(variantSearchResultDocument);
								if (workBucket.size() >= workBucketSize) {
									objectQueue.put(workBucket);
									workBucket = new ArrayList<>();
								}
								ph2.progressProcess("objectQueue: " + objectQueue.size());
							}
						} catch (Exception e) {
							e.printStackTrace();
							ExceptionCatcher.report(e);
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

		private ObjectMapper mapper = RestConfig.createSmileObjectMapper();
		private ObjectWriter cachedWriter;
		private ObjectWriter sequenceWriter;
		private ObjectWriter searchWriter;

		// Welford's online algorithm state for mean, variance, and skewness
		private long n;
		private double mean;
		private double m2; // second central moment (for variance/SD)
		private double m3; // third central moment (for skewness)

		@Override
		public void run() {
			mapper.disable(SerializationFeature.INDENT_OUTPUT);
			searchWriter = mapper.writerWithView(CurationView.VariantSearchResultDocument.class);
			cachedWriter = mapper.writerWithView(CurationView.VariantSummaryDocument.class);
			sequenceWriter = mapper.writerWithView(CurationView.SequenceSummaryDocument.class);
			while (!(Thread.currentThread().isInterrupted())) {
				try {
					List<ESDocument> docList = objectQueue.take();

					List<byte[]> docs1 = new ArrayList<>();
					List<byte[]> docs2 = new ArrayList<>();
					List<byte[]> docs3 = new ArrayList<>();
					List<byte[]> docs4 = new ArrayList<>();
					List<byte[]> docs5 = new ArrayList<>();
					List<byte[]> docs6 = new ArrayList<>();
					List<byte[]> docs7 = new ArrayList<>();
					List<byte[]> docs8 = new ArrayList<>();

					if (!docList.isEmpty()) {
						for (ESDocument doc : docList) {
							try {
								byte[] smileDoc = null;
								if (doc instanceof SequenceSummaryDocument ssd) {
									//jsonDoc = sequenceWriter.writeValueAsString(ssd);
									smileDoc = sequenceWriter.writeValueAsBytes(ssd);
								} else if (doc instanceof VariantSummaryDocument vsd) {
									//jsonDoc = cachedWriter.writeValueAsString(vsd);
									smileDoc = cachedWriter.writeValueAsBytes(vsd);
								} else if (doc instanceof VariantSearchResultDocument vsrd) {
									//jsonDoc = searchWriter.writeValueAsString(vsrd);
									smileDoc = searchWriter.writeValueAsBytes(vsrd);
								} else {
									log.error("Unexpected ESDocument type: " + doc.getClass().getName());
									continue;									// This should never happen
								}

								int len = smileDoc.length;

								// Welford's online update for mean, M2, M3
								// This code distributes the document via size over the 8
								// queues so that each bulk processor works with same sized docs
								n++;
								double delta = len - mean;
								double deltaN = delta / n;
								double term1 = delta * deltaN * (n - 1);
								mean += deltaN;
								m3 += term1 * deltaN * (n - 2) - 3 * deltaN * m2;
								m2 += term1;

								double sd = n > 1 ? Math.sqrt(m2 / (n - 1)) : 0.0;
								double skew = (n > 2 && m2 > 0) ? (Math.sqrt(n) * m3 / Math.pow(m2, 1.5)) : 0.0;

								int lowerWidth = skew != 0.0 ? (int) (sd / skew) : (int) sd;
								int upperWidth = (int) sd;

								int t1 = (int) (mean - (1.5 * lowerWidth));
								int t2 = (int) (mean - (1.0 * lowerWidth));
								int t3 = (int) (mean - (0.5 * lowerWidth));
								int t4 = (int) mean;
								int t5 = (int) (mean + (0.5 * upperWidth));
								int t6 = (int) (mean + (1.0 * upperWidth));
								int t7 = (int) (mean + (2.0 * upperWidth));

								if (len < t1) {
									docs1.add(smileDoc);
									jqs[0][2] += len;
								} else if (len < t2) {
									docs2.add(smileDoc);
									jqs[1][2] += len;
								} else if (len < t3) {
									docs3.add(smileDoc);
									jqs[2][2] += len;
								} else if (len < t4) {
									docs4.add(smileDoc);
									jqs[3][2] += len;
								} else if (len < t5) {
									docs5.add(smileDoc);
									jqs[4][2] += len;
								} else if (len < t6) {
									docs6.add(smileDoc);
									jqs[5][2] += len;
								} else if (len < t7) {
									docs7.add(smileDoc);
									jqs[6][2] += len;
								} else {
									docs8.add(smileDoc);
									jqs[7][2] += len;
								}

								// Left here for debugging purposes
//								ph5.progressProcess("M: " + (int) mean + " SD: " + (int) sd + " SK: " + skew
//									//+ " lw: " + lowerWidth + " uw: " + upperWidth + " t1: " + t1 + " t2: " + t2 + " t3: " + t3 + " t4: " + t4 + " t5: " + t5 + " t6: " + t6 + " t7: " + t7
//									+ " jsonQueue1(" + jqs[0][0] + "," + jqs[0][1] + "," + jqs[0][2] + "): " + jsonQueue1.size()
//									+ " jsonQueue2(" + jqs[1][0] + "," + jqs[1][1] + "," + jqs[1][2] + "): " + jsonQueue2.size()
//									+ " jsonQueue3(" + jqs[2][0] + "," + jqs[2][1] + "," + jqs[2][2] + "): " + jsonQueue3.size()
//									+ " jsonQueue4(" + jqs[3][0] + "," + jqs[3][1] + "," + jqs[3][2] + "): " + jsonQueue4.size()
//									+ " jsonQueue5(" + jqs[4][0] + "," + jqs[4][1] + "," + jqs[4][2] + "): " + jsonQueue5.size()
//									+ " jsonQueue6(" + jqs[5][0] + "," + jqs[5][1] + "," + jqs[5][2] + "): " + jsonQueue6.size()
//									+ " jsonQueue7(" + jqs[6][0] + "," + jqs[6][1] + "," + jqs[6][2] + "): " + jsonQueue7.size()
//									+ " jsonQueue8(" + jqs[7][0] + "," + jqs[7][1] + "," + jqs[7][2] + "): " + jsonQueue8.size()
//								);
								
								ph3.progressProcess();

							} catch (Exception e) {
								ExceptionCatcher.report(e);
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
							ExceptionCatcher.report(e);
							e.printStackTrace();
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}

			}
		}
	}

}
