package org.alliancegenome.indexer.variant.es.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
	private final Map<String, Integer> severityRanking;
	private String downloadPath;
	private DownloadSource source;
	private SpeciesType speciesType;
	private String[] header;
	public static String indexName;

	private LinkedBlockingDeque<List<VariantContext>> vcQueue;
	private LinkedBlockingDeque<List<ESDocument>> objectQueue;

	private LinkedBlockingDeque<List<byte[]>> jsonQueue;

	private ProcessDisplayHelper ph1 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph2 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
	private ProcessDisplayHelper ph4 = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());

	private VariantSummaryConverter variantSummaryConverter;
	private SequenceSummaryConverter sequenceSummaryConverter;
	private VariantSearchResultConverter variantSearchResultConverter;

	private String messageHeader = "";

	public SourceDocumentCreation(String downloadPath, DownloadSource source, GeneDocumentCache geneCache, HashSet<String> variantsCache, Map<String, Integer> severityRanking) {
		this.downloadPath = downloadPath;
		this.source = source;
		this.geneCache = geneCache;
		this.variantsCache = variantsCache;
		this.severityRanking = severityRanking;
		speciesType = SpeciesType.getTypeByID(source.getTaxonId());
		messageHeader = speciesType.getModName() + " ";
		int vcQueueSize = source.getVcQueueSize() != null ? source.getVcQueueSize() : VariantConfigHelper.getSourceDocumentCreatorVCQueueSize();
		int objectQueueSize = source.getObjectQueueSize() != null ? source.getObjectQueueSize() : VariantConfigHelper.getSourceDocumentCreatorObjectQueueSize();
		vcQueue = new LinkedBlockingDeque<>(vcQueueSize);
		objectQueue = new LinkedBlockingDeque<>(objectQueueSize);
		jsonQueue = new LinkedBlockingDeque<>(250);
	}

	@Override
	public void run() {

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

		ph4.startProcess(messageHeader + "RoutedBulkIndexers");
		ArrayList<RoutedBulkIndexer> indexers = new ArrayList<>();
		for (int i = 0; i < shardCount * 2; i++) {
			RoutedBulkIndexer indexer = new RoutedBulkIndexer(jsonQueue, indexName, messageHeader + "BP(" + (i + 1) + ")", ph4);
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
			while (!jsonQueue.isEmpty()) {
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
				variantSummaryConverter = new VariantSummaryConverter(header, geneCache, severityRanking);
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

		@Override
		public void run() {
			mapper.disable(SerializationFeature.INDENT_OUTPUT);
			searchWriter = mapper.writerWithView(CurationView.VariantSearchResultDocument.class);
			cachedWriter = mapper.writerWithView(CurationView.VariantSummaryDocument.class);
			sequenceWriter = mapper.writerWithView(CurationView.SequenceSummaryDocument.class);
			while (!(Thread.currentThread().isInterrupted())) {
				try {
					List<ESDocument> docList = objectQueue.take();

					List<byte[]> workBucket = new ArrayList<>();

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
									continue;
								}

								workBucket.add(smileDoc);

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
							if (workBucket.size() > 0) {
								jsonQueue.put(workBucket);
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
