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

	private VariantSummaryConverter variantSummaryConverter;
	private SequenceSummaryConverter sequenceSummaryConverter;
	private VariantSearchResultConverter variantSearchResultConverter;

	private String messageHeader = "";

	public SourceDocumentCreation(String downloadPath, DownloadSource source, GeneDocumentCache geneCache, HashSet<String> variantsCache, Map<String, Integer> severityRanking, LinkedBlockingDeque<List<byte[]>> jsonQueue) {
		this.downloadPath = downloadPath;
		this.source = source;
		this.geneCache = geneCache;
		this.variantsCache = variantsCache;
		this.severityRanking = severityRanking;
		this.jsonQueue = jsonQueue;
		speciesType = SpeciesType.getTypeByID(source.getTaxonId());
		messageHeader = speciesType.getModName() + " ";
		int vcQueueSize = source.getVcQueueSize() != null ? source.getVcQueueSize() : VariantConfigHelper.getSourceDocumentCreatorVCQueueSize();
		int objectQueueSize = source.getObjectQueueSize() != null ? source.getObjectQueueSize() : VariantConfigHelper.getSourceDocumentCreatorObjectQueueSize();
		vcQueue = new LinkedBlockingDeque<>(vcQueueSize);
		objectQueue = new LinkedBlockingDeque<>(objectQueueSize);
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

		} catch (Exception e) {
			ExceptionCatcher.report(e);
			e.printStackTrace();
		}
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

		private final long workBucketMaxBytes = VariantConfigHelper.getSourceDocumentCreatorJsonQueueBucketSize();

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

			List<byte[]> workBucket = new ArrayList<>();
			long workBucketBytes = 0;

			while (!(Thread.currentThread().isInterrupted())) {
				try {
					List<ESDocument> docList = objectQueue.take();

					if (!docList.isEmpty()) {
						for (ESDocument doc : docList) {
							try {
								byte[] smileDoc = null;
								if (doc instanceof SequenceSummaryDocument ssd) {
									smileDoc = sequenceWriter.writeValueAsBytes(ssd);
								} else if (doc instanceof VariantSummaryDocument vsd) {
									smileDoc = cachedWriter.writeValueAsBytes(vsd);
								} else if (doc instanceof VariantSearchResultDocument vsrd) {
									smileDoc = searchWriter.writeValueAsBytes(vsrd);
								} else {
									log.error("Unexpected ESDocument type: " + doc.getClass().getName());
									continue;
								}

								workBucket.add(smileDoc);
								workBucketBytes += smileDoc.length;

								if (workBucketBytes >= workBucketMaxBytes) {
									jsonQueue.put(workBucket);
									workBucket = new ArrayList<>();
									workBucketBytes = 0;
								}

								ph3.progressProcess();

							} catch (Exception e) {
								ExceptionCatcher.report(e);
								e.printStackTrace();
							}
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			// Flush remaining docs
			try {
				if (!workBucket.isEmpty()) {
					jsonQueue.put(workBucket);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
