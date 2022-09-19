package org.alliancegenome.cacher.cachers;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.service.DiseaseRibbonService;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.ConditionService;
import org.alliancegenome.cache.repository.helper.DiseaseAnnotationSorting;
import org.alliancegenome.cache.repository.helper.SortingField;
import org.alliancegenome.core.util.ModelHelper;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.DiseaseEntityJoin;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.PublicationJoin;
import org.alliancegenome.neo4j.entity.node.SequenceTargetingReagent;
import org.alliancegenome.neo4j.entity.node.Source;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class DiseaseCacher extends Cacher {

	private static DiseaseRepository diseaseRepository;
	private DiseaseRibbonService diseaseRibbonService;

	@Override
	protected void init() {
		diseaseRepository = new DiseaseRepository();
		diseaseRibbonService = new DiseaseRibbonService(diseaseRepository);
	}

	protected void cache() {

		// model type of diseases
		populateModelsWithDiseases();

		startProcess("diseaseRepository.getAllDiseaseEntityGeneJoins");
		Set<DiseaseEntityJoin> joinList = diseaseRepository.getAllDiseaseEntityGeneJoins();
		if (joinList == null)
			return;

		if (useCache) {
			joinList = joinList.stream()
					.filter(diseaseEntityJoin -> diseaseEntityJoin.getGene() != null)
					.filter(diseaseEntityJoin -> diseaseEntityJoin.getGene().getPrimaryKey().equals("WB:WBGene00018878") ||
							diseaseEntityJoin.getGene().getPrimaryKey().equals("MGI:109583"))
					//.filter(diseaseEntityJoin -> diseaseEntityJoin.getGene().getPrimaryKey().equals("FB:FBgn0030343"))
					.collect(toSet());
		}
		finishProcess();

		startProcess("Add PAEs to DiseaseAnnotations");
		List<DiseaseAnnotation> allDiseaseAnnotations = getDiseaseAnnotationsFromDEJs(joinList);
		finishProcess();

		joinList.clear();


		log.info("Number of DiseaseAnnotation object before merge: " + String.format("%,d", allDiseaseAnnotations.size()));
		// merge disease Annotations with the same
		// disease / gene / association type combination
		//mergeDiseaseAnnotationsByAGM(allDiseaseAnnotations);
		log.info("Number of DiseaseAnnotation object after merge: " + String.format("%,d", allDiseaseAnnotations.size()));


		// default sorting
		DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
		allDiseaseAnnotations.sort(sorting.getComparator(SortingField.DEFAULT, Boolean.TRUE));


		// loop over all disease IDs (termID)
		// and store the annotations in a map for quick retrieval

		Map<String, List<DiseaseAnnotation>> diseaseAnnotationTermMap = allDiseaseAnnotations.stream()
				.collect(groupingBy(annotation -> annotation.getDisease().getPrimaryKey()));

		// add annotations for all parent terms
		Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = getAnnotationMapIncludingClosure(diseaseAnnotationTermMap);

		log.info("Number of IDs in Map before adding gene IDs: " + diseaseAnnotationMap.size());

		// Create map with genes as keys and their associated disease annotations as values
		// Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
		Map<String, List<DiseaseAnnotation>> redundantDiseaseAnnotationGeneMap = allDiseaseAnnotations.stream()
				.filter(annotation -> annotation.getSortOrder() < 10)
				.filter(annotation -> annotation.getGene() != null)
				.collect(groupingBy(o -> o.getGene().getPrimaryKey(), Collectors.toList()));
		Map<String, List<DiseaseAnnotation>> diseaseAnnotationExperimentGeneMap = mergeDiseaseAnnotationsForGenes(redundantDiseaseAnnotationGeneMap);

		diseaseAnnotationMap.putAll(diseaseAnnotationExperimentGeneMap);

		redundantDiseaseAnnotationGeneMap.clear();

		log.info("Number of IDs in the Map after adding genes IDs: " + diseaseAnnotationMap.size());

		storeIntoCache(allDiseaseAnnotations, diseaseAnnotationMap, CacheAlliance.DISEASE_ANNOTATION_GENE_LEVEL_GENE_DISEASE);

		diseaseAnnotationMap.clear();
		diseaseAnnotationTermMap.clear();
		diseaseAnnotationExperimentGeneMap.clear();
		allDiseaseAnnotations.clear();


		// take care of allele
		populateAllelesCache();

		diseaseRepository.clearCache();

	}

	private Map<String, List<DiseaseAnnotation>> getAnnotationMapIncludingClosure(Map<String, List<DiseaseAnnotation>> diseaseAnnotationTermMap) {
		Map<String, Set<String>> closureMapping = diseaseRepository.getClosureMapping();
		log.info("Number of Disease IDs: " + closureMapping.size());
		final Set<String> allIDs = closureMapping.keySet();

		Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = new HashMap<>();
		allIDs.forEach(termID -> {
			Set<String> allDiseaseIDs = closureMapping.get(termID);
			List<DiseaseAnnotation> allAnnotations = new ArrayList<>();
			allDiseaseIDs.stream()
					.filter(id -> diseaseAnnotationTermMap.get(id) != null)
					.forEach(id -> allAnnotations.addAll(diseaseAnnotationTermMap.get(id)));
			diseaseAnnotationMap.put(termID, allAnnotations);
		});
		return diseaseAnnotationMap;
	}

	private Map<String, List<DiseaseAnnotation>> mergeDiseaseAnnotationsForGenes(Map<String, List<DiseaseAnnotation>> map) {
		if (map == null)
			return null;

		Map<String, List<DiseaseAnnotation>> returnMap = new HashMap<>();
		map.forEach((geneID, annotations) -> {
			// group by association type and Disease
			Map<String, Map<String, List<DiseaseAnnotation>>> groupedDA = annotations.stream()
					.collect(groupingBy(DiseaseAnnotation::getAssociationType, groupingBy(annotation -> annotation.getDisease().getPrimaryKey())));
			// merge all DAs in a single group
			List<DiseaseAnnotation> distinctAnnotations = new ArrayList<>();
			groupedDA.forEach((assocType, annotationMap) -> {
				annotationMap.forEach((diseaseID, diseaseAnnotations) -> {
					// if orthologyGenes present do not merge (SGD-specific rule)
					diseaseAnnotations.stream()
							.filter(diseaseAnnotation -> CollectionUtils.isNotEmpty(diseaseAnnotation.getOrthologyGenes()))
							.forEach(distinctAnnotations::add);

					// remove those from list of annotations that need to be merged.
					diseaseAnnotations.removeIf(diseaseAnnotation -> CollectionUtils.isNotEmpty(diseaseAnnotation.getOrthologyGenes()));
					if (CollectionUtils.isEmpty(diseaseAnnotations))
						return;
					// use first element as the merging element
					// merge: primaryAnnotatedEntity, PublicationJoin
					DiseaseAnnotation annotation = diseaseAnnotations.get(0);
					diseaseAnnotations.remove(0);
					diseaseAnnotations.forEach(singleAnnotation -> {
						annotation.addAllPrimaryAnnotatedEntities(singleAnnotation.getPrimaryAnnotatedEntities());
						annotation.addPublicationJoins(singleAnnotation.getPublicationJoins());
					});
					distinctAnnotations.add(annotation);
				});
			});
			returnMap.put(geneID, distinctAnnotations);
		});
		return returnMap;
	}

	private Map<String, List<DiseaseAnnotation>> mergeDiseaseAnnotationsForAlleles(Map<String, List<DiseaseAnnotation>> map) {
		if (map == null)
			return null;

		Map<String, List<DiseaseAnnotation>> returnMap = new HashMap<>();
		map.forEach((geneID, annotations) -> {
			// group by association type, Disease, alleleID
			Map<String, Map<String, Map<String, List<DiseaseAnnotation>>>> groupedDA = annotations.stream()
					.filter(annotation -> annotation.getFeature() != null)
					.collect(groupingBy(DiseaseAnnotation::getAssociationType, groupingBy(annotation -> annotation.getDisease().getPrimaryKey(), groupingBy(annotation -> annotation.getFeature().getPrimaryKey()))));
			// merge all DAs in a single group
			List<DiseaseAnnotation> distinctAnnotations = new ArrayList<>();
			groupedDA.forEach((assocType, annotationMap) -> {
				annotationMap.forEach((diseaseID, diseaseAnnotations) -> {
					diseaseAnnotations.forEach((alleleID, diseaseAnnot) -> {
						// use first element as the merging element
						// merge: primaryAnnotatedEntity, PublicationJoin
						DiseaseAnnotation annotation = diseaseAnnot.get(0);
						diseaseAnnot.remove(0);
						diseaseAnnot.forEach(singleAnnotation -> {
							annotation.addAllPrimaryAnnotatedEntities(singleAnnotation.getPrimaryAnnotatedEntities());
							annotation.addPublicationJoins(singleAnnotation.getPublicationJoins());
						});
						distinctAnnotations.add(annotation);
					});
				});
			});
			returnMap.put(geneID, distinctAnnotations);
		});
		return returnMap;
	}

	private void storeIntoCache(List<DiseaseAnnotation> diseaseAnnotations, Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap, CacheAlliance cacheSpace) {
		populateCacheFromMap(diseaseAnnotationMap, View.DiseaseCacher.class, cacheSpace);

		log.debug("Calculate statistics...");
		CacheStatus status = new CacheStatus(cacheSpace);
		status.setNumberOfEntities(diseaseAnnotations.size());
		status.setNumberOfEntityIDs(diseaseAnnotationMap.size());
		status.setCollectionEntity(DiseaseAnnotation.class.getSimpleName());
		status.setJsonViewClass(View.DiseaseAnnotationSummary.class.getSimpleName());

		Map<String, List<DiseaseAnnotation>> speciesStats = diseaseAnnotations.stream()
				.collect(groupingBy(annotation -> annotation.getSpecies().getName()));

		Map<String, Integer> stats = new TreeMap<>();
		diseaseAnnotationMap.forEach((diseaseID, annotations) -> stats.put(diseaseID, annotations.size()));

		Arrays.stream(SpeciesType.values())
				.filter(speciesType -> !speciesStats.keySet().contains(speciesType.getName()))
				.forEach(speciesType -> speciesStats.put(speciesType.getName(), new ArrayList<>()));

		Map<String, Integer> speciesStatsInt = new HashMap<>();
		speciesStats.forEach((species, alleles) -> speciesStatsInt.put(species, alleles.size()));

		Map<String, Integer> sortedMap = speciesStatsInt
				.entrySet()
				.stream()
				.sorted(Collections.reverseOrder(comparingByValue()))
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

		status.setEntityStats(stats);
		status.setSpeciesStats(sortedMap);
		setCacheStatus(status);
		log.debug("Finished calculating statistics");
		finishProcess();
	}

	public void storeIntoCachePAE(Map<String, List<PrimaryAnnotatedEntity>> primaryAnnotatedMap, CacheAlliance cacheSpace) {
		populateCacheFromMap(primaryAnnotatedMap, View.PrimaryAnnotation.class, cacheSpace);
		log.debug("Calculate statistics...");
		CacheStatus status = new CacheStatus(cacheSpace);
		status.setNumberOfEntityIDs(primaryAnnotatedMap.size());
		status.setCollectionEntity(PrimaryAnnotatedEntity.class.getSimpleName());
		status.setJsonViewClass(View.PrimaryAnnotation.class.getSimpleName());

		List<PrimaryAnnotatedEntity> annotatedEntities = primaryAnnotatedMap.values().stream()
				.flatMap(Collection::stream)
				.collect(toList());
		status.setNumberOfEntities(annotatedEntities.size());

		Map<String, List<Species>> speciesStats = annotatedEntities.stream()
				.map(PrimaryAnnotatedEntity::getSpecies)
				.collect(groupingBy(Species::getName));

		Map<String, Integer> entityStats = new TreeMap<>();
		primaryAnnotatedMap.forEach((geneID, alleles) -> entityStats.put(geneID, alleles.size()));

		populateStatisticsOnStatus(status, entityStats, speciesStats);
		setCacheStatus(status);

		log.debug("Finished calculating statistics");
		finishProcess();
	}

	private void populateModelsWithDiseases() {
		// disease annotations for models (AGMs)
		List<DiseaseEntityJoin> modelDiseaseJoins = diseaseRepository.getAllDiseaseAnnotationsModelLevel();
		log.info("Retrieved " + String.format("%,d", modelDiseaseJoins.size()) + " DiseaseEntityJoin records for AGMs");

		// diseaseEntityJoin PK, List<Gene>
		Map<String, List<Gene>> modelGenesMap = new HashMap<>();

		modelDiseaseJoins.stream()
				.filter(join -> CollectionUtils.isNotEmpty(join.getModel().getAlleles()))
				.forEach(join -> {
					Set<Gene> geneList = join.getModel().getAlleles().stream()
							.map(Allele::getGene)
							.collect(toSet());
					geneList.removeIf(Objects::isNull);
					if (CollectionUtils.isEmpty(geneList))
						return;
					final String primaryKey = join.getPrimaryKey();
					List<Gene> genes = modelGenesMap.get(primaryKey);
					if (genes == null) {
						genes = new ArrayList<>();
					}
					genes.addAll(geneList);
					genes = genes.stream().distinct().collect(toList());
					modelGenesMap.put(primaryKey, genes);
				});
		modelDiseaseJoins.stream()
				.filter(join -> CollectionUtils.isNotEmpty(join.getModel().getSequenceTargetingReagents()))
				.forEach(join -> {
					Set<Gene> geneList = join.getModel().getSequenceTargetingReagents().stream()
							.map(SequenceTargetingReagent::getGene)
							.collect(toSet());
					geneList.removeIf(Objects::isNull);
					if (CollectionUtils.isEmpty(geneList))
						return;
					final String primaryKey = join.getPrimaryKey();
					List<Gene> genes = modelGenesMap.get(primaryKey);
					if (genes == null) {
						genes = new ArrayList<>();
					}
					genes.addAll(geneList);
					genes = genes.stream().distinct().collect(toList());
					modelGenesMap.put(primaryKey, genes);
				});

		List<DiseaseAnnotation> diseaseModelAnnotations = modelDiseaseJoins.stream()
				.map(join -> {
					DiseaseAnnotation document = new DiseaseAnnotation();
					final AffectedGenomicModel model = join.getModel();
					document.setModel(model);
					document.setPrimaryKey(join.getPrimaryKey());
					document.setDisease(join.getDisease());
					document.setPublications(join.getPublications());
					document.setAssociationType(join.getJoinType());
					document.addModifier(DiseaseAnnotation.ConditionType.AMELIORATES, join.getAmeliorateConditionList());
					document.addModifier(DiseaseAnnotation.ConditionType.EXACERBATES, join.getExacerbateConditionList());
					document.addConditions(DiseaseAnnotation.ConditionType.HAS_CONDITION, join.getHasConditionList());
					document.addConditions(DiseaseAnnotation.ConditionType.INDUCES, join.getInducerConditionList());
					document.addPublicationJoins(join.getPublicationJoins());
					Source source = new Source();
					source.setName(model.getDataProvider());
					document.setSource(source);
					document.setEcoCodes(join.getEvidenceCodes());
					return document;
				})
				.collect(Collectors.toList());

		Map<String, List<DiseaseAnnotation>> diseaseModelsMap = diseaseModelAnnotations.stream()
				.collect(groupingBy(annotation -> annotation.getDisease().getPrimaryKey()));

		// add annotations for all parent terms
		Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = getAnnotationMapIncludingClosure(diseaseModelsMap);


		storeIntoCache(diseaseModelAnnotations, diseaseAnnotationMap, CacheAlliance.DISEASE_ANNOTATION_MODEL_LEVEL_DISEASE);
		modelDiseaseJoins.clear();

		Map<String, DiseaseAnnotation> diseaseAnnotationMap1 = diseaseModelAnnotations.stream()
				.collect(toMap(DiseaseAnnotation::getPrimaryKey, entity -> entity));

		diseaseModelAnnotations.clear();

		// get index by geneID
		// <geneID, Map<modelID, List<DiseaseAnnotation>>
		Map<String, Map<String, List<DiseaseAnnotation>>> diseaseModelGeneMap = new HashMap<>();

		modelGenesMap.forEach((diseaseEntityJoinID, genes) -> {
			DiseaseAnnotation diseaseAnnot = diseaseAnnotationMap1.get(diseaseEntityJoinID);

			genes.forEach(gene -> {
				Map<String, List<DiseaseAnnotation>> annotations = diseaseModelGeneMap.get(gene.getPrimaryKey());
				if (annotations == null) {
					annotations = new HashMap<>();
					diseaseModelGeneMap.put(gene.getPrimaryKey(), annotations);
				}

				List<DiseaseAnnotation> diseaseAnnotations = annotations.get(diseaseAnnot.getModel().getPrimaryKey());
				if (diseaseAnnotations == null) {
					diseaseAnnotations = new ArrayList<>();
					annotations.put(diseaseAnnot.getModel().getPrimaryKey(), diseaseAnnotations);
				}
				diseaseAnnotations.add(diseaseAnnot);
			});
		});
		diseaseAnnotationMap.clear();
		diseaseAnnotationMap1.clear();
		modelGenesMap.clear();

		Map<String, List<PrimaryAnnotatedEntity>> diseaseAnnotationPureMap = new HashMap<>();
		Map<String, List<PrimaryAnnotatedEntity>> diseaseAnnotationPerModeIDMap = new HashMap<>();

		diseaseModelGeneMap.forEach((geneID, modelIdMap) -> {
			List<PrimaryAnnotatedEntity> geneMergedAnnotations = diseaseAnnotationPureMap.computeIfAbsent(geneID, id -> new ArrayList<>());
			modelIdMap.forEach((modelID, diseaseAnnotations) -> {
				if (CollectionUtils.isEmpty(diseaseAnnotations))
					return;
				List<PrimaryAnnotatedEntity> modelMergedAnnotations = diseaseAnnotationPerModeIDMap.computeIfAbsent(modelID, id -> new ArrayList<>());
				diseaseAnnotations.forEach(diseaseAnnotation -> {

					// merge all annotations for a given modelID that do not have an exp Cond
					PrimaryAnnotatedEntity primaryEntityWithoutExpCondition =
							modelMergedAnnotations.stream().filter(primaryAnnotatedEntity -> !primaryAnnotatedEntity.hasExperimentalCondition())
									.findFirst()
									.orElse(null);
					// create new PAE if experimental condition exists or
					// if expCond does not exist and no nonExpCondition is already accounted for in this list
					if (diseaseAnnotation.hasExperimentalCondition() ||
							(!diseaseAnnotation.hasExperimentalCondition() && primaryEntityWithoutExpCondition == null)) {
						PrimaryAnnotatedEntity entity = ModelHelper.getPrimaryAnnotatedEntity(diseaseAnnotation);
						entity.addDisease(diseaseAnnotation.getDisease(), diseaseAnnotation.getAssociationType());
						entity.setConditionModifiers(diseaseAnnotation.getConditionModifiers());
						entity.setConditions(diseaseAnnotation.getConditions());
						entity.addPublicationEvidenceCode(diseaseAnnotation.getPublicationJoins());
						modelMergedAnnotations.add(entity);
					} else {
						// otherwise just add the disease and the publication evidence codes.
						primaryEntityWithoutExpCondition.addDisease(diseaseAnnotation.getDisease(), diseaseAnnotation.getAssociationType());
						primaryEntityWithoutExpCondition.addPublicationEvidenceCode(diseaseAnnotation.getPublicationJoins());
					}
				});
				geneMergedAnnotations.addAll(modelMergedAnnotations);
			});
			diseaseAnnotationPureMap.put(geneID, geneMergedAnnotations);
		});

		diseaseModelGeneMap.clear();

		storeIntoCachePAE(diseaseAnnotationPureMap, CacheAlliance.DISEASE_ANNOTATION_MODEL_LEVEL_GENE);
		diseaseAnnotationPureMap.clear();
	}

	private void populateAllelesCache() {

		Set<DiseaseEntityJoin> alleleEntityJoins = diseaseRepository.getAllDiseaseAlleleEntityJoins();
		List<DiseaseAnnotation> alleleList = getDiseaseAnnotationsFromDEJs(alleleEntityJoins);
		if (alleleList == null)
			return;

		alleleEntityJoins.clear();

		log.info("Number of DiseaseAnnotation objects with Alleles: " + String.format("%,d", alleleList.size()));
		Map<String, List<DiseaseAnnotation>> diseaseAlleleAnnotationTermMap = alleleList.stream()
				.collect(groupingBy(annotation -> annotation.getDisease().getPrimaryKey()));
		Map<String, List<DiseaseAnnotation>> mergedAlleleList = mergeDiseaseAnnotationsForAlleles(diseaseAlleleAnnotationTermMap);

		// add annotations to parent terms
		Map<String, List<DiseaseAnnotation>> diseaseAlleleAnnotationMap = getAnnotationMapIncludingClosure(mergedAlleleList);
		storeIntoCache(alleleList, diseaseAlleleAnnotationMap, CacheAlliance.DISEASE_ANNOTATION_ALLELE_LEVEL_ALLELE);

		diseaseAlleleAnnotationMap.clear();

		// <AlleleID, List of DAs>
		Map<String, List<DiseaseAnnotation>> diseaseAlleleMap = alleleList.stream()
				.filter(diseaseAnnotation -> diseaseAnnotation.getFeature() != null)
				.collect(groupingBy(annotation -> annotation.getFeature().getPrimaryKey()));
		mergedAlleleList = mergeDiseaseAnnotationsForAlleles(diseaseAlleleMap);
		storeIntoCache(alleleList, mergedAlleleList, CacheAlliance.ALLELE_DISEASE);

		alleleList.clear();
		diseaseAlleleMap.clear();
	}

	private List<DiseaseAnnotation> getDiseaseAnnotationsFromDEJs(Collection<DiseaseEntityJoin> joinList) {

		return joinList.stream()
				.map(join -> {
					DiseaseAnnotation document = new DiseaseAnnotation();
					document.setPrimaryKey(join.getPrimaryKey());
					document.setGene(join.getGene());
					document.setFeature(join.getAllele());
					document.setModel(join.getModel());
					document.setDisease(join.getDisease());
					document.setSource(join.getSource());
					document.setAssociationType(join.getJoinType().toLowerCase());
					document.setSortOrder(join.getSortOrder());
					document.addConditions(DiseaseAnnotation.ConditionType.HAS_CONDITION, join.getHasConditionList());
					document.addConditions(DiseaseAnnotation.ConditionType.INDUCES, join.getInducerConditionList());
					document.addModifier(DiseaseAnnotation.ConditionType.AMELIORATES, join.getAmeliorateConditionList());
					document.addModifier(DiseaseAnnotation.ConditionType.EXACERBATES, join.getExacerbateConditionList());
					if (join.getDataProviderList() != null) {
						document.setProviders(join.getDataProviderList());
					}
					List<Gene> orthologyGenes = join.getOrthologyGenes();
					if (orthologyGenes != null) {
						orthologyGenes.sort(Comparator.comparing(gene -> gene.getSymbol().toLowerCase()));
						document.setOrthologyGenes(orthologyGenes);
					}

					// used to populate the DOTerm object on the PrimaryAnnotationEntity object
					// Needed as the same AGM can be reused on multiple pubJoin nodes.
					Map<String, List<PrimaryAnnotatedEntity>> entities = new HashMap<>();

					// sort to ensure subsequent caching processes will generate the same PAEs with the
					// same PK. Note the merging that is happening
					List<PublicationJoin> publicationJoins1 = join.getPublicationJoins();
					if (publicationJoins1 == null)
						System.out.println(join.getPrimaryKey());
					publicationJoins1.sort(Comparator.comparing(PublicationJoin::getPrimaryKey));
					if (CollectionUtils.isNotEmpty(publicationJoins1)) {
						// create PAEs from AGMs
						join.getPublicationJoins()
								.stream()
								.filter(pubJoin -> pubJoin.getModels() != null)
								.forEach(pubJoin -> {
									pubJoin.getModels().forEach(model -> {
										// split out DiseaseEntityJoin nodes off the (ExpCond)--(:DiseaseEntityJoin)--(:Allele)/(:AffectiveGenomicModel)
										List<PrimaryAnnotatedEntity> entityList = entities.get(model.getPrimaryKey());
										if (entityList == null) {
											entityList = model.getDiseaseEntityJoins().stream()
													.filter(diseaseJoin -> diseaseJoin.getDisease().getPrimaryKey().equals(join.getDisease().getPrimaryKey()))
													.map(diseaseJoin -> {
														PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
														entity.setId(model.getPrimaryKey());
														entity.setName(model.getName());
														entity.setUrl(model.getModCrossRefCompleteUrl());
														entity.setDisplayName(model.getNameText());
														entity.setType(GeneticEntity.getType(model.getSubtype()));
														ConditionService.populateExperimentalConditions(diseaseJoin, entity);
														document.addPrimaryAnnotatedEntity(entity);
														entity.addPublicationEvidenceCode(pubJoin);
														entity.setDiseaseAssociationType(join.getJoinType());
														return entity;
													})
													.collect(toList());
											document.addAllPrimaryAnnotatedEntities(entityList);
											entities.put(model.getPrimaryKey(), entityList);
										} else {
											entityList.forEach(entity -> {
												entity.addPublicationEvidenceCode(pubJoin);
												entity.setDiseaseAssociationType(join.getJoinType());
											});
										}
									});
								});
						// create PAEs from Alleles
						join.getPublicationJoins()
								.stream()
								.filter(pubJoin -> CollectionUtils.isNotEmpty(pubJoin.getAlleles()))
								.forEach(pubJoin -> pubJoin.getAlleles().forEach(allele -> {
									// split out DiseaseEntityJoin nodes off the (ExpCond)--(:DiseaseEntityJoin)--(:Allele)/(:AffectiveGenomicModel)
									List<PrimaryAnnotatedEntity> entityList = entities.get(allele.getPrimaryKey());
									if (entityList == null) {
										entityList = allele.getDiseaseEntityJoins().stream()
												.filter(diseaseJoin -> diseaseJoin.getDisease().getPrimaryKey().equals(join.getDisease().getPrimaryKey()))
												.map(diseaseJoin -> {
													PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
													entity.setId(allele.getPrimaryKey());
													entity.setName(allele.getSymbol());
													ConditionService.populateExperimentalConditions(diseaseJoin, entity);
													List<CrossReference> refs = allele.getCrossReferences();
													if (org.apache.commons.collections.CollectionUtils.isNotEmpty(refs))
														entity.setUrl(refs.get(0).getCrossRefCompleteUrl());

													entity.setDisplayName(allele.getSymbolText());
													entity.setType(GeneticEntity.CrossReferenceType.ALLELE);
													entity.addPublicationEvidenceCode(pubJoin);
													entity.setDiseaseAssociationType(join.getJoinType());
													return entity;
												})
												.collect(Collectors.toList());
										document.addAllPrimaryAnnotatedEntities(entityList);
										entities.put(allele.getPrimaryKey(), entityList);
									} else {
										entityList.forEach(entity -> {
											entity.addPublicationEvidenceCode(pubJoin);
											entity.setDiseaseAssociationType(join.getJoinType());
										});
									}
								}));
						// create PAE from Allele when allele-level annotation or Gene when gene-level annotation,
						// i.e. no model / AGM or Allele off PublicationJoin node
						// needed for showing experimental conditions
						if (join.getPublicationJoins().stream().anyMatch(pubJoin -> CollectionUtils.isEmpty(pubJoin.getAlleles())
								&& CollectionUtils.isEmpty(pubJoin.getModels()) && join.getModel() == null)
								&& join.hasExperimentalConditions()) {
							GeneticEntity geneticEntity = join.getAllele();
							if (geneticEntity == null) {
								geneticEntity = join.getGene();
							}
							PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
							entity.setId(geneticEntity.getPrimaryKey());
							entity.setName(geneticEntity.getSymbol());
							ConditionService.populateExperimentalConditions(join, entity);
							List<CrossReference> refs = geneticEntity.getCrossReferences();
							if (org.apache.commons.collections.CollectionUtils.isNotEmpty(refs))
								entity.setUrl(refs.get(0).getCrossRefCompleteUrl());

							//entity.setDisplayName(geneticEntity.getSymbolText());
							entity.setType(geneticEntity.getCrossReferenceType());
							entity.addPublicationEvidenceCode(join.getPublicationJoins());
							entity.setDiseaseAssociationType(join.getJoinType());
							document.addPrimaryAnnotatedEntity(entity);
						}
					}
					List<PublicationJoin> publicationJoins = join.getPublicationJoins();
					if (useCache) {
						populatePublicationJoinsFromCache(join.getPublicationJoins());
					} else {
						diseaseRepository.populatePublicationJoins(publicationJoins);
					}
					document.setPublicationJoins(publicationJoins);
	/*
						List<ECOTerm> ecoList = join.getPublicationJoins().stream()
								.filter(join -> CollectionUtils.isNotEmpty(join.getEcoCode()))
								.map(PublicationJoin::getEcoCode)
								.flatMap(Collection::stream).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
						document.setEcoCodes(ecoList.stream().distinct().collect(Collectors.toList()));
	*/
					// work around as I cannot figure out how to include the ECOTerm in the overall query without slowing down the performance.
					List<ECOTerm> evidences;
					if (useCache) {
						evidences = getEcoTermsFromCache(join.getPublicationJoins());
					} else {
						evidences = diseaseRepository.getEcoTerm(join.getPublicationJoins());
						Set<String> slimId = diseaseRibbonService.getAllParentIDs(join.getDisease().getPrimaryKey());
						document.setParentIDs(slimId);
					}
					document.setEcoCodes(evidences);
					progressProcess();
					return document;
				})
				.collect(toList());
	}

	public List<ECOTerm> getEcoTerm(PublicationJoin join) {
		if (join == null)
			return null;
		return cacheService.getCacheEntries(join.getPrimaryKey(), CacheAlliance.ECO_MAP);
	}

	public List<ECOTerm> getEcoTermsFromCache(List<PublicationJoin> joins) {
		if (joins == null)
			return null;

		return joins.stream()
				.map(join -> getEcoTerm(join))
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	public void populatePublicationJoinsFromCache(List<PublicationJoin> joins) {
		if (joins == null)
			return;

		joins.forEach(publicationJoin -> {
			List<ECOTerm> cacheValue = getEcoTerm(publicationJoin);
			if (cacheValue != null) {
				publicationJoin.setEcoCode(cacheValue);
			}
		});
	}

	private void mergeDiseaseAnnotationsByAGM(List<DiseaseAnnotation> allDiseaseAnnotations) {
		Map<DOTerm, Map<Gene, Map<String, List<DiseaseAnnotation>>>> multiIndex = allDiseaseAnnotations.stream()
				.filter(diseaseAnnotation -> diseaseAnnotation.getGene() != null)
				.collect(groupingBy(DiseaseAnnotation::getDisease,
						groupingBy(DiseaseAnnotation::getGene,
								groupingBy(DiseaseAnnotation::getAssociationType))));

		multiIndex.forEach((doTerm, geneMapMap) -> {
			geneMapMap.forEach((gene, stringListMap) -> {
				stringListMap.forEach((type, diseaseAnnotations) -> {
					if (type.equals("is_implicated_in") && diseaseAnnotations.size() > 1) {

						List<PrimaryAnnotatedEntity> models = diseaseAnnotations.stream()
								.filter(diseaseAnnotation -> diseaseAnnotation.getPrimaryAnnotatedEntities() != null)
								.map(DiseaseAnnotation::getPrimaryAnnotatedEntities)
								.flatMap(Collection::stream)
								.collect(toList());

						// only merge annotations that have at least one PAE
/*
						if (CollectionUtils.isEmpty(models))
							return;
*/
						// sort so we always pick the same base annotation to merge into
						diseaseAnnotations.sort(Comparator.comparing(DiseaseAnnotation::getPrimaryKey));
						DiseaseAnnotation annotation = diseaseAnnotations.get(0);
						int index = 0;
						for (DiseaseAnnotation annot : diseaseAnnotations) {
							if (index++ == 0)
								continue;
							annotation.addAllPrimaryAnnotatedEntities(annot.getPrimaryAnnotatedEntities());
							annotation.addPublicationJoins(annot.getPublicationJoins());
							annot.setRemove(true);
						}
					}
				});
			});
		});

		allDiseaseAnnotations.removeIf(DiseaseAnnotation::isRemove);
	}

	@Override
	public void close() {
		diseaseRepository.close();
	}

}

