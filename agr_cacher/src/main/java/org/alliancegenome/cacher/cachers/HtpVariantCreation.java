package org.alliancegenome.cacher.cachers;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.core.filedownload.model.DownloadableFile;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.core.variant.converters.AlleleVariantSequenceConverter;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.TranscriptLevelConsequence;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.*;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class HtpVariantCreation extends Thread {

    private final DownloadableFile file;
    private final SpeciesType speciesType;
    private String[] header = null;
    private String chromosome;

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<AlleleVariantSequence>> sequenceMap;


    public HtpVariantCreation(String taxonID, String chromosome, DownloadableFile file, ConcurrentHashMap<String, ConcurrentLinkedDeque<AlleleVariantSequence>> sequenceMap) {
        this.file = file;
        this.chromosome = chromosome;
        speciesType = SpeciesType.getTypeByID(taxonID);
        this.sequenceMap = sequenceMap;
    }

    public void run() {

        VCFReader reader = new VCFReader(file);
        reader.start();

        try {

            log.debug("Waiting for VCFReader to finish");
            reader.join();

            log.debug("Threads finished: ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class VCFReader extends Thread {

        private final DownloadableFile df;
        private final AlleleVariantSequenceConverter converter = new AlleleVariantSequenceConverter();
        GeneDocumentCache geneCache = new GeneDocumentCache();
        
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
            }

            try {
                while (iter1.hasNext()) {
                    VariantContext vc = iter1.next();


                    List<AlleleVariantSequence> docList = converter.convertContextToAlleleVariantSequence(vc, header, speciesType, geneCache);
                        //for (AlleleVariantSequence doc : docList) {
                            //for (TranscriptLevelConsequence transcriptFeature : doc.getTranscriptLevelConsequences()) {

//                              String geneID = transcriptFeature.getGene();
//                              // do not handle variants without gene relationship
//                              if (StringUtils.isEmpty(geneID))
//                                  continue;
    //
//                              ConcurrentLinkedDeque<AlleleVariantSequence> list = sequenceMap.get(geneID);
//                              if (list == null) {
//                                  list = new ConcurrentLinkedDeque<>();
//                                  sequenceMap.put(geneID, list);
//                              }
    //
//                              Allele allele = new Allele(transcriptFeature.getGene(), GeneticEntity.CrossReferenceType.VARIANT);
//                              // hack until the ID column is set to the right thing by the MODs
//                              if (StringUtils.isEmpty(doc.getId()) || doc.getId().equals(".")) {
//                                  allele.setSymbol(transcriptFeature.getHgvsg());
//                                  allele.setSymbolText(transcriptFeature.getHgvsg());
//                              } else {
//                                  allele.setSymbol(doc.getId());
//                                  allele.setSymbolText(doc.getId());
//                              }
//                              Gene gene = new Gene();
//                              String assocatedGeneID = transcriptFeature.getGene();
//                              if (assocatedGeneID.startsWith("ZDB-GENE"))
//                                  assocatedGeneID = "ZFIN:" + assocatedGeneID;
//                              gene.setPrimaryKey(assocatedGeneID);
//                              gene.setSymbol(transcriptFeature.getSymbol());
//                              allele.setGene(gene);
//                              Variant variant = new Variant();
//                              TranscriptLevelConsequence consequence = new TranscriptLevelConsequence();
//                              variant.setHgvsNomenclature(transcriptFeature.getHgvsc());
//                              // TODO: Needs to be set somewhere does not come through the vcf file.
//                              variant.setGenomicReferenceSequence(transcriptFeature.getReferenceSequence());
//                              variant.setGenomicVariantSequence(transcriptFeature.getAllele());
//                              variant.setStart(transcriptFeature.getGenomicStart());
//                              variant.setEnd(transcriptFeature.getGenomicEnd());
//                              variant.setConsequence((transcriptFeature.getConsequence()));
//                              variant.setHgvsNomenclature(transcriptFeature.getHgvsg());
//                              SOTerm soTerm = new SOTerm();
//                              soTerm.setName(doc.getVariantType().stream().findFirst().get());
//                              soTerm.setPrimaryKey(doc.getVariantType().stream().findFirst().get());
//                              variant.setVariantType(soTerm);
//                              consequence.setImpact(transcriptFeature.getImpact());
//                              consequence.setSequenceFeatureType(transcriptFeature.getBiotype());
//                              consequence.setTranscriptName(transcriptFeature.getFeature());
//                              consequence.setTranscriptLevelConsequence(transcriptFeature.getConsequence());
//                              consequence.setPolyphenPrediction(transcriptFeature.getPolyphenPrediction());
//                              consequence.setPolyphenScore(transcriptFeature.getPolyphenScore());
//                              consequence.setSiftPrediction(transcriptFeature.getSiftPrediction());
//                              consequence.setSiftScore(transcriptFeature.getSiftScore());
//                              consequence.setTranscriptLocation(transcriptFeature.getExon());
//                              consequence.setAssociatedGene(gene);
//                              String location = "";
//                              if (StringUtils.isNotEmpty(transcriptFeature.getExon()))
//                                  location += "Exon " + transcriptFeature.getExon();
//                              if (StringUtils.isNotEmpty(transcriptFeature.getIntron()))
//                                  location += "Intron " + transcriptFeature.getIntron();
//                              consequence.setTranscriptLocation(location);
//                              list.add(new AlleleVariantSequence(allele, variant, consequence));
                            //}
                        //}
                    
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            reader.close();
        }
    }

}
