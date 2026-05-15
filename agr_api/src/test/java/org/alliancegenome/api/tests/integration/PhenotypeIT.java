package org.alliancegenome.api.tests.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.alliancegenome.api.entity.GenePhenotypeAnnotationDocument;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.api.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.PhenotypeAnnotation;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;

public class PhenotypeIT {

	private ObjectMapper mapper = new ObjectMapper();
	
	@Inject
	private GeneService geneService;

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
		ConfigHelper.init();

		//geneService = new GeneService();

		mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}


	@Test
	// ZFIN gene: mkks
	public void checkPhenotypeByGeneWithPagination() {

		String geneID = "ZFIN:ZDB-GENE-040426-757";

		Pagination pagination = new Pagination(1, 11, null, null);
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations(geneID, pagination);
		assertResponse(response, 11, 19);

		// add containsFilterValue on phenotype
		pagination.makeSingleFieldFilter(FieldFilter.PHENOTYPE, "som");
		response = geneService.getPhenotypeAnnotations(geneID, pagination);
		assertResponse(response, 6, 6);

	}

	@Test
	public void checkPhenotypesWithoutGenePopup() {

		// ATP7
		String geneID = "FB:FBgn0030343";

		Pagination pagination = new Pagination(1, 60, null, null);
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations(geneID, pagination);
		response.getResults()
				.stream()
				.filter(phenotypeAnnotation -> phenotypeAnnotation.getPrimaryAnnotations() != null)
				.forEach(phenotypeAnnotation -> phenotypeAnnotation.getPrimaryAnnotations().forEach(entity -> {
					assertNotEquals("Direct Gene annotation found. Should be suppressed for: " + entity.getId(), entity.getRelation().getName(), GeneticEntity.CrossReferenceType.GENE);
				}));
	}

	@Test
	public void checkUrlForAllelesInPopup() {

		// cua-1
		String geneID = "WB:WBGene00000834";

		Pagination pagination = new Pagination(1, 10, null, null);
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations(geneID, pagination);
		assertResponse(response, 1, 1);

/*
		response.getResults()
				.stream()
				.filter(phenotypeAnnotation -> phenotypeAnnotation.getPrimaryAnnotations() != null)
				.forEach(phenotypeAnnotation -> {
					phenotypeAnnotation.getPrimaryAnnotations().forEach(entity -> {
						assertNotNull("URL for AGM should not be null: " + entity.getId(), entity.getUrl());
					});
				});
*/
	}


	@Test
	public void checkPhenotypeReferenceNonDuplicated() {

		// top2b
		String geneID = "ZFIN:ZDB-GENE-041008-136";

		Pagination pagination = new Pagination(1, 10, null, null);
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations(geneID, pagination);
		assertResponse(response, 10, 10);

		response.getResults().forEach(phenotypeAnnotation -> {
			int beforeSize = phenotypeAnnotation.getReferences().size();
			int afterSize = phenotypeAnnotation.getReferences().stream().distinct().toList().size();
			assertEquals("No duplicated references", beforeSize, afterSize);
		});
	}

	@Test
	// ZFIN gene: pax2a
	public void checkPhenotypeByGeneWithPaginationPax2a() {

		String geneID = "ZFIN:ZDB-GENE-990415-8";

		Pagination pagination = new Pagination(1, 11, null, null);
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations(geneID, pagination);
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

		int zfinRefCount = (int) response.getTotal();

		pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "pmid");
		response = geneService.getPhenotypeAnnotations(geneID, pagination);
		assertResponse(response, 11, 115);

		assertThat("zfin pubs plus PUB MED pubs gives total number ", zfinRefCount + (int) response.getTotal(), greaterThanOrEqualTo(totalSize));

		// add containsFilterValue on reference: pubmed
		pagination.makeSingleFieldFilter(FieldFilter.FREFERENCE, "239");
		response = geneService.getPhenotypeAnnotations(geneID, pagination);
		assertResponse(response, 11, 15);
	}

	@Test
	public void checkPhenotypeDownload() {
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations("MGI:105043", new Pagination());
		PhenotypeAnnotationToTdfTranslator translator = new PhenotypeAnnotationToTdfTranslator();
/*
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
*/
	}

	@Test
	// ZFIN gene: Pten
	public void checkPhenotypeByGeneWithPaginationPten() {

		String geneID = "MGI:109583";
		Pagination pagination = new Pagination(1, 42, null, null);
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations(geneID, pagination);
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
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations(geneID, pagination);
		assertResponse(response, 10, 50);
		GenePhenotypeAnnotationDocument annotation = response.getResults().get(0);
		assertEquals(annotation.getPhenotypeStatement(), "corpus cardiacum primordium");
		final List<PhenotypeAnnotation> primaryAnnotatedEntities = annotation.getPrimaryAnnotations();
		assertNotNull("Phenotype annotation has Allele as the inferred AGM but missing.", primaryAnnotatedEntities);
		assertEquals("Phenotype annotation with Allele as an inferred AGM", primaryAnnotatedEntities.get(0).getRelation().getName(), GeneticEntity.CrossReferenceType.ALLELE);
	}

	@Test
	// Fly gene: WB:WBGene00000898
	public void checkPhenotypeByGeneWorm() {

		String geneID = "WB:WBGene00002992";
		Pagination pagination = new Pagination(1, 10, null, null);
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations(geneID, pagination);
		assertResponse(response, 10, 17);
		final String ectopicExpressionTransgene = "ectopic expression transgene";
		Optional<GenePhenotypeAnnotationDocument> annotation = response.getResults().stream()
				.filter(annot -> annot.getPhenotypeStatement().equals(ectopicExpressionTransgene))
				.findFirst();
		assertTrue("Did not find a phenotype: " + ectopicExpressionTransgene, annotation.isPresent());
		final List<PhenotypeAnnotation> primaryAnnotatedEntities = annotation.get().getPrimaryAnnotations();
		assertNotNull("Phenotype annotation has Allele as the inferred AGM but missing.", primaryAnnotatedEntities);
		assertEquals("Phenotype annotation with Allele as an inferred AGM", primaryAnnotatedEntities.get(0).getRelation(), GeneticEntity.CrossReferenceType.ALLELE);
	}

	@Test
	public void checkPhenotypeByGeneWithPaginationCua_1() {

		String geneID = "WB:WBGene00000834";
		Pagination pagination = new Pagination(1, 42, null, null);
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations(geneID, pagination);
		assertResponse(response, 1, 1);
		final List<PhenotypeAnnotation> primaryAnnotatedEntities = response.getResults().get(0).getPrimaryAnnotations();
		assertNull("Allele phenotype annotation", primaryAnnotatedEntities);
	}

	@Test
	public void checkPhenotypeOnWBGenes() {

		String geneID = "WB:WBGene00000898";
		Pagination pagination = new Pagination(1, 10, null, null);
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations(geneID, pagination);
		assertResponse(response, 1, 1);
	}

	@Test
	public void checkPhenotypeOnZFINpax2a() {

		String geneID = "ZFIN:ZDB-GENE-990415-8";
		Pagination pagination = new Pagination(1, 10, null, null);
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = geneService.getPhenotypeAnnotations(geneID, pagination);
		assertResponse(response, 1, 1);
		assertThat(response.getResults().get(0).getPhenotypeStatement(), equalTo("anatomical system quality, abnormal"));
		assertNotNull(response.getResults().get(0).getPrimaryAnnotations());
		// more than 4 fish are found for primary entity annotations
		assertThat(response.getResults().get(0).getPrimaryAnnotations().size(), greaterThanOrEqualTo(4));
	}


	private void assertResponse(JsonResultResponse response, int resultSize, int totalSize) {
		assertNotNull(response);
		assertThat("Number of returned records", response.getResults().size(), greaterThanOrEqualTo(resultSize));
		assertThat("Number of total records", (int) response.getTotal(), greaterThanOrEqualTo(totalSize));
	}


}
