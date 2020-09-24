package org.alliancegenome.core.translators.tdf;

import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.commons.collections.CollectionUtils;
import org.alliancegenome.neo4j.entity.node.Allele;

public class AlleleToTdfTranslator {

    public String getAllRows(List<Allele> annotations) {

        List<AlleleDownloadRow> list = getAlleleDownloadRowsForGenes(annotations);
        List<DownloadHeader> headers = List.of(
                new DownloadHeader<>("Allele ID", (AlleleDownloadRow::getAlleleID)),
                new DownloadHeader<>("Allele Symbol", (AlleleDownloadRow::getAlleleSymbol)),
                new DownloadHeader<>("Allele Synonyms", (AlleleDownloadRow::getAlleleSynonyms)),
                new DownloadHeader<>("Category", (AlleleDownloadRow::getVariantCategory)),
                new DownloadHeader<>("Variant Symbol", (AlleleDownloadRow::getVariantSymbol)),
                new DownloadHeader<>("Variant consequence", (AlleleDownloadRow::getVariantConsequence)),
                new DownloadHeader<>("Has Phenotype", (AlleleDownloadRow::getHasPhenotype)),
                new DownloadHeader<>("Has Disease", (AlleleDownloadRow::getHasDisease)),
                new DownloadHeader<>("References", (AlleleDownloadRow::getReference))
        );

        return DownloadHeader.getDownloadOutput(list, headers);
    }

    public List<AlleleDownloadRow> getAlleleDownloadRowsForGenes(List<Allele> annotations) {

denormalizeAnnotations(annotations);
        return annotations.stream()

                .filter(annotation -> annotation.getVariants()!=null)
                .filter(annotation -> !CollectionUtils.isEmpty(annotation.getVariants()))
                .map(annotation -> annotation.getVariants().stream()
                        .map(join -> {
                            AlleleDownloadRow row = getBaseDownloadRow(annotation, join, null);
                            return row;
                        })
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }


        private void denormalizeAnnotations(List<Allele> alleles) {
            // add genetic entity info for annotations with pure genes



            alleles.stream()
                    .filter(annotation -> CollectionUtils.isEmpty(annotation.getVariants()))
                    .forEach(annotation -> {
                        Variant entity = createNewVariant();
                        annotation.addVariant(entity);
                    });


        }

        private Variant createNewVariant() {
            Variant entity = new Variant();
            entity.setConsequence("");
            entity.setSymbol("");
            entity.setName("");
            //entity.setPublications(Collections.<Publication>emptySet());

            return entity;
        }

    private AlleleDownloadRow getBaseDownloadRow(Allele annotation, Variant join,Publication pub) {
        AlleleDownloadRow row = new AlleleDownloadRow();

        row.setAlleleID(annotation.getPrimaryKey());
        row.setAlleleSymbol(annotation.getSymbol());
        String synonyms = "";
        if (CollectionUtils.isNotEmpty(annotation.getSynonyms())) {
            StringJoiner synonymJoiner = new StringJoiner(",");
            annotation.getSynonyms().forEach(synonym -> synonymJoiner.add(synonym.getName()));
            synonyms = synonymJoiner.toString();
        }
        row.setAlleleSynonyms(synonyms);
        row.setVariantCategory(annotation.getCategory());
        if (join!=null) {
            row.setVariantSymbol(join.getName());
            row.setVariantConsequence(join.getConsequence());
        }
        row.setHasPhenotype(annotation.hasPhenotype().toString());
        row.setHasDisease(annotation.hasDisease().toString());
        if (pub!=null) {
            row.setReference(pub.getPubId());
        }
        return row;
    }



    public String getAllVariantsRows(List<Variant> variants) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Variant");
        headerJoiner.add("Variant Type");
        headerJoiner.add("Chromosome:Position");
        headerJoiner.add("Nucleotide Change");
        headerJoiner.add("Most severe Consequence");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        variants.forEach(variant -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(variant.getName());
//            joiner.add(variant.getPrimaryKey());
            // add list of synonyms
            joiner.add(variant.getVariantType().getName());
            joiner.add(variant.getLocation().getChromosomeAndPosition());
            joiner.add(variant.getNucleotideChange());
            joiner.add(variant.getConsequence());
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();

    }



}
