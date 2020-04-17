package org.alliancegenome.api.tests.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.controller.DiseaseController;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@Log4j2
public class DiseaseIT {

    private ObjectMapper mapper = new ObjectMapper();
    private DiseaseService diseaseService = new DiseaseService();
    private DiseaseController diseaseController = new DiseaseController();
    DiseaseService service = new DiseaseService();

    @Before
    public void before() {
//        Configurator.setRootLevel(Level.INFO);
        ConfigHelper.init();

        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new OrthologyModule());
    }

    @Test
    public void checkAlleleDiseaseAssociationByDisease() {
        Pagination pagination = new Pagination(1, 100, null, null);
        // Menkes
        String diseaseID = "DOID:1838";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithAlleles(diseaseID, pagination);
        assertLimitResponse(response, 10, 10);
        assertEquals(response.getDistinctFieldValues().size(), 2);
    }

    @Test
    public void checkAlleleDiseaseAssociationSort() {
        Pagination pagination = new Pagination(1, 100, "allele", null);
        // Menkes
        String diseaseID = "DOID:0110042";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithAlleles(diseaseID, pagination);
        assertLimitResponse(response, 10, 10);
        assertThat(response.getResults().get(0).getFeature().getSymbol().substring(0, 5), equalTo("Psen1"));
    }

    @Test
    public void checkAlleleDiseaseAssociationPhylogeneticSort() {
        Pagination pagination = new Pagination(1, 100, "allele", null);
        // Menkes
        String diseaseID = "DOID:162";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithAlleles(diseaseID, pagination);
        assertLimitResponse(response, 10, 10);
        assertThat(response.getResults().get(0).getFeature().getSpecies().getName(), equalTo("Mus musculus"));
    }

    @Test
    public void checkAlleleDiseaseAssociationFilteredBySpecies() {
        Pagination pagination = new Pagination(1, 100, null, null);
        pagination.addFieldFilter(FieldFilter.ALLELE, "tg");
        // Alzheimer's disease
        String diseaseID = "DOID:10652";
        // filtered by transgenes
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithAlleles(diseaseID, pagination);
        assertLimitResponse(response, 60, 60);

        // filtered by transgenes and mouse species
        pagination.addFieldFilter(FieldFilter.SPECIES, "mus musculus");
        response = diseaseService.getDiseaseAnnotationsWithAlleles(diseaseID, pagination);
        assertLimitResponse(response, 60, 60);
    }

    @Test
    public void checkAlleleDiseaseAssociationFilterSpecies() {
        Pagination pagination = new Pagination(1, 100, null, null);
        pagination.addFieldFilter(FieldFilter.SPECIES, "drosophila melanogaster");
        // Menkes
        String diseaseID = "DOID:10652";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithAlleles(diseaseID, pagination);
        assertLimitResponse(response, 10, 10);
        assertEquals(response.getDistinctFieldValues().size(), 2);
    }

    @Test
    public void checkGeneDiseaseAssociationByDisease() {
        Pagination pagination = new Pagination(1, 100, null, null);
        // Menkes
        String diseaseID = "DOID:1838";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithGenes(diseaseID, pagination);
        assertLimitResponse(response, 20, 20);
    }

    @Test
    public void checkGeneDiseaseAnnotations() {
        Pagination pagination = new Pagination(1, 100, null, null);
        pagination.addFieldFilter(FieldFilter.DISEASE, "acro");
        // acrocephalosyndactylia
        // Missing human and mouse genes
        String diseaseID = "DOID:12960";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithGenes(diseaseID, pagination);
        assertLimitResponse(response, 2, 2);
    }

    @Test
    public void checkAssociatedGenesFly() {
        Pagination pagination = new Pagination(1, 100, null, null);
        BaseFilter baseFilter = new BaseFilter();
        baseFilter.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, "is_implicated_in");
        baseFilter.addFieldFilter(FieldFilter.SPECIES, "Drosophila melanogaster");
        pagination.setFieldFilterValueMap(baseFilter);
        // Menkes
        String diseaseID = "DOID:1838";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithGenes(diseaseID, pagination);
        assertLimitResponse(response, 1, 1);
        assertTrue("At least one PAE", CollectionUtils.isNotEmpty(response.getResults().get(0).getPrimaryAnnotatedEntities()));
    }

    @Test
    public void checkAssociatedGenesOrthology() {
        Pagination pagination = new Pagination(1, 100, null, null);
        BaseFilter baseFilter = new BaseFilter();
        baseFilter.addFieldFilter(FieldFilter.SOURCE, "Alliance");
        baseFilter.addFieldFilter(FieldFilter.GENE_NAME, "atp7a");
        baseFilter.addFieldFilter(FieldFilter.SPECIES, "homo sapiens");
        pagination.setFieldFilterValueMap(baseFilter);
        // Menkes
        String diseaseID = "DOID:1838";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithGenes(diseaseID, pagination);
        assertLimitResponse(response, 1, 1);
        assertTrue("More than one ortholgous gene", CollectionUtils.isNotEmpty(response.getResults().get(0).getOrthologyGenes()));
        assertThat("More than one ortholgous gene", response.getResults().get(0).getOrthologyGenes().size(), greaterThanOrEqualTo(4));
    }

    @Test
    public void checkGetDiseaseAnnotationsWithAGM() {
        Pagination pagination = new Pagination(1, 100, null, null);
        // Menkes
        String diseaseID = "DOID:1838";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithAGM(diseaseID, pagination);
        assertLimitResponse(response, 11, 11);
        assertEquals(response.getDistinctFieldValues().size(), 1);
    }

    @Test
    public void checkGetDiseaseAnnotationsWithAGMAndGene() {
        Pagination pagination = new Pagination(1, 100, null, null);
        // Menkes
        //String geneID = "ZFIN:ZDB-GENE-060825-45";
        String geneID = "MGI:109583";
        JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithGeneAndAGM(geneID, pagination);
        assertLimitResponse(response, 3, 4);
    }

    @Test
    public void checkGetDiseaseAnnotationsWithAGMAndSTRAndGene() {
        Pagination pagination = new Pagination(1, 100, null, null);
        // get fish with 'zf' allele in fish name
        pagination.addFieldFilter(FieldFilter.MODEL_NAME, "ZF");
        // spaw
        String geneID = "ZFIN:ZDB-GENE-030219-1";
        JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithGeneAndAGM(geneID, pagination);
        assertLimitResponse(response, 1, 1);
        assertThat(response.getResults().get(0).getDiseases().stream().map(DOTerm::getName).collect(Collectors.joining()), equalTo("anxiety disorder"));
        assertThat(response.getResults().get(0).getId(), equalTo("ZFIN:ZDB-FISH-160331-6"));
    }

    @Test
    public void checkGetGeneDiseaseTable() {
        Pagination pagination = new Pagination(1, 100, null, null);
        // Menkes
        String diseaseID = "DOID:1838";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithGenes(diseaseID, pagination);
        assertLimitResponse(response, 18, 18);

        // make sure there are multiple orthology genes for HGNC:869
        BaseFilter baseFilter = new BaseFilter();
        baseFilter.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, "implicated_via_orthology");
        pagination.setFieldFilterValueMap(baseFilter);
        response = diseaseService.getDiseaseAnnotationsWithGenes(diseaseID, pagination);
        assertLimitResponse(response, 16, 16);

        response.getResults().stream()
                .filter(annotation -> annotation.getGene().getPrimaryKey().equals("HGNC:869"))
                .forEach(diseaseAnnotation -> {
                    assertThat(4, greaterThanOrEqualTo(diseaseAnnotation.getOrthologyGenes().size()));
                });
    }

    @Test
    public void checkDiseaseAssociationByGene() {
        Pagination pagination = new Pagination(1, 100, null, null);
        String diseaseID = "MGI:107718";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsByDisease(diseaseID, pagination);
        assertLimitResponse(response, 5, 5);
    }

    @Test
    public void checkDiseaseAssociationByDisease() {
        Pagination pagination = new Pagination(1, 33, null, null);
        // choriocarcinoma
        String diseaseID = "DOID:3594";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsByDisease(diseaseID, pagination);
        assertResponse(response, 33, 48);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("choriocarcinoma"));
        assertThat(annotation.getGene().getSymbol(), equalTo("FGF8"));
        assertThat(annotation.getAssociationType().toLowerCase(), equalTo("is_marker_for"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:11764380"));

        annotation = response.getResults().get(1);
        assertThat(annotation.getGene().getSymbol(), equalTo("IGF2"));
        assertNull(annotation.getFeature());
        assertThat(annotation.getDisease().getName(), equalTo("choriocarcinoma"));
        assertThat(annotation.getAssociationType().toLowerCase(), equalTo("is_implicated_in"));

        assertEquals(response.getDistinctFieldValues().size(), 2);
        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getAllRowsForGenes(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        String result = "Species ID\tSpecies Name\tGene ID\tGene Symbol\tGenetic Entity ID\tGenetic Entity Name\tGenetic Entity Type\tAssociation\tDisease ID\tDisease Name\tEvidence Code\tEvidence Code Name\tBased On ID\tBased On Name\tSource\tReference\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:3686\tFGF8\tHGNC:3686\t\tgene\tis_marker_for\tDOID:3594\tchoriocarcinoma\tECO:0000270\texpression pattern evidence used in manual assertion\t\t\tRGD\tPMID:11764380\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:5466\tIGF2\tHGNC:5466\t\tgene\tis_implicated_in\tDOID:3594\tchoriocarcinoma\tECO:0000314\tdirect assay evidence used in manual assertion\t\t\tRGD\tPMID:17556377\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:6091\tINSR\tHGNC:6091\t\tgene\tis_implicated_in\tDOID:3594\tchoriocarcinoma\tECO:0000314\tdirect assay evidence used in manual assertion\t\t\tRGD\tPMID:17556377\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:8800\tPDGFB\tHGNC:8800\t\tgene\tis_marker_for\tDOID:3594\tchoriocarcinoma\tECO:0000270\texpression pattern evidence used in manual assertion\t\t\tRGD\tPMID:8504434\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:8804\tPDGFRB\tHGNC:8804\t\tgene\tis_marker_for\tDOID:3594\tchoriocarcinoma\tECO:0000270\texpression pattern evidence used in manual assertion\t\t\tRGD\tPMID:8504434\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:11822\tTIMP3\tHGNC:11822\t\tgene\tis_marker_for\tDOID:3594\tchoriocarcinoma\tECO:0000270\texpression pattern evidence used in manual assertion\t\t\tRGD\tPMID:15507671\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:70891\tFgf8\tRGD:70891\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:3686\tFGF8\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:2870\tIgf2\tRGD:2870\t\tgene\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:5466\tIGF2\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:2917\tInsr\tRGD:2917\t\tgene\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:6091\tINSR\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:3283\tPdgfb\tRGD:3283\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:8800\tPDGFB\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:3285\tPdgfrb\tRGD:3285\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:8804\tPDGFRB\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:3865\tTimp3\tRGD:3865\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:11822\tTIMP3\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10090\tMus musculus\tMGI:99604\tFgf8\tMGI:99604\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:3686\tFGF8\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10090\tMus musculus\tMGI:96434\tIgf2\tMGI:96434\t\tgene\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:5466\tIGF2\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10090\tMus musculus\tMGI:96575\tInsr\tMGI:96575\t\tgene\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:6091\tINSR\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10090\tMus musculus\tMGI:97528\tPdgfb\tMGI:97528\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:8800\tPDGFB\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10090\tMus musculus\tMGI:97531\tPdgfrb\tMGI:97531\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:8804\tPDGFRB\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10090\tMus musculus\tMGI:98754\tTimp3\tMGI:98754\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:11822\tTIMP3\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7955\tDanio rerio\tZFIN:ZDB-GENE-990415-72\tfgf8a\tZFIN:ZDB-GENE-990415-72\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:3686\tFGF8\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7955\tDanio rerio\tZFIN:ZDB-GENE-010122-1\tfgf8b\tZFIN:ZDB-GENE-010122-1\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:3686\tFGF8\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7955\tDanio rerio\tZFIN:ZDB-GENE-991111-3\tigf2a\tZFIN:ZDB-GENE-991111-3\t\tgene\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:5466\tIGF2\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7955\tDanio rerio\tZFIN:ZDB-GENE-030131-2935\tigf2b\tZFIN:ZDB-GENE-030131-2935\t\tgene\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:5466\tIGF2\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7955\tDanio rerio\tZFIN:ZDB-GENE-020503-3\tinsra\tZFIN:ZDB-GENE-020503-3\t\tgene\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:6091\tINSR\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7955\tDanio rerio\tZFIN:ZDB-GENE-020503-4\tinsrb\tZFIN:ZDB-GENE-020503-4\t\tgene\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:6091\tINSR\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7955\tDanio rerio\tZFIN:ZDB-GENE-050208-525\tpdgfba\tZFIN:ZDB-GENE-050208-525\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:8800\tPDGFB\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7955\tDanio rerio\tZFIN:ZDB-GENE-131121-332\tpdgfbb\tZFIN:ZDB-GENE-131121-332\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:8800\tPDGFB\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7955\tDanio rerio\tZFIN:ZDB-GENE-030805-2\tpdgfrb\tZFIN:ZDB-GENE-030805-2\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:8804\tPDGFRB\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7227\tDrosophila melanogaster\tFB:FBgn0283499\tInR\tFB:FBgn0283499\t\tgene\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:6091\tINSR\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7227\tDrosophila melanogaster\tFB:FBgn0030964\tPvf1\tFB:FBgn0030964\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:8800\tPDGFB\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7227\tDrosophila melanogaster\tFB:FBgn0032006\tPvr\tFB:FBgn0032006\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:8804\tPDGFRB\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:7227\tDrosophila melanogaster\tFB:FBgn0025879\tTimp\tFB:FBgn0025879\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:11822\tTIMP3\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:6239\tCaenorhabditis elegans\tWB:WBGene00019478\tcri-2\tWB:WBGene00019478\t\tgene\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:11822\tTIMP3\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:6239\tCaenorhabditis elegans\tWB:WBGene00000898\tdaf-2\tWB:WBGene00000898\t\tgene\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tevidence used in automatic assertion\tHGNC:6091\tINSR\tAlliance\tMGI:6194238\n";
        assertEquals(result, output);

    }

    @Test
    @Ignore
    public void checkDiseaseAssociationByDiseaseAcuteLymphocyticLeukemia() {
        Pagination pagination = new Pagination(1, 25, null, null);
        // acute lymphocytic lukemia
        String diseaseID = "DOID:9952";

        pagination.setSortBy("associationType");
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsByDisease(diseaseID, pagination);
        assertResponse(response, 25, 74);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("B- and T-cell mixed leukemia"));
        assertThat(annotation.getGene().getSymbol(), equalTo("DOT1L"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:23801631"));

        annotation = response.getResults().get(3);
        assertThat(annotation.getGene().getSymbol(), equalTo("Ezh2"));
        assertNotNull(annotation.getFeature());
        assertThat(annotation.getDisease().getName(), equalTo("acute lymphocytic leukemia"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getAllRowsForGenes(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        String result = "Gene ID\tGene Symbol\tSpecies\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tAssociation Type\tDisease ID\tDisease Name\tEvidence Code\tSource\tReferences\n" +
                "HGNC:24948\tDOT1L\tHomo sapiens\t\t\t\tis_implicated_in\tDOID:9953\tB- and T-cell mixed leukemia\tIDA\tRGD\tPMID:23801631\n" +
                "HGNC:7132\tKMT2A\tHomo sapiens\t\t\t\tis_implicated_in\tDOID:9953\tB- and T-cell mixed leukemia\tIAGP\tRGD\tRGD:7240710\n" +
                "MGI:104518\tCntn2\tMus musculus\t\t\t\tis_implicated_in\tDOID:5602\tT-cell adult acute lymphocytic leukemia\tTAS\tMGI\tPMID:16550188,PMID:25035162\n" +
                "MGI:107940\tEzh2\tMus musculus\tMGI:3823217\tEzh2<sup>tm2Sho</sup>\tallele\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:22431509\n" +
                "MGI:107940\tEzh2\tMus musculus\tMGI:3823218\tEzh2<sup>tm2.1Sho</sup>\tallele\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:22431509\n" +
                "MGI:107940\tEzh2\tMus musculus\t\t\t\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:22431509\n" +
                "MGI:96995\tKmt2a\tMus musculus\tMGI:3814567\tKmt2a<sup>tm1Saam</sup>\tallele\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:18977325\n" +
                "MGI:96995\tKmt2a\tMus musculus\t\t\t\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:18977325\n" +
                "MGI:102811\tLmo2\tMus musculus\t\t\t\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:1461647\n" +
                "MGI:99460\tNotch3\tMus musculus\t\t\t\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:10880446\n" +
                "MGI:109583\tPten\tMus musculus\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:21262837\n" +
                "MGI:109583\tPten\tMus musculus\t\t\t\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:21262837\n" +
                "MGI:1344407\tZeb2\tMus musculus\t\t\t\tis_implicated_in\tDOID:5602\tT-cell adult acute lymphocytic leukemia\tTAS\tMGI\tPMID:25565005\n" +
                "WB:WBGene00000469\tces-2\tCaenorhabditis elegans\tWB:WBVar00089714\tn732\tallele\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tIMP\tWB\tPMID:8700229\n" +
                "WB:WBGene00000469\tces-2\tCaenorhabditis elegans\t\t\t\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tIMP\tWB\tPMID:8700229\n" +
                "HGNC:2172\tCNTN2\tHomo sapiens\t\t\t\timplicated_via_orthology\tDOID:5602\tT-cell adult acute lymphocytic leukemia\tIEA\tAlliance\tMGI:6194238\n" +
                "HGNC:2697\tDBP\tHomo sapiens\t\t\t\timplicated_via_orthology\tDOID:9952\tacute lymphocytic leukemia\tIEA\tAlliance\tMGI:6194238\n" +
                "HGNC:3527\tEZH2\tHomo sapiens\t\t\t\timplicated_via_orthology\tDOID:9952\tacute lymphocytic leukemia\tIEA\tAlliance\tMGI:6194238\n" +
                "HGNC:7132\tKMT2A\tHomo sapiens\t\t\t\timplicated_via_orthology\tDOID:9952\tacute lymphocytic leukemia\tIEA\tAlliance\tMGI:6194238\n" +
                "HGNC:6642\tLMO2\tHomo sapiens\t\t\t\timplicated_via_orthology\tDOID:9952\tacute lymphocytic leukemia\tIEA\tAlliance\tMGI:6194238\n" +
                "HGNC:7883\tNOTCH3\tHomo sapiens\t\t\t\timplicated_via_orthology\tDOID:9952\tacute lymphocytic leukemia\tIEA\tAlliance\tMGI:6194238\n" +
                "HGNC:9588\tPTEN\tHomo sapiens\t\t\t\timplicated_via_orthology\tDOID:9952\tacute lymphocytic leukemia\tIEA\tAlliance\tMGI:6194238\n" +
                "HGNC:11722\tTEF\tHomo sapiens\t\t\t\timplicated_via_orthology\tDOID:9952\tacute lymphocytic leukemia\tIEA\tAlliance\tMGI:6194238\n" +
                "HGNC:14881\tZEB2\tHomo sapiens\t\t\t\timplicated_via_orthology\tDOID:5602\tT-cell adult acute lymphocytic leukemia\tIEA\tAlliance\tMGI:6194238\n" +
                "RGD:3821\tCntn2\tRattus norvegicus\t\t\t\timplicated_via_orthology\tDOID:5602\tT-cell adult acute lymphocytic leukemia\tIEA\tAlliance\tMGI:6194238\n";
        assertEquals(result, output);

    }

    @Test
    @Ignore
    public void checkDiseaseAssociationByDiseaseSorting() {
        Pagination pagination = new Pagination(1, 5, null, null);
        // acute lymphocytic lukemia
        String diseaseID = "DOID:9952";
        pagination.setSortBy("species");
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsByDisease(diseaseID, pagination);

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getAllRowsForGenes(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        String result = "Gene ID\tGene Symbol\tSpecies\tAssociation Type\tDisease ID\tDisease Name\tEvidence Code\tBased On\tSource\tReferences\n" +
                "WB:WBGene00000469\tces-2\tCaenorhabditis elegans\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tECO:0000315\t\tWB\tPMID:8700229\n" +
                "WB:WBGene00000469\tces-2\tCaenorhabditis elegans\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tECO:0000315\t\tWB\tPMID:8700229\n" +
                "WB:WBGene00000913\tdaf-18\tCaenorhabditis elegans\timplicated_via_orthology\tDOID:9952\tacute lymphocytic leukemia\tECO:0000501\tMGI:109583:Pten\tAlliance\tMGI:6194238\n" +
                "WB:WBGene00021474\tdot-1.1\tCaenorhabditis elegans\timplicated_via_orthology\tDOID:9953\tB- and T-cell mixed leukemia\tECO:0000501\tHGNC:24948:DOT1L\tAlliance\tMGI:6194238\n" +
                "WB:WBGene00010067\tdot-1.2\tCaenorhabditis elegans\timplicated_via_orthology\tDOID:9953\tB- and T-cell mixed leukemia\tECO:0000501\tHGNC:24948:DOT1L\tAlliance\tMGI:6194238\n";
        assertEquals(result, output);

        // descending sorting
        pagination.setAsc(false);
        response = diseaseService.getDiseaseAnnotationsByDisease(diseaseID, pagination);

/*
        translator = new DiseaseAnnotationToTdfTranslator();
        output = translator.getAllRowsForGenes(response.getResults());
        lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        result = "Gene ID\tGene Symbol\tSpecies\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tAssociation Type\tDisease ID\tDisease Name\tEvidence Code\tSource\tReferences\n" +
                "HGNC:7132\tKMT2A\tHomo sapiens\t\t\t\tis_implicated_in\tDOID:9953\tB- and T-cell mixed leukemia\tIAGP\tRGD\tRGD:7240710\n" +
                "HGNC:24948\tDOT1L\tHomo sapiens\t\t\t\tis_implicated_in\tDOID:9953\tB- and T-cell mixed leukemia\tIDA\tRGD\tPMID:23801631\n" +
                "MGI:1344407\tZeb2\tMus musculus\t\t\t\tis_implicated_in\tDOID:5602\tT-cell adult acute lymphocytic leukemia\tTAS\tMGI\tPMID:25565005\n" +
                "MGI:109583\tPten\tMus musculus\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:21262837\n" +
                "MGI:109583\tPten\tMus musculus\t\t\t\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:21262837\n";
        assertEquals(result, output);
        pagination.setAsc(true);

        // sort by association type
        pagination.setSortBy("associationType");
        response = diseaseService.getDiseaseAnnotationsByDisease(diseaseID, pagination);

        translator = new DiseaseAnnotationToTdfTranslator();
        output = translator.getAllRowsForGenes(response.getResults());
        lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        result = "Gene ID\tGene Symbol\tSpecies\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tAssociation Type\tDisease ID\tDisease Name\tEvidence Code\tSource\tReferences\n" +
                "HGNC:24948\tDOT1L\tHomo sapiens\t\t\t\tis_implicated_in\tDOID:9953\tB- and T-cell mixed leukemia\tIDA\tRGD\tPMID:23801631\n" +
                "HGNC:7132\tKMT2A\tHomo sapiens\t\t\t\tis_implicated_in\tDOID:9953\tB- and T-cell mixed leukemia\tIAGP\tRGD\tRGD:7240710\n" +
                "MGI:104518\tCntn2\tMus musculus\t\t\t\tis_implicated_in\tDOID:5602\tT-cell adult acute lymphocytic leukemia\tTAS\tMGI\tPMID:16550188,PMID:25035162\n" +
                "MGI:107940\tEzh2\tMus musculus\tMGI:3823218\tEzh2<sup>tm2.1Sho</sup>\tallele\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:22431509\n" +
                "MGI:107940\tEzh2\tMus musculus\tMGI:3823217\tEzh2<sup>tm2Sho</sup>\tallele\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:22431509\n";
        assertEquals(result, output);

        // sort by disease and containsFilterValue.species
        pagination.setSortBy("disease,species");
        response = diseaseService.getDiseaseAnnotationsByDisease(diseaseID, pagination);

        translator = new DiseaseAnnotationToTdfTranslator();
        output = translator.getAllRowsForGenes(response.getResults());
        lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        result = "Gene ID\tGene Symbol\tSpecies\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tAssociation Type\tDisease ID\tDisease Name\tEvidence Code\tSource\tReferences\n" +
                "HGNC:24948\tDOT1L\tHomo sapiens\t\t\t\tis_implicated_in\tDOID:9953\tB- and T-cell mixed leukemia\tIDA\tRGD\tPMID:23801631\n" +
                "HGNC:7132\tKMT2A\tHomo sapiens\t\t\t\tis_implicated_in\tDOID:9953\tB- and T-cell mixed leukemia\tIAGP\tRGD\tRGD:7240710\n" +
                "MGI:107940\tEzh2\tMus musculus\tMGI:3823218\tEzh2<sup>tm2.1Sho</sup>\tallele\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:22431509\n" +
                "MGI:107940\tEzh2\tMus musculus\tMGI:3823217\tEzh2<sup>tm2Sho</sup>\tallele\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:22431509\n" +
                "MGI:107940\tEzh2\tMus musculus\t\t\t\tis_implicated_in\tDOID:9952\tacute lymphocytic leukemia\tTAS\tMGI\tPMID:22431509\n";
        assertEquals(result, output);
*/

    }

    @Test
    public void checkEmpiricalDiseaseByGene() {
        Pagination pagination = new Pagination(1, 2, null, null);
        // Pten
        String geneID = "MGI:109583";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertResponse(response, 2, 14);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("acute lymphocytic leukemia"));
        assertThat(annotation.getAssociationType().toLowerCase(), equalTo("is_implicated_in"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:21262837"));

        annotation = response.getResults().get(1);
        assertThat(annotation.getDisease().getName(), equalTo("autism spectrum disorder"));
        assertThat(annotation.getAssociationType().toLowerCase(), equalTo("is_implicated_in"));
        assertNotNull(annotation.getPrimaryAnnotatedEntities());

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getEmpiricalDiseaseByGene(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
/*
        String result = "Disease\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tAssociation Type\tEvidence Code\tSource\tReferences\n" +
                "acute lymphocytic leukemia\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tECO:0000033\tPMID:21262837\n" +
                "autism spectrum disorder\t\t\tgene\tis_implicated_in\tECO:0000033\tPMID:19208814,PMID:22302806,PMID:25561290\n";

        assertEquals(result, output);
*/

    }

    @Test
    public void checkDiseaseAssociationByGeneMultipleAGM() {
        Pagination pagination = new Pagination(1, 2, null, null);
        // nlg-1
        // has allele and strain AGMs
        String geneID = "WB:WBGene00006412";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertResponse(response, 1, 1);
        final List<PrimaryAnnotatedEntity> primaryAnnotatedEntities = response.getResults().get(0).getPrimaryAnnotatedEntities();
//        assertTrue(primaryAnnotatedEntities.size() > 2);
        assertTrue(primaryAnnotatedEntities.stream().anyMatch(entity -> entity.getType().equals(GeneticEntity.CrossReferenceType.ALLELE)));
        assertTrue(primaryAnnotatedEntities.stream().anyMatch(entity -> entity.getType().equals(GeneticEntity.CrossReferenceType.STRAIN)));
    }

    @Test
    public void checkDiseaseAssociationForYeast() {
        Pagination pagination = new Pagination(1, 10, null, null);
        // FAA1
        String geneID = "SGD:S000005844";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertResponse(response, 2, 2);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("Sjogren-Larsson syndrome"));
        assertThat(annotation.getAssociationType().toLowerCase(), equalTo("is_implicated_in"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:24269233"));
        assertThat(annotation.getOrthologyGenes().stream().map(Gene::getPrimaryKey).collect(Collectors.joining()), equalTo("HGNC:29567HGNC:3570HGNC:3571HGNC:16526HGNC:16496HGNC:10996HGNC:10998"));
        assertThat(annotation.getOrthologyGenes().stream().map(Gene::getSymbol).collect(Collectors.joining(",")), equalTo("ACSBG1,ACSL3,ACSL4,ACSL5,ACSL6,SLC27A2,SLC27A4"));

        annotation = response.getResults().get(1);
        assertThat(annotation.getDisease().getName(), equalTo("Sjogren-Larsson syndrome"));
        assertThat(annotation.getAssociationType().toLowerCase(), equalTo("is_implicated_in"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:22633490"));
        assertThat(annotation.getOrthologyGenes().stream().map(Gene::getPrimaryKey).collect(Collectors.joining()), equalTo("HGNC:3569"));
        assertThat(annotation.getOrthologyGenes().stream().map(Gene::getSymbol).collect(Collectors.joining()), equalTo("ACSL1"));

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getEmpiricalDiseaseByGene(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        assertEquals(output, "Species ID\tSpecies Name\tGene ID\tGene Symbol\tGenetic Entity ID\tGenetic Entity Name\tGenetic Entity Type\tDisease ID\tDisease Name\tAssociation\tEvidence Code\tEvidence Code Name\tSource\tBased On ID\tBased On Name\tReference\n" +
                "NCBITaxon:559292\tSaccharomyces cerevisiae\tSGD:S000005844\tFAA1\tSGD:S000005844\tFAA1\tgene\tDOID:14501\tSjogren-Larsson syndrome\tis_implicated_in\tECO:0000316|ECO:0000250\tgenetic interaction evidence used in manual assertion|sequence similarity evidence used in manual assertion\tSGD\t\t\tPMID:24269233\n" +
                "NCBITaxon:559292\tSaccharomyces cerevisiae\tSGD:S000005844\tFAA1\tSGD:S000005844\tFAA1\tgene\tDOID:14501\tSjogren-Larsson syndrome\tis_implicated_in\tECO:0000316|ECO:0000250\tgenetic interaction evidence used in manual assertion|sequence similarity evidence used in manual assertion\tSGD\t\t\tPMID:22633490\n"
        );
    }

    @Test
    public void checkDiseaseAssociationJoinTypeOrthologous() {
        Pagination pagination = new Pagination(1, 10, null, null);
        // tmc-2
        String geneID = "WB:WBGene00015177";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertResponse(response, 0, 0);

    }

    @Test
    public void checkDiseaseForInference() {
        Pagination pagination = new Pagination(1, 10, null, null);
        // adamts16
        String geneID = "ZFIN:ZDB-GENE-130530-760";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertResponse(response, 1, 1);
        assertTrue("No PAE found for Disease Annotation", CollectionUtils.isNotEmpty(response.getResults().get(0).getPrimaryAnnotatedEntities()));
    }

    @Test
    public void checkEmpiricalDiseaseFilterByDisease() {

        // Pten
        String geneID = "MGI:109583";
        DiseaseSummary summary = diseaseService.getDiseaseSummary(geneID, DiseaseSummary.Type.EXPERIMENT);
        assertNotNull(summary);
        assertThat(50L, equalTo(summary.getNumberOfAnnotations()));
        assertThat(14L, equalTo(summary.getNumberOfEntities()));

        summary = diseaseService.getDiseaseSummary(geneID, DiseaseSummary.Type.ORTHOLOGY);
        assertNotNull(summary);
        assertThat(summary.getNumberOfAnnotations(), greaterThan(30L));
        assertThat(summary.getNumberOfEntities(), greaterThan(27L));

        Pagination pagination = new Pagination(1, null, null, null);
        // add containsFilterValue on disease
        pagination.makeSingleFieldFilter(FieldFilter.DISEASE, "BL");
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertResponse(response, 1, 1);


        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("urinary bladder cancer"));
        assertThat(annotation.getAssociationType().toLowerCase(), equalTo("is_implicated_in"));
        //assertThat(annotation.getFeature().getSymbol(), equalTo("Pten<sup>tm1Hwu</sup>"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:16951148"));

    }

    @Test
    public void checkEmpiricalDiseaseFilterByAssociation() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "HGNC:3686";
        DiseaseSummary summary = diseaseService.getDiseaseSummary(geneID, DiseaseSummary.Type.EXPERIMENT);
        assertNotNull(summary);
        assertThat(summary.getNumberOfAnnotations(), greaterThan(6L));
        assertThat(summary.getNumberOfEntities(), greaterThanOrEqualTo(6L));

        summary = diseaseService.getDiseaseSummary(geneID, DiseaseSummary.Type.ORTHOLOGY);
        assertNotNull(summary);
        assertThat(3L, equalTo(summary.getNumberOfAnnotations()));
        assertThat(3L, equalTo(summary.getNumberOfEntities()));

        // add containsFilterValue on feature symbol
        pagination.makeSingleFieldFilter(FieldFilter.ASSOCIATION_TYPE, "IS_IMPLICATED_IN");
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertResponse(response, 2, 2);

        DiseaseAnnotation annotation = response.getResults().get(1);
        assertThat(annotation.getDisease().getName(), equalTo("prostate carcinoma in situ"));
        assertThat(annotation.getAssociationType().toLowerCase(), equalTo("is_implicated_in"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:12208767"));

        pagination.makeSingleFieldFilter(FieldFilter.ASSOCIATION_TYPE, "IS_MARKER_FOR");
        response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertLimitResponse(response, 4, 4);
        assertNull(annotation.getFeature());
    }

    @Test
    public void checkEmpiricalDiseaseFilterByEvidence() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "HGNC:3686";

        // add containsFilterValue on evidence code
        pagination.makeSingleFieldFilter(FieldFilter.EVIDENCE_CODE, "expression pattern");
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertLimitResponse(response, 4, 4);

        pagination.makeSingleFieldFilter(FieldFilter.EVIDENCE_CODE, "inference by association");
        response = diseaseService.getDiseaseAnnotations(geneID, pagination);
        assertResponse(response, 1, 1);
    }

    @Test
    public void checkDiseaseAnnotationWithOrthology() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Tmc1
        String geneID = "MGI:2151016";

        // add containsFilterValue on evidence code
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertLimitResponse(response, 2, 2);

    }

    @Test
    public void checkEmpiricalDiseaseFilterByPublication() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "HGNC:3686";

        // add filter on reference
        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "380");
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertResponse(response, 3, 3);

        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "710");
        response = diseaseService.getDiseaseAnnotations(geneID, pagination);
        assertResponse(response, 1, 1);
    }

    @Test
    public void checkGeneAnnotationReferences() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Ccm2
        String geneID = "MGI:2384924";

        // add filter on reference
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertResponse(response, 1, 1);

        Set<String> pubIds = response.getResults().get(0).getPublicationJoins().stream()
                .map(join -> join.getPublication().getPubId())
                .collect(Collectors.toSet());

        assertFalse(pubIds.contains("PMID:25486933"));
    }

    @Test
    public void checkSingleDiseaseTerm() {
        DiseaseService service = new DiseaseService();
        DOTerm term = service.getById("DOID:3594");
        assertNotNull(term);
        assertThat(term.getName(), equalTo("choriocarcinoma"));
        assertThat(term.getSynonyms().stream().map(Synonym::getPrimaryKey).collect(Collectors.toList()), containsInAnyOrder("Chorioepithelioma"));
        assertThat(term.getChildren().size(), greaterThanOrEqualTo(8));
        assertThat(term.getParents().size(), equalTo(1));
        assertThat(term.getDefLinks().size(), equalTo(1));
    }

    @Test
    public void checkDiseaseOnZFIN() {
        // atp7a
        JsonResultResponse<DiseaseAnnotation> response = service.getRibbonDiseaseAnnotations(List.of("ZFIN:ZDB-GENE-060825-45"), null, new Pagination(1, 30, null, null));
        assertNotNull(response);
        // no gene-level disease annotation
        assertEquals(response.getTotal(), 0);
    }

    @Test
    // Test SHH from Human for disease via experiment records
    public void checkDiseaseAnnotationNonDuplicated3() {
        JsonResultResponse<DiseaseAnnotation> annotations = service.getRibbonDiseaseAnnotations(List.of("HGNC:10848"), null, new Pagination(1, 30, null, null));

        assertNotNull(annotations);
        // 14 different disease terms
        assertThat(annotations.getTotal(), greaterThanOrEqualTo(14));
        // pick autism spectrum disorder
        // one record (no duplication
        List<DiseaseAnnotation> annots = annotations.getResults().stream().filter(diseaseDocument -> diseaseDocument.getDisease().getName().equals("autism spectrum disorder")).collect(Collectors.toList());
        assertThat(1, equalTo(annots.size()));

    }

    @Test
    // Test Sox9 from MGI for disease via experiment records
    public void checkDiseaseAnnotationNonDuplicated() {
        JsonResultResponse<DiseaseAnnotation> annotations = service.getRibbonDiseaseAnnotations(List.of("MGI:98371"), null, new Pagination(1, 80, null, null));
        assertNotNull(annotations);

        assertThat(1, equalTo(annotations.getTotal()));
    }


    @Test
    // Test Sox9 from MGI for disease via experiment records
    public void checkDiseaseRibbonHeader() {
        DiseaseRibbonSummary summary = diseaseService.getDiseaseRibbonSummary(List.of("MGI:98297"));
        assertNotNull(summary);
    }

    @Test
    public void checkUrlForAllelesInPopup() {

        // cua-1
        String geneID = "FB:FBgn0030343";

        Pagination pagination = new Pagination(1, 10, null, null);
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(geneID), null, pagination);
        assertResponse(response, 1, 1);

        response.getResults().forEach(annotation -> {
            annotation.getPrimaryAnnotatedEntities().forEach(entity -> {
                assertNotNull("URL for AGM should not be null: " + entity.getId(), entity.getUrl());
                assertTrue("URL for AGM should not be empty: " + entity.getId(), StringUtils.isNotEmpty(entity.getUrl()));
            });
        });
    }

    @Test
    public void diseaseModelDownload() {

        // Diamond-Blackfan anemia
        String diseaseID = "DOID:1838";

        JsonResultResponse<DiseaseAnnotation> response = diseaseController.getDiseaseAnnotationsForModel(diseaseID,
                15,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertResponse(response, 15, 17);

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getAllRowsForModel(response.getResults());
        assertEquals(output, "Model ID\tModel Symbol\tSpecies ID\tSpecies Name\tDisease ID\tDisease Name\tEvidence Code\tEvidence Code Name\tSource\tReference\n" +
                "MGI:6324209\tAtp7a<Mo-blo>/? [background:] involves: C57BL/6J\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:6685755\n" +
                "MGI:6324210\tAtp7a<Mo-blo>/Atp7a<+> [background:] involves: C57BL/6J\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:6685755\n" +
                "MGI:3793780\tAtp7a<Mo-br>/? [background:] involves: C57BL\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:4858102\n" +
                "MGI:5696621\tAtp7a<Mo-dp>/? [background:] involves: 101/H * C3H/HeH\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:25456742\n" +
                "MGI:5696613\tAtp7a<Mo-dp>/Atp7a<+> [background:] involves: 101/H * C3H/HeH\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:25456742\n" +
                "MGI:6324231\tAtp7a<Mo-ml>/? [background:] involves: C3Hf/He\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tMGI:60964\n" +
                "MGI:6324231\tAtp7a<Mo-ml>/? [background:] involves: C3Hf/He\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:1819648\n" +
                "MGI:4940051\tAtp7a<Mo-ms>/? [background:] Not Specified\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:20831904\n" +
                "MGI:3618244\tAtp7a<Mo-Tohm>/Atp7a<+> [background:] B6.Cg-Atp7a<Mo-Tohm>\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:16338116\n" +
                "MGI:3793729\tAtp7a<Mo-vbr>/? [background:] Not Specified\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:10098864\n" +
                "MGI:2175712\tAtp7a<Mo>/Atp7a<+> [background:] Not Specified\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:13103353\n" +
                "MGI:2657020\tLox<tm1Ikh>/Lox<tm1Ikh> [background:] involves: 129X1/SvJ * C57BL/6J\tNCBITaxon:10090\tMus musculus\tDOID:1838\tMenkes disease\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:12473682\n" +
                "ZFIN:ZDB-FISH-180905-22\tatp7a<gw71/gw71>\tNCBITaxon:7955\tDanio rerio\tDOID:1838\tMenkes disease\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:29507920\n" +
                "ZFIN:ZDB-FISH-150901-6650\tatp7a<j246/j246>\tNCBITaxon:7955\tDanio rerio\tDOID:1838\tMenkes disease\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:18316734\n" +
                "ZFIN:ZDB-FISH-150901-17526\tatp7a<vu69/vu69>\tNCBITaxon:7955\tDanio rerio\tDOID:1838\tMenkes disease\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:16890543\n" +
                "ZFIN:ZDB-FISH-150901-27568\tatp7a<vu69/vu69>\tNCBITaxon:7955\tDanio rerio\tDOID:1838\tMenkes disease\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:18316734\n");

        diseaseID = "DOID:1324";

        response = diseaseController.getDiseaseAnnotationsForModel(diseaseID,
                100,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        int rowSize = translator.getDiseaseModelDownloadRows(response.getResults()).size();
        assertNotNull(response);
        assertThat(rowSize, greaterThan(response.getTotal()));
    }

    @Test
    public void diseaseGeneAnnotations() {

        String pten = "MGI:109583";
        final Pagination pagination = new Pagination();
        pagination.addFieldFilter(FieldFilter.DISEASE, "BL");
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(List.of(pten), null, pagination);
        assertEquals(1, response.getTotal());
        DiseaseAnnotation annotation = response.getResults().get(0);
        assertEquals(annotation.getPrimaryAnnotatedEntities().size(), 1);
        assertEquals(annotation.getPrimaryAnnotatedEntities().get(0).getId(), "MGI:5004866");
        // do not use the AGM that is inference for an allele annotation
        assertFalse(annotation.getPrimaryAnnotatedEntities().stream().anyMatch(entity -> entity.getId().equals("MGI:3844324")));
    }

    @Test
    public void diseaseGeneDownload() {

        // Diamond-Blackfan anemia
        String diseaseID = "DOID:1838";

        JsonResultResponse<DiseaseAnnotation> response = diseaseController.getDiseaseAnnotationsByGene(diseaseID,
                7,
                1,
                null,
                null,
                null,
                null,
                "Alliance",
                null,
                null,
                null,
                null
        );

        assertResponse(response, 7, 20);

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getAllRowsForGenes(response.getResults());
        assertEquals(output, "Species ID\tSpecies Name\tGene ID\tGene Symbol\tGenetic Entity ID\tGenetic Entity Name\tGenetic Entity Type\tAssociation\tDisease ID\tDisease Name\tEvidence Code\tEvidence Code Name\tBased On ID\tBased On Name\tSource\tReference\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:869\tATP7A\tHGNC:869\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tFB:FBgn0030343\tATP7\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:869\tATP7A\tHGNC:869\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tMGI:99400\tAtp7a\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:869\tATP7A\tHGNC:869\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tSGD:S000002678\tCCC2\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:869\tATP7A\tHGNC:869\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tWB:WBGene00000834\tcua-1\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:870\tATP7B\tHGNC:870\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tFB:FBgn0030343\tATP7\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:870\tATP7B\tHGNC:870\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tSGD:S000002678\tCCC2\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:870\tATP7B\tHGNC:870\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tWB:WBGene00000834\tcua-1\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:6664\tLOX\tHGNC:6664\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tMGI:96817\tLox\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:9606\tHomo sapiens\tHGNC:11017\tSLC31A2\tHGNC:11017\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tSGD:S000006328\tCTR1\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:2179\tAtp7a\tRGD:2179\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tFB:FBgn0030343\tATP7\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:2179\tAtp7a\tRGD:2179\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tHGNC:869\tATP7A\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:2179\tAtp7a\tRGD:2179\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tMGI:99400\tAtp7a\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:2179\tAtp7a\tRGD:2179\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tSGD:S000002678\tCCC2\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:2179\tAtp7a\tRGD:2179\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tWB:WBGene00000834\tcua-1\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:2180\tAtp7b\tRGD:2180\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tFB:FBgn0030343\tATP7\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:2180\tAtp7b\tRGD:2180\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tSGD:S000002678\tCCC2\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:2180\tAtp7b\tRGD:2180\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tWB:WBGene00000834\tcua-1\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:3015\tLox\tRGD:3015\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tHGNC:6664\tLOX\tAlliance\tMGI:6194238\n" +
                "NCBITaxon:10116\tRattus norvegicus\tRGD:3015\tLox\tRGD:3015\t\tgene\timplicated_via_orthology\tDOID:1838\tMenkes disease\tECO:0000501\tevidence used in automatic assertion\tMGI:96817\tLox\tAlliance\tMGI:6194238\n");

        diseaseID = "DOID:1324";

        response = diseaseController.getDiseaseAnnotationsByGene(diseaseID,
                2000,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        int rowSize = translator.getDownloadRowsFromAnnotations(response.getResults()).size();
        assertNotNull(response);
        assertThat(rowSize, greaterThan(response.getTotal()));
    }

    @Test
    public void diseaseAlleleDownload() {

        // Diamond-Blackfan anemia
        String diseaseID = "DOID:1339";

        Pagination pagination = new Pagination(1, 10, null, null);

        JsonResultResponse<DiseaseAnnotation> response = diseaseController.getDiseaseAnnotationsByAllele(diseaseID,
                10,
                1,
                "DiseaseAlleleDefault",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertResponse(response, 6, 6);

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getAllRowsForAllele(response.getResults());
        assertEquals(output, "Allele ID\tAllele Symbol\tGenetic Entity ID\tGenetic Entity Name\tGenetic Entity Type\tSpecies ID\tSpecies Name\tAssociation\tDisease ID\tDisease Name\tEvidence Code\tEvidence Code Name\tSource\tReference\n" +
                "MGI:3776022\tFlvcr1<tm1.1Jlab>\tMGI:3807528\tFlvcr1<tm1.1Jlab>/Flvcr1<tm1.1Jlab> [background:] involves: 129S4/SvJae * C57BL/6 * DBA/2\tgenotype\tNCBITaxon:10090\tMus musculus\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:18258918\n" +
                "MGI:3776021\tFlvcr1<tm1Jlab>\tMGI:3807529\tFlvcr1<tm1Jlab>/Flvcr1<tm1Jlab> Tg(Mx1-cre)1Cgn/? [background:] involves: 129S4/SvJae * C57BL/6 * CBA\tgenotype\tNCBITaxon:10090\tMus musculus\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:18258918\n" +
                "MGI:3803603\tRpsa<tm1Ells>\tMGI:3804635\tRpsa<tm1Ells>/Rpsa<+> [background:] involves: 129S6/SvEvTac * C57BL/6\tgenotype\tNCBITaxon:10090\tMus musculus\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tMGI:3804630\n" +
                "MGI:4839313\tTg(CAG-RPS19*R62W)#Dmb\tMGI:4839332\tTg(CAG-RPS19*R62W)#Dmb/? Tg(Prnp-GFP/cre)1Blw/? [background:] involves: 129S6/SvEvTac * FVB/N\tgenotype\tNCBITaxon:10090\tMus musculus\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000033\tauthor statement supported by traceable reference\tMGI\tPMID:20606162\n" +
                "ZFIN:ZDB-ALT-041001-12\thi3820bTg\tZFIN:ZDB-FISH-150901-16866\trpl11<hi3820bTg/hi3820bTg>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:24812435\n" +
                "ZFIN:ZDB-ALT-041001-12\thi3820bTg\tZFIN:ZDB-FISH-150901-16866\trpl11<hi3820bTg/hi3820bTg>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:25058426\n" +
                "ZFIN:ZDB-ALT-041001-12\thi3820bTg\tZFIN:ZDB-FISH-150901-8506\trpl11<hi3820bTg/hi3820bTg>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:26109203\n" +
                "ZFIN:ZDB-ALT-041001-12\thi3820bTg\tZFIN:ZDB-FISH-150901-8506\trpl11<hi3820bTg/hi3820bTg>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:29225165\n" +
                "ZFIN:ZDB-ALT-041001-12\thi3820bTg\tZFIN:ZDB-FISH-150901-8506\trpl11<hi3820bTg/hi3820bTg>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:29581525\n" +
                "ZFIN:ZDB-ALT-151012-9\tzf556\tZFIN:ZDB-FISH-151013-1\trps19<zf556/zf556>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:25058426\n" +
                "ZFIN:ZDB-ALT-151012-9\tzf556\tZFIN:ZDB-FISH-151013-1\trps19<zf556/zf556>\tfish\tNCBITaxon:7955\tDanio rerio\tis_implicated_in\tDOID:1339\tDiamond-Blackfan anemia\tECO:0000304\tauthor statement supported by traceable reference used in manual assertion\tZFIN\tPMID:26109203\n");

        // Menkes Disease
        diseaseID = "DOID:1838";
        response = diseaseController.getDiseaseAnnotationsByAllele(diseaseID,
                10,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertResponse(response, 10, 16);

        translator = new DiseaseAnnotationToTdfTranslator();
        output = translator.getAllRowsForAllele(response.getResults());
    }


    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        ConfigHelper.init();

        //String str = translator.getAllRowsForGenes(service.getDiseaseAnnotationsDownload("DOID:9351", Pagination.getDownloadPagination()));
        Pagination pagination = new Pagination(1, 20, "gene", "true");
        pagination.addFieldFilter(FieldFilter.GENE_NAME, "l");
//        System.out.println("Number of results " + response.getTotal());

        pagination = new Pagination(1, Integer.MAX_VALUE, null, null);
        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        DiseaseService diseaseService = new DiseaseService();
        System.out.println(translator.getAllRowsForGenes(diseaseService.getDiseaseAnnotationsByDisease("DOID:3594", pagination).getResults()));

    }

    private void assertResponse(JsonResultResponse response, int resultSize, int totalSize) {
        assertNotNull(response);
        assertThat("Number of returned records", response.getResults().size(), equalTo(resultSize));
        assertThat("Number of total records", response.getTotal(), equalTo(totalSize));
    }

    private void assertLimitResponse(JsonResultResponse response, int resultSize, int totalSize) {
        assertNotNull(response);
        assertThat("Number of returned records", response.getResults().size(), greaterThanOrEqualTo(resultSize));
        assertThat("Number of total records", response.getTotal(), greaterThanOrEqualTo(totalSize));
    }


}
