package org.alliancegenome.indexer.indexers.curation;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.AGMDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.DiseaseAnnotationDocument;
import org.alliancegenome.api.entity.GeneDiseaseAnnotationDocument;
import org.alliancegenome.core.helpers.DiseaseAnnotationHelper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AffectedGenomicModel;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.BiologicalEntity;
import org.alliancegenome.curation_api.model.entities.ConditionRelation;
import org.alliancegenome.curation_api.model.entities.CrossReference;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.ExperimentalCondition;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.model.entities.base.SubmittedObject;
import org.alliancegenome.curation_api.model.entities.ontology.DOTerm;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.alliancegenome.curation_api.model.entities.ontology.OntologyTerm;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.AGMDiseaseAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.AlleleDiseaseAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.GeneDiseaseAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.VocabularyService;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiseaseAnnotationCurationIndexer extends Indexer {

	private GeneDiseaseAnnotationService geneService = new GeneDiseaseAnnotationService();
	private AlleleDiseaseAnnotationService alleleService = new AlleleDiseaseAnnotationService();
	private AGMDiseaseAnnotationService agmService = new AGMDiseaseAnnotationService();
	private VocabularyService vocabService = new VocabularyService();
	private DiseaseRepository diseaseRepository;

	private Map<String, Set<String>> closureMap;
	private Map<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> geneMap = new HashMap<>();
	Map<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> generatedImplicatedGeneMap = new HashMap<>();

	private Map<String, Pair<Allele, ArrayList<DiseaseAnnotation>>> alleleMap = new HashMap<>();
	private Map<String, Pair<AffectedGenomicModel, ArrayList<DiseaseAnnotation>>> agmMap = new HashMap<>();

	private Map<Gene, List<DiseaseAnnotation>> geneViaOrthologyMap = new HashMap<>();

	public DiseaseAnnotationCurationIndexer(IndexerConfig indexerConfig) {
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

		List<GeneDiseaseAnnotationDocument> list = createGeneDiseaseAnnotationDocuments();
		createDiseaseAnnotationsFromOrthology();

		List<GeneDiseaseAnnotationDocument> viaOrthologyList = getGeneDiseaseAnnotationViaOrthologyDocuments();
		list.addAll(viaOrthologyList);
		log.info("Indexing " + list.size() + " gene documents");
		indexDocuments(list);

		List<AlleleDiseaseAnnotationDocument> alleleList = createAlleleDiseaseAnnotationDocuments();
		log.info("Indexing " + alleleList.size() + " allele documents");
		indexDocuments(alleleList);

		List<AGMDiseaseAnnotationDocument> agmList = createAGMDiseaseAnnotationDocuments();
		log.info("Indexing " + agmList.size() + " agm documents");
		indexDocuments(agmList);
		log.info("Finished Indexing Disease Annotations");
		diseaseRepository.close();
	}

	private void createDiseaseAnnotationsFromOrthology() {
		geneMap.forEach((geneID, geneArrayListPair) -> {
			Pair<Gene, ArrayList<DiseaseAnnotation>> pairs = generatedImplicatedGeneMap.computeIfAbsent(geneID, id -> geneArrayListPair);
			pairs.getRight().addAll(geneArrayListPair.getRight());
		});
		geneViaOrthologyMap = geneService.getOrthologousGeneDiseaseAnnotations(generatedImplicatedGeneMap);
	}

	private List<GeneDiseaseAnnotationDocument> getGeneDiseaseAnnotationViaOrthologyDocuments() {
		return createGeneDiseaseAnnotationViaOrthologyDocuments();
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
							if (CollectionUtils.isEmpty(terms)) {
								return "null";
							}
							return diseaseAnnotation.getDiseaseQualifiers().stream().map(VocabularyTerm::getName).sorted().collect(Collectors.joining("_"));
						}, groupingBy(diseaseAnnotation -> {
							List<Gene> genes = diseaseAnnotation.getWith();
							// allow for grouping by missing based-on genes
							if (CollectionUtils.isEmpty(genes)) {
								return "null";
							}
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
							Set<String> diseaseQualifiers = diseaseAnnotations1.stream().filter(diseaseAnnotation1 -> CollectionUtils.isNotEmpty(diseaseAnnotation1.getDiseaseQualifiers()))
								.map(diseaseAnnotation1 -> diseaseAnnotation1.getDiseaseQualifiers().stream().map(VocabularyTerm::getName).toList()).flatMap(Collection::stream).collect(Collectors.toSet());
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

	private List<GeneDiseaseAnnotationDocument> createGeneDiseaseAnnotationDocuments() {

		List<GeneDiseaseAnnotationDocument> ret = new ArrayList<>();
		ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
		ph.startProcess("Creating Gene Disease Annotations", geneMap.size());

		final VocabularyTerm relationIsImplicatedIn = vocabService.getDiseaseRelationTerms().get("is_implicated_in");

		for (Entry<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> entry : geneMap.entrySet()) {
			HashMap<String, GeneDiseaseAnnotationDocument> lookup = new HashMap<>();

			for (DiseaseAnnotation da : entry.getValue().getRight()) {
				Gene gene = entry.getValue().getLeft();
				VocabularyTerm relation = relationIsImplicatedIn;

				if (da instanceof GeneDiseaseAnnotation) {
					relation = da.getRelation();
				} else if (da instanceof AGMDiseaseAnnotation agmAnnotation) {
					AGMDiseaseAnnotation agmAnno = new AGMDiseaseAnnotation();
					agmAnno.setDiseaseAnnotationSubject(agmAnnotation.getDiseaseAnnotationSubject());
					copyDAFields(da, agmAnno);
					addCreatedDiseaseAnnotationsImplicatedToMap(agmAnno, gene);
				} else if (da instanceof AlleleDiseaseAnnotation alleleAnno) {
					AlleleDiseaseAnnotation alleleDA = new AlleleDiseaseAnnotation();
					alleleDA.setDiseaseAnnotationSubject(alleleAnno.getDiseaseAnnotationSubject());
					copyDAFields(da, alleleDA);
					addCreatedDiseaseAnnotationsImplicatedToMap(alleleDA, gene);
				} else {
					// Not sure what you want to do here?
					//throw new RuntimeException("CreateImplicatedDA() Disease Annotations can only be used for AGM DAs or Allele DAs.");
				}

				String key = getConsolidationKey(da, relation.getName());

				if (da.getWith() != null && da.getWith().size() > 0) {
					key += "_" + da.getWith().stream().map(Gene::getIdentifier).sorted().collect(Collectors.joining("_"));
				}

				GeneDiseaseAnnotationDocument gdad = lookup.computeIfAbsent(key, k -> new GeneDiseaseAnnotationDocument());
				if (gdad.getSubject() == null) {
					gdad.setSubject(gene);
					HashMap<String, Integer> order = SpeciesType.getSpeciesOrderByTaxonID(gene.getTaxon().getCurie());
					gdad.setSpeciesOrder(order);
					gdad.setRelation(relation);
					String generatedRelationString = getGeneratedRelationString(gdad.getRelation().getName(), da.getNegated());
					gdad.setGeneratedRelationString(generatedRelationString);
					gdad.setObject(da.getDiseaseAnnotationObject());
				}
				populateBaseDiseaseAnnotationDocument(gene, da, gdad);
				gdad.addBasedOnGenes(da.getWith());
			}
			ph.progressProcess();
			ret.addAll(lookup.values());
			lookup.clear();
		}
		ph.finishProcess();

		return ret;
	}
	
	private void copyDAFields(DiseaseAnnotation source, DiseaseAnnotation target) {
		target.setRelation(source.getRelation());
		target.setDiseaseAnnotationObject(source.getDiseaseAnnotationObject());
		target.setDiseaseQualifiers(source.getDiseaseQualifiers());
		target.setSingleReference(source.getSingleReference());
		target.setEvidenceCodes(source.getEvidenceCodes());
	}

	private static int getPhylogeneticSortOrder(String taxonID) {
		int phylogeneticSortOrder = 0;
		SpeciesType speciesType = SpeciesType.getTypeByID(taxonID);
		if (speciesType != null) {
			phylogeneticSortOrder = speciesType.getOrderID();
		}
		return phylogeneticSortOrder;
	}



	private String getPubmedPubModID(Reference singleReference) {
		if (singleReference == null || CollectionUtils.isEmpty(singleReference.getCrossReferences())) {
			return null;
		}
		String[] prefixes = { "PMID", "MGI", "RGD", "ZFIN", "FB", "WB", "MGI" };
		for (String prefix : prefixes) {
			Optional<CrossReference> opt = singleReference.getCrossReferences().stream().filter(reference -> reference.getReferencedCurie().startsWith(prefix + ":")).findFirst();
			if (opt.isPresent()) {
				return opt.get().getReferencedCurie();
			}
		}
		return null;
	}

	private String getGeneratedRelationString(String relation, Boolean negated) {
		if (!negated) {
			return relation;
		}
		if (relation.equals("is_model_of")) {
			return "does_not_model";
		}
		return relation.replaceFirst("_", "_not_");
	}

	private List<AlleleDiseaseAnnotationDocument> createAlleleDiseaseAnnotationDocuments() {

		List<AlleleDiseaseAnnotationDocument> ret = new ArrayList<>();

		ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
		ph.startProcess("Creating Allele Disease Annotations", alleleMap.size());

		VocabularyTerm relation = vocabService.getDiseaseRelationTerms().get("is_implicated_in");

		for (Entry<String, Pair<Allele, ArrayList<DiseaseAnnotation>>> entry : alleleMap.entrySet()) {
			HashMap<String, AlleleDiseaseAnnotationDocument> lookup = new HashMap<>();

			for (DiseaseAnnotation da : entry.getValue().getRight()) {

				// use this relation if inherited (inferred or asserted) from an AGM DA.
				if (da instanceof AlleleDiseaseAnnotation) {
					relation = da.getRelation();
				}

				String key = getConsolidationKey(da, relation.getName());
				AlleleDiseaseAnnotationDocument adad = lookup.computeIfAbsent(key, k -> new AlleleDiseaseAnnotationDocument());
				Allele allele = entry.getValue().getLeft();
				if (adad.getSubject() == null) {
					HashMap<String, Integer> order = SpeciesType.getSpeciesOrderByTaxonID(allele.getTaxon().getCurie());
					adad.setSpeciesOrder(order);
					adad.setSubject(allele);
					adad.setRelation(relation);
					String generatedRelationString = getGeneratedRelationString(relation.getName(), da.getNegated());
					adad.setGeneratedRelationString(generatedRelationString);
					adad.setObject(da.getDiseaseAnnotationObject());
				}
				populateBaseDiseaseAnnotationDocument(allele, da, adad);
			}
			ph.progressProcess();
			ret.addAll(lookup.values());
			lookup.clear();
		}
		ph.finishProcess();
		return ret;
	}

	private List<AGMDiseaseAnnotationDocument> createAGMDiseaseAnnotationDocuments() {

		List<AGMDiseaseAnnotationDocument> ret = new ArrayList<>();

		ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
		ph.startProcess("Creating AGM Disease Annotations", agmMap.size());

		for (Entry<String, Pair<AffectedGenomicModel, ArrayList<DiseaseAnnotation>>> entry : agmMap.entrySet()) {
			HashMap<String, AGMDiseaseAnnotationDocument> lookup = new HashMap<>();
			AffectedGenomicModel model = entry.getValue().getLeft();
			for (DiseaseAnnotation da : entry.getValue().getRight()) {
				String key = getConsolidationKey(da);
				// include genetic modifier info
				key += getGeneticModifierConsolidatedKey(da);
				// include experiment condition info
				key += getExperimentConditionConsolidatedKey(da);

				AGMDiseaseAnnotationDocument adad = lookup.computeIfAbsent(key, k -> new AGMDiseaseAnnotationDocument());
				if (adad.getSubject() == null) {
					HashMap<String, Integer> order = SpeciesType.getSpeciesOrderByTaxonID(model.getTaxon().getCurie());
					adad.setSpeciesOrder(order);
					adad.setSubject(model);
					adad.setRelation(da.getRelation());
					String generatedRelationString = getGeneratedRelationString(da.getRelation().getName(), da.getNegated());
					adad.setGeneratedRelationString(generatedRelationString);
					adad.setObject(da.getDiseaseAnnotationObject());
				}
				populateBaseDiseaseAnnotationDocument(model, da, adad);
				populateConditionModifier(da, adad);
				populateExperimentalConditions(da, adad);
				populateGeneticModifier(da, adad);
			}
			ph.progressProcess();
			ret.addAll(lookup.values());
			lookup.clear();
		}
		ph.finishProcess();
		return ret;
	}

	private void populateBaseDiseaseAnnotationDocument(BiologicalEntity biologicalEntity, DiseaseAnnotation da, DiseaseAnnotationDocument dad) {
		dad.setParentSlimIDs(closureMap.get(da.getDiseaseAnnotationObject().getCurie()));
		// gdad.setDataProvider(da.getDataProvider());
		dad.addReference(da.getSingleReference());
		dad.addPubMedPubModID(getPubmedPubModID(da.getSingleReference()));
		dad.addPrimaryAnnotation(da);
		dad.setPhylogeneticSortingIndex(getPhylogeneticSortOrder(biologicalEntity.getTaxon().getCurie()));
		dad.addEvidenceCodes(da.getEvidenceCodes());
		if (CollectionUtils.isNotEmpty(da.getDiseaseQualifiers())) {
			Set<String> diseaseQualifiers = da.getDiseaseQualifiers().stream().map(VocabularyTerm::getName).collect(Collectors.toSet());
			dad.setDiseaseQualifiers(diseaseQualifiers);
		}
	}

	private static String getGeneticModifierConsolidatedKey(DiseaseAnnotation da) {
		if (da.getDiseaseGeneticModifierRelation() == null && CollectionUtils.isEmpty(da.getDiseaseGeneticModifiers())) {
			return null;
		}
		return da.getDiseaseGeneticModifierRelation() + "_" + da.getDiseaseGeneticModifiers().stream().map(SubmittedObject::getIdentifier).collect(Collectors.joining(","));
	}

	private static String getConditionRelationConsolidatedKey(ConditionRelation relation) {
		if (relation == null) {
			return null;
		}
		return relation.getConditions().stream().map(ExperimentalCondition::getConditionSummary).collect(Collectors.joining(","));
	}

	private static String getExperimentConditionConsolidatedKey(DiseaseAnnotation da) {
		if (CollectionUtils.isEmpty(da.getConditionRelations())) {
			return null;
		}
		return da.getConditionRelations().stream().map(conditionRelation -> conditionRelation.getConditionRelationType().getName() + "_" + getConditionRelationConsolidatedKey(conditionRelation)).collect(Collectors.joining(","));
	}

	private static String getConsolidationKey(DiseaseAnnotation da) {
		return getConsolidationKey(da, null);
	}

	// Consolidated fields
	// association type
	// disease name
	// negation
	// disease qualifier
	private static String getConsolidationKey(DiseaseAnnotation da, String relation) {
		String key = da.getRelation().getName();
		if (relation != null) {
			key = relation;
		}
		key += "_" + da.getDiseaseAnnotationObject().getName() + "_" + da.getNegated();
		if (da.getDiseaseQualifiers() != null) {
			key += "_" + da.getDiseaseQualifiers().stream().map(VocabularyTerm::getName).sorted().collect(Collectors.joining("_"));
		}
		return key;
	}

	private static void populateGeneticModifier(DiseaseAnnotation da, DiseaseAnnotationDocument adad) {
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifiers())) {
			List<BiologicalEntity> geneticModifiers = da.getDiseaseGeneticModifiers().stream().filter(Objects::nonNull).toList();
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
			List<ConditionRelation> conditionModifiers = da.getConditionRelations().stream().filter(conditionRelation -> conditionRelation.getConditionRelationType() != null)
				.filter(conditionRelation -> conditionRelation.getConditionRelationType().getName().contains("ameliorated") || conditionRelation.getConditionRelationType().getName().contains("exacerbated")).toList();
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
			List<ConditionRelation> conditionModifiers = da.getConditionRelations().stream().filter(conditionRelation -> conditionRelation.getConditionRelationType() != null)
				.filter(conditionRelation -> conditionRelation.getConditionRelationType().getName().contains("has_condition") || conditionRelation.getConditionRelationType().getName().contains("induced")).toList();
			adad.setExperimentalConditionList(conditionModifiers);
			List<String> experimentalConditionComponents = new ArrayList<>(conditionModifiers.stream().map(conditionRelation -> conditionRelation.getConditionRelationType().getName()).toList());
			conditionModifiers.forEach(conditionRelation -> conditionRelation.getConditions().forEach(experimentalCondition -> {
				experimentalConditionComponents.add(experimentalCondition.getConditionSummary());
			}));
			adad.setExperimentalConditionsAggregated(String.join(",", experimentalConditionComponents));
		}
	}

	private void indexGenes() {
		List<GeneDiseaseAnnotation> geneDiseaseAnnotations = geneService.getFiltered();
		addDiseaseAnnotationsToLGlobalMap(geneDiseaseAnnotations);
	}

	private void addDiseaseAnnotationsToLGlobalMap(List<GeneDiseaseAnnotation> geneDiseaseAnnotations) {
		log.info("Filtered Genes: " + geneDiseaseAnnotations.size());
		for (GeneDiseaseAnnotation da : geneDiseaseAnnotations) {
			Gene gene = da.getDiseaseAnnotationSubject();
			Pair<Gene, ArrayList<DiseaseAnnotation>> pair = geneMap.computeIfAbsent(gene.getIdentifier(), geneCurie -> Pair.of(gene, new ArrayList<>()));
			pair.getRight().add(da);
		}
	}

	private void addCreatedDiseaseAnnotationsImplicatedToMap(DiseaseAnnotation geneDiseaseAnnotations, Gene gene) {
		Pair<Gene, ArrayList<DiseaseAnnotation>> pair = generatedImplicatedGeneMap.computeIfAbsent(gene.getIdentifier(), geneCurie -> Pair.of(gene, new ArrayList<>()));
		pair.getRight().add(geneDiseaseAnnotations);
	}

	private void indexAlleles() {

		List<AlleleDiseaseAnnotation> alleleDiseaseAnnotations = alleleService.getFiltered();
		log.info("Filtered Alleles: " + alleleDiseaseAnnotations.size());
		for (AlleleDiseaseAnnotation da : alleleDiseaseAnnotations) {
			Allele allele = da.getDiseaseAnnotationSubject();
			Pair<Allele, ArrayList<DiseaseAnnotation>> allelePair = alleleMap.computeIfAbsent(allele.getIdentifier(), alleleCurie -> Pair.of(allele, new ArrayList<>()));
			allelePair.getRight().add(da);

			Gene inferredGene = da.getInferredGene();
			extractGeneDiseaseAnnotations(da, inferredGene);
			if (da.getAssertedGenes() != null) {
				for (Gene gene : da.getAssertedGenes()) {
					extractGeneDiseaseAnnotations(da, gene);
				}
			}
		}

	}

	private void extractGeneDiseaseAnnotations(DiseaseAnnotation da, Gene inferredGene) {
		if (inferredGene != null && !inferredGene.getInternal()) {
			Pair<Gene, ArrayList<DiseaseAnnotation>> pair = geneMap.computeIfAbsent(inferredGene.getIdentifier(), k -> Pair.of(inferredGene, new ArrayList<>()));
			pair.getRight().add(da);
		}
	}

	private void extractAlleleDiseaseAnnotations(DiseaseAnnotation da, Allele inferredAllele) {
		if (inferredAllele != null && !inferredAllele.getInternal()) {
			Pair<Allele, ArrayList<DiseaseAnnotation>> pair = alleleMap.computeIfAbsent(inferredAllele.getIdentifier(), k -> Pair.of(inferredAllele, new ArrayList<>()));
			pair.getRight().add(da);
		}
	}

	private void indexAGMs() {

		List<AGMDiseaseAnnotation> agmDiseaseAnnotations = agmService.getFiltered();
		log.info("Filtered AGMs: " + agmDiseaseAnnotations.size());

		for (AGMDiseaseAnnotation da : agmDiseaseAnnotations) {
			AffectedGenomicModel genomicModel = da.getDiseaseAnnotationSubject();
			Pair<AffectedGenomicModel, ArrayList<DiseaseAnnotation>> allelePair = agmMap.computeIfAbsent(genomicModel.getIdentifier(), agmCurie -> Pair.of(genomicModel, new ArrayList<>()));
			allelePair.getRight().add(da);

			Gene inferredGene = da.getInferredGene();
			extractGeneDiseaseAnnotations(da, inferredGene);
			if (da.getAssertedGenes() != null) {
				for (Gene gene : da.getAssertedGenes()) {
					extractGeneDiseaseAnnotations(da, gene);
				}
			}

			Allele inferredAllele = da.getInferredAllele();
			extractAlleleDiseaseAnnotations(da, inferredAllele);
			if (da.getAssertedAllele() != null) {
				extractAlleleDiseaseAnnotations(da, da.getAssertedAllele());
			}
		}
	}

}
