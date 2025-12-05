package org.alliancegenome.core.translators.tdf;

import java.util.List;
import java.util.StringJoiner;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.curation_api.model.entities.CrossReference;
import org.alliancegenome.curation_api.model.entities.GeneExpressionAnnotation;
import org.apache.commons.collections.CollectionUtils;

public class ExpressionToTdfTranslator {

	public String getAllRows(List<GeneExpressionDocument> annotations, boolean isMultipleGenes) {
		StringBuilder builder = new StringBuilder();
		builder.append(buildHeader(isMultipleGenes));
		builder.append(ConfigHelper.getJavaLineSeparator());

		for (GeneExpressionDocument doc : annotations) {
			var annotation = doc.getGeneExpressionAnnotation();
			var crossRefs = annotation.getCrossReferences();
			var refIds = doc.getReferenceId();

			int pubSize = CollectionUtils.isNotEmpty(refIds) ? refIds.size() : 0;
			int crossRefSize = CollectionUtils.isNotEmpty(crossRefs) ? crossRefs.size() : 0;

			if (pubSize > 1) {
				for (int i = 0; i < pubSize; i++) {
					builder.append(buildRow(annotation, refIds, crossRefs, i, isMultipleGenes));
					builder.append(ConfigHelper.getJavaLineSeparator());
				}
			} else {
				int rows = crossRefSize > 0 ? crossRefSize : 1;
				for (int i = 0; i < rows; i++) {
					builder.append(buildRow(annotation, refIds, crossRefs, i, isMultipleGenes));
					builder.append(ConfigHelper.getJavaLineSeparator());
				}
			}
		}
		return builder.toString();
	}

	private String buildHeader(boolean isMultipleGenes) {
		StringJoiner header = new StringJoiner("\t");
		if (isMultipleGenes) {
			header.add("Species").add("Gene Symbol").add("Gene ID");
		}
		header.add("Location").add("Stage").add("Assay").add("Source").add("Reference");
		return header.toString();
	}

	private String buildRow(
		GeneExpressionAnnotation annotation,
		List<String> refIds,
		List<CrossReference> crossRefs,
		int index,
		boolean isMultipleGenes
	) {
		StringJoiner joiner = new StringJoiner("\t");
		if (isMultipleGenes) {
			var subject = annotation.getExpressionAnnotationSubject();
			joiner.add(subject.getTaxon().getName())
					.add(subject.getGeneSymbol().getDisplayText())
					.add(subject.getPrimaryExternalId());
		}
		joiner.add(annotation.getWhereExpressedStatement())
				.add(annotation.getWhenExpressedStageName())
				.add(annotation.getExpressionAssayUsed().getName());

		String crossRef = (CollectionUtils.isNotEmpty(crossRefs) && index < crossRefs.size())
			? crossRefs.get(index).getDisplayName() : "";
		joiner.add(crossRef);

		String refId = (CollectionUtils.isNotEmpty(refIds) && index < refIds.size())
			? refIds.get(index) : (CollectionUtils.isNotEmpty(refIds) ? refIds.get(0) : "");
		joiner.add(refId);

		return joiner.toString();
	}
	}
