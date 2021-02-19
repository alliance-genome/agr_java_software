package org.alliancegenome.indexer.variant.es.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.core.filedownload.model.DownloadSource;
import org.alliancegenome.core.filedownload.model.DownloadableFile;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.core.variant.converters.VariantContextConverterNew;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.neo4j.entity.SpeciesType;
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
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Log4j2
public class SourceDocumentCreationNew extends Thread {

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

    private RestHighLevelClient client = EsClientFactory.getDefaultEsClient();
    private boolean indexing = VariantConfigHelper.isIndexing();

    private LinkedBlockingDeque<List<VariantContext>> vcQueue = new LinkedBlockingDeque<List<VariantContext>>(VariantConfigHelper.getSourceDocumentCreatorVCQueueSize());
    private LinkedBlockingDeque<List<AlleleVariantSequence>> objectQueue = new LinkedBlockingDeque<List<AlleleVariantSequence>>(VariantConfigHelper.getSourceDocumentCreatorObjectQueueSize());

    private LinkedBlockingDeque<List<String>> jsonQueue1;
    private LinkedBlockingDeque<List<String>> jsonQueue2;
    private LinkedBlockingDeque<List<String>> jsonQueue3;
    private LinkedBlockingDeque<List<String>> jsonQueue4;

    private ProcessDisplayHelper ph1 = new ProcessDisplayHelper(log, VariantConfigHelper.getDisplayInterval());
    private ProcessDisplayHelper ph2 = new ProcessDisplayHelper(log, VariantConfigHelper.getDisplayInterval());
    private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(log, VariantConfigHelper.getDisplayInterval());
    private ProcessDisplayHelper ph4 = new ProcessDisplayHelper(log, VariantConfigHelper.getDisplayInterval());
    private ProcessDisplayHelper ph5 = new ProcessDisplayHelper(log, VariantConfigHelper.getDisplayInterval());

    public SourceDocumentCreationNew(DownloadSource source) {
        this.source = source;
        speciesType = SpeciesType.getTypeByID(source.getTaxonId());
    }

    public void run() {

        int[][] settings = VariantConfigHelper.getBulkProcessorSettingsArray();

        jsonQueue1 = new LinkedBlockingDeque<List<String>>(settings[0][3]); // Max 10K * 10K = 100M
        jsonQueue2 = new LinkedBlockingDeque<List<String>>(settings[1][3]); // Max 75K * 1333 = 100M
        jsonQueue3 = new LinkedBlockingDeque<List<String>>(settings[2][3]); // Max 100K * 1000 = 100M
        jsonQueue4 = new LinkedBlockingDeque<List<String>>(settings[3][3]); // Max 200K * 500 = 100M if documents are larger then we might need to split this down more

        log.info("Creating Bulk Processor 0 - 10K");
        builder1 = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() { 
            @Override
            public void beforeBulk(long executionId, BulkRequest request) { }
            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) { }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Bulk Request Failure: " + failure.getMessage());
                List<String> list = new ArrayList<String>();
                for(DocWriteRequest<?> req: request.requests()) {
                    IndexRequest idxreq = (IndexRequest)req;
                    list.add(idxreq.source().toString());
                }
                try {
                    jsonQueue1.put(list);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.error("Finished Adding requests to Queue:");
            }
        });

        log.info("Creating Bulk Processor 10K - 75K");
        builder2 = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() { 
            @Override
            public void beforeBulk(long executionId, BulkRequest request) { }
            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) { }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Bulk Request Failure: " + failure.getMessage());
                for(DocWriteRequest<?> req: request.requests()) {
                    IndexRequest idxreq = (IndexRequest)req;
                    bulkProcessor2.add(idxreq);
                }
                log.error("Finished Adding requests to Queue:");
            }
        });

        log.info("Creating Bulk Processor 75K - 100K");
        builder3 = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() { 
            @Override
            public void beforeBulk(long executionId, BulkRequest request) { }
            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) { }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Bulk Request Failure: " + failure.getMessage());
                for(DocWriteRequest<?> req: request.requests()) {
                    IndexRequest idxreq = (IndexRequest)req;
                    bulkProcessor3.add(idxreq);
                }
                log.error("Finished Adding requests to Queue:");
            }
        });

        log.info("Creating Bulk Processor 100K - 200K");
        builder4 = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() { 
            @Override
            public void beforeBulk(long executionId, BulkRequest request) { }
            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) { }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Bulk Request Failure: " + failure.getMessage());
                for(DocWriteRequest<?> req: request.requests()) {
                    IndexRequest idxreq = (IndexRequest)req;
                    bulkProcessor4.add(idxreq);
                }
                log.error("Finished Adding requests to Queue:");
            }
        });

        builder1.setBulkActions(settings[0][0]);
        builder1.setConcurrentRequests(settings[0][1]);
        builder1.setBulkSize(new ByteSizeValue(settings[0][2], ByteSizeUnit.MB));
        builder1.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
        bulkProcessor1 = builder1.build();

        builder2.setBulkActions(settings[1][0]);
        builder2.setConcurrentRequests(settings[1][1]);
        builder2.setBulkSize(new ByteSizeValue(settings[1][2], ByteSizeUnit.MB));
        builder2.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
        bulkProcessor2 = builder2.build();

        builder3.setBulkActions(settings[2][0]);
        builder3.setConcurrentRequests(settings[2][1]);
        builder3.setBulkSize(new ByteSizeValue(settings[2][2], ByteSizeUnit.MB));
        builder3.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
        bulkProcessor3 = builder3.build();

        builder4.setBulkActions(settings[3][0]);
        builder4.setConcurrentRequests(settings[3][1]);
        builder4.setBulkSize(new ByteSizeValue(settings[3][2], ByteSizeUnit.MB));
        builder4.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
        bulkProcessor4 = builder4.build();

        ph1.startProcess("VCFReader Readers: ");
        List<VCFReader> readers = new ArrayList<VCFReader>();
        for(DownloadableFile df: source.getFileList()) {
            VCFReader reader = new VCFReader(df);
            reader.start();
            readers.add(reader);
        }

        List<DocumentTransformer> transformers = new ArrayList<>();
        ph2.startProcess("VCFTransformers: " + speciesType.getName());
        for(int i = 0; i < VariantConfigHelper.getTransformerThreads(); i++) {
            DocumentTransformer transformer = new DocumentTransformer();
            transformer.start();
            transformers.add(transformer);
        }

        List<JSONProducer> producers = new ArrayList<>();
        ph5.startProcess("JSONProducers: " + speciesType.getName());
        for(int i = 0; i < VariantConfigHelper.getProducerThreads(); i++) {
            JSONProducer producer = new JSONProducer();
            producer.start();
            producers.add(producer);
        }

        ArrayList<VCFJsonBulkIndexer> indexers = new ArrayList<>();

        ph3.startProcess("VCFJsonIndexer BulkProcessor: " + indexName);
        ph4.startProcess("VCFJsonIndexer Buckets: " + indexName);
        for(int i = 0; i < VariantConfigHelper.getIndexerBulkProcessorThreads(); i++) {
            VCFJsonBulkIndexer indexer1 = new VCFJsonBulkIndexer(jsonQueue1, bulkProcessor1); indexer1.start(); indexers.add(indexer1);
            VCFJsonBulkIndexer indexer2 = new VCFJsonBulkIndexer(jsonQueue2, bulkProcessor2); indexer2.start(); indexers.add(indexer2);
            VCFJsonBulkIndexer indexer3 = new VCFJsonBulkIndexer(jsonQueue3, bulkProcessor3); indexer3.start(); indexers.add(indexer3);
            VCFJsonBulkIndexer indexer4 = new VCFJsonBulkIndexer(jsonQueue4, bulkProcessor4); indexer4.start(); indexers.add(indexer4);
        }

        try {

            log.info("Waiting for jsonQueue to empty");
            while(!(jsonQueue1.isEmpty() && jsonQueue2.isEmpty() && jsonQueue3.isEmpty() && jsonQueue4.isEmpty())) {
                Thread.sleep(1000);
            }

            log.info("Waiting for VCFReader's to finish");
            for(VCFReader r: readers) {
                r.join();
            }
            ph1.finishProcess();

            log.info("Waiting for VC Queue to empty");
            while(!vcQueue.isEmpty()) {
                Thread.sleep(15000);
            }
            TimeUnit.MILLISECONDS.sleep(15000);
            log.info("VC Queue Empty shuting down transformers");

            log.info("Shutting down transformers");
            for(DocumentTransformer t: transformers) {
                t.interrupt();
                t.join();
            }
            log.info("Transformers shutdown");
            ph2.finishProcess();


            log.info("Waiting for Object Queue to empty");
            while(!objectQueue.isEmpty()) {
                Thread.sleep(15000);
            }
            TimeUnit.MILLISECONDS.sleep(15000);
            log.info("Object Empty shuting down producers");

            log.info("Shutting down producers");
            for(JSONProducer p: producers) {
                p.interrupt();
                p.join();
            }
            log.info("JSONProducers shutdown");
            ph5.finishProcess();


            log.info("Waiting for jsonQueue to empty");
            while(!jsonQueue1.isEmpty() || !jsonQueue2.isEmpty() || !jsonQueue3.isEmpty() || !jsonQueue4.isEmpty()) {
                Thread.sleep(1000);
            }

            log.info("Waiting for bulk processors to finish");
            bulkProcessor1.flush();
            bulkProcessor2.flush();
            bulkProcessor3.flush();
            bulkProcessor4.flush();
            
            bulkProcessor1.awaitClose(10, TimeUnit.DAYS);
            bulkProcessor2.awaitClose(10, TimeUnit.DAYS);
            bulkProcessor3.awaitClose(10, TimeUnit.DAYS);
            bulkProcessor4.awaitClose(10, TimeUnit.DAYS);
            log.info("Bulk Processors finished");

            log.info("JSon Queue Empty shuting down bulk indexers");
            for(VCFJsonBulkIndexer indexer: indexers) {
                indexer.interrupt();
                indexer.join();
            }
            log.info("Bulk Indexers shutdown");
            ph3.finishProcess();
            ph4.finishProcess();

            log.info("Threads finished: ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
            if(header == null) {
                log.info("Setting VCF File Header: " + df.getLocalGzipFilePath());
                VCFInfoHeaderLine fileHeader = reader.getFileHeader().getInfoHeaderLine("CSQ");
                header = fileHeader.getDescription().split("Format: ")[1].split("\\|");
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                List<VariantContext> workBucket = new ArrayList<>();
                while(iter1.hasNext()) {
                    VariantContext vc = iter1.next();
                    workBucket.add(vc);

                    if(workBucket.size() >= workBucketSize) {
                        vcQueue.put(workBucket);
                        workBucket = new ArrayList<>();
                    }
                    ph1.progressProcess("vcQueue: " + vcQueue.size());
                }
                if(workBucket.size() > 0) {
                    vcQueue.put(workBucket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            reader.close();
        }
    }


    private class DocumentTransformer extends Thread {

        private VariantContextConverterNew converter = new VariantContextConverterNew();
        private int workBucketSize = VariantConfigHelper.getSourceDocumentCreatorObjectQueueBucketSize();

        public void run() {
            List<AlleleVariantSequence> workBucket = new ArrayList<>();
            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    List<VariantContext> ctxList = vcQueue.take();
                    for(VariantContext ctx: ctxList) {
                        //    for(VariantDocument doc: converter.convertVariantContext(ctx, speciesType, header)) {
                        try {
                            for(AlleleVariantSequence doc: converter.convertVariantContext(ctx, speciesType, header)) {
                                workBucket.add(doc);
                                ph2.progressProcess("objectQueue: " + objectQueue.size());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if(workBucket.size() >= workBucketSize) {
                        objectQueue.put(workBucket);
                        workBucket = new ArrayList<>();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            try {
                if(workBucket.size() > 0) {
                    objectQueue.put(workBucket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class JSONProducer extends Thread {

        private ObjectMapper mapper = new ObjectMapper();


        public void run() {

            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    List<AlleleVariantSequence> docList = objectQueue.take();

                    List<String> docs1 = new ArrayList<String>();
                    List<String> docs2 = new ArrayList<String>();
                    List<String> docs3 = new ArrayList<String>();
                    List<String> docs4 = new ArrayList<String>();

                    for(AlleleVariantSequence doc: docList) {
                        try {
                            String jsonDoc = mapper.writeValueAsString(doc);
                            if(jsonDoc.length() < 10000) {
                                docs1.add(jsonDoc);
                            } else if(jsonDoc.length() < 75000) {
                                docs2.add(jsonDoc);
                            } else if(jsonDoc.length() < 100000) {
                                docs3.add(jsonDoc);
                            } else {
                                docs4.add(jsonDoc);
                            }
                            ph5.progressProcess("jsonQueue1: " + jsonQueue1.size() + " jsonQueue2: " + jsonQueue2.size() + " jsonQueue3: " + jsonQueue3.size() +  " jsonQueue4: " + jsonQueue4.size());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        if(docs1.size() > 0) jsonQueue1.put(docs1);
                        if(docs2.size() > 0) jsonQueue2.put(docs2);
                        if(docs3.size() > 0) jsonQueue3.put(docs3);
                        if(docs4.size() > 0) jsonQueue4.put(docs4);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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
            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    List<String> docs = jsonQueue.take();
                    for(String doc: docs) {
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
