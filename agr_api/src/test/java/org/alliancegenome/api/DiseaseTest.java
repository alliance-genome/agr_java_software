package org.alliancegenome.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.es.index.site.dao.DiseaseDAO;
import org.alliancegenome.es.index.site.dao.GeneDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

public class DiseaseTest {

    private static Logger log = Logger.getLogger(DiseaseTest.class);

    private ObjectMapper mapper = new ObjectMapper();
    private GeneDAO geneDAO = new GeneDAO();

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new OrthologyModule());
    }

    @Test
    public void checkEmpiricalDiseaseByGene() {
        Pagination pagination = new Pagination(1, 10, null, null);
        // Pten
        String geneID = "MGI:109583";
        JsonResultResponse<DiseaseAnnotation> response = geneDAO.getEmpiricalDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 10, 50);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("acute lymphocytic leukemia"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
        assertNotNull(annotation.getFeature());
        assertThat(annotation.getFeature().getSymbol(), equalTo("Pten<sup>tm1Hwu</sup>"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:21262837"));

        annotation = response.getResults().get(1);
        assertNull(annotation.getFeature());
        assertThat(annotation.getDisease().getName(), equalTo("acute lymphocytic leukemia"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getEmpiricalDiseaseByGene(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        String result = "Disease\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tAssociation Type\tEvidence Code\tSource\tReferences\n" +
                "acute lymphocytic leukemia\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:21262837\n" +
                "acute lymphocytic leukemia\t\t\tgene\tis_implicated_in\tTAS\tPMID:21262837\n" +
                "autistic disorder\tMGI:2151804\tPten<sup>tm1Rps</sup>\tallele\tis_implicated_in\tTAS\tPMID:23142422,PMID:25561290,PMID:19208814\n" +
                "autistic disorder\tMGI:2679886\tPten<sup>tm2.1Ppp</sup>\tallele\tis_implicated_in\tTAS\tPMID:22302806\n" +
                "autistic disorder\t\t\tgene\tis_implicated_in\tTAS\tPMID:22302806,PMID:25561290\n" +
                "Bannayan-Riley-Ruvalcaba syndrome\tMGI:1857937\tPten<sup>tm1Mak</sup>\tallele\tis_implicated_in\tTAS\tPMID:10910075\n" +
                "Bannayan-Riley-Ruvalcaba syndrome\tMGI:1857936\tPten<sup>tm1Ppp</sup>\tallele\tis_implicated_in\tTAS\tPMID:9697695\n" +
                "Bannayan-Riley-Ruvalcaba syndrome\tMGI:2151804\tPten<sup>tm1Rps</sup>\tallele\tis_implicated_in\tTAS\tPMID:9990064,PMID:27889578\n" +
                "Bannayan-Riley-Ruvalcaba syndrome\t\t\tgene\tis_implicated_in\tTAS\tPMID:9990064,PMID:9697695,PMID:10910075\n" +
                "brain disease\tMGI:2182005\tPten<sup>tm2Mak</sup>\tallele\tis_implicated_in\tTAS\tPMID:25752454,PMID:29476105,PMID:19470613\n";
        assertEquals(result, output);

    }

    @Test
    public void checkEmpiricalDiseaseFilterByDisease() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "MGI:109583";

        // add filter on disease
        pagination.makeSingleFieldFilter(FieldFilter.DISEASE, "BL");
        JsonResultResponse<DiseaseAnnotation> response = geneDAO.getEmpiricalDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 3, 3);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("urinary bladder cancer"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
        assertNotNull(annotation.getFeature());
        assertThat(annotation.getFeature().getSymbol(), equalTo("Pten<sup>tm1Hwu</sup>"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:25533675PMID:19261747"));

        annotation = response.getResults().get(2);
        assertNull(annotation.getFeature());
        assertThat(annotation.getDisease().getName(), equalTo("urinary bladder cancer"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getEmpiricalDiseaseByGene(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        String result = "Disease\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tAssociation Type\tEvidence Code\tSource\tReferences\n" +
                "urinary bladder cancer\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:25533675,PMID:19261747\n" +
                "urinary bladder cancer\tMGI:2182005\tPten<sup>tm2Mak</sup>\tallele\tis_implicated_in\tTAS\tPMID:25533675,PMID:16951148,PMID:21283818\n" +
                "urinary bladder cancer\t\t\tgene\tis_implicated_in\tTAS\tPMID:16951148\n";
        assertEquals(result, output);
    }

    @Test
    public void checkEmpiricalDiseaseFilterByGeneticEntity() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "MGI:109583";

        // add filter on feature symbol
        pagination.makeSingleFieldFilter(FieldFilter.GENETIC_ENTITY, "tm1h");
        JsonResultResponse<DiseaseAnnotation> response = geneDAO.getEmpiricalDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 10, 10);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("acute lymphocytic leukemia"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
        assertNotNull(annotation.getFeature());
        assertThat(annotation.getFeature().getSymbol(), equalTo("Pten<sup>tm1Hwu</sup>"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:21262837"));

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getEmpiricalDiseaseByGene(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        String result = "Disease\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tAssociation Type\tEvidence Code\tSource\tReferences\n" +
                "acute lymphocytic leukemia\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:21262837\n" +
                "Cowden disease\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:23873941,PMID:12163417,PMID:23873941,PMID:18757421,PMID:27889578,PMID:17237784\n" +
                "endometrial cancer\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:20418913,PMID:18632614\n" +
                "fatty liver disease\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:24802098\n" +
                "follicular thyroid carcinoma\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:22167068\n" +
                "hepatocellular carcinoma\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:20837017,PMID:24027047,PMID:25132272\n" +
                "intestinal pseudo-obstruction\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:19884655\n" +
                "persistent fetal circulation syndrome\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:23023706\n" +
                "prostate cancer\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:25948589,PMID:23610450,PMID:27357679,PMID:23348745,PMID:25526087,PMID:23300485,PMID:27345403,PMID:28059767,PMID:25693195,PMID:25455686,PMID:23434594,PMID:16489020,PMID:29720449,PMID:22350410,PMID:27345403,PMID:21620777,PMID:22836754,PMID:28515147,PMID:14522255,PMID:26640144\n" +
                "urinary bladder cancer\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:25533675,PMID:19261747\n";
        assertEquals(result, output);
    }

    @Test
    public void checkDiseaseViaOrthologyByGene() {
        Pagination pagination = new Pagination(1, 10, null, null);
        // Ogg1
        String geneID = "MGI:1097693";
        JsonResultResponse<DiseaseAnnotation> response = geneDAO.getEmpiricalDiseaseAnnotations(geneID, pagination, false);
        assertResponse(response, 10, 22);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("angiomyolipoma"));
        assertThat(annotation.getAssociationType(), equalTo("biomarker_via_orthology"));
        assertNull(annotation.getFeature());
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("MGI:6194238"));

        annotation = response.getResults().get(1);
        assertNull(annotation.getFeature());
        assertThat(annotation.getDisease().getName(), equalTo("angiomyolipoma"));
        assertThat(annotation.getAssociationType(), equalTo("implicated_via_orthology"));

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getDiseaseViaOrthologyByGene(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        String result = "Disease\tAssociation\tOrtholog Gene ID\tOrtholog Gene Symbol\tOrtholog Species\tEvidence Code\tSource\tReferences\n" +
                "angiomyolipoma\tbiomarker_via_orthology\tHGNC:8125\tOGG1\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "angiomyolipoma\timplicated_via_orthology\tHGNC:8125\tOGG1\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "basal cell carcinoma\tbiomarker_via_orthology\tHGNC:8125\tOGG1\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "cataract\timplicated_via_orthology\tHGNC:8125\tOGG1\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "cholangiocarcinoma\timplicated_via_orthology\tHGNC:8125\tOGG1\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "Graves' disease\timplicated_via_orthology\tHGNC:8125\tOGG1\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "head and neck squamous cell carcinoma\tbiomarker_via_orthology\tHGNC:8125\tOGG1\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "head and neck squamous cell carcinoma\timplicated_via_orthology\tHGNC:8125\tOGG1\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "hepatitis\tbiomarker_via_orthology\tRGD:621168\tOgg1\tRattus norvegicus\tIEA\tAlliance\tMGI:6194238\n" +
                "middle cerebral artery infarction\tbiomarker_via_orthology\tRGD:621168\tOgg1\tRattus norvegicus\tIEA\tAlliance\tMGI:6194238\n";
        assertEquals(result, output);

    }


    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        ConfigHelper.init();

        DiseaseDAO service = new DiseaseDAO();

        service.init();
        System.out.println("Number of Diseases with Genes Info: ");

        //DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        //String str = translator.getAllRows(service.getDiseaseAnnotationsDownload("DOID:9351", Pagination.getDownloadPagination()));
        Pagination pagination = new Pagination(1, 20, "gene", "true");
        pagination.addFieldFilter(FieldFilter.GENE_NAME, "l");
        SearchApiResponse response = service.getDiseaseAnnotations("DOID:655", pagination);
        if (response.results != null) {
            response.results.forEach(entry -> {
                Map<String, Object> map1 = (Map<String, Object>) entry.get("geneDocument");
                if (map1 != null)
                    log.info(entry.get("diseaseID") + "\t" + entry.get("diseaseName") + ": " + "\t" + map1.get("species") + ": " + map1.get("symbol") + ": " + map1.get("primaryId"));

            });
        }
        System.out.println("Number of results " + response.total);

        pagination = new Pagination(1, Integer.MAX_VALUE, null, null);
        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        DiseaseService diseaseService = new DiseaseService();
        System.out.println(translator.getAllRows(diseaseService.getDiseaseAnnotationsDownload("DOID:655", pagination)));

    }

    private void assertResponse(JsonResultResponse<DiseaseAnnotation> response, int resultSize, int totalSize) {
        assertNotNull(response);
        assertThat("Number of returned records", response.getResults().size(), equalTo(resultSize));
        assertThat("Number of total records", response.getTotal(), equalTo(totalSize));
    }


}