package org.alliancegenome.api.tests.unit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.alliancegenome.cache.repository.helper.DiseaseAnnotationFiltering;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.alliancegenome.cache.repository.helper.DiseaseAnnotationFiltering;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.junit.Test;

public class FilteringTest {

    @Test
    public void checkAllelesBySpecies() {

        DiseaseAnnotation annotation = new DiseaseAnnotation();
        Gene gene = new Gene();
        gene.setName("pax2a");
        annotation.setGene(gene);
        annotation.setGene(gene);

        DiseaseAnnotation annotation2 = new DiseaseAnnotation();
        Allele allele = new Allele();
        allele.setSymbol("t1ggf");
        annotation2.setFeature(allele);
        DiseaseAnnotationFiltering filtering = new DiseaseAnnotationFiltering();
        assertTrue("simple 'gene' record", filtering.geneticEntityTypeFilter.containsFilterValue(annotation, "gene"));
        assertFalse("simple 'gene' record is not an allele", filtering.geneticEntityTypeFilter.containsFilterValue(annotation, "allele"));
        assertTrue("simple 'allele' record", filtering.geneticEntityTypeFilter.containsFilterValue(annotation2, "allele"));
        assertFalse("simple 'allele' record not a gene", filtering.geneticEntityTypeFilter.containsFilterValue(annotation2, "gene"));

        // gene OR allele
        assertTrue("simple 'gene' record", filtering.geneticEntityTypeFilter.containsFilterValue(annotation, "gene|allele"));
        assertTrue("simple 'gene' record", filtering.geneticEntityTypeFilter.containsFilterValue(annotation, "allele|gene"));
        assertFalse("simple 'gene' record with exact match", filtering.geneticEntityTypeFilter.containsFilterValue(annotation, "allele|gene3"));

    }


}
