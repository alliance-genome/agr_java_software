package org.alliancegenome.api.tests.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Variant;
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

        response = alleleService.getAllelesByGene("WB:WBGene00015146", pagination);
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
    }

    private void assertResponse(JsonResultResponse<Allele> response, int resultSize, int totalSize) {
        assertNotNull(response);
        assertThat("Number of returned records", response.getResults().size(), greaterThanOrEqualTo(resultSize));
        assertThat("Number of total records", response.getTotal(), greaterThanOrEqualTo(totalSize));
    }


}