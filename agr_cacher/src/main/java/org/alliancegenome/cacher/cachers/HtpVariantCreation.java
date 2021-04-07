package org.alliancegenome.cacher.cachers;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.core.filedownload.model.DownloadableFile;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.core.variant.converters.*;
import org.alliancegenome.es.variant.model.TranscriptFeature;
import org.alliancegenome.es.variant.model.VariantDocument;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Log4j2
public class HtpVariantCreation extends Thread {

    private final DownloadableFile file;
    private final SpeciesType speciesType;
    private String[] header = null;
    private String chromosome;

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<AlleleVariantSequence>> sequenceMap;

    private final LinkedBlockingDeque<List<VariantContext>> vcQueue = new LinkedBlockingDeque<>(VariantConfigHelper.getSourceDocumentCreatorVCQueueSize());
    private final LinkedBlockingDeque<List<AlleleVariantSequence>> objectQueue = new LinkedBlockingDeque<>(VariantConfigHelper.getSourceDocumentCreatorObjectQueueSize());

    public HtpVariantCreation(String taxonID, String chromosome, DownloadableFile file, ConcurrentHashMap<String, ConcurrentLinkedDeque<AlleleVariantSequence>> sequenceMap) {
        this.file = file;
        this.chromosome = chromosome;
        speciesType = SpeciesType.getTypeByID(taxonID);
        this.sequenceMap = sequenceMap;
    }

    public void run() {

        List<VCFReader> readers = new ArrayList<>();
        VCFReader reader = new VCFReader(file);
        reader.start();
        readers.add(reader);

        List<DocumentTransformer> transformers = new ArrayList<>();
        for (int i = 0; i < VariantConfigHelper.getTransformerThreads(); i++) {
            DocumentTransformer transformer = new DocumentTransformer();
            transformer.start();
            transformers.add(transformer);
        }

        List<JSONProducer> producers = new ArrayList<>();
        for (int i = 0; i < VariantConfigHelper.getProducerThreads(); i++) {
            JSONProducer producer = new JSONProducer();
            producer.start();
            producers.add(producer);
        }

        try {

            log.debug("Waiting for VCFReader's to finish");
            for (VCFReader r : readers) {
                r.join();
            }

            log.debug("Waiting for VC Queue to empty");
            while (!vcQueue.isEmpty()) {
                Thread.sleep(15000);
            }
            TimeUnit.MILLISECONDS.sleep(15000);
            log.debug("VC Queue Empty shutting down transformers");

            log.debug("Shutting down transformers");
            for (DocumentTransformer t : transformers) {
                t.interrupt();
                t.join();
            }
            log.debug("Transformers shutdown");
            log.debug("Waiting for Object Queue to empty");
            while (!objectQueue.isEmpty()) {
                Thread.sleep(15000);
            }
            TimeUnit.MILLISECONDS.sleep(15000);
            log.debug("Object Empty shutting down producers");

            log.debug("Shutting down producers");
            for (JSONProducer p : producers) {
                p.interrupt();
                p.join();
            }
            log.debug("JSONProducers shutdown");
            log.debug("Bulk Indexers shutdown");
            log.debug("Threads finished: ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class VCFReader extends Thread {

        private final DownloadableFile df;
        private final int workBucketSize = VariantConfigHelper.getSourceDocumentCreatorVCQueueBucketSize();

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

        private final AlleleVariantSequenceConverter converter = new AlleleVariantSequenceConverter();

        private final int workBucketSize = VariantConfigHelper.getSourceDocumentCreatorObjectQueueBucketSize();

        public void run() {
            List<AlleleVariantSequence> workBucket = new ArrayList<>();
            while (!(Thread.currentThread().isInterrupted())) {
                try {
                    List<VariantContext> ctxList = vcQueue.take();
                    for (VariantContext ctx : ctxList) {
                        workBucket.addAll(converter.convertContextToAlleleVariantSequence(ctx, header, speciesType));
                    }
                    if (workBucket.size() >= workBucketSize) {
                        objectQueue.put(workBucket);
                        workBucket = new ArrayList<>();
                    }
                } catch (Exception e) {
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
                    List<AlleleVariantSequence> docList = objectQueue.take();
                    for (AlleleVariantSequence doc : docList) {
                        for (TranscriptLevelConsequence transcriptFeature : doc.getTranscriptLevelConsequences()) {

//                          String geneID = transcriptFeature.getGene();
//                          // do not handle variants without gene relationship
//                          if (StringUtils.isEmpty(geneID))
//                              continue;
//
//                          ConcurrentLinkedDeque<AlleleVariantSequence> list = sequenceMap.get(geneID);
//                          if (list == null) {
//                              list = new ConcurrentLinkedDeque<>();
//                              sequenceMap.put(geneID, list);
//                          }
//
//                          Allele allele = new Allele(transcriptFeature.getGene(), GeneticEntity.CrossReferenceType.VARIANT);
//                          // hack until the ID column is set to the right thing by the MODs
//                          if (StringUtils.isEmpty(doc.getId()) || doc.getId().equals(".")) {
//                              allele.setSymbol(transcriptFeature.getHgvsg());
//                              allele.setSymbolText(transcriptFeature.getHgvsg());
//                          } else {
//                              allele.setSymbol(doc.getId());
//                              allele.setSymbolText(doc.getId());
//                          }
//                          Gene gene = new Gene();
//                          String assocatedGeneID = transcriptFeature.getGene();
//                          if (assocatedGeneID.startsWith("ZDB-GENE"))
//                              assocatedGeneID = "ZFIN:" + assocatedGeneID;
//                          gene.setPrimaryKey(assocatedGeneID);
//                          gene.setSymbol(transcriptFeature.getSymbol());
//                          allele.setGene(gene);
//                          Variant variant = new Variant();
//                          TranscriptLevelConsequence consequence = new TranscriptLevelConsequence();
//                          variant.setHgvsNomenclature(transcriptFeature.getHgvsc());
//                          // TODO: Needs to be set somewhere does not come through the vcf file.
//                          variant.setGenomicReferenceSequence(transcriptFeature.getReferenceSequence());
//                          variant.setGenomicVariantSequence(transcriptFeature.getAllele());
//                          variant.setStart(transcriptFeature.getGenomicStart());
//                          variant.setEnd(transcriptFeature.getGenomicEnd());
//                          variant.setConsequence((transcriptFeature.getConsequence()));
//                          variant.setHgvsNomenclature(transcriptFeature.getHgvsg());
//                          SOTerm soTerm = new SOTerm();
//                          soTerm.setName(doc.getVariantType().stream().findFirst().get());
//                          soTerm.setPrimaryKey(doc.getVariantType().stream().findFirst().get());
//                          variant.setVariantType(soTerm);
//                          consequence.setImpact(transcriptFeature.getImpact());
//                          consequence.setSequenceFeatureType(transcriptFeature.getBiotype());
//                          consequence.setTranscriptName(transcriptFeature.getFeature());
//                          consequence.setTranscriptLevelConsequence(transcriptFeature.getConsequence());
//                          consequence.setPolyphenPrediction(transcriptFeature.getPolyphenPrediction());
//                          consequence.setPolyphenScore(transcriptFeature.getPolyphenScore());
//                          consequence.setSiftPrediction(transcriptFeature.getSiftPrediction());
//                          consequence.setSiftScore(transcriptFeature.getSiftScore());
//                          consequence.setTranscriptLocation(transcriptFeature.getExon());
//                          consequence.setAssociatedGene(gene);
//                          String location = "";
//                          if (StringUtils.isNotEmpty(transcriptFeature.getExon()))
//                              location += "Exon " + transcriptFeature.getExon();
//                          if (StringUtils.isNotEmpty(transcriptFeature.getIntron()))
//                              location += "Intron " + transcriptFeature.getIntron();
//                          consequence.setTranscriptLocation(location);
//                          list.add(new AlleleVariantSequence(allele, variant, consequence));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
