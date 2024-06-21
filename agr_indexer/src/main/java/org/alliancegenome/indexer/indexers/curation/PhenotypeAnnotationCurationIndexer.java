package org.alliancegenome.indexer.indexers.curation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.*;
import org.alliancegenome.core.helpers.DiseaseAnnotationHelper;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.curation_api.model.entities.base.SubmittedObject;
import org.alliancegenome.curation_api.model.entities.ontology.DOTerm;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.alliancegenome.curation_api.model.entities.ontology.OntologyTerm;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.AGMPhenotypeAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.AllelePhenotypeAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.GenePhenotypeAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.VocabularyService;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
public class PhenotypeAnnotationCurationIndexer extends Indexer {

	private GenePhenotypeAnnotationService geneService = new GenePhenotypeAnnotationService();
	private AllelePhenotypeAnnotationService alleleService = new AllelePhenotypeAnnotationService();
	private AGMPhenotypeAnnotationService agmService = new AGMPhenotypeAnnotationService();
	private VocabularyService vocabService = new VocabularyService();
	private DiseaseRepository diseaseRepository;

	private Map<String, Set<String>> closureMap;
	private Map<String, Pair<Gene, ArrayList<PhenotypeAnnotation>>> geneMap = new HashMap<>();
	Map<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> generatedImplicatedGeneMap = new HashMap<>();

	private Map<String, Pair<Allele, ArrayList<PhenotypeAnnotation>>> alleleMap = new HashMap<>();
	private Map<String, Pair<AffectedGenomicModel, ArrayList<PhenotypeAnnotation>>> agmMap = new HashMap<>();

	private Map<Gene, List<DiseaseAnnotation>> geneViaOrthologyMap = new HashMap<>();

	public PhenotypeAnnotationCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {

	}

	@Override
	protected void index() {

		diseaseRepository = new DiseaseRepository();
		closureMap = diseaseRepository.getDOClosureChildMapping();

		indexGenes();
		indexAlleles();
		indexAGMs();

		List<GenePhenotypeAnnotationDocument> geneList = createGeneDiseaseAnnotationDocuments();
		log.info("Indexing " + String.format("%,d", geneList.size()) + " Gene PA documents");
		indexDocuments(geneList);

/*
		List<AlleleDiseaseAnnotationDocument> alleleList = createAlleleDiseaseAnnotationDocuments();
		log.info("Indexing " + alleleList.size() + " allele documents");
		indexDocuments(alleleList);

		List<AGMPhenotypeAnnotationDocument> agmList = createAGMPhenotypeAnnotationDocuments();
		log.info("Indexing " + String.format("%,d", agmList.size()) + " AGM PA documents");
		indexDocuments(agmList);
*/
		log.info("Finished Indexing Phenotype Annotations");
		diseaseRepository.close();
	}

	private List<GeneDiseaseAnnotationDocument> createGeneDiseaseAnnotationViaOrthologyDocuments() {
		ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
		ph.startProcess("Creating Gene Disease Annotations via Orthology", geneViaOrthologyMap.size());
		List<GeneDiseaseAnnotationDocument> returnList = new ArrayList<>();

		geneViaOrthologyMap.forEach((gene, diseaseAnnotations) -> {

			// Group By:
			// Disease, association type, Disease qualifiers, BasedOn Gene List (names),
			Map<DOTerm, Map<VocabularyTerm, Map<String, Map<String, List<DiseaseAnnotation>>>>> groupedByAnnotations = diseaseAnnotations.stream()
				.collect(groupingBy(DiseaseAnnotation::getDiseaseAnnotationObject,
					groupingBy(DiseaseAnnotation::getRelation,
						groupingBy(diseaseAnnotation -> {
							List<VocabularyTerm> terms = diseaseAnnotation.getDiseaseQualifiers();
							// allow for grouping by empty disease qualifiers
							if (CollectionUtils.isEmpty(terms))
								return "null";
							return diseaseAnnotation.getDiseaseQualifiers().stream().map(VocabularyTerm::getName).sorted().collect(Collectors.joining("_"));
						}, groupingBy(diseaseAnnotation -> {
							List<Gene> genes = diseaseAnnotation.getWith();
							// allow for grouping by missing based-on genes
							if (CollectionUtils.isEmpty(genes))
								return "null";
							return diseaseAnnotation.getWith().stream().map(Gene::getIdentifier).sorted().collect(Collectors.joining("_"));
						})))));

			groupedByAnnotations.forEach((diseaseTerm, associationTypeMap) -> {
				associationTypeMap.forEach((associationType, diseaseQualifierMap) -> {
					diseaseQualifierMap.forEach((diseaseQualifier, stringListMap) -> {
						stringListMap.forEach((basedOnGenesList, diseaseAnnotations1) -> {
							DiseaseAnnotation diseaseAnnotation = diseaseAnnotations1.get(0);
							GeneDiseaseAnnotationDocument gdad = new GeneDiseaseAnnotationDocument();
							gdad.setViaOrthologyAnnotation(true);
							gdad.setSubject(gene);
							gdad.setRelation(associationType);
							String generatedRelationString = getGeneratedRelationString(gdad.getRelation().getName(), diseaseAnnotation.getNegated());
							gdad.setGeneratedRelationString(generatedRelationString);
							gdad.setObject(diseaseTerm);
							gdad.setParentSlimIDs(closureMap.get(diseaseAnnotation.getDiseaseAnnotationObject().getCurie()));

							// create distinct and sorted list of ECOTerm objects
							Set<ECOTerm> ecoTerms = diseaseAnnotations1.stream().map(DiseaseAnnotation::getEvidenceCodes).flatMap(Collection::stream).collect(Collectors.toSet());
							gdad.setEvidenceCodes((new ArrayList<>(ecoTerms)).stream().sorted(Comparator.comparing(OntologyTerm::getName)).toList());

							// Create distinct list of disease qualifier term names (nullable)
							Set<String> diseaseQualifiers = diseaseAnnotations1.stream().filter(diseaseAnnotation1 -> CollectionUtils.isNotEmpty(diseaseAnnotation1.getDiseaseQualifiers())).map(diseaseAnnotation1 ->
								diseaseAnnotation1.getDiseaseQualifiers().stream().map(VocabularyTerm::getName).toList()).flatMap(Collection::stream).collect(Collectors.toSet());
							if (CollectionUtils.isNotEmpty(diseaseQualifiers)) {
								gdad.setDiseaseQualifiers(diseaseQualifiers);
							}

							// create distinct list of basedOn Genes
							Set<Gene> basedOnGenes = diseaseAnnotations1.stream().map(DiseaseAnnotation::getWith).flatMap(Collection::stream).collect(Collectors.toSet());
							List<String> ids = basedOnGenes.stream().map(SubmittedObject::getIdentifier).toList();
							gdad.setBasedOnGenes(new ArrayList<>(basedOnGenes));

							gdad.addReference(diseaseAnnotation.getSingleReference());
							gdad.addPubMedPubModID(getPubmedPubModID(diseaseAnnotation.getSingleReference()));

							HashMap<String, Integer> order = SpeciesType.getSpeciesOrderByTaxonID(gene.getTaxon().getCurie());
							gdad.setSpeciesOrder(order);
							int phylogeneticSortOrder = getPhylogeneticSortOrder(gene.getTaxon().getCurie());
							gdad.setPhylogeneticSortingIndex(phylogeneticSortOrder);
							gdad.addPrimaryAnnotation(diseaseAnnotation);
							returnList.add(gdad);
						});
					});
				});
			});
			ph.progressProcess();
		});
		ph.finishProcess();
		return returnList;
	}

	private List<GenePhenotypeAnnotationDocument> createGeneDiseaseAnnotationDocuments() {

		List<GenePhenotypeAnnotationDocument> ret = new ArrayList<>();
		ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
		ph.startProcess("Creating Gene Phenotype Annotations", geneMap.size());

		final VocabularyTerm relationIsImplicatedIn = vocabService.getDiseaseRelationTerms().get("is_implicated_in");

		for (Entry<String, Pair<Gene, ArrayList<PhenotypeAnnotation>>> entry : geneMap.entrySet()) {
			HashMap<String, GenePhenotypeAnnotationDocument> lookup = new HashMap<>();

			for (PhenotypeAnnotation da : entry.getValue().getRight()) {
				Gene gene = entry.getValue().getLeft();
				VocabularyTerm relation = relationIsImplicatedIn;

				if (da instanceof GenePhenotypeAnnotation) {
					relation = da.getRelation();
				} else {
/*
					DiseaseAnnotation generatedAnnotation = createImplicatedDA(da);
					addCreatedDiseaseAnnotationsImplicatedToMap(generatedAnnotation, gene);
*/
				}

				String key = getConsolidationKey(da);

				GenePhenotypeAnnotationDocument gdad = lookup.computeIfAbsent(key, (k) -> new GenePhenotypeAnnotationDocument());
				if (gdad.getSubject() == null) {
					gdad.setSubject(gene);
					gdad.setRelation(relation);
					gdad.setPhenotypeStatement(da.getPhenotypeAnnotationObject());
				}
				populateBasePhenotypeAnnotationDocument(gene, da, gdad);
			}
			ph.progressProcess();
			ret.addAll(lookup.values());
			lookup.clear();
		}
		ph.finishProcess();

		return ret;
	}

	private static int getPhylogeneticSortOrder(String taxonID) {
		int phylogeneticSortOrder = 0;
		SpeciesType speciesType = SpeciesType.getTypeByID(taxonID);
		if (speciesType != null) {
			phylogeneticSortOrder = speciesType.getOrderID();
		}
		return phylogeneticSortOrder;
	}

	private DiseaseAnnotation createImplicatedDA(DiseaseAnnotation da) {
		DiseaseAnnotation implicatedDA = null;
		if (da instanceof AGMDiseaseAnnotation agmAnnotation) {
			AGMDiseaseAnnotation agmAnno = new AGMDiseaseAnnotation();
			agmAnno.setDiseaseAnnotationSubject(agmAnnotation.getDiseaseAnnotationSubject());
			implicatedDA = agmAnno;
		} else if (da instanceof AlleleDiseaseAnnotation alleleAnno) {
			AlleleDiseaseAnnotation alleleDA = new AlleleDiseaseAnnotation();
			alleleDA.setDiseaseAnnotationSubject(alleleAnno.getDiseaseAnnotationSubject());
			implicatedDA = alleleDA;
		}
		implicatedDA.setRelation(da.getRelation());
		implicatedDA.setDiseaseAnnotationObject(da.getDiseaseAnnotationObject());
		implicatedDA.setDiseaseQualifiers(da.getDiseaseQualifiers());
		implicatedDA.setDiseaseQualifiers(da.getDiseaseQualifiers());
		implicatedDA.setSingleReference(da.getSingleReference());
		implicatedDA.setEvidenceCodes(da.getEvidenceCodes());
		return implicatedDA;
	}

	private String getPubmedPubModID(Reference singleReference) {
		List<CrossReference> crossReferences = singleReference.getCrossReferences();
		if (CollectionUtils.isEmpty(crossReferences))
			return null;
		String[] prefixes = {"PMID", "MGI", "RGD", "ZFIN", "FB", "WB", "MGI"};
		for (String prefix : prefixes) {
			Optional<CrossReference> opt = crossReferences.stream().filter((reference) -> reference.getReferencedCurie().startsWith(prefix + ":")).findFirst();
			if (opt.isPresent())
				return opt.get().getReferencedCurie();
		}
		return null;
	}

	private String getGeneratedRelationString(String relation, Boolean negated) {
		if (!negated)
			return relation;
		if (relation.equals("is_model_of")) {
			return "does_not_model";
		}
		return relation.replaceFirst("_", "_not_");
	}

	private List<AGMPhenotypeAnnotationDocument> createAGMPhenotypeAnnotationDocuments() {

		List<AGMPhenotypeAnnotationDocument> ret = new ArrayList<>();

		ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
		ph.startProcess("Creating AGM PS Annotations", agmMap.size());

		for (Entry<String, Pair<AffectedGenomicModel, ArrayList<PhenotypeAnnotation>>> entry : agmMap.entrySet()) {
			HashMap<String, AGMPhenotypeAnnotationDocument> lookup = new HashMap<>();
			AffectedGenomicModel model = entry.getValue().getLeft();
			for (PhenotypeAnnotation da : entry.getValue().getRight()) {
				String key = getConsolidationKey(da);
				// include experiment condition info
				//key += getExperimentConditionConsolidatedKey(da);

				AGMPhenotypeAnnotationDocument adad = lookup.computeIfAbsent(key, (k) -> new AGMPhenotypeAnnotationDocument());
				if (adad.getSubject() == null) {
					adad.setSubject(model);
					adad.setRelation(da.getRelation());
					adad.setPhenotypeStatement(da.getPhenotypeAnnotationObject());
				}
				populateBasePhenotypeAnnotationDocument(model, da, adad);
/*
				populateConditionModifier(da, adad);
				populateExperimentalConditions(da, adad);
*/
			}
			ph.progressProcess();
			ret.addAll(lookup.values());
			lookup.clear();
		}
		ph.finishProcess();
		return ret;
	}

	private void populateBasePhenotypeAnnotationDocument(BiologicalEntity biologicalEntity, PhenotypeAnnotation da, PhenotypeAnnotationDocument dad) {
		dad.addReference(da.getSingleReference());
		dad.addPubMedPubModID(getPubmedPubModID(da.getSingleReference()));
		dad.addPrimaryAnnotation(da);
	}

	private static String getGeneticModifierConsolidatedKey(DiseaseAnnotation da) {
		if (da.getDiseaseGeneticModifierRelation() == null && CollectionUtils.isEmpty(da.getDiseaseGeneticModifiers()))
			return null;
		return da.getDiseaseGeneticModifierRelation() + "_"
			+ da.getDiseaseGeneticModifiers().stream().map(SubmittedObject::getIdentifier).collect(Collectors.joining(","));
	}

	private static String getConditionRelationConsolidatedKey(ConditionRelation relation) {
		if (relation == null)
			return null;
		return relation.getConditions().stream().map(ExperimentalCondition::getConditionSummary).collect(Collectors.joining(","));
	}

	private static String getExperimentConditionConsolidatedKey(DiseaseAnnotation da) {
		if (CollectionUtils.isEmpty(da.getConditionRelations()))
			return null;
		return da.getConditionRelations().stream().map(conditionRelation ->
			conditionRelation.getConditionRelationType().getName() + "_" + getConditionRelationConsolidatedKey(conditionRelation)).collect(Collectors.joining(","));
	}

	private static String getConsolidationKey(DiseaseAnnotation da) {
		return getConsolidationKey(da);
	}

	// Consolidated fields
	// phenotype statement
	private static String getConsolidationKey(PhenotypeAnnotation da) {
		String key = da.getPhenotypeAnnotationObject();
		return key;
	}

	private static void populateGeneticModifier(DiseaseAnnotation da, DiseaseAnnotationDocument adad) {
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifiers())) {
			List<BiologicalEntity> geneticModifiers = da.getDiseaseGeneticModifiers().stream()
				.filter(Objects::nonNull)
				.toList();
			adad.setGeneticModifierList(geneticModifiers);
			List<String> geneticModifierComponents = new ArrayList<>();
			geneticModifierComponents.add(da.getDiseaseGeneticModifierRelation().getName());
			geneticModifierComponents.addAll(geneticModifiers.stream().map(DiseaseAnnotationHelper::getEntityName).toList());
			adad.setGeneticModifierAggregated(String.join(",", geneticModifierComponents));
			adad.setGeneticModifierRelation(da.getDiseaseGeneticModifierRelation());
		}
	}

	private static void populateConditionModifier(DiseaseAnnotation da, DiseaseAnnotationDocument adad) {
		if (CollectionUtils.isNotEmpty(da.getConditionRelations())) {
			List<ConditionRelation> conditionModifiers = da.getConditionRelations().stream()
				.filter(conditionRelation -> conditionRelation.getConditionRelationType() != null)
				.filter(conditionRelation -> conditionRelation.getConditionRelationType().getName().contains("ameliorated") ||
					conditionRelation.getConditionRelationType().getName().contains("exacerbated"))
				.toList();
			adad.setConditionModifierList(conditionModifiers);
			List<String> conditionComponents = new ArrayList<>(conditionModifiers.stream().map(conditionRelation -> conditionRelation.getConditionRelationType().getName()).toList());
			conditionModifiers.forEach(conditionRelation -> conditionRelation.getConditions().forEach(experimentalCondition -> {
				conditionComponents.add(experimentalCondition.getConditionSummary());
			}));
			adad.setConditionModifierAggregated(String.join(",", conditionComponents));
		}
	}

	private static void populateExperimentalConditions(DiseaseAnnotation da, AGMDiseaseAnnotationDocument adad) {
		if (CollectionUtils.isNotEmpty(da.getConditionRelations())) {
			List<ConditionRelation> conditionModifiers = da.getConditionRelations().stream()
				.filter(conditionRelation -> conditionRelation.getConditionRelationType() != null)
				.filter(conditionRelation -> conditionRelation.getConditionRelationType().getName().contains("has_condition") ||
					conditionRelation.getConditionRelationType().getName().contains("induced"))
				.toList();
			adad.setExperimentalConditionList(conditionModifiers);
			List<String> experimentalConditionComponents = new ArrayList<>(conditionModifiers.stream().map(conditionRelation -> conditionRelation.getConditionRelationType().getName()).toList());
			conditionModifiers.forEach(conditionRelation -> conditionRelation.getConditions().forEach(experimentalCondition -> {
				experimentalConditionComponents.add(experimentalCondition.getConditionSummary());
			}));
			adad.setExperimentalConditionsAggregated(String.join(",", experimentalConditionComponents));
		}
	}

	private void indexGenes() {
		List<GenePhenotypeAnnotation> genePhenotypeAnnotations = geneService.getFiltered();
		addPhenotypeAnnotationsToLGlobalMap(genePhenotypeAnnotations);
	}

	private void addPhenotypeAnnotationsToLGlobalMap(List<GenePhenotypeAnnotation> genePhenotypeAnnotations) {
		log.info("Filtered Gene PAs: " + String.format("%,d", genePhenotypeAnnotations.size()));
		for (GenePhenotypeAnnotation da : genePhenotypeAnnotations) {
			Gene gene = da.getPhenotypeAnnotationSubject();
			Pair<Gene, ArrayList<PhenotypeAnnotation>> pair = geneMap.computeIfAbsent(gene.getIdentifier(), geneCurie -> Pair.of(gene, new ArrayList<>()));
			pair.getRight().add(da);
		}
	}

	private void addCreatedDiseaseAnnotationsImplicatedToMap(DiseaseAnnotation geneDiseaseAnnotations, Gene gene) {
		Pair<Gene, ArrayList<DiseaseAnnotation>> pair = generatedImplicatedGeneMap.computeIfAbsent(gene.getIdentifier(), geneCurie -> Pair.of(gene, new ArrayList<>()));
		pair.getRight().add(geneDiseaseAnnotations);
	}

	private void indexAlleles() {

		List<AllelePhenotypeAnnotation> allelePhenotypeAnnotations = alleleService.getFiltered();
		log.info("Filtered Alleles: " + allelePhenotypeAnnotations.size());
		for (AllelePhenotypeAnnotation da : allelePhenotypeAnnotations) {
			Allele allele = da.getPhenotypeAnnotationSubject();
			Pair<Allele, ArrayList<PhenotypeAnnotation>> allelePair = alleleMap.computeIfAbsent(allele.getIdentifier(), alleleCurie -> Pair.of(allele, new ArrayList<>()));
			allelePair.getRight().add(da);

			Gene inferredGene = da.getInferredGene();
			extractGenePhenotypeAnnotations(da, inferredGene);
			if (da.getAssertedGenes() != null) {
				for (Gene gene : da.getAssertedGenes()) {
					extractGenePhenotypeAnnotations(da, gene);
				}
			}
		}

	}

	private void extractGenePhenotypeAnnotations(PhenotypeAnnotation da, Gene inferredGene) {
		if (inferredGene != null && !inferredGene.getInternal()) {
			Pair<Gene, ArrayList<PhenotypeAnnotation>> pair = geneMap.computeIfAbsent(inferredGene.getIdentifier(), k -> Pair.of(inferredGene, new ArrayList<>()));
			pair.getRight().add(da);
		}
	}

	private void indexAGMs() {

		List<AGMPhenotypeAnnotation> agmDiseaseAnnotations = agmService.getFiltered();
		log.info("Filtered AGM PAs: " + String.format("%,d", agmDiseaseAnnotations.size()));

		for (AGMPhenotypeAnnotation da : agmDiseaseAnnotations) {
			AffectedGenomicModel genomicModel = da.getPhenotypeAnnotationSubject();
			Pair<AffectedGenomicModel, ArrayList<PhenotypeAnnotation>> allelePair = agmMap.computeIfAbsent(genomicModel.getIdentifier(), agmCurie -> Pair.of(genomicModel, new ArrayList<>()));
			allelePair.getRight().add(da);

			Gene inferredGene = da.getInferredGene();
			extractGenePhenotypeAnnotations(da, inferredGene);
			if (da.getAssertedGenes() != null) {
				for (Gene gene : da.getAssertedGenes()) {
					extractGenePhenotypeAnnotations(da, gene);
				}
			}
		}
	}

}
