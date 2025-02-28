package org.alliancegenome.core.translators.tdf;

import org.alliancegenome.api.entity.PhenotypeAnnotationDocument;
import org.alliancegenome.curation_api.model.entities.AGMPhenotypeAnnotation;
import org.alliancegenome.curation_api.model.entities.PhenotypeAnnotation;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PhenotypeAnnotationToTdfTranslator {

	public String getAllRows(List<? extends PhenotypeAnnotationDocument> annotations) {
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


	public List<PhenotypeDownloadRow> getDownloadRowsFromAnnotations(List<? extends PhenotypeAnnotationDocument> phenotypeAnnotations) {
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