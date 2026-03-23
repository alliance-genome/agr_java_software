package org.alliancegenome.api.tests.integration;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;
import static org.alliancegenome.api.service.ExpressionService.CELLULAR_COMPONENT;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.api.controller.ExpressionController;
import org.alliancegenome.api.controller.GeneController;
import org.alliancegenome.api.dto.ExpressionSummary;
import org.alliancegenome.api.dto.ExpressionSummaryGroup;
import org.alliancegenome.api.dto.ExpressionSummaryGroupTerm;
import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;

public class GeneIT {

	@Inject
	private GeneService geneService;

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
	public void checkForSecondaryId() {
		// ZFIN:ZDB-GENE-030131-3355 is a secondary ID for ZFIN:ZDB-LINCRNAG-160518-1
		Gene gene = geneService.getById("ZFIN:ZDB-GENE-030131-3355");
		assertNotNull(gene);
		assertThat(gene.getPrimaryKey(), equalTo("ZFIN:ZDB-LINCRNAG-160518-1"));
		assertThat(gene.getSpecies().getName(), equalTo("Danio rerio"));
	}

	@Test
	public void checkForSynonyms() {
		// ZFIN:ZDB-GENE-030131-3355 is a secondary ID for ZFIN:ZDB-LINCRNAG-160518-1
		Gene gene = geneService.getById("ZFIN:ZDB-GENE-001103-1");
		assertNotNull(gene);
		assertNotNull(gene.getSynonyms());
		assertThat(gene.getSynonyms().size(), greaterThan(3));
	}

	@Test
	public void checkOrthologyAPIWithFilter() {

		GeneController controller = new GeneController();
		String[] geneIDs = {"RGD:2129"};
		JsonResultResponse<GeneToGeneOrthologyDocument> response = controller.getGeneOrthology("MGI:109583", asList(geneIDs), null, "stringENT", null, null, 20, 1);
		assertThat("Matches found for containsFilterValue 'stringent", (int) response.getTotal(), greaterThan(0));
	}

	@Test
	public void checkOrthologyForListOfGenes() {

		GeneController controller = new GeneController();
		JsonResultResponse<GeneToGeneOrthologyDocument> response = controller.getGeneOrthology("MGI:109583", null, null, "stringENT", null, null, 20, 1);
		assertThat("Matches found for containsFilterValue 'stringent", (int) response.getTotal(), greaterThan(0));
	}

	@Test
	public void checkOrthologyAPIWithSpecies() {

		GeneController controller = new GeneController();
		JsonResultResponse<GeneToGeneOrthologyDocument> response = controller.getGeneOrthology("MGI:109583", null, null, "stringent", null, null, 20, 1);
		assertThat("No matches found for species 'NCBITaxon:10115", (int) response.getTotal(), greaterThan(5));

		String taxonArray = "NCBITaxon:10116";
		response = controller.getGeneOrthology("MGI:109583", null, null, null, taxonArray, null, 20, 1);
		assertThat("matches found for method species NCBITaxon:10116", (int) response.getTotal(), greaterThan(0));

		response = controller.getGeneOrthology("MGI:109583", null, null, "stringent", taxonArray, null, 20, 1);
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
	public void checkOrthologyAPIWithMethods() {

		GeneController controller = new GeneController();
		String methods = "ZFIN";
		JsonResultResponse<GeneToGeneOrthologyDocument> response = controller.getGeneOrthology("MGI:109583", null, null, null, null, methods, 20, 1);
		assertThat("No match against method 'ZFIN'", (int) response.getTotal(), greaterThan(0));

		methods = "OrthoFinder";
		response = controller.getGeneOrthology("MGI:109583", null, null, null, null, methods, 20, 1);
		assertThat("matches found for method 'OrthoFinder'", (int) response.getTotal(), greaterThan(0));

		methods = "ZFIN";
		response = controller.getGeneOrthology("MGI:109583", null, null, null, null, methods, 20, 1);
		assertThat("no matches found for method 'OrthoFinder and ZFIN'", (int) response.getTotal(), greaterThan(0));

		methods = "PANTHER";
		response = controller.getGeneOrthology("MGI:109583", null, null, null, null, methods, 20, 1);
		assertThat("matches found for method 'OrthoFinder and Panther'", (int) response.getTotal(), greaterThan(0));
	}

	@Test
	public void checkOrthologyAPINoFilters() {

		GeneController controller = new GeneController();
		JsonResultResponse<GeneToGeneOrthologyDocument> response = controller.getGeneOrthology("MGI:109583", null, null, null, null, null, 20, 1);
		assertThat("matches found for gene MGI:109583'", (int) response.getTotal(), greaterThan(0));
	}

	@Test
	public void checkExpressionSummary() {

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
			switch (expressionSummaryGroupTerm.getName()) {
				case "extracellular region":
					assertThat(expressionSummaryGroupTerm.getNumberOfAnnotations(), equalTo(4));
					break;
				case "protein-containing complex":
					assertThat(expressionSummaryGroupTerm.getNumberOfAnnotations(), equalTo(3));
					break;
				case "other locations":
					assertThat(expressionSummaryGroupTerm.getNumberOfAnnotations(), equalTo(3));
					break;
				default:
					assertThat(expressionSummaryGroupTerm.getNumberOfAnnotations(), equalTo(0));
					break;
			}
		});
	}

	@Test
	public void checkExpressionSummaryGOAndAO() {

		GeneController controller = new GeneController();
		ExpressionSummary response = controller.getExpressionSummary("ZFIN:ZDB-GENE-980526-188");
		assertThat("matches found for gene ZFIN:ZDB-GENE-980526-188'", response.getTotalAnnotations(), greaterThanOrEqualTo(26));
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
		assertThat("first element species", response.getResults().get(0).getGeneExpressionAnnotation().getExpressionAnnotationSubject().getTaxon().getName(), equalTo("Danio rerio"));
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
		assertThat("first element species", response.getResults().get(0).getGeneExpressionAnnotation().getExpressionAnnotationSubject().getTaxon().getName(), equalTo("Danio rerio"));
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

	@Test
	public void checkCrossReferenceOnGene() {
		GeneRepository repository = new GeneRepository();
		Gene gene = repository.getOneGene("ZFIN:ZDB-GENE-001103-1");
		assertNotNull(gene);
		assertTrue("No CrossReferences on gene object", CollectionUtils.isNotEmpty(gene.getCrossReferences()));
	}

	@Test
	public void getAlleleConstructInfoOnGenePage() {
		GeneRepository repository = new GeneRepository();
		//final String geneID = "WB:WBGene00002992";
		final String geneID = "FB:FBgn0284084";
		Gene gene = repository.getOneGene(geneID);
		assertNotNull(gene);
		AlleleRepository alleleRepository = new AlleleRepository();
		List<Allele> transgenicAlleles = alleleRepository.getTransgenicAlleles(geneID);

		assertTrue("No CrossReferences on gene object", CollectionUtils.isNotEmpty(gene.getCrossReferences()));
	}

	@Test
	public void getAlleleConstructInfoOnZfinGenePage() {
		GeneRepository repository = new GeneRepository();
		final String geneID = "ZFIN:ZDB-GENE-060526-31";
		Gene gene = repository.getOneGene(geneID);
		assertNotNull(gene);
		AlleleRepository alleleRepository = new AlleleRepository();
		List<Allele> transgenicAlleles = alleleRepository.getTransgenicAlleles(geneID);

		assertNotNull(transgenicAlleles);
		assertTrue(transgenicAlleles.size() > 0);
	}


}
