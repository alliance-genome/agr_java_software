package org.alliancegenome.api.tests.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.alliancegenome.api.controller.GeneController;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.api.service.CacheStatusService;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.api.service.VariantService;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Construct;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


public class AlleleIT {
	private ObjectMapper mapper = new ObjectMapper();

	private AlleleService alleleService = new AlleleService();
	private AlleleRepository alleleRepository = new AlleleRepository();

	@Inject
	private VariantService variantService;

	@Inject
	private GeneService geneService;

	@Inject
	private CacheStatusService cacheStatusService;

	@Before
	public void before() {
		ConfigHelper.init();

		//alleleService = new AlleleService();

		mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.registerModule(new OrthologyModule());
	}


	@Test
	@Ignore
	public void checkAllelesBySpecies() {
		Pagination pagination = new Pagination();
		pagination.setLimit(100000);
		JsonResultResponse<Allele> response = alleleService.getAllelesBySpecies("dani", pagination);
		System.out.println(response.getTotal());
		assertResponse(response, 40000, 40000);
	}

	@Test
	public void checkAllelesByGene() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Allele> response = alleleService.getAllelesByGene("MGI:109583", pagination);
		assertResponse(response, 19, 19);
	}

	@Test
	public void checkTranscriptExpressedNonBGICC() {
		Pagination pagination = new Pagination();
		//Allele allele = alleleService.getById("FB:FBal0138114");
		Allele allele = alleleService.getById("ZFIN:ZDB-ALT-181128-7");
		assertNotNull(allele);
		assertTrue(allele.getConstructs().get(0).getExpressedGenes().size() > 0);
		assertTrue(allele.getConstructs().get(0).getNonBGIConstructComponents().size() > 0);
	}

	@Test
	public void checkTransgeneCrossReference() {
		Allele allele = alleleService.getById("WB:WBVar00143949");
		assertNotNull(allele);
		assertTrue(allele.getCrossReferenceMap().keySet().contains("primary"));
		assertTrue(allele.getCrossReferenceMap().keySet().contains("references"));

		allele = alleleService.getById("WB:WBTransgene00004656");
		assertNotNull(allele);
		assertTrue(allele.getCrossReferenceMap().keySet().contains("primary"));
		assertTrue(allele.getCrossReferenceMap().keySet().contains("references"));
	}

	@Test
	// Test Sox9 from MGI for disease via experiment records
	public void checkStatus() {
		CacheStatus status = cacheStatusService.getCacheStatus(CacheAlliance.ALLELE_GENE, "FB:FBgn0031717");
		assertNotNull(status);
		Map<CacheAlliance, CacheStatus> map = cacheStatusService.getAllCachStatusRecords();
		assertNotNull(map);
	}

	@Test
	public void checkAllelesGeneLocation() {
		Allele allele = alleleService.getById("ZFIN:ZDB-ALT-161003-18461");
		assertNotNull(allele);
		assertNotNull(allele.getGene());
		List<GenomeLocation> genomeLocations = allele.getGene().getGenomeLocations();
		assertNotNull("No Genome location found on associated gene", genomeLocations);
		assertThat(genomeLocations.size(), greaterThanOrEqualTo(1));
		GenomeLocation location = genomeLocations.get(0);
		assertThat(location.getChromosome(), equalTo("22"));
		assertTrue(location.getStart() > 0);
		assertTrue(location.getEnd() > 0);
	}

	@Test
	public void checkAlleleWithoutGene() {
		Allele allele = alleleService.getById("FB:FBal0316905");
		assertNotNull(allele);
	}

	@Test
	@Ignore
	public void checkAllelesVariantCategory() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Allele> response = alleleService.getAllelesByGene("WB:WBGene00004879", pagination);
		assertResponse(response, 20, 25);
		assertEquals(response.getResults().get(0).getCategory(), "allele with one associated variant");
	}

	@Test
	public void checkAllelesSortedByVariantExistAndAlleleSymbol() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Allele> response = alleleService.getAllelesByGene("WB:WBGene00004879", pagination);
		assertResponse(response, 20, 25);

		//check that alleles with variants come first
		List<Boolean> hasVariants = new ArrayList<>();
		boolean lastVariantsExists = true;
		hasVariants.add(lastVariantsExists);
		for (Allele allele : response.getResults()) {
			boolean currentVariantsExists = CollectionUtils.isNotEmpty(allele.getVariants());
			if (currentVariantsExists != lastVariantsExists) {
				hasVariants.add(currentVariantsExists);
			}
			lastVariantsExists = currentVariantsExists;
			assertTrue("Not all allele with variants come before alleles without variants.", (currentVariantsExists && hasVariants.size() == 1) ||
				(!currentVariantsExists && hasVariants.size() == 2));
		}

		// check that all alleles with variants are sorted by allele symbol
		List<Allele> allVariantAlleles = response.getResults().stream()
			.filter(allele -> CollectionUtils.isNotEmpty(allele.getVariants()))
			.collect(Collectors.toList());

		String alleleIDs = allVariantAlleles.stream().map(GeneticEntity::getPrimaryKey)
			.collect(Collectors.joining(","));

		String alleleIDsAfterSorting = allVariantAlleles.stream()
			.sorted(Comparator.comparing(Allele::getSymbolText))
			.map(GeneticEntity::getPrimaryKey)
			.collect(Collectors.joining(","));

		assertEquals("The alleles are not sorted by symbol", alleleIDsAfterSorting, alleleIDs);

	}

	@Test
	public void checkAllelesByGeneFiltering() {
		Pagination pagination = new Pagination();
		BaseFilter filter = new BaseFilter();
		filter.addFieldFilter(FieldFilter.MOLECULAR_CONSEQUENCE, "deletion");
		pagination.setFieldFilterValueMap(filter);
		JsonResultResponse<Allele> response = alleleService.getAllelesByGene("WB:WBGene00000898", pagination);
		assertResponse(response, 1, 1);
	}

	@Test
	public void checkVariantLocation() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Allele> response = alleleService.getAllelesByGene("FB:FBgn0025832", pagination);
		assertResponse(response, 2, 2);

		response.getResults().stream()
		.map(Allele::getVariants)
		.flatMap(Collection::stream)
		.filter(Objects::nonNull)
		.filter(variant -> variant.getPrimaryKey().equals("NT_033778.4:g.16856124_16856125ins"))
		.forEach(variant -> {
			assertNotNull("Variant location is missing", variant.getLocation());
		}
			);

		response = alleleService.getAllelesByGene("WB:WBGene00006616", pagination);
		assertResponse(response, 1, 1);

		response.getResults().stream()
		.map(Allele::getVariants)
		.flatMap(Collection::stream)
		.filter(Objects::nonNull)
		.filter(variant -> variant.getPrimaryKey().equals("NC_003281.10:g.5690389_5691072del"))
		.forEach(variant -> {
			assertNotNull("Variant location is missing", variant.getLocation());
			assertNotNull("Variant consequence is missing", variant.getGeneLevelConsequence());
		}
			);

	}

	@Test
	public void checkAllelesWithDiseases() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Allele> response = alleleService.getAllelesByGene("ZFIN:ZDB-GENE-040426-1716", pagination);
		List<Allele> term = response.getResults().stream().filter(allele -> allele.getDiseases() != null).collect(Collectors.toList());
		assertThat(term.size(), greaterThanOrEqualTo(1));
	}

	@Test
	public void getVariantsPerAllele() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Variant> response = variantService.getVariants("ZFIN:ZDB-ALT-180515-5", pagination);
		assertThat(response.getTotal(), greaterThanOrEqualTo(1));
		assertNotNull("Computed Gene exists", response.getResults().get(0).getGene());
		assertNotNull("Genomic Location exists on computed Gene", response.getResults().get(0).getGene().getGenomeLocations());
	}

	@Test
	public void getVariantsPerAlleleWithNotes() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Variant> response = variantService.getVariants("FB:FBal0000017", pagination);
		assertThat(response.getTotal(), greaterThanOrEqualTo(1));
		assertNotNull("Computed Gene exists", response.getResults().get(0).getNotes());
	}

	@Test
	public void getVariantsCompleteHgvsNames() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Variant> response = variantService.getVariants("ZFIN:ZDB-ALT-131217-14827", pagination);
		final Variant variant = response.getResults().get(0);
		assertNotNull("Variant exists", variant);
		List<String> expectedValues = List.of("(GRCz11)2:2095171C>T", "2:g.2095171C>T", "NC_007113.7:g.2095171C>T");
		expectedValues.forEach(value -> assertTrue(value + " does not exist as HGVS,g value", variant.getHgvsG().contains(value)));
	}

	@Test
	public void getVariantsPerAlleleWitCrossReference() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Variant> response = variantService.getVariants("WB:WBVar00252636", pagination);
		assertThat(response.getTotal(), greaterThanOrEqualTo(1));
		assertNotNull("Computed Gene exists", response.getResults().get(0).getCrossReferences());
	}

	@Test
	public void getVariantsPerAlleleWitPubliation() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Variant> response = variantService.getVariants("WB:WBVar00087798", pagination);
		assertThat(response.getTotal(), greaterThanOrEqualTo(1));
		assertNotNull("Computed Gene exists", response.getResults().get(0).getCrossReferences());
	}

	@Test
	public void getAllelesPerGene() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Allele> response = geneService.getAlleles("ZFIN:ZDB-GENE-990415-234", pagination);
		assertThat(response.getTotal(), greaterThanOrEqualTo(1));
	}

	@Test
	public void getVariantsWithInsertionDeletion() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Variant> response = variantService.getVariants("ZFIN:ZDB-ALT-181010-2", pagination);
		assertThat(response.getTotal(), greaterThanOrEqualTo(1));
		Variant variant = response.getResults().get(0);
		assertEquals("Nucleotide change of Insertion", "t>tTCCAGAA", variant.getNucleotideChange());

		response = variantService.getVariants("ZFIN:ZDB-ALT-180925-10", pagination);
		assertThat(response.getTotal(), greaterThanOrEqualTo(1));
		variant = response.getResults().get(0);
		assertEquals("Nucleotide change: Deletion", "aGCAGAGGTCA>a", variant.getNucleotideChange());

		response = variantService.getVariants("ZFIN:ZDB-ALT-161003-18461", pagination);
		assertThat(response.getTotal(), greaterThanOrEqualTo(1));
		variant = response.getResults().get(0);
		assertEquals("Nucleotide change: non-insertion, non-deletion", "A>G", variant.getNucleotideChange());
	}

	@Test
	public void getVariantsWithTransposon() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Variant> response = variantService.getVariants("FB:FBal0125489", pagination);
		assertThat(response.getTotal(), greaterThanOrEqualTo(1));
		Variant variant = response.getResults().get(0);
		assertEquals("Nucleotide change of Insertion", "c>cN+", variant.getNucleotideChange());

	}

	@Test
	public void getAlleleInfo() {
		Allele allele = alleleService.getById("ZFIN:ZDB-ALT-161003-18461");
		assertNotNull(allele.getCrossReferences());
	}

	@Test
	public void getAlleleWithConstruct() {
		Allele allele = alleleService.getById("ZFIN:ZDB-ALT-130702-3");
		assertNotNull(allele.getCrossReferences());
		assertNotNull(allele.getConstructs());
		Construct c = allele.getConstructs().get(0);
		assertEquals(c.getNameText(), "Tg(mitfa:GAL4-VP16,UAS:mCherry)");
		assertNotNull(c.getRegulatedByGenes());
		assertEquals(c.getRegulatedByGenes().get(0).getPrimaryKey(), "ZFIN:ZDB-GENE-990910-11");
		assertEquals(c.getRegulatedByGenes().get(0).getSpecies().getType(), SpeciesType.ZEBRAFISH);
	}

	@Test
	public void getAlleleWithConstructFly() {
		Allele allele = alleleService.getById("FB:FBal0240920");
		assertNotNull(allele.getCrossReferences());
		assertNotNull(allele.getConstructs());
		Construct c = allele.getConstructs().get(0);
		assertEquals(c.getNameText(), "P{UAS-imp13.G}");
		assertTrue(CollectionUtils.isNotEmpty(c.getCrossReferences()));
		assertNotNull(c.getExpressedGenes());
		assertEquals(c.getExpressedGenes().get(0).getPrimaryKey(), "FB:FBgn0261532");
		assertEquals(c.getExpressedGenes().get(0).getSpecies().getType(), SpeciesType.FLY);
		assertNotNull(c.getRegulatedByGenes());
		assertEquals(c.getRegulatedByGenes().get(0).getSymbol(), "UASt");
		assertEquals(c.getRegulatedByGenes().get(0).getCrossReferenceType(), GeneticEntity.CrossReferenceType.NON_BGI_CONSTRUCT_COMPONENTS);
	}

	@Test
	public void getAllelePhenotype() {
		//String alleleID = "ZFIN:ZDB-ALT-041001-12";
		//String alleleID = "MGI:5442117";
		// hu3335
		String alleleID = "ZFIN:ZDB-ALT-980203-692";
		JsonResultResponse<PhenotypeAnnotation> response = alleleService.getPhenotype(alleleID, new Pagination());
		assertNotNull(response);
		assertThat(response.getTotal(), greaterThanOrEqualTo(20));
	}

	@Test
	public void getAllelePhenotypeNoAllelePAESelfReference() {
		// Ptentm1.1Mwst
		String alleleID = "MGI:4366755";
		JsonResultResponse<PhenotypeAnnotation> response = alleleService.getPhenotype(alleleID, new Pagination());
		assertNotNull(response);
		assertThat(response.getTotal(), greaterThanOrEqualTo(8));
		response.getResults().stream()
		.filter(phenotypeAnnotation -> phenotypeAnnotation.getPrimaryAnnotatedEntities() != null)
		.forEach(annotation -> {
			annotation.getPrimaryAnnotatedEntities().forEach(entity -> {
				assertNotEquals("Do not have allele direct annotations reference alleles as PAE", entity.getType(), GeneticEntity.CrossReferenceType.ALLELE);
			});
		});
	}

	@Test
	public void checkPhenotypeOnMouseTransgeneAlleles() {

		String alleleID = "MGI:3832950";
		Pagination pagination = new Pagination(1, 10, null, null);
		JsonResultResponse<PhenotypeAnnotation> response = alleleService.getPhenotype(alleleID, pagination);
		assertTrue(response.getTotal() > 0);

		assertPhenotype(response, "abnormal motor learning");
		assertPhenotype(response, "short stride length");
	}

	private void assertPhenotype(JsonResultResponse<PhenotypeAnnotation> response, String phenotype) {
		Optional<PhenotypeAnnotation> phenotytpeOptional = response.getResults().stream()
			.filter(phenotypeAnnotation -> phenotypeAnnotation.getPhenotype().equals(phenotype)).findFirst();
		assertTrue("No phenotype: " + phenotype + " found", phenotytpeOptional.isPresent());
		List<PrimaryAnnotatedEntity> abnormalMotorLearning = phenotytpeOptional.get().getPrimaryAnnotatedEntities();
		assertNotNull(abnormalMotorLearning);
		assertThat("Mouse genotype not found for phenotype annotation: " + phenotype, abnormalMotorLearning.get(0).getId(), equalTo("MGI:3832988"));
	}


	@Test
	public void getAlleleDisease() {
		//String alleleID = "ZFIN:ZDB-ALT-041001-12";
		//String alleleID = "MGI:5442117";
		// hps5
		//String alleleID = "ZFIN:ZDB-ALT-980203-692";
		String alleleID = "MGI:1856424";
		JsonResultResponse<DiseaseAnnotation> response = alleleService.getDisease(alleleID, new Pagination());
		assertNotNull(response);
		assertThat(response.getTotal(), greaterThanOrEqualTo(3));
	}

	@Test
	public void getAlleleVariantDetail(){
		Set<Allele> allAlleles = alleleRepository.getAllAlleleVariantInfoOnGene();
		assertNotNull(allAlleles);
	}

	@Test
	public void getVariantTranscripts() {
		String variantID = "NC_007121.7:g.14484476_14484482del";
		JsonResultResponse<Transcript> response = variantService.getTranscriptsByVariant(variantID, new Pagination());
		assertNotNull(response);
		assertThat(response.getTotal(), greaterThanOrEqualTo(2));
		assertEquals(response.getResults().get(0).getType().getName(), "mRNA");
	}

	private void assertResponse(JsonResultResponse<Allele> response, int resultSize, int totalSize) {
		assertNotNull(response);
		assertThat("Number of returned records", response.getResults().size(), greaterThanOrEqualTo(resultSize));
		assertThat("Number of total records", response.getTotal(), greaterThanOrEqualTo(totalSize));
	}

	@Test
	@Ignore
	public void checkAllelesGeneTableSynonymFilter() {
		GeneController ctrl=new GeneController();
		JsonResultResponse<Allele> response= ctrl.getAllelesPerGene("RGD:2219",10,1,"","true","","brc","","","","","allele");
		assertNotNull(response);

	}
}
