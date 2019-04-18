package org.alliancegenome.api.tests.unit;

import static org.alliancegenome.core.service.DiseaseAnnotationFiltering.geneticEntityTypeFilter;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertTrue("simple 'gene' record", geneticEntityTypeFilter.containsFilterValue(annotation, "gene"));
        assertFalse("simple 'gene' record is not an allele", geneticEntityTypeFilter.containsFilterValue(annotation, "allele"));
        assertTrue("simple 'allele' record", geneticEntityTypeFilter.containsFilterValue(annotation2, "allele"));
        assertFalse("simple 'allele' record not a gene", geneticEntityTypeFilter.containsFilterValue(annotation2, "gene"));

        // gene OR allele
        assertTrue("simple 'gene' record", geneticEntityTypeFilter.containsFilterValue(annotation, "gene|allele"));
        assertTrue("simple 'gene' record", geneticEntityTypeFilter.containsFilterValue(annotation, "allele|gene"));
        assertFalse("simple 'gene' record with exact match", geneticEntityTypeFilter.containsFilterValue(annotation, "allele|gene3"));

    }


}
