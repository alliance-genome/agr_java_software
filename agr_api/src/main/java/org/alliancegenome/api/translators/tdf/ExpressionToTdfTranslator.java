package org.alliancegenome.api.translators.tdf;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.curation_api.model.entities.GeneExpressionAnnotation;
import org.apache.commons.collections.CollectionUtils;

public class ExpressionToTdfTranslator {

	private static final String SOURCE_DELIMITER = "|";

	/**
	 * The consolidation in agr_curation's GeneExpressionDocumentBuilder pads crossReferences and referenceId so
	 * entry i of each describes the same underlying annotation, and one annotation is one publication. Group the
	 * cross references by the reference they are aligned to and emit one row per distinct reference, so the row
	 * count matches the annotation count.
	 *
	 * MGI and WB take their cross references from the expression experiment rather than the annotation, so a
	 * single publication commonly carries many sources; those share one row with the sources pipe-joined rather
	 * than fanning out into rows that would each claim a specific source.
	 */
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
			int pairCount = Math.max(1, Math.max(pubSize, crossRefSize));

			// Insertion-ordered so rows follow the document's reference order; the source sets drop exact repeats.
			Map<String, Set<String>> sourcesByReference = new LinkedHashMap<>();
			for (int i = 0; i < pairCount; i++) {
				/*
				 * Cross references past the end of the reference list belong to the first publication — the
				 * group.size() == 1 short-circuit upstream skips the padding, so the lists are not always
				 * equal length.
				 */
				String refId = pubSize == 0 ? "" : (i < pubSize ? refIds.get(i) : refIds.get(0));
				String source = i < crossRefSize ? crossRefs.get(i).getDisplayName() : "";
				Set<String> sources = sourcesByReference.computeIfAbsent(refId, r -> new LinkedHashSet<>());
				if (source != null && !source.isEmpty()) {
					sources.add(source);
				}
			}

			for (Map.Entry<String, Set<String>> entry : sourcesByReference.entrySet()) {
				builder.append(buildRow(annotation, entry.getKey(), String.join(SOURCE_DELIMITER, entry.getValue()), isMultipleGenes));
				builder.append(ConfigHelper.getJavaLineSeparator());
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
		String reference,
		String source,
		boolean isMultipleGenes
	) {
		StringJoiner joiner = new StringJoiner("\t");
		if (isMultipleGenes) {
			var subject = annotation.getExpressionAnnotationSubject();
			joiner.add(subject.getTaxon().getSpecies().getFullName())
					.add(subject.getGeneSymbol().getDisplayText())
					.add(subject.getPrimaryExternalId());
		}
		joiner.add(annotation.getWhereExpressedStatement())
				.add(annotation.getWhenExpressedStageName())
				.add(annotation.getExpressionAssayUsed().getName());

		joiner.add(source);
		joiner.add(reference);

		return joiner.toString();
	}
	}
