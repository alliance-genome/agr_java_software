package org.alliancegenome.core.variant.converters;

import htsjdk.variant.variantcontext.VariantContext;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.es.variant.model.TranscriptFeature;
import org.alliancegenome.es.variant.model.VariantDocument;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class AlleleVariantSequenceConverter {

    public List<AlleleVariantSequence> convertVariantContext(VariantContext ctx, SpeciesType speciesType, String[] header) {
        VariantContextConverter converter = new VariantContextConverter();
        List<VariantDocument> docs = converter.convertVariantContext(ctx, speciesType, header);
        List<AlleleVariantSequence> avls = new ArrayList<>();
        docs.forEach(variantDocument -> avls.addAll(getAVSFromVariantDocument(variantDocument)));
        return avls;
    }

    public List<AlleleVariantSequence> getAVSFromVariantDocument(VariantDocument doc) {
        List<AlleleVariantSequence> list = new ArrayList<>();
        for (TranscriptFeature transcriptFeature : doc.getConsequences()) {

            String geneID = transcriptFeature.getGene();
            // do not handle variants without gene relationship
            if (StringUtils.isEmpty(geneID))
                continue;

            org.alliancegenome.neo4j.entity.node.Allele allele = new org.alliancegenome.neo4j.entity.node.Allele(transcriptFeature.getGene(), GeneticEntity.CrossReferenceType.VARIANT);
            // hack until the ID column is set to the right thing by the MODs
            if (StringUtils.isEmpty(doc.getId()) || doc.getId().equals(".")) {
                allele.setSymbol(transcriptFeature.getHgvsg());
                allele.setSymbolText(transcriptFeature.getHgvsg());
            } else {
                allele.setSymbol(doc.getId());
                allele.setSymbolText(doc.getId());
            }
            Gene gene = new Gene();
            String assocatedGeneID = transcriptFeature.getGene();
            if (assocatedGeneID.startsWith("ZDB-GENE"))
                assocatedGeneID = "ZFIN:" + assocatedGeneID;
            gene.setPrimaryKey(assocatedGeneID);
            gene.setSymbol(transcriptFeature.getSymbol());
            allele.setGene(gene);
            Variant variant = new Variant();
            TranscriptLevelConsequence consequence = new TranscriptLevelConsequence();
            variant.setHgvsNomenclature(transcriptFeature.getHgvsc());
            // TODO: Needs to be set somewhere does not come through the vcf file.
            variant.setGenomicReferenceSequence(transcriptFeature.getReferenceSequence());
            variant.setGenomicVariantSequence(transcriptFeature.getAllele());
            variant.setStart(transcriptFeature.getGenomicStart());
            variant.setEnd(transcriptFeature.getGenomicEnd());
            variant.setConsequence((transcriptFeature.getConsequence()));
            variant.setHgvsNomenclature(transcriptFeature.getHgvsg());
            SOTerm soTerm = new SOTerm();
            soTerm.setName(doc.getVariantType().stream().findFirst().get());
            soTerm.setPrimaryKey(doc.getVariantType().stream().findFirst().get());
            variant.setVariantType(soTerm);
            consequence.setImpact(transcriptFeature.getImpact());
            consequence.setSequenceFeatureType(transcriptFeature.getBiotype());
            consequence.setTranscriptName(transcriptFeature.getFeature());
            consequence.setTranscriptLevelConsequence(transcriptFeature.getConsequence());
            consequence.setPolyphenPrediction(transcriptFeature.getPolyphenPrediction());
            consequence.setPolyphenScore(transcriptFeature.getPolyphenScore());
            consequence.setSiftPrediction(transcriptFeature.getSiftPrediction());
            consequence.setSiftScore(transcriptFeature.getSiftScore());
            consequence.setTranscriptLocation(transcriptFeature.getExon());
            consequence.setAssociatedGene(gene);
            String location = "";
            if (StringUtils.isNotEmpty(transcriptFeature.getExon()))
                location += "Exon " + transcriptFeature.getExon();
            if (StringUtils.isNotEmpty(transcriptFeature.getIntron()))
                location += "Intron " + transcriptFeature.getIntron();
            consequence.setTranscriptLocation(location);
            list.add(new AlleleVariantSequence(allele, variant, consequence));
        }
        return list;
    }

    public List<AlleleVariantSequence> getAllAllelicVariants() {
        final Set<Allele> collect = (new AlleleRepository()).getAllAlleles().values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        return getAvsFromAlleles(collect);
    }

    public List<AlleleVariantSequence> getAvsFromAlleles(Collection<Allele> alleles) {
        return alleles.stream()
                .map(allele -> {
                    if (CollectionUtils.isEmpty(allele.getVariants())) {
                        return List.of(new AlleleVariantSequence(allele, null, null));
                    } else {
                        return allele.getVariants().stream()
                                .map(variant -> {
                                    if (CollectionUtils.isEmpty(variant.getTranscriptLevelConsequence())) {
                                        return List.of(new AlleleVariantSequence(allele, variant, null));
                                    } else {
                                        return variant.getTranscriptLevelConsequence().stream()
                                                .map(transcriptLevelConsequence -> new AlleleVariantSequence(allele, variant, transcriptLevelConsequence))
                                                .collect(Collectors.toList());
                                    }
                                })
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList());
                    }
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
