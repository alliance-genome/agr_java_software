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
    @Ignore
    public void checkEmpiricalDiseaseByFilter() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "MGI:109583";

        // add filter on disease
        pagination.makeSingleFieldFilter(FieldFilter.DISEASE, "BL");
        JsonResultResponse<DiseaseAnnotation> response = geneDAO.getEmpiricalDiseaseAnnotations(geneID, pagination, true);
///        assertResponse(response, 7, 7);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("urinary bladder cancer"));
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