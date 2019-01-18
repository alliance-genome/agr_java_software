package org.alliancegenome.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.entity.node.Synonym;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DiseaseTest {

    private static Logger log = Logger.getLogger(DiseaseTest.class);

    private ObjectMapper mapper = new ObjectMapper();
    private GeneService geneService = new GeneService();

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();

        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new OrthologyModule());
    }

    @Test
    @Ignore
    public void checkEmpiricalDiseaseByGene() {
        Pagination pagination = new Pagination(1, 10, null, null);
        // Pten
        String geneID = "MGI:109583";
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, true);
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
                "autistic disorder\tMGI:2151804\tPten<sup>tm1Rps</sup>\tallele\tis_implicated_in\tTAS\tPMID:19208814,PMID:23142422,PMID:25561290\n" +
                "autistic disorder\tMGI:2679886\tPten<sup>tm2.1Ppp</sup>\tallele\tis_implicated_in\tTAS\tPMID:22302806\n" +
                "autistic disorder\t\t\tgene\tis_implicated_in\tTAS\tPMID:22302806,PMID:25561290\n" +
                "Bannayan-Riley-Ruvalcaba syndrome\tMGI:1857937\tPten<sup>tm1Mak</sup>\tallele\tis_implicated_in\tTAS\tPMID:10910075\n" +
                "Bannayan-Riley-Ruvalcaba syndrome\tMGI:1857936\tPten<sup>tm1Ppp</sup>\tallele\tis_implicated_in\tTAS\tPMID:9697695\n" +
                "Bannayan-Riley-Ruvalcaba syndrome\tMGI:2151804\tPten<sup>tm1Rps</sup>\tallele\tis_implicated_in\tTAS\tPMID:27889578,PMID:9990064\n" +
                "Bannayan-Riley-Ruvalcaba syndrome\t\t\tgene\tis_implicated_in\tTAS\tPMID:10910075,PMID:9697695,PMID:9990064\n" +
                "brain disease\tMGI:2182005\tPten<sup>tm2Mak</sup>\tallele\tis_implicated_in\tTAS\tPMID:19470613,PMID:25752454,PMID:29476105\n";
        assertEquals(result, output);

    }

    @Test
    @Ignore
    public void checkEmpiricalDiseaseFilterByDisease() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "MGI:109583";

        // add filter on disease
        pagination.makeSingleFieldFilter(FieldFilter.DISEASE, "BL");
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 3, 3);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("urinary bladder cancer"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
        assertNotNull(annotation.getFeature());
        assertThat(annotation.getFeature().getSymbol(), equalTo("Pten<sup>tm1Hwu</sup>"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:19261747PMID:25533675"));

        annotation = response.getResults().get(2);
        assertNull(annotation.getFeature());
        assertThat(annotation.getDisease().getName(), equalTo("urinary bladder cancer"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getEmpiricalDiseaseByGene(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        String result = "Disease\tGenetic Entity ID\tGenetic Entity Symbol\tGenetic Entity Type\tAssociation Type\tEvidence Code\tSource\tReferences\n" +
                "urinary bladder cancer\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:19261747,PMID:25533675\n" +
                "urinary bladder cancer\tMGI:2182005\tPten<sup>tm2Mak</sup>\tallele\tis_implicated_in\tTAS\tPMID:16951148,PMID:21283818,PMID:25533675\n" +
                "urinary bladder cancer\t\t\tgene\tis_implicated_in\tTAS\tPMID:16951148\n";
        assertEquals(result, output);
    }

    @Test
    @Ignore
    public void checkEmpiricalDiseaseFilterByGeneticEntity() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "MGI:109583";

        // add filter on feature symbol
        pagination.makeSingleFieldFilter(FieldFilter.GENETIC_ENTITY, "tm1h");
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, true);
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
                "Cowden disease\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:12163417,PMID:17237784,PMID:18757421,PMID:23873941,PMID:23873941,PMID:27889578\n" +
                "endometrial cancer\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:18632614,PMID:20418913\n" +
                "fatty liver disease\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:24802098\n" +
                "follicular thyroid carcinoma\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:22167068\n" +
                "hepatocellular carcinoma\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:20837017,PMID:24027047,PMID:25132272\n" +
                "intestinal pseudo-obstruction\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:19884655\n" +
                "persistent fetal circulation syndrome\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:23023706\n" +
                "prostate cancer\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:14522255,PMID:16489020,PMID:21620777,PMID:22350410,PMID:22836754,PMID:23300485,PMID:23348745,PMID:23434594,PMID:23610450,PMID:25455686,PMID:25526087,PMID:25693195,PMID:25948589,PMID:26640144,PMID:27345403,PMID:27345403,PMID:27357679,PMID:28059767,PMID:28515147,PMID:29720449\n" +
                "urinary bladder cancer\tMGI:2156086\tPten<sup>tm1Hwu</sup>\tallele\tis_implicated_in\tTAS\tPMID:19261747,PMID:25533675\n";
        assertEquals(result, output);
    }

    @Test
    @Ignore
    public void checkEmpiricalDiseaseFilterByGeneticEntityType() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "MGI:109583";

        // add filter on feature symbol
        pagination.makeSingleFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, "allele");
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 36, 36);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("acute lymphocytic leukemia"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
        assertNotNull(annotation.getFeature());
        assertThat(annotation.getFeature().getSymbol(), equalTo("Pten<sup>tm1Hwu</sup>"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("PMID:21262837"));

        pagination.makeSingleFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, "gene");
        response = geneService.getDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 14, 14);
        annotation = response.getResults().get(1);
        assertThat(annotation.getDisease().getName(), equalTo("autistic disorder"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
        assertNull(annotation.getFeature());
    }

    @Test
    @Ignore
    public void checkEmpiricalDiseaseFilterByAssociation() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "HGNC:3686";

        // add filter on feature symbol
        pagination.makeSingleFieldFilter(FieldFilter.ASSOCIATION_TYPE, "IMPLICATED");
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 2, 2);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("Kallmann syndrome"));
        assertThat(annotation.getAssociationType(), equalTo("is_implicated_in"));
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("RGD:7240710"));

        pagination.makeSingleFieldFilter(FieldFilter.ASSOCIATION_TYPE, "MARKER");
        response = geneService.getDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 4, 4);
        annotation = response.getResults().get(1);
        assertThat(annotation.getDisease().getName(), equalTo("embryonal carcinoma"));
        assertThat(annotation.getAssociationType(), equalTo("is_marker_of"));
        assertNull(annotation.getFeature());
    }

    @Test
    @Ignore
    public void checkEmpiricalDiseaseFilterByEvidence() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "HGNC:3686";

        // add filter on evidence code
        pagination.makeSingleFieldFilter(FieldFilter.EVIDENCE_CODE, "IEP");
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 4, 4);

        pagination.makeSingleFieldFilter(FieldFilter.EVIDENCE_CODE, "iAG");
        response = geneService.getDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 1, 1);
    }

    @Test
    @Ignore
    public void checkEmpiricalDiseaseFilterByPublication() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "HGNC:3686";

        // add filter on reference
        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "380");
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 3, 3);

        pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "710");
        response = geneService.getDiseaseAnnotations(geneID, pagination, true);
        assertResponse(response, 1, 1);
    }

    @Test
    @Ignore
    public void checkDiseaseViaOrthologyByGene() {
        Pagination pagination = new Pagination(1, 10, null, null);
        // Ogg1
        String geneID = "MGI:1097693";
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, false);
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

    @Test
    @Ignore
    public void checkDiseaseOrthologyFilterByDisease() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "MGI:109583";

        // add filter on disease
        pagination.makeSingleFieldFilter(FieldFilter.DISEASE, "OmA");
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, false);
        assertResponse(response, 11, 11);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("angiomyolipoma"));
        assertThat(annotation.getAssociationType(), equalTo("biomarker_via_orthology"));
        assertNull(annotation.getFeature());
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("MGI:6194238"));

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String output = translator.getDiseaseViaOrthologyByGene(response.getResults());
        List<String> lines = Arrays.asList(output.split("\n"));
        assertNotNull(lines);
        String result = "Disease\tAssociation\tOrtholog Gene ID\tOrtholog Gene Symbol\tOrtholog Species\tEvidence Code\tSource\tReferences\n" +
                "angiomyolipoma\tbiomarker_via_orthology\tHGNC:9588\tPTEN\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "cervix uteri carcinoma in situ\tbiomarker_via_orthology\tHGNC:9588\tPTEN\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "familial meningioma\timplicated_via_orthology\tHGNC:9588\tPTEN\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "head and neck squamous cell carcinoma\timplicated_via_orthology\tHGNC:9588\tPTEN\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "in situ carcinoma\tbiomarker_via_orthology\tHGNC:9588\tPTEN\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "leiomyoma\tbiomarker_via_orthology\tHGNC:9588\tPTEN\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "malignant glioma\timplicated_via_orthology\tHGNC:9588\tPTEN\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "non-small cell lung carcinoma\timplicated_via_orthology\tHGNC:9588\tPTEN\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "renal cell carcinoma\tbiomarker_via_orthology\tHGNC:9588\tPTEN\tHomo sapiens\tIEA\tAlliance\tMGI:6194238\n" +
                "renal cell carcinoma\tbiomarker_via_orthology\tRGD:61995\tPten\tRattus norvegicus\tIEA\tAlliance\tMGI:6194238\n" +
                "stomach disease\tbiomarker_via_orthology\tRGD:61995\tPten\tRattus norvegicus\tIEA\tAlliance\tMGI:6194238\n";
        assertEquals(result, output);
    }

    @Test
    @Ignore
    public void checkDiseaseOrthologyFilterByAssociation() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "MGI:109583";

        // add filter on disease
        pagination.makeSingleFieldFilter(FieldFilter.ASSOCIATION_TYPE, "ImplicaT");
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, false);
        assertResponse(response, 13, 13);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("Bannayan-Riley-Ruvalcaba syndrome"));
        assertThat(annotation.getAssociationType(), equalTo("implicated_via_orthology"));
        assertNull(annotation.getFeature());
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("MGI:6194238"));

        pagination.makeSingleFieldFilter(FieldFilter.ASSOCIATION_TYPE, "BIo");
        response = geneService.getDiseaseAnnotations(geneID, pagination, false);
        assertResponse(response, 17, 17);

    }

    @Test
    @Ignore
    public void checkDiseaseOrthologyFilterByOrthoGene() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "MGI:109583";

        // add filter on disease
        pagination.makeSingleFieldFilter(FieldFilter.ORTHOLOG, "daF");
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, false);
        assertResponse(response, 1, 1);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("cancer"));
        assertThat(annotation.getAssociationType(), equalTo("implicated_via_orthology"));
        assertNull(annotation.getFeature());
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("MGI:6194238"));

    }

    @Test
    @Ignore
    public void checkDiseaseOrthologyFilterByOrthoGeneSpecies() {
        Pagination pagination = new Pagination(1, null, null, null);
        // Pten
        String geneID = "MGI:109583";

        // add filter on disease
        pagination.makeSingleFieldFilter(FieldFilter.ORTHOLOG_SPECIES, "ratt");
        JsonResultResponse<DiseaseAnnotation> response = geneService.getDiseaseAnnotations(geneID, pagination, false);
        assertResponse(response, 10, 10);

        DiseaseAnnotation annotation = response.getResults().get(0);
        assertThat(annotation.getDisease().getName(), equalTo("diabetes mellitus"));
        assertThat(annotation.getAssociationType(), equalTo("biomarker_via_orthology"));
        assertNull(annotation.getFeature());
        assertThat(annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining()), equalTo("MGI:6194238"));
    }


    @Test
    public void checkSingleDiseaseTerm() {
        DiseaseService service = new DiseaseService();
        DOTerm term = service.getById("DOID:3594");
        assertNotNull(term);
        assertThat(term.getName(), equalTo("choriocarcinoma"));
        assertThat(term.getSynonyms().stream().map(Synonym::getPrimaryKey).collect(Collectors.toList()), containsInAnyOrder("Chorioepithelioma"));
        assertThat(term.getChildren().size(), equalTo(8));
        assertThat(term.getParents().size(), equalTo(1));
        assertThat(term.getDefLinks().size(), equalTo(1));
    }


    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        ConfigHelper.init();

        //DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        //String str = translator.getAllRows(service.getDiseaseAnnotationsDownload("DOID:9351", Pagination.getDownloadPagination()));
        Pagination pagination = new Pagination(1, 20, "gene", "true");
        pagination.addFieldFilter(FieldFilter.GENE_NAME, "l");
//        System.out.println("Number of results " + response.getTotal());

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