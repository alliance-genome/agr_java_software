package org.alliancegenome.api.tests.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

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

        // 5 annotations with different orthology geneMap
/*
        assertThat(doc.getAnnotations().stream().filter(annotationDocument -> annotationDocument.getOrthologyGeneDocument() != null).count(), equalTo(5L));
        List<String> orthoGeneName = doc.getAnnotations().stream()
                .filter(annotationDocument -> annotationDocument.getOrthologyGeneDocument() != null)
                .map(annotationDocument -> annotationDocument.getOrthologyGeneDocument().getSymbol())
                .collect(Collectors.toList());
        // five ortho geneMap (symbols)
        assertThat(orthoGeneName, containsInAnyOrder("IGF1R",
                "Igf1r",
                "Insr",
                "INSR",
                "Igf1r"));
*/

    }

    @Test
    // ZFIN gene: mkks
    public void checkPhenotypeByGeneWithPagination() throws JsonProcessingException {

        String geneID = "ZFIN:ZDB-GENE-040426-757";

        Pagination pagination = new Pagination(1, 11, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 11, 19);
    }

    @Test
    // ZFIN gene: pax2a
    public void checkPhenotypeByGeneWithPaginationPax2a() throws JsonProcessingException {

        String geneID = "ZFIN:ZDB-GENE-990415-8";

        Pagination pagination = new Pagination(1, 11, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        int resultSize = 11;
        int totalSize = 295;
        assertResponse(response, resultSize, totalSize);

        EntitySummary summary = geneService.getPhenotypeSummary(geneID);
        assertNotNull(summary);
        assertThat(summary.getNumberOfAnnotations(), greaterThanOrEqualTo(295L));
        assertThat(summary.getNumberOfEntities(), greaterThanOrEqualTo(110L));


        // add filter on phenotype
        pagination.makeSingleFieldFilter(FieldFilter.PHENOTYPE, "CirC");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 7, 7);

        // add filter on allele
        pagination.makeSingleFieldFilter(FieldFilter.GENETIC_ENTITY, "21");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 11, 27);

        // add filter on genetic entity type
        pagination.makeSingleFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, "allele");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 11, 185);

        pagination.makeSingleFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, "gene");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 11, 110);


        // add filter on reference: pubmod
        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "zfin:zdb-pub");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 11, 50);

        int zfinRefCount = response.getTotal();

        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "pmid");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 11, 245);

        assertThat("zfin pubs plus PUB MED pubs gives total number ", zfinRefCount + response.getTotal(), greaterThanOrEqualTo(totalSize));

        // add filter on reference: pubmed
        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "239");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 11, 63);
    }

    @Test
    public void checkPhenotypeDownload() throws JsonProcessingException {
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations("MGI:105043", new Pagination());
        PhenotypeAnnotationToTdfTranslator translator = new PhenotypeAnnotationToTdfTranslator();
        String line = translator.getAllRows(response.getResults());
        assertNotNull(line);
        String[] lines = line.split("\n");
        assertThat(21, equalTo(lines.length));
        assertThat(response.getTotal(), greaterThan(300));
        assertThat("Phenotype\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tReferences", equalTo(lines[0]));
        assertThat("abnormal atrial thrombosis\tMGI:2151800\tAhr<sup>tm1Gonz</sup>\tallele\tPMID:9396142", equalTo(lines[1]));
        assertThat("abnormal atrial thrombosis\t\t\tgene\tPMID:9396142", equalTo(lines[2]));

        response = geneService.getPhenotypeAnnotations("MGI:109583", new Pagination());
        line = translator.getAllRows(response.getResults());
        assertNotNull(line);
        assertThat(response.getTotal(), greaterThan(1200));
    }

    @Test
    // ZFIN gene: Pten
    public void checkPhenotypeByGeneWithPaginationPten() throws JsonProcessingException {

        String geneID = "MGI:109583";
        Pagination pagination = new Pagination(1, 42, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 42, 1251);

        EntitySummary summary = geneService.getPhenotypeSummary(geneID);
        assertNotNull(summary);
        assertThat(summary.getNumberOfAnnotations(), greaterThanOrEqualTo(1251L));
        assertThat(summary.getNumberOfEntities(), greaterThanOrEqualTo(526L));


        // add filter on phenotype
        pagination.makeSingleFieldFilter(FieldFilter.PHENOTYPE, "DEV");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 25, 25);

        // add filter on phenotype
        pagination.makeSingleFieldFilter(FieldFilter.GENETIC_ENTITY, "1hW");
        response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 42, 337);
    }

    private void assertResponse(JsonResultResponse<PhenotypeAnnotation> response, int resultSize, int totalSize) {
        assertNotNull(response);
        assertThat("Number of returned records", response.getResults().size(), greaterThanOrEqualTo(resultSize));
        assertThat("Number of total records", response.getTotal(), greaterThanOrEqualTo(totalSize));
    }


}