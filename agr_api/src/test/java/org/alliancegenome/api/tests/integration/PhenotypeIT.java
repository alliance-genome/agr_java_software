package org.alliancegenome.api.tests.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@Api(value = "Phenotype Tests")
public class PhenotypeIT {

    private ObjectMapper mapper = new ObjectMapper();
    private GeneService geneService;

    @ApiOperation(value = "Retrieve a Gene for given ID")
    public static void main(String[] args) {

/*
        PhenotypeTest test = new PhenotypeTest();
        Api annotation = test.getClass().getAnnotation(Api.class);
        Method method = new Object() {
        }
                .getClass()
                .getEnclosingMethod();
        Annotation[] annotations = method.getDeclaredAnnotations();
*/

        System.out.println("Number of Diseases with Genes Info: ");
    }

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();

        geneService = new GeneService();

        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new OrthologyModule());
    }


    @Test
    @Ignore
    // This can go away as we do not display those numbers any longer.
    // Just waiting for curators to confirm
    public void checkPhenotypeByGeneWithoutPagination() throws JsonProcessingException {
        Pagination pagination = new Pagination(1, 100, null, null);
        // mkks

        String geneID = "ZFIN:ZDB-GENE-040426-757";
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);

        assertResponse(response, 19, 19);

        EntitySummary summary = geneService.getPhenotypeSummary(geneID);
        assertNotNull(summary);
        assertThat(19L, equalTo(summary.getNumberOfAnnotations()));
        assertThat(19L, equalTo(summary.getNumberOfEntities()));

    }

    @Test
    // ZFIN gene: mkks
    public void checkPhenotypeByGeneWithPagination() {

        String geneID = "ZFIN:ZDB-GENE-040426-757";

        Pagination pagination = new Pagination(1, 11, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 11, 19);

        // add containsFilterValue on phenotype
        pagination.makeSingleFieldFilter(FieldFilter.PHENOTYPE, "som");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 6, 6);

    }

    @Test
    public void checkPhenotypesByModels() {

        // Tnf
        String geneID = "MGI:104798";

        Pagination pagination = new Pagination(1, 11, null, null);
        DiseaseService diseaseService = new DiseaseService();
        JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithGeneAndAGM(geneID, pagination);
        assertResponse(response, 11, 95);
        assertTrue("More than one phenotype", response.getResults().get(0).getPhenotypes().size() > 1);
    }

    @Test
    public void checkPhenotypeReferenceNonDuplicated() {

        // top2b
        String geneID = "ZFIN:ZDB-GENE-041008-136";

        Pagination pagination = new Pagination(1, 10, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 10, 10);

        response.getResults().forEach(phenotypeAnnotation -> {
            int beforeSize = phenotypeAnnotation.getPublications().size();
            int afterSize = phenotypeAnnotation.getPublications().stream().distinct().collect(Collectors.toList()).size();
            assertEquals("No duplicated references", beforeSize, afterSize);
        });
    }

    @Test
    // ZFIN gene: pax2a
    public void checkPhenotypeByGeneWithPaginationPax2a() {

        String geneID = "ZFIN:ZDB-GENE-990415-8";

        Pagination pagination = new Pagination(1, 11, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        int resultSize = 11;
        int totalSize = 122;
        assertResponse(response, resultSize, totalSize);

        // add containsFilterValue on phenotype
        pagination.makeSingleFieldFilter(FieldFilter.PHENOTYPE, "CirC");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 1, 1);

        // add containsFilterValue on reference: pubmod
        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "zfin:zdb-pub");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 7, 7);

        int zfinRefCount = response.getTotal();

        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "pmid");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 11, 115);

        assertThat("zfin pubs plus PUB MED pubs gives total number ", zfinRefCount + response.getTotal(), greaterThanOrEqualTo(totalSize));

        // add containsFilterValue on reference: pubmed
        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "239");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 11, 15);
    }

    @Test
    public void checkPhenotypeDownload() {
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations("MGI:105043", new Pagination());
        PhenotypeAnnotationToTdfTranslator translator = new PhenotypeAnnotationToTdfTranslator();
        String line = translator.getAllRows(response.getResults());
        assertNotNull(line);
        String[] lines = line.split("\n");
        assertThat(21, equalTo(lines.length));
        assertThat(response.getTotal(), greaterThan(130));
        assertThat("Phenotype\tReferences", equalTo(lines[0]));
        assertThat("abnormal atrial thrombosis\tPMID:9396142", equalTo(lines[1]));
        assertThat("abnormal auchene hair morphology\tPMID:9396142", equalTo(lines[2]));

        response = geneService.getPhenotypeAnnotations("MGI:109583", new Pagination());
        line = translator.getAllRows(response.getResults());
        assertNotNull(line);
        assertThat(response.getTotal(), greaterThan(500));
    }

    @Test
    // ZFIN gene: Pten
    public void checkPhenotypeByGeneWithPaginationPten() {

        String geneID = "MGI:109583";
        Pagination pagination = new Pagination(1, 42, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 42, 515);


        // add containsFilterValue on phenotype
        pagination.makeSingleFieldFilter(FieldFilter.PHENOTYPE, "DEV");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 11, 1);

    }

    @Test
    public void checkPhenotypeByGeneWithPaginationCua_1() {

        String geneID = "WB:WBGene00000834";
        Pagination pagination = new Pagination(1, 42, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 1, 1);
        final List<PrimaryAnnotatedEntity> primaryAnnotatedEntities = response.getResults().get(0).getPrimaryAnnotatedEntities();
        assertNotNull("Allele phenotype annotation", primaryAnnotatedEntities);
        assertThat("Allele phenotype annotation", primaryAnnotatedEntities.get(0).getType(), equalTo(GeneticEntity.CrossReferenceType.ALLELE));

    }

    private void assertResponse(JsonResultResponse response, int resultSize, int totalSize) {
        assertNotNull(response);
        assertThat("Number of returned records", response.getResults().size(), greaterThanOrEqualTo(resultSize));
        assertThat("Number of total records", response.getTotal(), greaterThanOrEqualTo(totalSize));
    }


}