package org.alliancegenome.api.tests.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCacheManager;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.entity.node.Synonym;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@Log4j2
public class DiseaseIT {

    private ObjectMapper mapper = new ObjectMapper();
    private DiseaseService diseaseService = new DiseaseService();

    @Before
    public void before() {
        //Configurator.setRootLevel(Level.WARN);
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
    public void checkGetDiseaseAnnotationsWithAGM() {
        Pagination pagination = new Pagination(1, 100, null, null);
        // Menkes
        String diseaseID = "DOID:1838";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithAGM(diseaseID, pagination);
        assertLimitResponse(response, 11, 11);
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
    @Ignore
    public void checkDiseaseAssociationByDisease() {
        Pagination pagination = new Pagination(1, 100, null, null);
        // choriocarcinoma
        String diseaseID = "DOID:3594";
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsByDisease(diseaseID, pagination);
        assertResponse(response, 35, 35);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("choriocarcinoma"));
        assertThat(annotation.getGene().getSymbol(), equalTo("FGF8"));
        assertThat(annotation.getAssociationType(), equalTo("is_marker_of"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:11764380"));

        annotation = response.getResults().get(1);
        assertThat(annotation.getGene().getSymbol(), equalTo("IGF2"));
        assertNull(annotation.getFeature());
        assertThat(annotation.getDisease().getName(), equalTo("choriocarcinoma"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getAllRows(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        String result = "Gene ID\tGene Symbol\tSpecies\tAssociation Type\tDisease ID\tDisease Name\tEvidence Code\tBased On\tSource\tReferences\n" +
                "HGNC:3686\tFGF8\tHomo sapiens\tis_marker_of\tDOID:3594\tchoriocarcinoma\tECO:0000270\t\tRGD\tPMID:11764380\n" +
                "HGNC:5466\tIGF2\tHomo sapiens\tis_implicated_in\tDOID:3594\tchoriocarcinoma\tECO:0000314\t\tRGD\tPMID:17556377\n" +
                "HGNC:6091\tINSR\tHomo sapiens\tis_implicated_in\tDOID:3594\tchoriocarcinoma\tECO:0000314\t\tRGD\tPMID:17556377\n" +
                "HGNC:8800\tPDGFB\tHomo sapiens\tis_marker_of\tDOID:3594\tchoriocarcinoma\tECO:0000270\t\tRGD\tPMID:8504434\n" +
                "HGNC:8804\tPDGFRB\tHomo sapiens\tis_marker_of\tDOID:3594\tchoriocarcinoma\tECO:0000270\t\tRGD\tPMID:8504434\n" +
                "HGNC:11822\tTIMP3\tHomo sapiens\tis_marker_of\tDOID:3594\tchoriocarcinoma\tECO:0000270\t\tRGD\tPMID:15507671\n" +
                "RGD:70891\tFgf8\tRattus norvegicus\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:3686:FGF8\tAlliance\tMGI:6194238\n" +
                "RGD:2870\tIgf2\tRattus norvegicus\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:5466:IGF2\tAlliance\tMGI:6194238\n" +
                "RGD:2917\tInsr\tRattus norvegicus\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:6091:INSR\tAlliance\tMGI:6194238\n" +
                "RGD:3283\tPdgfb\tRattus norvegicus\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:8800:PDGFB\tAlliance\tMGI:6194238\n" +
                "RGD:3285\tPdgfrb\tRattus norvegicus\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:8804:PDGFRB\tAlliance\tMGI:6194238\n" +
                "RGD:3865\tTimp3\tRattus norvegicus\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:11822:TIMP3\tAlliance\tMGI:6194238\n" +
                "MGI:99604\tFgf8\tMus musculus\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:3686:FGF8\tAlliance\tMGI:6194238\n" +
                "MGI:96434\tIgf2\tMus musculus\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:5466:IGF2\tAlliance\tMGI:6194238\n" +
                "MGI:96575\tInsr\tMus musculus\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:6091:INSR\tAlliance\tMGI:6194238\n" +
                "MGI:97528\tPdgfb\tMus musculus\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:8800:PDGFB\tAlliance\tMGI:6194238\n" +
                "MGI:97531\tPdgfrb\tMus musculus\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:8804:PDGFRB\tAlliance\tMGI:6194238\n" +
                "MGI:98754\tTimp3\tMus musculus\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:11822:TIMP3\tAlliance\tMGI:6194238\n" +
                "ZFIN:ZDB-GENE-990415-72\tfgf8a\tDanio rerio\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:3686:FGF8\tAlliance\tMGI:6194238\n" +
                "ZFIN:ZDB-GENE-010122-1\tfgf8b\tDanio rerio\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:3686:FGF8\tAlliance\tMGI:6194238\n" +
                "ZFIN:ZDB-GENE-991111-3\tigf2a\tDanio rerio\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:5466:IGF2\tAlliance\tMGI:6194238\n" +
                "ZFIN:ZDB-GENE-030131-2935\tigf2b\tDanio rerio\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:5466:IGF2\tAlliance\tMGI:6194238\n" +
                "ZFIN:ZDB-GENE-020503-3\tinsra\tDanio rerio\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:6091:INSR\tAlliance\tMGI:6194238\n" +
                "ZFIN:ZDB-GENE-020503-4\tinsrb\tDanio rerio\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:6091:INSR\tAlliance\tMGI:6194238\n" +
                "ZFIN:ZDB-GENE-050208-525\tpdgfba\tDanio rerio\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:8800:PDGFB\tAlliance\tMGI:6194238\n" +
                "ZFIN:ZDB-GENE-131121-332\tpdgfbb\tDanio rerio\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:8800:PDGFB\tAlliance\tMGI:6194238\n" +
                "ZFIN:ZDB-GENE-030805-2\tpdgfrb\tDanio rerio\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:8804:PDGFRB\tAlliance\tMGI:6194238\n" +
                "FB:FBgn0283499\tInR\tDrosophila melanogaster\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:6091:INSR\tAlliance\tMGI:6194238\n" +
                "FB:FBgn0030964\tPvf1\tDrosophila melanogaster\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:8800:PDGFB\tAlliance\tMGI:6194238\n" +
                "FB:FBgn0032006\tPvr\tDrosophila melanogaster\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:8804:PDGFRB\tAlliance\tMGI:6194238\n" +
                "FB:FBgn0025879\tTimp\tDrosophila melanogaster\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:11822:TIMP3\tAlliance\tMGI:6194238\n" +
                "WB:WBGene00019478\tcri-2\tCaenorhabditis elegans\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:11822:TIMP3\tAlliance\tMGI:6194238\n" +
                "WB:WBGene00000898\tdaf-2\tCaenorhabditis elegans\timplicated_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:6091:INSR\tAlliance\tMGI:6194238\n" +
                "WB:WBGene00004249\tpvf-1\tCaenorhabditis elegans\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:8800:PDGFB\tAlliance\tMGI:6194238\n" +
                "WB:WBGene00019476\ttimp-1\tCaenorhabditis elegans\tbiomarker_via_orthology\tDOID:3594\tchoriocarcinoma\tECO:0000501\tHGNC:11822:TIMP3\tAlliance\tMGI:6194238\n";
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
        String output = translator.getAllRows(response.getResults());
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
        String output = translator.getAllRows(response.getResults());
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
        output = translator.getAllRows(response.getResults());
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
        output = translator.getAllRows(response.getResults());
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
        output = translator.getAllRows(response.getResults());
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
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(Collections.singletonList(geneID), null, pagination);
        assertResponse(response, 2, 14);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("acute lymphocytic leukemia"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:21262837"));

        annotation = response.getResults().get(1);
        assertThat(annotation.getDisease().getName(), equalTo("autism spectrum disorder"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
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
    public void checkEmpiricalDiseaseFilterByDisease() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "MGI:109583";
        // add containsFilterValue on disease
        pagination.makeSingleFieldFilter(FieldFilter.DISEASE, "BL");
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(Collections.singletonList(geneID), null, pagination);
        assertResponse(response, 1, 1);

        DiseaseSummary summary = diseaseService.getDiseaseSummary(geneID, DiseaseSummary.Type.EXPERIMENT);
        assertNotNull(summary);
        assertThat(50L, equalTo(summary.getNumberOfAnnotations()));
        assertThat(14L, equalTo(summary.getNumberOfEntities()));

        summary = diseaseService.getDiseaseSummary(geneID, DiseaseSummary.Type.ORTHOLOGY);
        assertNotNull(summary);
        assertThat(summary.getNumberOfAnnotations(), greaterThan(30L));
        assertThat(summary.getNumberOfEntities(), greaterThan(27L));

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("urinary bladder cancer"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
        assertNotNull(annotation.getFeature());
        //assertThat(annotation.getFeature().getSymbol(), equalTo("Pten<sup>tm1Hwu</sup>"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:16951148PMID:19261747PMID:21283818PMID:25533675PMID:28082400"));

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
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(Collections.singletonList(geneID), null, pagination);
        assertResponse(response, 2, 2);

        DiseaseAnnotation annotation = response.getResults().get(1);
        assertThat(annotation.getDisease().getName(), equalTo("prostate carcinoma in situ"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:12208767"));

        pagination.makeSingleFieldFilter(FieldFilter.ASSOCIATION_TYPE, "IS_MARKER_OF");
        response = diseaseService.getRibbonDiseaseAnnotations(Collections.singletonList(geneID), null, pagination);
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
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(Collections.singletonList(geneID), null, pagination);
        assertLimitResponse(response, 4, 4);

        pagination.makeSingleFieldFilter(FieldFilter.EVIDENCE_CODE, "inference by association");
        response = diseaseService.getDiseaseAnnotations(geneID, pagination);
        assertResponse(response, 1, 1);
    }

    @Test
    public void checkEmpiricalDiseaseFilterByPublication() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "HGNC:3686";

        // add filter on reference
        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "380");
        JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(Collections.singletonList(geneID), null, pagination);
        assertResponse(response, 3, 3);

        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "710");
        response = diseaseService.getDiseaseAnnotations(geneID, pagination);
        assertResponse(response, 1, 1);
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
    // Test SHH from Human for disease via experiment records
    public void checkDiseaseAnnotationNonDuplicated3() {
        DiseaseService service = new DiseaseService();
        JsonResultResponse<DiseaseAnnotation> annotations = service.getRibbonDiseaseAnnotations(Collections.singletonList("HGNC:10848"), null, new Pagination(1, 30, null, null));

        assertNotNull(annotations);
        // 14 different disease terms
        assertThat(annotations.getTotal(), equalTo(14));
        // pick autism spectrum disorder
        // one record (no duplication
        List<DiseaseAnnotation> annots = annotations.getResults().stream().filter(diseaseDocument -> diseaseDocument.getDisease().getName().equals("autism spectrum disorder")).collect(Collectors.toList());
        assertThat(1, equalTo(annots.size()));

    }

    @Test
    // Test Sox9 from MGI for disease via experiment records
    public void checkDiseaseAnnotationNonDuplicated() {
        DiseaseService service = new DiseaseService();
        JsonResultResponse<DiseaseAnnotation> annotations = service.getRibbonDiseaseAnnotations(Collections.singletonList("MGI:98371"), null, new Pagination(1, 80, null, null));
        assertNotNull(annotations);

        assertThat(1, equalTo(annotations.getTotal()));
        // just one annotation
        assertThat(annotations.getResults().stream().filter(annotationDocument -> annotationDocument.getFeature() != null).count(), equalTo(1L));

    }


    @Test
    // Test Sox9 from MGI for disease via experiment records
    public void checkDiseaseRibbonHeader() {
        DiseaseRibbonSummary summary = diseaseService.getDiseaseRibbonSummary(Collections.singletonList("MGI:98297"));
        assertNotNull(summary);
    }

    @Test
    // Test Sox9 from MGI for disease via experiment records
    public void checkStatus() {
        BasicCacheManager<CacheStatus> basicManager = new BasicCacheManager<>();
        CacheStatus status = basicManager.getCache(CacheAlliance.DISEASE_ANNOTATION.getCacheName(), CacheAlliance.CACHING_STATS);
        assertNotNull(status);
        Map<String, CacheStatus> map = basicManager.getAllCacheEntries(CacheAlliance.CACHING_STATS);
        assertNotNull(map);

    }


    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        ConfigHelper.init();

        //String str = translator.getAllRows(service.getDiseaseAnnotationsDownload("DOID:9351", Pagination.getDownloadPagination()));
        Pagination pagination = new Pagination(1, 20, "gene", "true");
        pagination.addFieldFilter(FieldFilter.GENE_NAME, "l");
//        System.out.println("Number of results " + response.getTotal());

        pagination = new Pagination(1, Integer.MAX_VALUE, null, null);
        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        DiseaseService diseaseService = new DiseaseService();
        System.out.println(translator.getAllRows(diseaseService.getDiseaseAnnotationsByDisease("DOID:3594", pagination).getResults()));

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
