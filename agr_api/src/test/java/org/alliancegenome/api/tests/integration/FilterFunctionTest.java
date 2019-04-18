package org.alliancegenome.api.tests.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.alliancegenome.core.service.DiseaseAnnotationFiltering;
import org.alliancegenome.core.service.FilterFunction;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.junit.Test;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class FilterFunctionTest {

    @Test
    public void testSingleFilterFunction() {
        FilterFunction<DiseaseAnnotation, String> diseaseFilterFunction = DiseaseAnnotationFiltering.termNameFilter;

        DiseaseAnnotation annotation = new DiseaseAnnotation();
        DOTerm disease = new DOTerm();
        disease.setName("Charcinoma1234");
        annotation.setDisease(disease);
        assertTrue(diseaseFilterFunction.containsFilterValue(annotation, "oma"));
        assertFalse(diseaseFilterFunction.containsFilterValue(annotation, "omas"));
    }

}
