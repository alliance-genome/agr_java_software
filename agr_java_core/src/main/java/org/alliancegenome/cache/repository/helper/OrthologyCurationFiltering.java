package org.alliancegenome.cache.repository.helper;

import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.view.OrthologyFilter.Stringency;
import org.apache.commons.collections.CollectionUtils;

public class OrthologyCurationFiltering extends AnnotationFiltering<GeneToGeneOrthologyDocument> {


	public FilterFunction<GeneToGeneOrthologyDocument, String> stringencyFilter =
			(orthoDoc, value) -> {
				Stringency stringency = Stringency.getOrthologyFilter(value);
				if (stringency == null) {
					return false;
				}
				if (stringency.equals(Stringency.STRINGENT)) {
					return FilterFunction.contains(orthoDoc.getStringencyFilter(), value);
				}
			if (stringency.equals(Stringency.MODERATE)) {
				return FilterFunction.contains(orthoDoc.getStringencyFilter(), value) || FilterFunction.contains(orthoDoc.getStringencyFilter(), Stringency.STRINGENT.name());
			}
				if (stringency.equals(Stringency.ALL)) {
					return true;
				}
				return false;
			};

	public FilterFunction<GeneToGeneOrthologyDocument, String> methodFilter =
			(orthoDoc, value) -> {
				if (CollectionUtils.isEmpty(orthoDoc.getGeneToGeneOrthologyGenerated().getPredictionMethodsMatched())) {
					return false;
				}
				List<String> concatenatedMethodsList = orthoDoc.getGeneToGeneOrthologyGenerated().getPredictionMethodsMatched()
						.stream()
						.map(method -> method.getVocabulary().getName())
						.collect(Collectors.toList());

				String concatenatedMethods = String.join(",", concatenatedMethodsList);

				return FilterFunction.contains(concatenatedMethods, value);
			};

	public OrthologyCurationFiltering() {
		filterFieldMap.put(FieldFilter.STRINGENCY, stringencyFilter);
		filterFieldMap.put(FieldFilter.ORTHOLOGY_METHOD, methodFilter);
	}

}
