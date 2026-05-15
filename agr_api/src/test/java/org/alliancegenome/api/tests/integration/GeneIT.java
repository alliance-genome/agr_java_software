package org.alliancegenome.api.tests.integration;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.api.controller.ExpressionController;
import org.alliancegenome.api.controller.GeneController;
import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;

public class GeneIT {

	private ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) {

		GeneIT test = new GeneIT();
		//Api annotation = test.getClass().getAnnotation(Api.class);
		Method method = new Object() {
		}
				.getClass()
				.getEnclosingMethod();
		//Annotation[] annotations = method.getDeclaredAnnotations();


	}

	@Before
	public void before() {
		ConfigHelper.init();
		//geneService = new GeneService();
		mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	@Test
	public void checkOrthologyAPIWithFilter() {

		GeneController controller = new GeneController();
		String[] geneIDs = {"RGD:2129"};
		JsonResultResponse<GeneToGeneOrthologyDocument> response = controller.getGeneOrthology("MGI:109583", asList(geneIDs), null, "stringENT", null, 20, 1);
		assertThat("Matches found for containsFilterValue 'stringent", (int) response.getTotal(), greaterThan(0));
	}

	@Test
	public void checkOrthologyForListOfGenes() {

		GeneController controller = new GeneController();
		JsonResultResponse<GeneToGeneOrthologyDocument> response = controller.getGeneOrthology("MGI:109583", null, null, "stringENT", null, 20, 1);
		assertThat("Matches found for containsFilterValue 'stringent", (int) response.getTotal(), greaterThan(0));
	}

	@Test
	public void checkOrthologyAPIWithSpecies() {

		GeneController controller = new GeneController();
		JsonResultResponse<GeneToGeneOrthologyDocument> response = controller.getGeneOrthology("MGI:109583", null, null, "stringent", null, 20, 1);
		assertThat("No matches found for species 'NCBITaxon:10115", (int) response.getTotal(), greaterThan(5));

		String taxonArray = "NCBITaxon:10116";
		response = controller.getGeneOrthology("MGI:109583", null, null, null, taxonArray, 20, 1);
		assertThat("matches found for method species NCBITaxon:10116", (int) response.getTotal(), greaterThan(0));

		response = controller.getGeneOrthology("MGI:109583", null, null, "stringent", taxonArray, 20, 1);
		assertThat("matches found for method species NCBITaxon:10116", (int) response.getTotal(), greaterThan(0));

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
	public void checkOrthologyAPINoFilters() {

		GeneController controller = new GeneController();
		JsonResultResponse<GeneToGeneOrthologyDocument> response = controller.getGeneOrthology("MGI:109583", null, null, null, null, 20, 1);
		assertThat("matches found for gene MGI:109583'", (int) response.getTotal(), greaterThan(0));
	}

	@Test
	public void checkExpressionAnnotation() {

		ExpressionController controller = new ExpressionController();
		//String[] geneIDs = {"MGI:97570", "ZFIN:ZDB-GENE-080204-52"};
		String[] geneIDs = {"ZFIN:ZDB-GENE-080204-52"};
		int limit = 15;
		JsonResultResponse<GeneExpressionDocument> response = controller.getExpressionAnnotations(null, null, null, null, null, null, null, null, null, limit, 1, null, "true", asList(geneIDs));
		assertThat("matches found for gene MGI:109583'", response.getReturnedRecords(), equalTo(15));

		List<String> symbolList = response.getResults().stream()
				.map(annotation -> annotation.getGeneExpressionAnnotation().getExpressionAnnotationSubject().getGeneSymbol().getDisplayText())
				.collect(Collectors.toList());
		List<String> termList = response.getResults().stream()
				.map(annotation -> annotation.getGeneExpressionAnnotation().getWhereExpressedStatement())
				.collect(Collectors.toList());
/*
		List<String> stageList = response.getResults().stream()
				.map(annotation -> annotation.getStage().getPrimaryKey())
				.collect(Collectors.toList());
*/
		List<String> assayList = response.getResults().stream()
				.map(annotation -> annotation.getGeneExpressionAnnotation().getExpressionAssayUsed().getName())
				.collect(Collectors.toList());

		String terms = String.join(",", termList);
//		  String stages = String.join(",", stageList);
		String symbols = String.join(",", symbolList);
		assertThat("first element species", response.getResults().get(0).getGeneExpressionAnnotation().getExpressionAnnotationSubject().getTaxon().getSpecies().getFullName(), equalTo("Danio rerio"));
		assertThat("first element symbol", response.getResults().get(0).getGeneExpressionAnnotation().getExpressionAnnotationSubject().getGeneSymbol().getDisplayText(), equalTo("abcb4"));
		assertThat("list of terms", terms, equalTo("bile canaliculus,head,head,head,head,head,head,head,head,hepatocyte intracellular canaliculus,intestinal bulb,intestine,intestine,intestine,intestine"));
		//		assertThat("list of stages", stages, equalTo("ZFS:0000029,ZFS:0000030,ZFS:0000031,ZFS:0000032,ZFS:0000033,ZFS:0000034,ZFS:0000035,ZFS:0000036,ZFS:0000037,ZFS:0000029,ZFS:0000030,ZFS:0000031,ZFS:0000032,ZFS:0000033,ZFS:0000034"));

		response = controller.getExpressionAnnotations(null, null, null, null, null, null, null, null, null, limit, 1, "assay", "false", asList(geneIDs));
		assayList = response.getResults().stream()
				.map(annotation -> annotation.getGeneExpressionAnnotation().getExpressionAssayUsed().getName())
				.collect(Collectors.toList());
		String assays = String.join(",", assayList);
		assertThat("matches found for gene MGI:109583'", response.getReturnedRecords(), equalTo(15));


		response = controller.getExpressionAnnotations(null, null, null, null, null, null, null, null, null, limit, 1, "source", "true", asList(geneIDs));
		assayList = response.getResults().stream()
				.map(annotation -> annotation.getGeneExpressionAnnotation().getExpressionAssayUsed().getName())
				.collect(Collectors.toList());
		assays = String.join(",", assayList);
		assertThat("matches found for gene MGI:109583'", response.getReturnedRecords(), equalTo(15));
	}

	@Test
	public void checkExpressionAnnotationWithTerm() {

		ExpressionController controller = new ExpressionController();
		String[] geneIDs = {"RGD:2129"};
		String termID = "GO:otherLocations";
		int limit = 15;
		JsonResultResponse<GeneExpressionDocument> response = controller.getExpressionAnnotations(termID, null, null, null, null, null, null, null, null, limit, 1, null, "true", asList(geneIDs));
		assertThat("matches found for gene MGI:109583'", response.getResults().size(), equalTo(3));

		termID = "GO:0032991";
		response = controller.getExpressionAnnotations(termID, null, null, null, null, null, null, null, null, limit, 1, null, "true", asList(geneIDs));
		assertThat("matches found for gene MGI:109583'", response.getResults().size(), equalTo(3));
	}

	@Test
	public void checkExpressionAnnotationWithTermOnZFIN() {

		ExpressionController controller = new ExpressionController();
		String[] geneIDs = {"ZFIN:ZDB-GENE-980526-188"};
		String termID = "GO:0005739";
		int limit = 15;
		JsonResultResponse<GeneExpressionDocument> response = controller.getExpressionAnnotations(termID, null, null, null, null, null, null, null, null, limit, 1, null, "true", asList(geneIDs));
		assertThat("matches found for gene MGI:109583'", response.getResults().size(), equalTo(1));

		// sensory system
		termID = "UBERON:0001032";
		limit = 15;
		response = controller.getExpressionAnnotations(termID, null, null, null, null, null, null, null, null, limit, 1, null, "true", asList(geneIDs));
		assertThat("matches found for gene MGI:109583'", response.getResults().size(), greaterThan(2));

		// Adult stage
		termID = "UBERON:0000113";
		limit = 15;
		response = controller.getExpressionAnnotations(termID, null, null, null, null, null, null, null, null, limit, 1, null, "true", asList(geneIDs));
		assertThat("matches found for gene MGI:109583'", response.getResults().size(), greaterThan(2));
	}

	@Test
	public void checkExpressionAnnotationFilter() {

		ExpressionController controller = new ExpressionController();
		String[] geneIDs = {"ZFIN:ZDB-GENE-980526-166"};
		int limit = 6;
		JsonResultResponse<GeneExpressionDocument> response = controller.getExpressionAnnotations(null, null, null, null, null, null, null, null, null, limit, 1, null, "true", asList(geneIDs));
		//assertThat("matches found for gene MGI:109583'", response.getReturnedRecords(), equalTo(limit));

		List<String> symbolList = response.getResults().stream()
				.map(annotation -> annotation.getGeneExpressionAnnotation().getExpressionAnnotationSubject().getGeneSymbol().getDisplayText())
				.collect(Collectors.toList());
		List<String> termList = response.getResults().stream()
				.map(geneExpressionDocument -> geneExpressionDocument.getGeneExpressionAnnotation().getWhereExpressedStatement())
				.collect(Collectors.toList());
		List<String> stageList = response.getResults().stream()
				.filter(annotation -> annotation.getGeneExpressionAnnotation().getWhenExpressedStageName() != null)
				.map(annotation -> annotation.getGeneExpressionAnnotation().getWhenExpressedStageName())
				.collect(Collectors.toList());
		List<String> assayList = response.getResults().stream()
				.map(annotation -> annotation.getGeneExpressionAnnotation().getExpressionAssayUsed().getName())
				.collect(Collectors.toList());

		String terms = String.join(",", termList);
//		  String stages = String.join(",", stageList);
		String symbols = String.join(",", symbolList);
		assertThat("first element species", response.getResults().get(0).getGeneExpressionAnnotation().getExpressionAnnotationSubject().getTaxon().getSpecies().getFullName(), equalTo("Danio rerio"));
		assertThat("first element symbol", response.getResults().get(0).getGeneExpressionAnnotation().getExpressionAnnotationSubject().getGeneSymbol().getDisplayText(), equalTo("shha"));
		assertThat("list of terms", terms, equalTo("anal fin,anterior neural keel,anterior neural keel ventral region,anterior neural rod,axial chorda mesoderm,axial chorda mesoderm"));
		//		assertThat("list of stages", stages, equalTo("ZFS:0000029,ZFS:0000030,ZFS:0000031,ZFS:0000032,ZFS:0000033,ZFS:0000034,ZFS:0000035,ZFS:0000036,ZFS:0000044"));
	}

	@Test
	public void checkExpressionAPI() {

		ExpressionController controller = new ExpressionController();
		int limit = 15;
//		  String responseString = controller.getExpressionAnnotationsByTaxon("danio", null, limit, 1);
	}

	@Test
	public void checkExpressionAnnotationFiltering() {

		ExpressionController controller = new ExpressionController();
		String[] geneIDs = {"MGI:97570", "ZFIN:ZDB-GENE-080204-52"};
		String termID = null;
		int limit = 15;
		JsonResultResponse<GeneExpressionDocument> response = controller.getExpressionAnnotations(termID, null, null, null, null, null, null, null, null, limit, 1, null, "true", asList(geneIDs));
		List<String> symbolList = response.getResults().stream()
				.map(annotation -> annotation.getGeneExpressionAnnotation().getExpressionAnnotationSubject().getGeneSymbol().getDisplayText())
				.collect(Collectors.toList());
		List<String> termList = response.getResults().stream()
				.map(annotation -> annotation.getGeneExpressionAnnotation().getWhereExpressedStatement())
				.collect(Collectors.toList());
	}

}
