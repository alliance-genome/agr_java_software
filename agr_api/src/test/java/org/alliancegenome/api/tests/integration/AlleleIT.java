package org.alliancegenome.api.tests.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.*;

@Api(value = "Allele Tests")
public class AlleleIT {

    private ObjectMapper mapper = new ObjectMapper();
    private AlleleService alleleService;

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();

        alleleService = new AlleleService();

        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new OrthologyModule());
    }


    @Test
    @Ignore
    public void checkAllelesBySpecies() {
        Pagination pagination = new Pagination();
        pagination.setLimit(100000);
        JsonResultResponse<Allele> response = alleleService.getAllelesBySpecies("dani", pagination);
        System.out.println(response.getTotal());
        assertResponse(response, 40000, 40000);
    }

    @Test
    public void checkAllelesByGene() {
        Pagination pagination = new Pagination();
        JsonResultResponse<Allele> response = alleleService.getAllelesByGene("MGI:109583", pagination);
        assertResponse(response, 19, 19);
    }

    @Test
    public void checkAllelesGeneLocation() {
        Allele allele = alleleService.getById("ZFIN:ZDB-ALT-161003-18461");
        assertNotNull(allele);
        assertNotNull(allele.getGene());
        List<GenomeLocation> genomeLocations = allele.getGene().getGenomeLocations();
        assertNotNull("No Genome location found on associated gene", genomeLocations);
        assertThat(genomeLocations.size(), greaterThanOrEqualTo(1));
        GenomeLocation location = genomeLocations.get(0);
        assertThat(location.getChromosome().getPrimaryKey(), equalTo("22"));
        assertTrue(location.getStart() > 0);
        assertTrue(location.getEnd() > 0);
    }

    @Test
    public void checkAllelesSortedByVariantExistAndAlleleSymbol() {
        Pagination pagination = new Pagination();
        JsonResultResponse<Allele> response = alleleService.getAllelesByGene("WB:WBGene00004879", pagination);
        assertResponse(response, 20, 25);

        //check that alleles with variants come first
        List<Boolean> hasVariants = new ArrayList<>();
        boolean lastVariantsExists = true;
        hasVariants.add(lastVariantsExists);
        for (Allele allele : response.getResults()) {
            boolean currentVariantsExists = CollectionUtils.isNotEmpty(allele.getVariants());
            if (currentVariantsExists != lastVariantsExists) {
                hasVariants.add(currentVariantsExists);
            }
            lastVariantsExists = currentVariantsExists;
            assertTrue("Not all allele with variants come before alleles without variants.", (currentVariantsExists && hasVariants.size() == 1) ||
                    (!currentVariantsExists && hasVariants.size() == 2));
        }

        // check that all alleles with variants are sorted by allele symbol
        List<Allele> allVariantAlleles = response.getResults().stream()
                .filter(allele -> CollectionUtils.isNotEmpty(allele.getVariants()))
                .collect(Collectors.toList());

        String alleleIDs = allVariantAlleles.stream().map(GeneticEntity::getPrimaryKey)
                .collect(Collectors.joining(","));

        String alleleIDsAfterSorting = allVariantAlleles.stream()
                .sorted(Comparator.comparing(Allele::getSymbolText))
                .map(GeneticEntity::getPrimaryKey)
                .collect(Collectors.joining(","));

        assertEquals("The alleles are not sorted by symbol", alleleIDsAfterSorting, alleleIDs);

    }

    @Test
    public void checkAllelesByGeneFiltering() {
        Pagination pagination = new Pagination();
        BaseFilter filter = new BaseFilter();
        filter.addFieldFilter(FieldFilter.VARIANT_CONSEQUENCE, "deletion");
        pagination.setFieldFilterValueMap(filter);
        JsonResultResponse<Allele> response = alleleService.getAllelesByGene("WB:WBGene00000898", pagination);
        assertResponse(response, 1, 1);
    }

    @Test
    public void checkVariantLocation() {
        Pagination pagination = new Pagination();
        JsonResultResponse<Allele> response = alleleService.getAllelesByGene("FB:FBgn0025832", pagination);
        assertResponse(response, 2, 2);

        response.getResults().stream()
                .map(Allele::getVariants)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(variant -> variant.getPrimaryKey().equals("NT_033778.4:g.16856124_16856125ins"))
                .forEach(variant -> {
                            assertNotNull("Variant location is missing", variant.getLocation());
                        }
                );

        response = alleleService.getAllelesByGene("WB:WBGene00006616", pagination);
        assertResponse(response, 1, 1);

        response.getResults().stream()
                .map(Allele::getVariants)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(variant -> variant.getPrimaryKey().equals("NC_003281.10:g.5690389_5691072del"))
                .forEach(variant -> {
                            assertNotNull("Variant location is missing", variant.getLocation());
                            assertNotNull("Variant consequence is missing", variant.getGeneLevelConsequence());
                        }
                );

    }

    @Test
    public void checkAllelesWithDiseases() {
        Pagination pagination = new Pagination();
        JsonResultResponse<Allele> response = alleleService.getAllelesByGene("ZFIN:ZDB-GENE-040426-1716", pagination);
        List<Allele> term = response.getResults().stream().filter(allele -> allele.getDiseases() != null).collect(Collectors.toList());
        assertThat(term.size(), greaterThanOrEqualTo(1));
    }

    @Test
    public void getVariantsPerAllele() {
        Pagination pagination = new Pagination();
        JsonResultResponse<Variant> response = alleleService.getVariants("ZFIN:ZDB-ALT-161003-18461", pagination);
        assertThat(response.getTotal(), greaterThanOrEqualTo(1));
        assertNotNull("Computed Gene exists", response.getResults().get(0).getGene());
        assertNotNull("Genomic Location exists on computed Gene", response.getResults().get(0).getGene().getGenomeLocations());
    }

    @Test
    public void getAllelesPerGene() {
        Pagination pagination = new Pagination();
        GeneService service = new GeneService();
        JsonResultResponse<Allele> response = service.getAlleles("ZFIN:ZDB-GENE-990415-234", pagination);
        assertThat(response.getTotal(), greaterThanOrEqualTo(1));
    }

    @Test
    public void getVariantsWithInsertionDeletion() {
        Pagination pagination = new Pagination();
        JsonResultResponse<Variant> response = alleleService.getVariants("ZFIN:ZDB-ALT-181010-2", pagination);
        assertThat(response.getTotal(), greaterThanOrEqualTo(1));
        Variant variant = response.getResults().get(0);
        assertEquals("Nucleotide change of Insertion", "t>tTCCAGAA", variant.getNucleotideChange());

        response = alleleService.getVariants("ZFIN:ZDB-ALT-180925-10", pagination);
        assertThat(response.getTotal(), greaterThanOrEqualTo(1));
        variant = response.getResults().get(0);
        assertEquals("Nucleotide change: Deletion", "aGCAGAGGTCA>a", variant.getNucleotideChange());

        response = alleleService.getVariants("ZFIN:ZDB-ALT-161003-18461", pagination);
        assertThat(response.getTotal(), greaterThanOrEqualTo(1));
        variant = response.getResults().get(0);
        assertEquals("Nucleotide change: non-insertion, non-deletion", "A>G", variant.getNucleotideChange());
    }

    @Test
    public void getAlleleInfo() {
        Allele allele = alleleService.getById("ZFIN:ZDB-ALT-161003-18461");
        assertNotNull(allele.getCrossReferences());
    }

    @Test
    public void getAllelePhenotype() {
        //String alleleID = "ZFIN:ZDB-ALT-041001-12";
        //String alleleID = "MGI:5442117";
        // hu3335
        String alleleID = "ZFIN:ZDB-ALT-980203-692";
        JsonResultResponse<PhenotypeAnnotation> response = alleleService.getPhenotype(alleleID, new Pagination());
        assertNotNull(response);
        assertThat(response.getTotal(), greaterThanOrEqualTo(20));
    }

    @Test
    public void getAllelePhenotypeNoAllelePAESelfReference() {
        // Ptentm1.1Mwst
        String alleleID = "MGI:4366755";
        JsonResultResponse<PhenotypeAnnotation> response = alleleService.getPhenotype(alleleID, new Pagination());
        assertNotNull(response);
        assertThat(response.getTotal(), greaterThanOrEqualTo(8));
        response.getResults().stream()
                .filter(phenotypeAnnotation -> phenotypeAnnotation.getPrimaryAnnotatedEntities() != null)
                .forEach(annotation -> {
                    annotation.getPrimaryAnnotatedEntities().forEach(entity -> {
                        assertNotEquals("Do not have allele direct annotations reference alleles as PAE", entity.getType(), GeneticEntity.CrossReferenceType.ALLELE);
                    });
                });
    }

    @Test
    public void getAlleleDisease() {
        //String alleleID = "ZFIN:ZDB-ALT-041001-12";
        //String alleleID = "MGI:5442117";
        // hps5
        //String alleleID = "ZFIN:ZDB-ALT-980203-692";
        String alleleID = "MGI:1856424";
        JsonResultResponse<DiseaseAnnotation> response = alleleService.getDisease(alleleID, new Pagination());
        assertNotNull(response);
        assertThat(response.getTotal(), greaterThanOrEqualTo(3));
    }

    private void assertResponse(JsonResultResponse<Allele> response, int resultSize, int totalSize) {
        assertNotNull(response);
        assertThat("Number of returned records", response.getResults().size(), greaterThanOrEqualTo(resultSize));
        assertThat("Number of total records", response.getTotal(), greaterThanOrEqualTo(totalSize));
    }


}