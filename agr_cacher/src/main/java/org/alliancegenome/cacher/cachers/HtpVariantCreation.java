package org.alliancegenome.cacher.cachers;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.core.filedownload.model.*;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.core.variant.converters.VariantContextConverter;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.es.variant.model.*;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.*;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HtpVariantCreation extends Thread {

    private DownloadSource source;
    private SpeciesType speciesType;
    private String[] header = null;
    public static String indexName;

    private ConcurrentHashMap<String, ConcurrentLinkedDeque<AlleleVariantSequence>> sequenceMap;
    
    private LinkedBlockingDeque<List<VariantContext>> vcQueue = new LinkedBlockingDeque<>(VariantConfigHelper.getSourceDocumentCreatorVCQueueSize());
    private LinkedBlockingDeque<List<VariantDocument>> objectQueue = new LinkedBlockingDeque<>(VariantConfigHelper.getSourceDocumentCreatorObjectQueueSize());

    private ProcessDisplayHelper ph1 = new ProcessDisplayHelper(log, VariantConfigHelper.getDisplayInterval());
    private ProcessDisplayHelper ph2 = new ProcessDisplayHelper(log, VariantConfigHelper.getDisplayInterval());
    private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(log, VariantConfigHelper.getDisplayInterval());
    private ProcessDisplayHelper ph4 = new ProcessDisplayHelper(log, VariantConfigHelper.getDisplayInterval());
    private ProcessDisplayHelper ph5 = new ProcessDisplayHelper(log, VariantConfigHelper.getDisplayInterval());

    public HtpVariantCreation(DownloadSource source, ConcurrentHashMap<String, ConcurrentLinkedDeque<AlleleVariantSequence>> sequenceMap) {
        this.source = source;
        speciesType = SpeciesType.getTypeByID(source.getTaxonId());
        this.sequenceMap = sequenceMap;
    }

    public void run() {

        ph1.startProcess("VCFReader Readers: ");
        List<VCFReader> readers = new ArrayList<>();
        log.info("Number of Download files for " + source.getSpecies() + ": " + source.getFileList().size());
        for (DownloadableFile df : source.getFileList()) {
            VCFReader reader = new VCFReader(df);
            reader.start();
            readers.add(reader);
        }

        List<DocumentTransformer> transformers = new ArrayList<>();
        ph2.startProcess("VCFTransformers: " + speciesType.getName());
        for (int i = 0; i < VariantConfigHelper.getTransformerThreads(); i++) {
            DocumentTransformer transformer = new DocumentTransformer();
            transformer.start();
            transformers.add(transformer);
        }

        List<JSONProducer> producers = new ArrayList<>();
        ph5.startProcess("JSONProducers: " + speciesType.getName());
        for (int i = 0; i < VariantConfigHelper.getProducerThreads(); i++) {
            JSONProducer producer = new JSONProducer();
            producer.start();
            producers.add(producer);
        }

        try {

            log.info("Waiting for VCFReader's to finish");
            for (VCFReader r : readers) {
                r.join();
            }
            ph1.finishProcess();

            log.info("Waiting for VC Queue to empty");
            while (!vcQueue.isEmpty()) {
                Thread.sleep(15000);
            }
            TimeUnit.MILLISECONDS.sleep(15000);
            log.info("VC Queue Empty shutting down transformers");

            log.info("Shutting down transformers");
            for (DocumentTransformer t : transformers) {
                t.interrupt();
                t.join();
            }
            log.info("Transformers shutdown");
            ph2.finishProcess();


            log.info("Waiting for Object Queue to empty");
            while (!objectQueue.isEmpty()) {
                Thread.sleep(15000);
            }
            TimeUnit.MILLISECONDS.sleep(15000);
            log.info("Object Empty shutting down producers");

            log.info("Shutting down producers");
            for (JSONProducer p : producers) {
                p.interrupt();
                p.join();
            }
            log.info("JSONProducers shutdown");
            ph5.finishProcess();

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
            if (header == null) {
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

        private VariantContextConverter converter = new VariantContextConverter();
        
        private int workBucketSize = VariantConfigHelper.getSourceDocumentCreatorObjectQueueBucketSize();

        public void run() {
            List<VariantDocument> workBucket = new ArrayList<>();
            while (!(Thread.currentThread().isInterrupted())) {
                try {
                    List<VariantContext> ctxList = vcQueue.take();
                    for (VariantContext ctx : ctxList) {
                        for (VariantDocument doc : converter.convertVariantContext(ctx, speciesType, header)) {
                            workBucket.add(doc);
                            ph2.progressProcess("objectQueue: " + objectQueue.size());
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
        public void run() {

            while (!(Thread.currentThread().isInterrupted())) {
                try {
                    List<VariantDocument> docList = objectQueue.take();
                    for (VariantDocument doc : docList) {
                        for(TranscriptFeature transcriptFeature: doc.getConsequences()) {
                            
                            String geneID = transcriptFeature.getGene();
                            
                            ConcurrentLinkedDeque<AlleleVariantSequence> list = sequenceMap.get(geneID);
                            if(list == null) {
                                list = new ConcurrentLinkedDeque<AlleleVariantSequence>();
                                sequenceMap.put(geneID, list);
                            }

                            Allele allele = new Allele();
                            Gene gene = new Gene();
                            gene.setPrimaryKey(transcriptFeature.getGene());
                            gene.setSymbol(transcriptFeature.getSymbol());
                            allele.setGene(gene);
                            Variant variant = new Variant();
                            TranscriptLevelConsequence consequence = new TranscriptLevelConsequence();
                            variant.setHgvsNomenclature(transcriptFeature.getHgvsc());
                            SOTerm variantType = new SOTerm();
                            variantType.setName(transcriptFeature.getBiotype());
                            // TODO: Needs to be set somewhere does not come through the vcf file.
                            variantType.setPrimaryKey("");
                            variant.setVariantType(variantType);
                            variant.setGenomicReferenceSequence(transcriptFeature.getReferenceSequence());
                            variant.setGenomicVariantSequence(transcriptFeature.getAllele());
                            consequence.setImpact(transcriptFeature.getImpact());
                            consequence.setTranscriptLevelConsequence(transcriptFeature.getConsequence());
                            consequence.setPolyphenScore(transcriptFeature.getPolyphen());
                            consequence.setSiftScore(transcriptFeature.getSift());
                            list.add(new AlleleVariantSequence(allele, variant, consequence));
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
