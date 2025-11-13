package org.alliancegenome.core.translators.tdf;

import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.apache.commons.collections.CollectionUtils;

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

			int crossRefSize = CollectionUtils.isNotEmpty(expressionDetail.getGeneExpressionAnnotation().getCrossReferences()) ? expressionDetail.getGeneExpressionAnnotation().getCrossReferences().size() : 0;
			int pubSize = CollectionUtils.isNotEmpty(expressionDetail.getReferenceId()) ? expressionDetail.getReferenceId().size() : 0;

			int numOfAnnotations = Math.max(crossRefSize, pubSize);
			for (int i=0;i<numOfAnnotations;i++) {
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
				if (CollectionUtils.isNotEmpty(expressionDetail.getGeneExpressionAnnotation().getCrossReferences()) && i<crossRefSize) {
					crossRefs = expressionDetail.getGeneExpressionAnnotation().getCrossReferences().get(i).getDisplayName();
				}
				joiner.add(crossRefs);

				String publications = "";
				if (CollectionUtils.isNotEmpty(expressionDetail.getReferenceId()) && i<pubSize) {
					publications = expressionDetail.getReferenceId().get(i);
				}
				joiner.add(publications);
				builder.append(joiner.toString());
				builder.append(ConfigHelper.getJavaLineSeparator());
			}
		});

		return builder.toString();

	}
}
