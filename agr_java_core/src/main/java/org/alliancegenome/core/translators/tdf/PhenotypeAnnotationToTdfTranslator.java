package org.alliancegenome.core.translators.tdf;

import org.alliancegenome.api.entity.GenePhenotypeAnnotationDocument;
import org.alliancegenome.curation_api.model.entities.AGMPhenotypeAnnotation;
import org.alliancegenome.curation_api.model.entities.PhenotypeAnnotation;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PhenotypeAnnotationToTdfTranslator {

	public String getAllRows(List<GenePhenotypeAnnotationDocument> annotations) {
		//denormalizeAnnotations(annotations);

		// convert collection of PhenotypeAnnotation records to PhenotypeDownloadRow records
		List<PhenotypeDownloadRow> list = getDownloadRowsFromAnnotations(annotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Phenotype", PhenotypeDownloadRow::getPhenotype),
			new DownloadHeader<>("Genetic Entity ID", PhenotypeDownloadRow::getGeneticEntityID),
			new DownloadHeader<>("Genetic Entity Name", PhenotypeDownloadRow::getGeneticEntityName),
			new DownloadHeader<>("Genetic Entity Type", PhenotypeDownloadRow::getGeneticEntityType),
			new DownloadHeader<>("Source", PhenotypeDownloadRow::getSource),
			new DownloadHeader<>("Reference", PhenotypeDownloadRow::getReference)
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}


	public String getAllRowsForAlleles(List<org.alliancegenome.neo4j.entity.PhenotypeAnnotation> annotations) {
		//denormalizeAnnotations(annotations);

		// convert collection of PhenotypeAnnotation records to PhenotypeDownloadRow records
/*
		List<PhenotypeDownloadRow> list = annotations.stream()
			.map(annotation -> annotation.getPrimaryAnnotatedEntities().stream()
				.map(entity -> entity.getPublicationEvidenceCodes().stream()
					.map(join -> {
						PhenotypeDownloadRow row = getBaseDownloadRow(annotation, join, null);
						//set entities
						row.setMainEntityID(annotation.getAllele().getPrimaryKey());
						row.setMainEntitySymbol(annotation.getAllele().getSymbolText());
						if (!entity.getType().equals(GeneticEntity.CrossReferenceType.GENE)) {
							row.setGeneticEntityID(entity.getId());
							row.setGeneticEntityName(entity.getDisplayName());
							row.setGeneticEntityType(entity.getType());
						}

						return row;
					})
					.collect(Collectors.toList()))
				.flatMap(Collection::stream)
				.collect(Collectors.toList()))
			.flatMap(Collection::stream)
			.collect(Collectors.toList());
*/

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Phenotype", PhenotypeDownloadRow::getPhenotype),
			new DownloadHeader<>("Genetic Entity ID", PhenotypeDownloadRow::getGeneticEntityID),
			new DownloadHeader<>("Genetic Entity Name", PhenotypeDownloadRow::getGeneticEntityName),
			new DownloadHeader<>("Genetic Entity Type", PhenotypeDownloadRow::getGeneticEntityType),
			new DownloadHeader<>("Reference", PhenotypeDownloadRow::getReference),
			new DownloadHeader<>("Source", PhenotypeDownloadRow::getSource)
		);

		//return DownloadHeader.getDownloadOutput(list, headers);
		return null;
	}


	private void denormalizeAnnotations(List<PhenotypeAnnotation> phenotypeAnnotation) {
		// add genetic entity info for annotations with pure genes

/*
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
*/


	}

/*
	private PrimaryAnnotatedEntity createNewPrimaryAnnotatedEntity(PhenotypeAnnotation annotation, PublicationJoin join) {
		PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
		if (annotation.getGene() != null) {
			entity.setId(annotation.getGene().getPrimaryKey());
			entity.setName(annotation.getGene().getSymbol());
			entity.setType(GeneticEntity.CrossReferenceType.GENE.getDisplayName());
		} else {
			entity.setId(annotation.getAllele().getPrimaryKey());
			entity.setName(annotation.getAllele().getSymbolText());
			entity.setType(GeneticEntity.CrossReferenceType.ALLELE.getDisplayName());
		}

		return entity;
	}
*/


	public List<PhenotypeDownloadRow> getDownloadRowsFromAnnotations(List<GenePhenotypeAnnotationDocument> phenotypeAnnotations) {
		//denormalizeAnnotations(phenotypeAnnotations);
		return phenotypeAnnotations.stream()
			.filter(annotation -> CollectionUtils.isNotEmpty(annotation.getPrimaryAnnotations()))
			.map(annotation -> annotation.getPrimaryAnnotations().stream()
				.map(this::getPhenotypeDownloadRow).toList()).flatMap(Collection::stream).collect(Collectors.toList());
	}

	private PhenotypeDownloadRow getPhenotypeDownloadRow(PhenotypeAnnotation annotation) {
		PhenotypeDownloadRow row = getBaseDownloadRow(annotation);

		row.setPhenotype(annotation.getPhenotypeAnnotationObject());
		if (annotation instanceof AGMPhenotypeAnnotation annot) {
			row.setGeneticEntityID(annot.getPhenotypeAnnotationSubject().getPrimaryExternalId());
			row.setGeneticEntityName(annot.getPhenotypeAnnotationSubject().getName());
			row.setGeneticEntityType(annot.getPhenotypeAnnotationSubject().getSubtype().getName());
		}
		return row;
	}

	private PhenotypeDownloadRow getBaseDownloadRow(PhenotypeAnnotation annotation) {
		PhenotypeDownloadRow row = new PhenotypeDownloadRow();
		row.setPhenotype(annotation.getPhenotypeAnnotationObject());

		row.setReference(annotation.getSingleReference().getReferenceID());
		if (annotation.getDataProviderString() != null) {
			row.setSource(annotation.getDataProviderString());
		} else {
			row.setSource("");
		}
		return row;
	}
}