package org.alliancegenome.api.tests.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
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
    public void checkPhenotypeByGeneWithoutPagination() {
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
        assertResponse(response, 11, 94);
        assertTrue("More than one phenotype", response.getResults().get(0).getPhenotypes().size() > 1);
        response.getResults().forEach(entity -> {
            assertNotEquals("No AGMs with Gene type", entity.getType(), GeneticEntity.CrossReferenceType.GENE);
            assertNotEquals("No AGMs with Allele type", entity.getType(), GeneticEntity.CrossReferenceType.ALLELE);
        });
    }

    @Test
    public void checkPhenotypesByModelsZfin() {

        // sox9a
        String geneID = "ZFIN:ZDB-GENE-001103-1";

        Pagination pagination = new Pagination(1, 11, null, null);
        DiseaseService diseaseService = new DiseaseService();
        JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithGeneAndAGM(geneID, pagination);
        assertResponse(response, 11, 38);
        assertTrue("More than one phenotype", response.getResults().get(0).getPhenotypes().size() > 1);
        response.getResults().forEach(entity -> {
            assertNotEquals("No AGMs with Gene type", entity.getType(), GeneticEntity.CrossReferenceType.GENE);
            assertNotEquals("No AGMs with Allele type", entity.getType(), GeneticEntity.CrossReferenceType.ALLELE);
        });
    }

    @Test
    public void checkPhenotypesWithReference() {

        // sox9a
        String geneID = "ZFIN:ZDB-GENE-001103-1";

        Pagination pagination = new Pagination(1, 60, null, null);
        DiseaseService diseaseService = new DiseaseService();
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        List<PhenotypeAnnotation> pa = response.getResults().stream()
                .filter(phenotypeAnnotation -> phenotypeAnnotation.getPhenotype().equals("cartilage development disrupted, abnormal"))
                .collect(Collectors.toList());
        assertNotNull(pa);
        String pmids = pa.get(0).getPublications().stream().map(Publication::getPubId).collect(Collectors.joining(","));
        assertEquals("Pmid list", "PMID:12397114,PMID:18950725,PMID:9007254", pmids);
    }

    @Test
    public void checkPhenotypesWithoutGenePopup() {

        // ATP7
        String geneID = "FB:FBgn0030343";

        Pagination pagination = new Pagination(1, 60, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        response.getResults()
                .stream()
                .filter(phenotypeAnnotation -> phenotypeAnnotation.getPrimaryAnnotatedEntities() != null)
                .forEach(phenotypeAnnotation -> phenotypeAnnotation.getPrimaryAnnotatedEntities().forEach(entity -> {
                    assertNotEquals("Direct Gene annotation found. Should be suppressed for: " + entity.getId(), entity.getType(), GeneticEntity.CrossReferenceType.GENE);
                }));
    }

    @Test
    public void checkPureModels() {

        // Abcc6
        String geneID = "RGD:620268";

        Pagination pagination = new Pagination(1, 10, null, null);
        DiseaseService diseaseService = new DiseaseService();
        JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithGeneAndAGM(geneID, pagination);
        assertResponse(response, 1, 1);
    }

    @Test
    public void checkPureModelsWithSTR() {

        // Abcc6
        String geneID = "ZFIN:ZDB-GENE-060526-68";

        Pagination pagination = new Pagination(1, 10, null, null);
        BaseFilter filter = new BaseFilter();
        filter.addFieldFilter(FieldFilter.MODEL_NAME, "WT");
        pagination.setFieldFilterValueMap(filter);
        DiseaseService diseaseService = new DiseaseService();
        JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithGeneAndAGM(geneID, pagination);
        assertResponse(response, 1, 1);
        assertNotNull(response.getResults().get(0).getSequenceTargetingReagents().get(0).getGene().getType());
    }

    @Test
    public void checkUrlForAllelesInPopup() {

        // cua-1
        String geneID = "WB:WBGene00000834";

        Pagination pagination = new Pagination(1, 10, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 1, 1);

        response.getResults()
                .stream()
                .filter(phenotypeAnnotation -> phenotypeAnnotation.getPrimaryAnnotatedEntities() != null)
                .forEach(phenotypeAnnotation -> {
                    phenotypeAnnotation.getPrimaryAnnotatedEntities().forEach(entity -> {
                        assertNotNull("URL for AGM should not be null: " + entity.getId(), entity.getUrl());
                    });
                });
    }

    @Test
    public void checkModelsForPhenotypeAndDisease() {

        // Tnf
        String geneID = "MGI:104798";

        Pagination pagination = new Pagination(1, 10, null, null);
        DiseaseService diseaseService = new DiseaseService();
        JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithGeneAndAGM(geneID, pagination);
        assertResponse(response, 10, 90);
    }

    @Test
    public void checkModelsForPhenotypeFiltering() {

        // Tnf
        String geneID = "MGI:105043";

        Pagination pagination = new Pagination(1, 10, null, null);
        BaseFilter filter = new BaseFilter();
        filter.addFieldFilter(FieldFilter.PHENOTYPE, "abnormal");
        pagination.setFieldFilterValueMap(filter);
        DiseaseService diseaseService = new DiseaseService();
        JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithGeneAndAGM(geneID, pagination);
        assertResponse(response, 9, 9);
    }

    @Test
    public void checkModelsForPhenotypeWithoutDisease() {

        // Arnt
        String geneID = "MGI:88071";

        Pagination pagination = new Pagination(1, 10, null, null);
        DiseaseService diseaseService = new DiseaseService();
        JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithGeneAndAGM(geneID, pagination);
        assertResponse(response, 8, 8);
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
        assertThat(24, equalTo(lines.length));
        assertThat(response.getTotal(), greaterThan(130));
        assertThat("Phenotype\tGenetic Entity ID\tGenetic Entity Name\tGenetic Entity Type\tSource\tReference", equalTo(lines[0]));
        assertThat("abnormal atrial thrombosis\tMGI:2450836\tAhr<tm1Gonz>/Ahr<tm1Gonz> [background:] involves: 129S4/SvJae * C57BL/6N\tgenotype\tMGI\tPMID:9396142", equalTo(lines[1]));
        assertThat("abnormal auchene hair morphology\tMGI:2450836\tAhr<tm1Gonz>/Ahr<tm1Gonz> [background:] involves: 129S4/SvJae * C57BL/6N\tgenotype\tMGI\tPMID:9396142", equalTo(lines[2]));

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
    // Fly gene: FB:FBgn0267821
    public void checkPhenotypeByGeneFly() {

        String geneID = "FB:FBgn0267821";
        Pagination pagination = new Pagination(1, 10, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 10, 50);
        PhenotypeAnnotation annotation = response.getResults().get(0);
        assertEquals(annotation.getPhenotype(), "corpus cardiacum primordium");
        final List<PrimaryAnnotatedEntity> primaryAnnotatedEntities = annotation.getPrimaryAnnotatedEntities();
        assertNotNull("Phenotype annotation has Allele as the inferred AGM but missing.", primaryAnnotatedEntities);
        assertEquals("Phenotype annotation with Allele as an inferred AGM", primaryAnnotatedEntities.get(0).getType(), GeneticEntity.CrossReferenceType.ALLELE);
    }

    @Test
    // Fly gene: WB:WBGene00000898
    public void checkPhenotypeByGeneWorm() {

        String geneID = "WB:WBGene00002992";
        Pagination pagination = new Pagination(1, 10, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 10, 17);
        final String ectopicExpressionTransgene = "ectopic expression transgene";
        Optional<PhenotypeAnnotation> annotation = response.getResults().stream()
                .filter(annot -> annot.getPhenotype().equals(ectopicExpressionTransgene))
                .findFirst();
        assertTrue("Did not find a phenotype: " + ectopicExpressionTransgene, annotation.isPresent());
        final List<PrimaryAnnotatedEntity> primaryAnnotatedEntities = annotation.get().getPrimaryAnnotatedEntities();
        assertNotNull("Phenotype annotation has Allele as the inferred AGM but missing.", primaryAnnotatedEntities);
        assertEquals("Phenotype annotation with Allele as an inferred AGM", primaryAnnotatedEntities.get(0).getType(), GeneticEntity.CrossReferenceType.ALLELE);
    }

    @Test
    public void checkPhenotypeByGeneWithPaginationCua_1() {

        String geneID = "WB:WBGene00000834";
        Pagination pagination = new Pagination(1, 42, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 1, 1);
        final List<PrimaryAnnotatedEntity> primaryAnnotatedEntities = response.getResults().get(0).getPrimaryAnnotatedEntities();
        assertNull("Allele phenotype annotation", primaryAnnotatedEntities);
    }

    @Test
    public void checkPhenotypeOnWBGenes() {

        String geneID = "WB:WBGene00000898";
        Pagination pagination = new Pagination(1, 10, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 1, 1);
    }

    @Test
    public void checkPhenotypeOnZFINpax2a() {

        String geneID = "ZFIN:ZDB-GENE-990415-8";
        Pagination pagination = new Pagination(1, 10, null, null);
        JsonResultResponse<PhenotypeAnnotation> response = geneService.getPhenotypeAnnotations(geneID, pagination);
        assertResponse(response, 1, 1);
        assertThat(response.getResults().get(0).getPhenotype(), equalTo("anatomical system quality, abnormal"));
        assertNotNull(response.getResults().get(0).getPrimaryAnnotatedEntities());
        // more than 4 fish are found for primary entity annotations
        assertThat(response.getResults().get(0).getPrimaryAnnotatedEntities().size(), greaterThanOrEqualTo(4));
    }

    @Test
    public void checkDiseasesByModels() {

        // Tnf
        String geneID = "MGI:104993";

        Pagination pagination = new Pagination(1, 11, null, null);
        DiseaseService diseaseService = new DiseaseService();
        JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithGeneAndAGM(geneID, pagination);
        assertResponse(response, 11, 92);
        assertTrue("More than one disease", response.getResults().get(1).getDiseases().size() > 1);
        assertTrue("More than one phenotype", response.getResults().get(0).getPhenotypes().size() > 1);
    }

    private void assertResponse(JsonResultResponse response, int resultSize, int totalSize) {
        assertNotNull(response);
        assertThat("Number of returned records", response.getResults().size(), greaterThanOrEqualTo(resultSize));
        assertThat("Number of total records", response.getTotal(), greaterThanOrEqualTo(totalSize));
    }


}