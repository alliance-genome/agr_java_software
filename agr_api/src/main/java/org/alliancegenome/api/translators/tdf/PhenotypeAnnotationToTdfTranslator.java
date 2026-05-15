package org.alliancegenome.api.translators.tdf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.core.document.PhenotypeAnnotationDocument;
import org.alliancegenome.api.translators.tdf.DownloadHeader;
import org.alliancegenome.api.translators.tdf.PhenotypeDownloadRow;
import org.alliancegenome.curation_api.model.entities.AGMPhenotypeAnnotation;
import org.alliancegenome.curation_api.model.entities.AllelePhenotypeAnnotation;
import org.alliancegenome.curation_api.model.entities.ConditionRelation;
import org.alliancegenome.curation_api.model.entities.GenePhenotypeAnnotation;
import org.alliancegenome.curation_api.model.entities.PhenotypeAnnotation;
import org.apache.commons.collections.CollectionUtils;

public class PhenotypeAnnotationToTdfTranslator extends BaseToTdfTranslator {

	public String getAllRows(List<? extends PhenotypeAnnotationDocument> annotations) {
		// convert collection of PhenotypeAnnotation records to PhenotypeDownloadRow records
		List<PhenotypeDownloadRow> list = getDownloadRowsFromAnnotations(annotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Phenotype", PhenotypeDownloadRow::getPhenotype),
			new DownloadHeader<>("Genetic Entity ID", PhenotypeDownloadRow::getGeneticEntityID),
			new DownloadHeader<>("Genetic Entity Name", PhenotypeDownloadRow::getGeneticEntityName),
			new DownloadHeader<>("Genetic Entity Type", PhenotypeDownloadRow::getGeneticEntityType),
			new DownloadHeader<>("Experimental Condition", PhenotypeDownloadRow::getExperimentalCondition),
			new DownloadHeader<>("Source", PhenotypeDownloadRow::getSource),
			new DownloadHeader<>("Reference", PhenotypeDownloadRow::getReference)
		);
		return DownloadHeader.getDownloadOutput(list, headers);
	}


	public List<PhenotypeDownloadRow> getDownloadRowsFromAnnotations(List<? extends PhenotypeAnnotationDocument> phenotypeAnnotations) {
		return phenotypeAnnotations.stream()
			.filter(annotation -> CollectionUtils.isNotEmpty(annotation.getPrimaryAnnotations()))
			.map(annotation -> annotation.getPrimaryAnnotations().stream()
				.map(pa -> getPhenotypeDownloadRow(pa, annotation)).toList()).flatMap(Collection::stream).collect(Collectors.toList());
	}

	private PhenotypeDownloadRow getPhenotypeDownloadRow(PhenotypeAnnotation annotation, PhenotypeAnnotationDocument document) {
		PhenotypeDownloadRow row = getBaseDownloadRow(annotation);

		row.setPhenotype(annotation.getPhenotypeAnnotationObject());
		if (annotation instanceof AGMPhenotypeAnnotation annot) {
			row.setGeneticEntityID(annot.getPhenotypeAnnotationSubject().getPrimaryExternalId());
			row.setGeneticEntityName(annot.getPhenotypeAnnotationSubject().getAgmFullName().getDisplayText());
			row.setGeneticEntityType(annot.getPhenotypeAnnotationSubject().getSubtype().getName());
		}
		if (annotation instanceof AllelePhenotypeAnnotation annot) {
			row.setGeneticEntityID(annot.getPhenotypeAnnotationSubject().getPrimaryExternalId());
			row.setGeneticEntityName(annot.getPhenotypeAnnotationSubject().getAlleleSymbol().getDisplayText());
			row.setGeneticEntityType("allele");
		}
		if (annotation instanceof GenePhenotypeAnnotation annot) {
			row.setGeneticEntityID(annot.getPhenotypeAnnotationSubject().getPrimaryExternalId());
			row.setGeneticEntityName(annot.getPhenotypeAnnotationSubject().getGeneSymbol().getDisplayText());
			row.setGeneticEntityType("gene");
		}

		// Experimental conditions from the primary annotation
		if (CollectionUtils.isNotEmpty(annotation.getConditionRelations())) {
			List<ConditionRelation> conditions = annotation.getConditionRelations().stream()
				.filter(cr -> cr.getConditionRelationType() != null)
				.filter(cr -> cr.getConditionRelationType().getName().contains("has_condition")
					|| cr.getConditionRelationType().getName().contains("induced")
					|| cr.getConditionRelationType().getName().contains("ameliorated")
					|| cr.getConditionRelationType().getName().contains("exacerbated"))
				.toList();
			List<String> components = new ArrayList<>();
			conditions.forEach(cr -> {
				List<String> parts = new ArrayList<>();
				parts.add(cr.getConditionRelationType().getName());
				cr.getConditions().forEach(ec -> parts.add(ec.getConditionSummary()));
				components.add(String.join(": ", parts));
			});
			if (!components.isEmpty()) {
				row.setExperimentalCondition(String.join(" | ", components));
			}
		}

		// Fallback to document-level aggregated conditions
		if (row.getExperimentalCondition() == null && document.getExperimentalConditionsAggregated() != null) {
			row.setExperimentalCondition(document.getExperimentalConditionsAggregated());
		}

		return row;
	}

	private PhenotypeDownloadRow getBaseDownloadRow(PhenotypeAnnotation annotation) {
		PhenotypeDownloadRow row = new PhenotypeDownloadRow();
		row.setPhenotype(annotation.getPhenotypeAnnotationObject());
		row.setReference(getReferenceString(annotation.getEvidenceItem()));
		if (annotation.getDataProviderString() != null) {
			row.setSource(annotation.getDataProviderString());
		} else {
			row.setSource("");
		}
		return row;
	}
}