package org.alliancegenome.indexer.indexers.curation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.AGMDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.GeneDiseaseAnnotationDocument;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.curation_api.model.entities.base.CurieAuditedObject;
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

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
public class DiseaseAnnotationCurationIndexer extends Indexer {

	private GeneDiseaseAnnotationService geneService = new GeneDiseaseAnnotationService();
	private AlleleDiseaseAnnotationService alleleService = new AlleleDiseaseAnnotationService();
	private AGMDiseaseAnnotationService agmService = new AGMDiseaseAnnotationService();
	private VocabularyService vocabService = new VocabularyService();
	private DiseaseRepository diseaseRepository;

	private Map<String, Set<String>> closureMap;
	private Map<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> geneMap = new HashMap<>();
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
		createDiseaseAnnotationsFromOrthology();

		List<GeneDiseaseAnnotationDocument> list = createGeneDiseaseAnnotationDocuments();
		System.out.println("No of DAs for Alz2: " + list.stream().filter(document -> document.getSubject().getCurie().equals("HGNC:613") && document.getObject().getCurie().equals("DOID:0110035")).toList().size());

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
		geneViaOrthologyMap = geneService.getOrthologousGeneDiseaseAnnotations(geneMap);
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
				.collect(groupingBy(DiseaseAnnotation::getObject,
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
							return diseaseAnnotation.getWith().stream().map(Gene::getCurie).sorted().collect(Collectors.joining("_"));
						})))));

			groupedByAnnotations.forEach((diseaseTerm, associationTypeMap) -> {
				associationTypeMap.forEach((associationType, diseaseQualifierMap) -> {
					diseaseQualifierMap.forEach((diseaseQualifier, stringListMap) -> {
						stringListMap.forEach((basedOnGenesList, diseaseAnnotations1) -> {
							DiseaseAnnotation diseaseAnnotation = diseaseAnnotations1.get(0);
							GeneDiseaseAnnotationDocument gdad = new GeneDiseaseAnnotationDocument();
							gdad.setSubject(gene);
							gdad.setRelation(associationType);
							String generatedRelationString = getGeneratedRelationString(gdad.getRelation().getName(), diseaseAnnotation.getNegated());
							gdad.setGeneratedRelationString(generatedRelationString);
							gdad.setObject(diseaseTerm);
							gdad.setParentSlimIDs(closureMap.get(diseaseAnnotation.getObject().getCurie()));

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
							List<String> ids = basedOnGenes.stream().map(CurieAuditedObject::getCurie).toList();
							gdad.setBasedOnGenes(new ArrayList<>(basedOnGenes));

							gdad.addReference(diseaseAnnotation.getSingleReference());
							gdad.addPubMedPubModID(getPubmedPubModID(diseaseAnnotation.getSingleReference()));

							HashMap<String, Integer> order = SpeciesType.getSpeciesOrderByTaxonID(gene.getTaxon().getCurie());
							gdad.setSpeciesOrder(order);
							int phylogeneticSortOrder = 0;
							SpeciesType speciesType = SpeciesType.getTypeByID(gene.getTaxon().getCurie());
							if (speciesType != null) {
								phylogeneticSortOrder = speciesType.getOrderID();
							}
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
				VocabularyTerm relation = relationIsImplicatedIn;

				int phylogeneticSortOrder = 0;
				if (da instanceof GeneDiseaseAnnotation gda) {
					relation = da.getRelation();
					SpeciesType speciesType = SpeciesType.getTypeByID(gda.getSubject().getTaxon().getCurie());
					if (speciesType != null) {
						phylogeneticSortOrder = speciesType.getOrderID();
					}
				}

				String key = relation.getName() + "_" + da.getObject().getName() + "_" + da.getNegated();

				if (da.getDiseaseQualifiers() != null) {
					key += "_" + da.getDiseaseQualifiers().stream().map(VocabularyTerm::getName).sorted().collect(Collectors.joining("_"));
				}

				if (da.getWith() != null && da.getWith().size() > 0) {
					key += "_" + da.getWith().stream().map(Gene::getCurie).sorted().collect(Collectors.joining("_"));
				}

				GeneDiseaseAnnotationDocument gdad = lookup.get(key);

				if (gdad == null) {
					gdad = new GeneDiseaseAnnotationDocument();
					gdad.setSubject(entry.getValue().getLeft());
					HashMap<String, Integer> order = SpeciesType.getSpeciesOrderByTaxonID(entry.getValue().getLeft().getTaxon().getCurie());
					gdad.setSpeciesOrder(order);
					gdad.setRelation(relation);
					String generatedRelationString = getGeneratedRelationString(gdad.getRelation().getName(), da.getNegated());
					gdad.setGeneratedRelationString(generatedRelationString);
					gdad.setObject(da.getObject());
					gdad.setParentSlimIDs(closureMap.get(da.getObject().getCurie()));
					lookup.put(key, gdad);
				}

				Map<String, ECOTerm> evidenceCodesMap = new HashMap<>();
				if (gdad.getEvidenceCodes() != null) {
					gdad.getEvidenceCodes().forEach(ecoTerm -> evidenceCodesMap.put(ecoTerm.getCurie(), ecoTerm));
				}
				if (da.getEvidenceCodes() != null) {
					da.getEvidenceCodes().forEach(ecoTerm -> evidenceCodesMap.put(ecoTerm.getCurie(), ecoTerm));
				}
				gdad.setEvidenceCodes(evidenceCodesMap.values().stream().toList());

				if (CollectionUtils.isNotEmpty(da.getDiseaseQualifiers())) {
					Set<String> diseaseQualifiers = da.getDiseaseQualifiers().stream().map(term -> term.getName().replace("_", " ")).collect(Collectors.toSet());
					gdad.setDiseaseQualifiers(diseaseQualifiers);
				}
				gdad.addReference(da.getSingleReference());
				gdad.addPubMedPubModID(getPubmedPubModID(da.getSingleReference()));
				gdad.addPrimaryAnnotation(da);
				gdad.addBasedOnGenes(da.getWith());
				gdad.setPhylogeneticSortingIndex(phylogeneticSortOrder);
			}
			ph.progressProcess();
			ret.addAll(lookup.values());
			lookup.clear();
		}
		ph.finishProcess();

		return ret;
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

				String key = relation.getName() + "_" + da.getObject().getName() + "_" + da.getNegated();
				AlleleDiseaseAnnotationDocument adad = lookup.get(key);

				if (adad == null) {
					adad = new AlleleDiseaseAnnotationDocument();
					HashMap<String, Integer> order = SpeciesType.getSpeciesOrderByTaxonID(entry.getValue().getLeft().getTaxon().getCurie());
					adad.setSpeciesOrder(order);
					adad.setSubject(entry.getValue().getLeft());
					adad.setRelation(relation);
					String generatedRelationString = getGeneratedRelationString(relation.getName(), da.getNegated());
					adad.setGeneratedRelationString(generatedRelationString);
					adad.setObject(da.getObject());
					lookup.put(key, adad);
				}
				adad.setEvidenceCodes(da.getEvidenceCodes());
				if (CollectionUtils.isNotEmpty(da.getDiseaseQualifiers())) {
					Set<String> diseaseQualifiers = da.getDiseaseQualifiers().stream().map(term -> term.getName().replace("_", " ")).collect(Collectors.toSet());
					adad.setDiseaseQualifiers(diseaseQualifiers);
				}

				// gdad.setDataProvider(da.getDataProvider());
				adad.addReference(da.getSingleReference());
				adad.addPubMedPubModID(getPubmedPubModID(da.getSingleReference()));
				if (da instanceof AlleleDiseaseAnnotation || da instanceof AGMDiseaseAnnotation) {
					adad.addPrimaryAnnotation(da);
				}

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

			for (DiseaseAnnotation da : entry.getValue().getRight()) {
				String key = da.getRelation().getName() + "_" + da.getObject().getName() + "_" + da.getNegated();
				AGMDiseaseAnnotationDocument adad = lookup.get(key);

				if (adad == null) {
					adad = new AGMDiseaseAnnotationDocument();
					HashMap<String, Integer> order = SpeciesType.getSpeciesOrderByTaxonID(entry.getValue().getLeft().getTaxon().getCurie());
					adad.setSpeciesOrder(order);
					adad.setSubject(entry.getValue().getLeft());
					adad.setRelation(da.getRelation());
					adad.setObject(da.getObject());
					lookup.put(key, adad);
				}
				adad.setEvidenceCodes(da.getEvidenceCodes());
				// gdad.setDataProvider(da.getDataProvider());
				adad.addReference(da.getSingleReference());
			}
			ph.progressProcess();
			ret.addAll(lookup.values());
			lookup.clear();
		}
		ph.finishProcess();
		return ret;
	}

	private void indexGenes() {
		List<GeneDiseaseAnnotation> geneDiseaseAnnotations = geneService.getFiltered();
		addDiseaseAnnotationsToLGlobalMap(geneDiseaseAnnotations);
	}

	private void addDiseaseAnnotationsToLGlobalMap(List<GeneDiseaseAnnotation> geneDiseaseAnnotations) {
		log.info("Filtered Genes: " + geneDiseaseAnnotations.size());
		for (GeneDiseaseAnnotation da : geneDiseaseAnnotations) {
			Gene gene = da.getSubject();
			Pair<Gene, ArrayList<DiseaseAnnotation>> pair = geneMap.computeIfAbsent(gene.getCurie(), geneCurie -> Pair.of(gene, new ArrayList<>()));
			pair.getRight().add(da);
		}
	}

	private void indexAlleles() {

		List<AlleleDiseaseAnnotation> alleleDiseaseAnnotations = alleleService.getFiltered();
		log.info("Filtered Alleles: " + alleleDiseaseAnnotations.size());
		for (AlleleDiseaseAnnotation da : alleleDiseaseAnnotations) {
			Allele allele = da.getSubject();
			Pair<Allele, ArrayList<DiseaseAnnotation>> allelePair = alleleMap.computeIfAbsent(allele.getCurie(), alleleCurie -> Pair.of(allele, new ArrayList<>()));
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
			Pair<Gene, ArrayList<DiseaseAnnotation>> pair = geneMap.computeIfAbsent(inferredGene.getCurie(), k -> Pair.of(inferredGene, new ArrayList<>()));
			pair.getRight().add(da);
		}
	}

	private void extractAlleleDiseaseAnnotations(DiseaseAnnotation da, Allele inferredAllele) {
		if (inferredAllele != null && !inferredAllele.getInternal()) {
			Pair<Allele, ArrayList<DiseaseAnnotation>> pair = alleleMap.computeIfAbsent(inferredAllele.getCurie(), k -> Pair.of(inferredAllele, new ArrayList<>()));
			pair.getRight().add(da);
		}
	}

	private void indexAGMs() {

		List<AGMDiseaseAnnotation> agmDiseaseAnnotations = agmService.getFiltered();
		log.info("Filtered AGMs: " + agmDiseaseAnnotations.size());

		for (AGMDiseaseAnnotation da : agmDiseaseAnnotations) {
			AffectedGenomicModel genomicModel = da.getSubject();
			Pair<AffectedGenomicModel, ArrayList<DiseaseAnnotation>> allelePair = agmMap.computeIfAbsent(genomicModel.getCurie(), agmCurie -> Pair.of(genomicModel, new ArrayList<>()));
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
