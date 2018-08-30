package org.alliancegenome.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.api.controller.GeneController;
import org.alliancegenome.api.controller.OrthologyController;
import org.alliancegenome.api.rest.interfaces.ExpressionController;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.api.service.helper.ExpressionSummary;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.index.site.dao.GeneDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchResult;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class GeneTest {

    private GeneService geneService;

    private static Logger log = Logger.getLogger(GeneTest.class);
    private ObjectMapper mapper = new ObjectMapper();


    public static void main(String[] args) {
        GeneDAO service = new GeneDAO();

        service.init();
        System.out.println("Number of Diseases with Genes Info: ");

        //DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        //String str = translator.getAllRows(service.getDiseaseAnnotationsDownload("DOID:9351", Pagination.getDownloadPagination()));
        Pagination pagination = new Pagination(1, 20, "gene", "true");
        pagination.addFieldFilter(FieldFilter.GENE_NAME, "l");
        SearchResult response = service.getAllelesByGene("ZFIN:ZDB-GENE-051127-5", pagination);
        if (response.results != null) {
            response.results.forEach(entry -> {
                Map<String, Object> map1 = (Map<String, Object>) entry.get("geneDocument");
                if (map1 != null)
                    log.info(entry.get("diseaseID") + "\t" + entry.get("diseaseName") + ": " + "\t" + map1.get("species") + ": " + map1.get("symbol") + ": " + map1.get("primaryId"));

            });
        }
        System.out.println("Number of results " + response.total);

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
    public void checkForSecondaryId() {
        // ZFIN:ZDB-GENE-030131-3355 is a secondary ID for ZFIN:ZDB-LINCRNAG-160518-1
        Map<String, Object> result = geneService.getById("ZFIN:ZDB-GENE-030131-3355");
        assertNotNull(result);
        assertThat(result.get("primaryId"), equalTo("ZFIN:ZDB-LINCRNAG-160518-1"));
        assertThat(result.get("species"), equalTo("Danio rerio"));
    }

    @Test
    @Ignore
    public void checkOrthologyAPIWithFilter() throws IOException {

        GeneController controller = new GeneController();
        String responseString = controller.getGeneOrthology("MGI:109583", "stringENT", null, null, 20, 0);
        JsonResultResponse response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("Matches found for filter 'stringent", response.getTotal(), greaterThan(0));
    }

    @Test
    @Ignore
    public void checkOrthologyForListOfGenes() throws IOException {

        GeneController controller = new GeneController();
        String responseString = controller.getGeneOrthology("MGI:109583", "stringENT", null, null, 20, 0);
        JsonResultResponse response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("Matches found for filter 'stringent", response.getTotal(), greaterThan(0));
    }

    @Test
    @Ignore
    public void checkOrthologyForTwoSpecies() throws IOException {

        OrthologyController controller = new OrthologyController();
        String responseString = controller.getDoubleSpeciesOrthology("7955", "10090", "stringent", "ZFIN", 20, 1);
        JsonResultResponse response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("Orthology records found for mouse - zebrafish", response.getTotal(), greaterThan(0));
    }

    @Test
    @Ignore
    public void checkOrthologyForSingleSpecies() throws IOException {

        OrthologyController controller = new OrthologyController();
        String responseString = controller.getSingleSpeciesOrthology("10090", "stringent", null, 20, 1);
        JsonResultResponse response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("Orthology records found for mouse genes", response.getTotal(), greaterThan(0));
    }

    @Test
    @Ignore
    public void checkOrthologyAPIWithSpecies() throws IOException {

        GeneController controller = new GeneController();
        String responseString = controller.getGeneOrthology("MGI:109583", "stringent", null, null, 20, 1);
        JsonResultResponse response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("No matches found for species 'NCBITaxon:10115", response.getTotal(), greaterThan(5));

        String[] taxonArray = {"NCBITaxon:10116"};
        responseString = controller.getGeneOrthology("MGI:109583", null, Arrays.asList(taxonArray), null, 20, 0);
        response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("matches found for method species NCBITaxon:10116", response.getTotal(), greaterThan(0));

        responseString = controller.getGeneOrthology("MGI:109583", "stringent", Arrays.asList(taxonArray), null, 20, 0);
        response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("matches found for method species NCBITaxon:10116", response.getTotal(), greaterThan(0));

/*
        response = controller.getDoubleSpeciesOrthology("MGI:109583", null, "NCBITaxon:10116,NCBITaxon:7955", null, null, null);
        assertThat(json, startsWith("[{\"gene"));
*/
/*
        json = controller.getDoubleSpeciesOrthology("MGI:109583", "stringENT", null, null, null, null);
        assertNotNull(json);
*/
    }

    @Test
    @Ignore
    public void checkOrthologyAPIWithMethods() throws IOException {

        GeneController controller = new GeneController();
        String[] methods = {"ZFIN"};
        String responseString = controller.getGeneOrthology("MGI:109583", null, null, Arrays.asList(methods), 20, 0);
        JsonResultResponse response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("No match against method 'ZFIN'", response.getTotal(), greaterThan(0));

        methods = new String[]{"OrthoFinder"};
        responseString = controller.getGeneOrthology("MGI:109583", null, null, Arrays.asList(methods), 20, 0);
        response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("matches found for method 'OrthoFinder'", response.getTotal(), greaterThan(0));

        methods = new String[]{"OrthoFinder", "ZFIN"};
        responseString = controller.getGeneOrthology("MGI:109583", null, null, Arrays.asList(methods), 20, 0);
        response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("no matches found for method 'OrthoFinder and ZFIN'", response.getTotal(), greaterThan(0));

        methods = new String[]{"OrthoFinder", "PANTHER"};
        responseString = controller.getGeneOrthology("MGI:109583", null, null, Arrays.asList(methods), 20, 0);
        response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("matches found for method 'OrthoFinder and Panther'", response.getTotal(), greaterThan(0));
    }

    @Test
    @Ignore
    public void checkOrthologyAPINoFilters() throws IOException {

        GeneController controller = new GeneController();
        String responseString = controller.getGeneOrthology("MGI:109583", null, null, null, 20, 0);
        JsonResultResponse response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("matches found for gene MGI:109583'", response.getTotal(), greaterThan(0));
    }

    @Test
    @Ignore
    public void getAllOrthologyMethods() throws IOException {

        OrthologyController controller = new OrthologyController();
        String responseString = controller.getAllMethodsCalculations();
    }

    @Ignore
    @Test
    public void checkExpressionSummary() throws IOException {

        GeneController controller = new GeneController();
        String responseString = controller.getExpressionSummary("RGD:2129");
        //String responseString = controller.getExpressionSummary("ZFIN:ZDB-GENE-080204-52", 5, 1);
        ExpressionSummary response = mapper.readValue(responseString, ExpressionSummary.class);
        assertThat("matches found for gene MGI:109583'", response.getTotalAnnotations(), greaterThan(0));
    }

    @Ignore
    @Test
    public void checkExpressionAnnotation() throws IOException {

        ExpressionController controller = new ExpressionController();
        String[] geneIDs = {"MGI:97570", "ZFIN:ZDB-GENE-080204-52"};
        String responseString = controller.getExpressionAnnotations(Arrays.asList(geneIDs), null, "abcb4",null, null, null, null, null,  5, 1, null, "true");
        JsonResultResponse response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("matches found for gene MGI:109583'", response.getTotal(), greaterThan(0));
    }


}