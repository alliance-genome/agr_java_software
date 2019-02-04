package org.alliancegenome.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.alliancegenome.api.controller.ExpressionController;
import org.alliancegenome.api.controller.GeneController;
import org.alliancegenome.api.controller.GenesController;
import org.alliancegenome.api.controller.OrthologyController;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.api.service.helper.ExpressionDetail;
import org.alliancegenome.api.service.helper.ExpressionSummary;
import org.alliancegenome.api.service.helper.ExpressionSummaryGroup;
import org.alliancegenome.api.service.helper.ExpressionSummaryGroupTerm;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.document.GeneTranslator;
import org.alliancegenome.es.index.site.document.DiseaseDocument;
import org.alliancegenome.es.index.site.document.GeneDocument;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.alliancegenome.api.service.ExpressionService.CELLULAR_COMPONENT;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@Api(value = "hallo")
@Ignore
public class GeneTest {

    private GeneService geneService;

    private static Logger log = Logger.getLogger(GeneTest.class);
    private ObjectMapper mapper = new ObjectMapper();

    @ApiOperation(value = "Retrieve a Gene for given ID")
    public static void main(String[] args) {

        GeneTest test = new GeneTest();
        Api annotation = test.getClass().getAnnotation(Api.class);
        Method method = new Object() {
        }
                .getClass()
                .getEnclosingMethod();
        Annotation[] annotations = method.getDeclaredAnnotations();


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
    public void checkForSecondaryId() {
        // ZFIN:ZDB-GENE-030131-3355 is a secondary ID for ZFIN:ZDB-LINCRNAG-160518-1
        Gene gene = geneService.getById("ZFIN:ZDB-GENE-030131-3355");
        assertNotNull(gene);
        assertThat(gene.getPrimaryKey(), equalTo("ZFIN:ZDB-LINCRNAG-160518-1"));
        assertThat(gene.getSpecies().getName(), equalTo("Danio rerio"));
    }

    @Test
    public void checkOrthologyAPIWithFilter() throws IOException {

        GeneController controller = new GeneController();
        String[] geneIDs = {"RGD:2129"};
        JsonResultResponse<OrthologView> response = controller.getGeneOrthology("MGI:109583", Arrays.asList(geneIDs), null, "stringENT", null, null, 20, 1);
        assertThat("Matches found for filter 'stringent", response.getTotal(), greaterThan(0));
    }

    @Test
    public void checkOrthologyForListOfGenes() throws IOException {

        GeneController controller = new GeneController();
        JsonResultResponse<OrthologView> response = controller.getGeneOrthology("MGI:109583", null, null, "stringENT", null, null, 20, 1);
        assertThat("Matches found for filter 'stringent", response.getTotal(), greaterThan(0));
    }

    @Test
    public void checkOrthologyForTwoSpecies() throws IOException {

        OrthologyController controller = new OrthologyController();
        JsonResultResponse<OrthologView> response = controller.getDoubleSpeciesOrthology("7955", "10090", "stringent", "ZFIN", 20, 1);
        assertThat("Orthology records found for mouse - zebrafish", response.getTotal(), greaterThan(0));
    }

    @Test
    public void checkOrthologyForSingleSpecies() throws IOException {

        OrthologyController controller = new OrthologyController();
        JsonResultResponse<OrthologView> response = controller.getSingleSpeciesOrthology("559292", "stringent", "OMA", 20, 1);
        assertThat("Orthology records found for mouse geneMap", response.getTotal(), greaterThan(0));
    }

    @Test
    public void checkOrthologyAPIWithSpecies() throws IOException {

        GeneController controller = new GeneController();
        JsonResultResponse<OrthologView> response = controller.getGeneOrthology("MGI:109583", null, null, "stringent", null, null, 20, 1);
        assertThat("No matches found for species 'NCBITaxon:10115", response.getTotal(), greaterThan(5));

        String[] taxonArray = {"NCBITaxon:10116"};
        response = controller.getGeneOrthology("MGI:109583", null, null, null, Arrays.asList(taxonArray), null, 20, 1);
        assertThat("matches found for method species NCBITaxon:10116", response.getTotal(), greaterThan(0));

        response = controller.getGeneOrthology("MGI:109583", null, null, "stringent", Arrays.asList(taxonArray), null, 20, 1);
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
    public void checkOrthologyAPIWithMethods() throws IOException {

        GeneController controller = new GeneController();
        String[] methods = {"ZFIN"};
        JsonResultResponse<OrthologView> response = controller.getGeneOrthology("MGI:109583", null, null, null, null, Arrays.asList(methods), 20, 1);
        assertThat("No match against method 'ZFIN'", response.getTotal(), greaterThan(0));

        methods = new String[]{"OrthoFinder"};
        response = controller.getGeneOrthology("MGI:109583", null, null, null, null, Arrays.asList(methods), 20, 1);
        assertThat("matches found for method 'OrthoFinder'", response.getTotal(), greaterThan(0));

        methods = new String[]{"OrthoFinder", "ZFIN"};
        response = controller.getGeneOrthology("MGI:109583", null, null, null, null, Arrays.asList(methods), 20, 1);
        assertThat("no matches found for method 'OrthoFinder and ZFIN'", response.getTotal(), greaterThan(0));

        methods = new String[]{"OrthoFinder", "PANTHER"};
        response = controller.getGeneOrthology("MGI:109583", null, null, null, null, Arrays.asList(methods), 20, 1);
        assertThat("matches found for method 'OrthoFinder and Panther'", response.getTotal(), greaterThan(0));
    }

    @Test
    public void checkOrthologyAPINoFilters() throws IOException {

        GeneController controller = new GeneController();
        JsonResultResponse<OrthologView> response = controller.getGeneOrthology("MGI:109583", null, null, null, null, null, 20, 0);
        assertThat("matches found for gene MGI:109583'", response.getTotal(), greaterThan(0));
    }

    @Test
    public void getAllOrthologyMethods() throws IOException {

        OrthologyController controller = new OrthologyController();
        JsonResultResponse<OrthoAlgorithm> response = controller.getAllMethodsCalculations();
    }

    @Test
    public void getAllGenes() throws IOException {

        GenesController controller = new GenesController();
        String[] taxonIDs = {"danio"};
        JsonResultResponse<Gene> response = controller.getGenes(Arrays.asList(taxonIDs), 10, 1);
        assertThat("matches found for gene MGI:109583'", response.getTotal(), greaterThan(5));
    }

    @Test
    public void getAllGeneIDs() throws IOException {

        GenesController controller = new GenesController();
        String[] taxonIDs = {"danio"};
        String responseString = controller.getGeneIDs(Arrays.asList(taxonIDs), 5, 1);
        assertThat("matches found for gene MGI:109583'", responseString,
                equalTo("ZFIN:ZDB-GENE-990706-1,ZFIN:ZDB-GENE-000710-5,ZFIN:ZDB-GENE-030516-5,ZFIN:ZDB-GENE-030131-8698,ZFIN:ZDB-GENE-030131-8358"));
    }

    @Test
    public void checkExpressionSummary() throws IOException {

        GeneController controller = new GeneController();
        ExpressionSummary response = controller.getExpressionSummary("RGD:2129");
        //String responseString = controller.getExpressionSummary("FB:FBgn0029123");
        //String responseString = controller.getExpressionSummary("ZFIN:ZDB-GENE-080204-52", 5, 1);

        assertThat("matches found for gene RGD:2129'", response.getTotalAnnotations(), equalTo(10));
        // GoCC
        List<ExpressionSummaryGroupTerm> terms = response.getGroups().stream()
                .filter(expressionSummaryGroup -> expressionSummaryGroup.getName().equals(CELLULAR_COMPONENT))
                .map(ExpressionSummaryGroup::getTerms)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        terms.forEach(expressionSummaryGroupTerm -> {
            if (expressionSummaryGroupTerm.getName().equals("extracellular region"))
                assertThat(expressionSummaryGroupTerm.getNumberOfAnnotations(), equalTo(4));
            else if (expressionSummaryGroupTerm.getName().equals("protein-containing complex"))
                assertThat(expressionSummaryGroupTerm.getNumberOfAnnotations(), equalTo(3));
            else if (expressionSummaryGroupTerm.getName().equals("other locations"))
                assertThat(expressionSummaryGroupTerm.getNumberOfAnnotations(), equalTo(3));
            else
                assertThat(expressionSummaryGroupTerm.getNumberOfAnnotations(), equalTo(0));
        });
    }

    @Test
    public void checkExpressionSummaryGOAndAO() throws IOException {

        GeneController controller = new GeneController();
        ExpressionSummary response = controller.getExpressionSummary("ZFIN:ZDB-GENE-980526-188");
        assertThat("matches found for gene ZFIN:ZDB-GENE-980526-188'", response.getTotalAnnotations(), equalTo(26));
    }

    @Test
    public void checkExpressionAnnotation() throws IOException {

        ExpressionController controller = new ExpressionController();
        //String[] geneIDs = {"MGI:97570", "ZFIN:ZDB-GENE-080204-52"};
        String[] geneIDs = {"ZFIN:ZDB-GENE-080204-52"};
        int limit = 15;
        String responseString = controller.getExpressionAnnotations(Arrays.asList(geneIDs), null, null, null, null, null, null, null, null, limit, 1, null, "true");
        JsonResultResponse<ExpressionDetail> response = mapper.readValue(responseString, new TypeReference<JsonResultResponse<ExpressionDetail>>() {
        });
        assertThat("matches found for gene MGI:109583'", response.getReturnedRecords(), equalTo(15));

        List<String> symbolList = response.getResults().stream()
                .map(annotation -> annotation.getGene().getSymbol())
                .collect(Collectors.toList());
        List<String> termList = response.getResults().stream()
                .map(ExpressionDetail::getTermName)
                .collect(Collectors.toList());
/*
        List<String> stageList = response.getResults().stream()
                .map(annotation -> annotation.getStage().getPrimaryKey())
                .collect(Collectors.toList());
*/
        List<String> assayList = response.getResults().stream()
                .map(annotation -> annotation.getAssay().getName())
                .collect(Collectors.toList());
        List<String> referenceList = response.getResults().stream()
                .map(annotation -> annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        String terms = String.join(",", termList);
//        String stages = String.join(",", stageList);
        String symbols = String.join(",", symbolList);
        String pubs = String.join(",", referenceList);
        assertThat("first element species", response.getResults().get(0).getGene().getSpecies().getName(), equalTo("Danio rerio"));
        assertThat("first element symbol", response.getResults().get(0).getGene().getSymbol(), equalTo("abcb4"));
        assertThat("list of terms", terms, equalTo("bile canaliculus,head,head,head,head,head,head,head,head,hepatocyte intracellular canaliculus,intestinal bulb,intestine,intestine,intestine,intestine"));
        //      assertThat("list of stages", stages, equalTo("ZFS:0000029,ZFS:0000030,ZFS:0000031,ZFS:0000032,ZFS:0000033,ZFS:0000034,ZFS:0000035,ZFS:0000036,ZFS:0000037,ZFS:0000029,ZFS:0000030,ZFS:0000031,ZFS:0000032,ZFS:0000033,ZFS:0000034"));

        responseString = controller.getExpressionAnnotations(Arrays.asList(geneIDs), null, null, null, null, null, null, null, null, limit, 1, "assay", "false");
        response = mapper.readValue(responseString, new TypeReference<JsonResultResponse<ExpressionDetail>>() {
        });
        assayList = response.getResults().stream()
                .map(annotation -> annotation.getAssay().getName())
                .collect(Collectors.toList());
        String assays = String.join(",", assayList);
        assertThat("matches found for gene MGI:109583'", response.getReturnedRecords(), equalTo(15));


        responseString = controller.getExpressionAnnotations(Arrays.asList(geneIDs), null, null, null, null, null, null, null, null, limit, 1, "source", "true");
        response = mapper.readValue(responseString, new TypeReference<JsonResultResponse<ExpressionDetail>>() {
        });
        assayList = response.getResults().stream()
                .map(annotation -> annotation.getAssay().getName())
                .collect(Collectors.toList());
        assays = String.join(",", assayList);
        assertThat("matches found for gene MGI:109583'", response.getReturnedRecords(), equalTo(15));
    }

    @Test
    public void checkExpressionAnnotationWithTerm() throws IOException {

        ExpressionController controller = new ExpressionController();
        String[] geneIDs = {"RGD:2129"};
        String termID = "GO:otherLocations";
        int limit = 15;
        String responseString = controller.getExpressionAnnotations(Arrays.asList(geneIDs), termID, null, null, null, null, null, null, null, limit, 1, null, "true");
        JsonResultResponse<ExpressionDetail> response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("matches found for gene MGI:109583'", response.getResults().size(), equalTo(3));

        termID = "GO:0032991";
        responseString = controller.getExpressionAnnotations(Arrays.asList(geneIDs), termID, null, null, null, null, null, null, null, limit, 1, null, "true");
        response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("matches found for gene MGI:109583'", response.getResults().size(), equalTo(3));
    }

    @Test
    public void checkExpressionAnnotationWithTermOnZFIN() throws IOException {

        ExpressionController controller = new ExpressionController();
        String[] geneIDs = {"ZFIN:ZDB-GENE-980526-188"};
        String termID = "GO:0005739";
        int limit = 15;
        String responseString = controller.getExpressionAnnotations(Arrays.asList(geneIDs), termID, null, null, null, null, null, null, null, limit, 1, null, "true");
        JsonResultResponse<ExpressionDetail> response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("matches found for gene MGI:109583'", response.getResults().size(), equalTo(1));

        // sensory system
        termID = "UBERON:0001032";
        limit = 15;
        responseString = controller.getExpressionAnnotations(Arrays.asList(geneIDs), termID, null, null, null, null, null, null, null, limit, 1, null, "true");
        response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("matches found for gene MGI:109583'", response.getResults().size(), greaterThan(2));

        // Adult stage
        termID = "UBERON:0000113";
        limit = 15;
        responseString = controller.getExpressionAnnotations(Arrays.asList(geneIDs), termID, null, null, null, null, null, null, null, limit, 1, null, "true");
        response = mapper.readValue(responseString, JsonResultResponse.class);
        assertThat("matches found for gene MGI:109583'", response.getResults().size(), greaterThan(2));
    }

    @Test
    public void checkExpressionAnnotationFilter() throws IOException {

        ExpressionController controller = new ExpressionController();
        String[] geneIDs = {"ZFIN:ZDB-GENE-980526-166"};
        int limit = 6;
        String responseString = controller.getExpressionAnnotations(Arrays.asList(geneIDs), null, null, null, null, null, null, null, null, limit, 1, null, "true");
        JsonResultResponse<ExpressionDetail> response = mapper.readValue(responseString, new TypeReference<JsonResultResponse<ExpressionDetail>>() {
        });
        //assertThat("matches found for gene MGI:109583'", response.getReturnedRecords(), equalTo(limit));

        List<String> symbolList = response.getResults().stream()
                .map(annotation -> annotation.getGene().getSymbol())
                .collect(Collectors.toList());
        List<String> termList = response.getResults().stream()
                .map(ExpressionDetail::getTermName)
                .collect(Collectors.toList());
        List<String> stageList = response.getResults().stream()
                .filter(annotation -> annotation.getStage() != null)
                .map(annotation -> annotation.getStage().getPrimaryKey())
                .collect(Collectors.toList());
        List<String> assayList = response.getResults().stream()
                .map(annotation -> annotation.getAssay().getName())
                .collect(Collectors.toList());
        List<String> referenceList = response.getResults().stream()
                .map(annotation -> annotation.getPublications().stream().map(Publication::getPubId).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        String terms = String.join(",", termList);
//        String stages = String.join(",", stageList);
        String symbols = String.join(",", symbolList);
        String pubs = String.join(",", referenceList);
        assertThat("first element species", response.getResults().get(0).getGene().getSpecies().getName(), equalTo("Danio rerio"));
        assertThat("first element symbol", response.getResults().get(0).getGene().getSymbol(), equalTo("shha"));
        assertThat("list of terms", terms, equalTo("anal fin,anterior neural keel,anterior neural keel ventral region,anterior neural rod,axial chorda mesoderm,axial chorda mesoderm"));
        //      assertThat("list of stages", stages, equalTo("ZFS:0000029,ZFS:0000030,ZFS:0000031,ZFS:0000032,ZFS:0000033,ZFS:0000034,ZFS:0000035,ZFS:0000036,ZFS:0000044"));
    }

    @Test
    public void checkExpressionAPI() throws IOException {

        ExpressionController controller = new ExpressionController();
        int limit = 15;
        String responseString = controller.getExpressionAnnotationsByTaxon("danio", null, limit, 1);
        JsonResultResponse<ExpressionDetail> response = mapper.readValue(responseString, new TypeReference<JsonResultResponse<ExpressionDetail>>() {
        });
    }

    @Test
    public void checkExpressionAnnotationFiltering() throws IOException {

        ExpressionController controller = new ExpressionController();
        String[] geneIDs = {"MGI:97570", "ZFIN:ZDB-GENE-080204-52"};
        String termID = null;
        int limit = 15;
        String responseString = controller.getExpressionAnnotations(Arrays.asList(geneIDs), termID, null, null, null, null, null, null, null, limit, 1, null, "true");
        JsonResultResponse<ExpressionDetail> response = mapper.readValue(responseString, new TypeReference<JsonResultResponse<ExpressionDetail>>() {
        });
        List<String> symbolList = response.getResults().stream()
                .map(annotation -> annotation.getGene().getSymbol())
                .collect(Collectors.toList());
        List<String> termList = response.getResults().stream()
                .map(ExpressionDetail::getTermName)
                .collect(Collectors.toList());
    }

}