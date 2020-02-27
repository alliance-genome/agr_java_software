package org.alliancegenome.core.translators.tdf;

import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.PublicationJoin;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PhenotypeAnnotationToTdfTranslator {

    public String getAllRows(List<PhenotypeAnnotation> annotations) {
        denormalizeAnnotations(annotations);

        // convert collection of PhenotypeAnnotation records to PhenotypeDownloadRow records
        List<PhenotypeDownloadRow> list = getDownloadRowsFromAnnotations(annotations);

        List<DownloadHeader> headers = List.of(
                new DownloadHeader<>("Phenotype", (PhenotypeDownloadRow::getPhenotype)),
                new DownloadHeader<>("Genetic Entity ID", (PhenotypeDownloadRow::getGeneticEntityID)),
                new DownloadHeader<>("Genetic Entity Name", (PhenotypeDownloadRow::getGeneticEntityName)),
                new DownloadHeader<>("Genetic Entity Type", (PhenotypeDownloadRow::getGeneticEntityType)),
                new DownloadHeader<>("Reference", (PhenotypeDownloadRow::getReference)),
                new DownloadHeader<>("Source", (PhenotypeDownloadRow::getSource))
        );

        return DownloadHeader.getDownloadOutput(list, headers);
    }


    public String getAllRowsForAlleles(List<PhenotypeAnnotation> annotations) {
        denormalizeAnnotations(annotations);

        // convert collection of PhenotypeAnnotation records to PhenotypeDownloadRow records
        List<PhenotypeDownloadRow> list = annotations.stream()
                .map(annotation -> annotation.getPrimaryAnnotatedEntities().stream()
                        .map(entity -> entity.getPublicationEvidenceCodes().stream()
                                .map(join -> {
                                    PhenotypeDownloadRow row = getBaseDownloadRow(annotation, join, null);

                                    if (!entity.getType().equals(GeneticEntity.CrossReferenceType.GENE)) {
                                        row.setGeneticEntityID(entity.getId());
                                        row.setGeneticEntityName(entity.getDisplayName());
                                        row.setGeneticEntityType(entity.getType().getDisplayName());
                                    }

                                    return row;
                                })
                                .collect(Collectors.toList()))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<DownloadHeader> headers = List.of(
                new DownloadHeader<>("Phenotype", (PhenotypeDownloadRow::getPhenotype)),
                new DownloadHeader<>("Genetic Entity ID", (PhenotypeDownloadRow::getGeneticEntityID)),
                new DownloadHeader<>("Genetic Entity Name", (PhenotypeDownloadRow::getGeneticEntityName)),
                new DownloadHeader<>("Genetic Entity Type", (PhenotypeDownloadRow::getGeneticEntityType)),
                new DownloadHeader<>("Reference", (PhenotypeDownloadRow::getReference)),
                new DownloadHeader<>("Source", (PhenotypeDownloadRow::getSource))
        );

        return DownloadHeader.getDownloadOutput(list, headers);
    }



    private void denormalizeAnnotations(List<PhenotypeAnnotation> phenotypeAnnotation) {
        // add genetic entity info for annotations with pure genes

        
        
        phenotypeAnnotation.stream()
                .filter(annotation -> CollectionUtils.isEmpty(annotation.getPrimaryAnnotatedEntities()))
                .forEach(annotation -> {
                    PrimaryAnnotatedEntity entity = createNewPrimaryAnnotatedEntity(annotation, null);
                    annotation.addPrimaryAnnotatedEntity(entity);
                    List<PublicationJoin> joins = annotation.getPublications().stream()
                            .map(publication -> {
                                PublicationJoin join = new PublicationJoin();
                                join.setPublication(publication);
                                return join;
                            })
                            .collect(Collectors.toList());
                    entity.addPublicationEvidenceCode(joins);
                });


    }

    private PrimaryAnnotatedEntity createNewPrimaryAnnotatedEntity(PhenotypeAnnotation annotation, PublicationJoin join) {
        PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
        entity.setId(annotation.getGene().getPrimaryKey());
        entity.setName(annotation.getGene().getSymbol());
        entity.setType(GeneticEntity.CrossReferenceType.GENE);

        return entity;
    }


    public List<PhenotypeDownloadRow> getDownloadRowsFromAnnotations(List<PhenotypeAnnotation> phenotypeAnnotations) {
        denormalizeAnnotations(phenotypeAnnotations);
        return phenotypeAnnotations.stream()
                .filter(annotation -> annotation.getPrimaryAnnotatedEntities()!=null)
                .filter(annotation -> !CollectionUtils.isEmpty(annotation.getPrimaryAnnotatedEntities()))
                .map(annotation -> annotation.getPrimaryAnnotatedEntities().stream()
                        .map(entity -> entity.getPublicationEvidenceCodes().stream()
                                .map(join -> {

                                    return List.of(getPhenotypeDownloadRow(annotation, entity, join, null));
                                })
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList()))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private PhenotypeDownloadRow getPhenotypeDownloadRow(PhenotypeAnnotation annotation, PrimaryAnnotatedEntity entity, PublicationJoin join, Gene homologousGene) {
        PhenotypeDownloadRow row = getBaseDownloadRow(annotation, join, homologousGene);

        row.setPhenotype(annotation.getPhenotype());
        row.setReference(join.getPublication().getPubId());
        if (entity != null) {
            row.setGeneticEntityID(entity.getId());
            row.setGeneticEntityName(entity.getDisplayName());
            row.setGeneticEntityType(entity.getType().getDisplayName());

        }
        return row;
    }

    private PhenotypeDownloadRow getBaseDownloadRow(PhenotypeAnnotation annotation, PublicationJoin join, Gene homologousGene) {
        PhenotypeDownloadRow row = new PhenotypeDownloadRow();
        row.setPhenotype(annotation.getPhenotype());

        row.setReference(join.getPublication().getPubId());
        if (annotation.getSource()!=null) {
            row.setSource(annotation.getSource().getName());
        }
        else{
            row.setSource("");
        }
        return row;
    }

}
