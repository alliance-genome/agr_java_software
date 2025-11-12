package org.alliancegenome.core.translators.tdf;

import java.util.List;
import java.util.StringJoiner;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;

public class ExpressionToTdfTranslator {

	public String getAllRows(List<GeneExpressionDocument> annotations, boolean isMultipleGenes) {
		StringBuilder builder = new StringBuilder();
		StringJoiner headerJoiner = new StringJoiner("\t");
		if (isMultipleGenes) {
			headerJoiner.add("Species");
			headerJoiner.add("Gene Symbol");
			headerJoiner.add("Gene ID");
		}
		headerJoiner.add("Location");
		headerJoiner.add("Stage");
		headerJoiner.add("Assay");
		headerJoiner.add("Source");
		headerJoiner.add("Reference");
		builder.append(headerJoiner.toString());
		builder.append(ConfigHelper.getJavaLineSeparator());

		annotations.forEach(expressionDetail -> {
			StringJoiner joiner = new StringJoiner("\t");
			if (isMultipleGenes) {
				joiner.add(expressionDetail.getGeneExpressionAnnotation().getExpressionAnnotationSubject().getTaxon().getName());
				joiner.add(expressionDetail.getGeneExpressionAnnotation().getExpressionAnnotationSubject().getGeneSymbol().getDisplayText());
				joiner.add(expressionDetail.getGeneExpressionAnnotation().getExpressionAnnotationSubject().getPrimaryExternalId());
			}
			joiner.add(expressionDetail.getGeneExpressionAnnotation().getWhereExpressedStatement());
			joiner.add(expressionDetail.getGeneExpressionAnnotation().getWhenExpressedStageName());
			joiner.add(expressionDetail.getGeneExpressionAnnotation().getExpressionAssayUsed().getName());
			String crossRefs = "";
			if (expressionDetail.getGeneExpressionAnnotation().getCrossReferences() != null) {
				StringJoiner crossRefJoiner = new StringJoiner(",");
				expressionDetail.getGeneExpressionAnnotation().getCrossReferences().forEach(crossReference -> crossRefJoiner.add(crossReference.getDisplayName()));
				crossRefs = crossRefJoiner.toString();
			}
			joiner.add(crossRefs);
			// add list of publications
			String publications = "";
			if (expressionDetail.getReferenceId() != null) {
				StringJoiner pubJoiner = new StringJoiner(",");
				expressionDetail.getReferenceId().forEach(reference -> pubJoiner.add(reference));
				publications = pubJoiner.toString();
			}
			joiner.add(publications);
			builder.append(joiner.toString());
			builder.append(ConfigHelper.getJavaLineSeparator());
		});

		return builder.toString();

	}
}
