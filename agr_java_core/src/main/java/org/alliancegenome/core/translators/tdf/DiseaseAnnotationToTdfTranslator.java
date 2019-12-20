package org.alliancegenome.core.translators.tdf;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.PublicationJoin;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class DiseaseAnnotationToTdfTranslator {

    public String getAllRows(List<DiseaseAnnotation> diseaseAnnotations) {

        denormalizeAnnotations(diseaseAnnotations);

        List<DiseaseDownloadRow> list = diseaseAnnotations.stream()
                .map(annotation -> annotation.getPrimaryAnnotatedEntities().stream()
                        .map(entity -> entity.getPublicationEvidenceCodes().stream()
                                .map(join -> {
                                    if (CollectionUtils.isNotEmpty(annotation.getOrthologyGenes()))
                                        return annotation.getOrthologyGenes().stream()
                                                .map(gene -> getDiseaseDownloadRow(annotation, entity, join, gene))
                                                .collect(Collectors.toList());
                                    else
                                        return List.of(getDiseaseDownloadRow(annotation, entity, join, null));
                                })
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList()))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<DownloadHeader> headers = Arrays.asList(
                new DownloadHeader<>("Gene ID", (DiseaseDownloadRow::getMainEntityID)),
                new DownloadHeader<>("Gene Symbol", (DiseaseDownloadRow::getMainEntitySymbol)),
                new DownloadHeader<>("Genetic Entity ID", (DiseaseDownloadRow::getGeneticEntityID)),
                new DownloadHeader<>("Genetic Entity Name", (DiseaseDownloadRow::getGeneticEntityName)),
                new DownloadHeader<>("Genetic Entity Type", (DiseaseDownloadRow::getGeneticEntityType)),
                new DownloadHeader<>("Species ID", (DiseaseDownloadRow::getSpeciesID)),
                new DownloadHeader<>("Species Name", (DiseaseDownloadRow::getSpeciesName)),
                new DownloadHeader<>("Association", (DiseaseDownloadRow::getAssociation)),
                new DownloadHeader<>("Disease ID", (DiseaseDownloadRow::getDiseaseID)),
                new DownloadHeader<>("Disease Name", (DiseaseDownloadRow::getDiseaseName)),
                new DownloadHeader<>("Evidence Code", (DiseaseDownloadRow::getEvidenceCode)),
                new DownloadHeader<>("Evidence Code Name", (DiseaseDownloadRow::getEvidenceCodeName)),
                new DownloadHeader<>("Based On ID", (DiseaseDownloadRow::getBasedOnID)),
                new DownloadHeader<>("Based On Name", (DiseaseDownloadRow::getBasedOnName)),
                new DownloadHeader<>("Source", (DiseaseDownloadRow::getSource)),
                new DownloadHeader<>("Reference", (DiseaseDownloadRow::getReference))
        );

        return DownloadHeader.getDownloadOutput(list, headers);
    }

    private DiseaseDownloadRow getDiseaseDownloadRow(DiseaseAnnotation annotation, PrimaryAnnotatedEntity entity, PublicationJoin join, Gene homologousGene) {
        DiseaseDownloadRow row = new DiseaseDownloadRow();
        row.setMainEntityID(annotation.getGene().getPrimaryKey());
        row.setMainEntitySymbol(annotation.getGene().getSymbol());
        row.setGeneticEntityID(entity.getId());
        row.setGeneticEntityName(entity.getName());
        row.setGeneticEntityType(entity.getType().getDisplayName());
        row.setSpeciesID(annotation.getGene().getSpecies().getPrimaryKey());
        row.setSpeciesName(annotation.getGene().getSpecies().getName());
        row.setAssociation(annotation.getAssociationType());
        row.setDiseaseID(annotation.getDisease().getPrimaryKey());
        row.setDiseaseName(annotation.getDisease().getName());
        row.setSource(annotation.getSource().getName());
        if (homologousGene != null) {
            row.setBasedOnID(homologousGene.getPrimaryKey());
            row.setBasedOnName(homologousGene.getSymbol());
        }
        StringJoiner evidenceJoiner = new StringJoiner("|");
        if (CollectionUtils.isNotEmpty(join.getEcoCode())) {
            Set<String> evidenceCodes = join.getEcoCode()
                    .stream()
                    .map(ECOTerm::getPrimaryKey)
                    .collect(Collectors.toSet());

            evidenceCodes.forEach(evidenceJoiner::add);
        }
        row.setEvidenceCode(evidenceJoiner.toString());

        StringJoiner evidenceJoinerName = new StringJoiner("|");
        if (CollectionUtils.isNotEmpty(join.getEcoCode())) {
            Set<String> evidenceCodes = join.getEcoCode()
                    .stream()
                    .map(ECOTerm::getName)
                    .collect(Collectors.toSet());

            evidenceCodes.forEach(evidenceJoinerName::add);
        }
        row.setEvidenceCodeName(evidenceJoinerName.toString());
        row.setReference(join.getPublication().getPubId());

        return row;
    }

    public String getAllRowsForModel(List<DiseaseAnnotation> diseaseAnnotations) {

        List<DiseaseDownloadRow> list = diseaseAnnotations.stream()
                .map(annotation -> annotation.getPublicationJoins().stream()
                        .map(join -> {
                            DiseaseDownloadRow row = new DiseaseDownloadRow();
                            row.setMainEntityID(annotation.getModel().getPrimaryKey());
                            row.setMainEntitySymbol(annotation.getModel().getNameText());
                            row.setSpeciesID(annotation.getModel().getSpecies().getPrimaryKey());
                            row.setSpeciesName(annotation.getModel().getSpecies().getName());
                            row.setAssociation(annotation.getAssociationType());
                            row.setDiseaseID(annotation.getDisease().getPrimaryKey());
                            row.setDiseaseName(annotation.getDisease().getName());
                            row.setSource(annotation.getSource().getName());

                            StringJoiner evidenceJoiner = new StringJoiner("|");
                            if (CollectionUtils.isNotEmpty(join.getEcoCode())) {
                                Set<String> evidenceCodes = join.getEcoCode()
                                        .stream()
                                        .map(ECOTerm::getPrimaryKey)
                                        .collect(Collectors.toSet());

                                evidenceCodes.forEach(evidenceJoiner::add);
                            }
                            row.setEvidenceCode(evidenceJoiner.toString());

                            StringJoiner evidenceJoinerName = new StringJoiner("|");
                            if (CollectionUtils.isNotEmpty(join.getEcoCode())) {
                                Set<String> evidenceCodes = join.getEcoCode()
                                        .stream()
                                        .map(ECOTerm::getName)
                                        .collect(Collectors.toSet());

                                evidenceCodes.forEach(evidenceJoinerName::add);
                            }
                            row.setEvidenceCodeName(evidenceJoinerName.toString());
                            row.setReference(join.getPublication().getPubId());

                            return row;
                        })
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<DownloadHeader> headers = Arrays.asList(
                new DownloadHeader<>("Model ID", (DiseaseDownloadRow::getMainEntityID)),
                new DownloadHeader<>("Model Symbol", (DiseaseDownloadRow::getMainEntitySymbol)),
                new DownloadHeader<>("Species ID", (DiseaseDownloadRow::getSpeciesID)),
                new DownloadHeader<>("Species Name", (DiseaseDownloadRow::getSpeciesName)),
                new DownloadHeader<>("Disease ID", (DiseaseDownloadRow::getDiseaseID)),
                new DownloadHeader<>("Disease Name", (DiseaseDownloadRow::getDiseaseName)),
                new DownloadHeader<>("Evidence Code", (DiseaseDownloadRow::getEvidenceCode)),
                new DownloadHeader<>("Evidence Code Name", (DiseaseDownloadRow::getEvidenceCodeName)),
                new DownloadHeader<>("Source", (DiseaseDownloadRow::getSource)),
                new DownloadHeader<>("Reference", (DiseaseDownloadRow::getReference))
        );

        return DownloadHeader.getDownloadOutput(list, headers);
    }

    public String getAllRowsForAllele(List<DiseaseAnnotation> diseaseAnnotations) {
        denormalizeAnnotations(diseaseAnnotations);

        List<DiseaseDownloadRow> list = diseaseAnnotations.stream()
                .map(annotation -> annotation.getPrimaryAnnotatedEntities().stream()
                        .map(entity -> entity.getPublicationEvidenceCodes().stream()
                                .map(join -> {
                                    DiseaseDownloadRow row = new DiseaseDownloadRow();
                                    row.setMainEntityID(annotation.getFeature().getPrimaryKey());
                                    row.setMainEntitySymbol(annotation.getFeature().getSymbolText());
                                    row.setGeneticEntityID(entity.getId());
                                    row.setGeneticEntityName(entity.getName());
                                    row.setGeneticEntityType(entity.getType().getDisplayName());
                                    row.setSpeciesID(annotation.getGene().getSpecies().getPrimaryKey());
                                    row.setSpeciesName(annotation.getGene().getSpecies().getName());
                                    row.setAssociation(annotation.getAssociationType());
                                    row.setDiseaseID(annotation.getDisease().getPrimaryKey());
                                    row.setDiseaseName(annotation.getDisease().getName());
                                    row.setSource(annotation.getSource().getName());

                                    StringJoiner evidenceJoiner = new StringJoiner("|");
                                    if (CollectionUtils.isNotEmpty(join.getEcoCode())) {
                                        Set<String> evidenceCodes = join.getEcoCode()
                                                .stream()
                                                .map(ECOTerm::getPrimaryKey)
                                                .collect(Collectors.toSet());

                                        evidenceCodes.forEach(evidenceJoiner::add);
                                    }
                                    row.setEvidenceCode(evidenceJoiner.toString());

                                    StringJoiner evidenceJoinerName = new StringJoiner("|");
                                    if (CollectionUtils.isNotEmpty(join.getEcoCode())) {
                                        Set<String> evidenceCodes = join.getEcoCode()
                                                .stream()
                                                .map(ECOTerm::getName)
                                                .collect(Collectors.toSet());

                                        evidenceCodes.forEach(evidenceJoinerName::add);
                                    }
                                    row.setEvidenceCodeName(evidenceJoinerName.toString());
                                    row.setReference(join.getPublication().getPubId());

                                    return row;
                                })
                                .collect(Collectors.toList()))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<DownloadHeader> headers = Arrays.asList(
                new DownloadHeader<>("Allele ID", (DiseaseDownloadRow::getMainEntityID)),
                new DownloadHeader<>("Allele Symbol", (DiseaseDownloadRow::getMainEntitySymbol)),
                new DownloadHeader<>("Genetic Entity ID", (DiseaseDownloadRow::getGeneticEntityID)),
                new DownloadHeader<>("Genetic Entity Name", (DiseaseDownloadRow::getGeneticEntityName)),
                new DownloadHeader<>("Genetic Entity Type", (DiseaseDownloadRow::getGeneticEntityType)),
                new DownloadHeader<>("Species ID", (DiseaseDownloadRow::getSpeciesID)),
                new DownloadHeader<>("Species Name", (DiseaseDownloadRow::getSpeciesName)),
                new DownloadHeader<>("Association", (DiseaseDownloadRow::getAssociation)),
                new DownloadHeader<>("Disease ID", (DiseaseDownloadRow::getDiseaseID)),
                new DownloadHeader<>("Disease Name", (DiseaseDownloadRow::getDiseaseName)),
                new DownloadHeader<>("Evidence Code", (DiseaseDownloadRow::getEvidenceCode)),
                new DownloadHeader<>("Evidence Code Name", (DiseaseDownloadRow::getEvidenceCodeName)),
                new DownloadHeader<>("Source", (DiseaseDownloadRow::getSource)),
                new DownloadHeader<>("Reference", (DiseaseDownloadRow::getReference))
        );

        return DownloadHeader.getDownloadOutput(list, headers);
    }

    private void denormalizeAnnotations(List<DiseaseAnnotation> diseaseAnnotations) {
        // add genetic entity info for annotations with pure genes
        diseaseAnnotations.stream()
                .filter(annotation -> CollectionUtils.isEmpty(annotation.getPrimaryAnnotatedEntities()))
                .forEach(annotation -> {
                    PrimaryAnnotatedEntity entity = createNewPrimaryAnnotatedEntity(annotation, null);
                    annotation.addPrimaryAnnotatedEntity(entity);
                });

        // add genetic entity info for annotations that are not accounted in PAE
        diseaseAnnotations.forEach(annotation -> {
            annotation.getPublicationJoins().stream()
                    // filter out the ones that are not found in an individual PAE
                    .filter(join -> annotation.getPrimaryAnnotatedEntities().stream()
                            .noneMatch(entity -> entity.getPublicationEvidenceCodes().contains(join)))
                    .forEach(join -> {
                        PrimaryAnnotatedEntity entity = createNewPrimaryAnnotatedEntity(annotation, join);
                        annotation.addPrimaryAnnotatedEntityDuplicate(entity);
                    });
        });
    }

    private PrimaryAnnotatedEntity createNewPrimaryAnnotatedEntity(DiseaseAnnotation annotation, PublicationJoin join) {
        PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
        entity.setId(annotation.getGene().getPrimaryKey());
        entity.setName(annotation.getGene().getSymbol());
        entity.setType(GeneticEntity.CrossReferenceType.GENE);
        if (join == null)
            entity.setPublicationEvidenceCodes(annotation.getPublicationJoins());
        else {
            entity.addPublicationEvidenceCode(join);
        }
        return entity;
    }

    public String getAllRowsForRibbon(List<DiseaseAnnotation> diseaseAnnotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Species");
        headerJoiner.add("Gene ID");
        headerJoiner.add("Gene Symbol");
        headerJoiner.add("Disease ID");
        headerJoiner.add("Disease Name");
        headerJoiner.add("Genetic entity type");
        headerJoiner.add("Genetic entity Symbol");
        headerJoiner.add("Genetic entity ID");
        headerJoiner.add("Association Type");
        headerJoiner.add("Evidence Codes");
        headerJoiner.add("Source");
        headerJoiner.add("Based-On Genes");
        headerJoiner.add("References");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        diseaseAnnotations.forEach(diseaseAnnotation -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(diseaseAnnotation.getGene().getSpecies().getSpecies());
            joiner.add(diseaseAnnotation.getGene().getPrimaryKey());
            joiner.add(diseaseAnnotation.getGene().getSymbol());
            joiner.add(diseaseAnnotation.getDisease().getPrimaryKey());
            joiner.add(diseaseAnnotation.getDisease().getName());
            joiner.add(diseaseAnnotation.getGeneticEntityType());
            if (diseaseAnnotation.getFeature() != null) {
                joiner.add(diseaseAnnotation.getFeature().getSymbolText());
                joiner.add(diseaseAnnotation.getFeature().getPrimaryKey());

            } else {
                joiner.add("");
                joiner.add("");
            }


            joiner.add(diseaseAnnotation.getAssociationType());

            // evidence code list
            StringJoiner evidenceJoiner = new StringJoiner(",");
            Set<String> evidenceCodes = diseaseAnnotation.getEcoCodes()
                    .stream()
                    .map(ECOTerm::getPrimaryKey)
                    .collect(Collectors.toSet());

            evidenceCodes.forEach(evidenceJoiner::add);
            joiner.add(evidenceJoiner.toString());
            // source list
            joiner.add(diseaseAnnotation.getSource().getName());

            // basedOn info

            List<Gene> orthologyGenes = diseaseAnnotation.getOrthologyGenes();
            if (orthologyGenes != null) {
                StringJoiner basedOnJoiner = new StringJoiner(",");
                orthologyGenes.forEach(gene -> basedOnJoiner.add(gene.getPrimaryKey() + ":" + gene.getSymbol()));
                joiner.add(basedOnJoiner.toString());
            } else
                joiner.add("");

            // publications list
            StringJoiner pubJoiner = new StringJoiner(",");
            diseaseAnnotation.getPublications().forEach(publication -> pubJoiner.add(publication.getPubId()));
            joiner.add(pubJoiner.toString());
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());

        });

        return builder.toString();

    }

    public String getEmpiricalDiseaseByGene(List<DiseaseAnnotation> diseaseAnnotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Disease");
        headerJoiner.add("Genetic Entity ID");
        headerJoiner.add("Genetic Entity Symbol");
        headerJoiner.add("Genetic Entity Type");
        headerJoiner.add("Association Type");
        headerJoiner.add("Evidence Code");
        headerJoiner.add("Source");
        headerJoiner.add("References");
        builder.append(headerJoiner.toString());
        builder.append(System.getProperty("line.separator"));

        diseaseAnnotations.forEach(diseaseAnnotation -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(diseaseAnnotation.getDisease().getName());
            if (diseaseAnnotation.getFeature() != null) {
                joiner.add(diseaseAnnotation.getFeature().getPrimaryKey());
                joiner.add(diseaseAnnotation.getFeature().getSymbol());
                joiner.add("allele");
            } else {
                joiner.add("");
                joiner.add("");
                joiner.add("gene");
            }
            joiner.add(diseaseAnnotation.getAssociationType());

            // evidence code list
            StringJoiner evidenceJoiner = new StringJoiner(",");
            if (diseaseAnnotation.getEcoCodes() != null) {
                Set<String> evidenceCodes = diseaseAnnotation.getEcoCodes()
                        .stream()
                        .map(ECOTerm::getPrimaryKey)
                        .collect(Collectors.toSet());

                evidenceCodes.forEach(evidenceJoiner::add);
                joiner.add(evidenceJoiner.toString());
            } else {
                joiner.add("");
            }
            //joiner.add(diseaseAnnotation.getSource().getName());

            // publications list
            StringJoiner pubJoiner = new StringJoiner(",");
            diseaseAnnotation.getPublications().forEach(publication -> pubJoiner.add(publication.getPubId()));
            joiner.add(pubJoiner.toString());
            builder.append(joiner.toString());
            builder.append(System.getProperty("line.separator"));
        });

        return builder.toString();
    }

    public String getDiseaseViaOrthologyByGene(List<DiseaseAnnotation> diseaseAnnotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Disease");
        headerJoiner.add("Association");
        headerJoiner.add("Ortholog Gene ID");
        headerJoiner.add("Ortholog Gene Symbol");
        headerJoiner.add("Ortholog Species");
        headerJoiner.add("Evidence Code");
        headerJoiner.add("Source");
        headerJoiner.add("References");
        builder.append(headerJoiner.toString());
        builder.append(System.getProperty("line.separator"));

        diseaseAnnotations.forEach(diseaseAnnotation -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(diseaseAnnotation.getDisease().getName());
            joiner.add(diseaseAnnotation.getAssociationType());
            joiner.add(diseaseAnnotation.getOrthologyGene().getPrimaryKey());
            joiner.add(diseaseAnnotation.getOrthologyGene().getSymbol());
            joiner.add(diseaseAnnotation.getOrthologyGene().getSpecies().getName());

            // evidence code list
            StringJoiner evidenceJoiner = new StringJoiner(",");
            Set<String> evidenceCodes = diseaseAnnotation.getEcoCodes()
                    .stream()
                    .map(ECOTerm::getPrimaryKey)
                    .collect(Collectors.toSet());

            evidenceCodes.forEach(evidenceJoiner::add);
            joiner.add(evidenceJoiner.toString());
            joiner.add("Alliance");
            //joiner.add(diseaseAnnotation.getSource().getName());

            // publications list
            StringJoiner pubJoiner = new StringJoiner(",");
            diseaseAnnotation.getPublications().forEach(publication -> pubJoiner.add(publication.getPubId()));
            joiner.add(pubJoiner.toString());
            builder.append(joiner.toString());
            builder.append(System.getProperty("line.separator"));
        });

        return builder.toString();
    }
}
