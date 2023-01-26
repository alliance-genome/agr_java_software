package org.alliancegenome.api.tests.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.alliancegenome.cache.repository.helper.DiseaseAnnotationFiltering;
import org.alliancegenome.cache.repository.helper.FilterFunction;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.junit.Test;

public class FilterFunctionTest {

	@Test
	public void testSingleFilterFunction() {
		DiseaseAnnotationFiltering filtering= new DiseaseAnnotationFiltering();
		FilterFunction<DiseaseAnnotation, String> diseaseFilterFunction = filtering.termNameFilter;

		DiseaseAnnotation annotation = new DiseaseAnnotation();
		DOTerm disease = new DOTerm();
		disease.setName("Charcinoma1234");
		annotation.setDisease(disease);
		assertTrue(diseaseFilterFunction.containsFilterValue(annotation, "oma"));
		assertFalse(diseaseFilterFunction.containsFilterValue(annotation, "omas"));
	}

}
