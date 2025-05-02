package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.AllelePhenotypeAnnotationDocument;
import org.alliancegenome.api.entity.GenePhenotypeAnnotationDocument;
import org.alliancegenome.api.entity.PhenotypeAnnotationDocument;
import org.alliancegenome.curation_api.model.entities.AGMPhenotypeAnnotation;
import org.alliancegenome.curation_api.model.entities.AffectedGenomicModel;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.AllelePhenotypeAnnotation;
import org.alliancegenome.curation_api.model.entities.BiologicalEntity;
import org.alliancegenome.curation_api.model.entities.CrossReference;
import org.alliancegenome.curation_api.model.entities.ExternalDatabaseReference;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.GenePhenotypeAnnotation;
import org.alliancegenome.curation_api.model.entities.PhenotypeAnnotation;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.AGMPhenotypeAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.AllelePhenotypeAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.GenePhenotypeAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.VocabularyTermService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PhenotypeAnnotationCurationIndexer extends Indexer {

	private GenePhenotypeAnnotationService geneService;
	private AllelePhenotypeAnnotationService alleleService;
	private AGMPhenotypeAnnotationService agmService;
	private VocabularyTermService vocabTermService;

	private Map<String, Pair<Gene, ArrayList<PhenotypeAnnotation>>> geneMap = new HashMap<>();
	private Map<String, Pair<Allele, ArrayList<PhenotypeAnnotation>>> alleleMap = new HashMap<>();
	private Map<String, Pair<AffectedGenomicModel, ArrayList<PhenotypeAnnotation>>> agmMap = new HashMap<>();

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

		geneService = new GenePhenotypeAnnotationService();
		alleleService = new AllelePhenotypeAnnotationService();
		agmService = new AGMPhenotypeAnnotationService();
		vocabTermService = new VocabularyTermService();

		indexGenes();
		indexAlleles();
		indexAGMs();

		List<GenePhenotypeAnnotationDocument> geneList = createGenePhenotypeAnnotationDocuments();
		log.info("Indexing " + String.format("%,d", geneList.size()) + " Gene PA documents");
		indexDocuments(geneList);

		List<AllelePhenotypeAnnotationDocument> alleleList = createAllelePhenotypeAnnotationDocuments();
		log.info("Indexing " + alleleList.size() + " allele documents");
		indexDocuments(alleleList);

		log.info("Finished Indexing Phenotype Annotations");
	}

	private List<GenePhenotypeAnnotationDocument> createGenePhenotypeAnnotationDocuments() {

		List<GenePhenotypeAnnotationDocument> ret = new ArrayList<>();
		ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
		ph.startProcess("Creating Gene Phenotype Annotations", geneMap.size());

		VocabularyTerm relationIsImplicatedIn = vocabTermService.getDiseaseRelationTerms().get("is_implicated_in");

		for (Entry<String, Pair<Gene, ArrayList<PhenotypeAnnotation>>> pairMap : geneMap.entrySet()) {
			HashMap<String, GenePhenotypeAnnotationDocument> lookup = new HashMap<>();

			for (PhenotypeAnnotation da : pairMap.getValue().getValue()) {
				Gene gene = pairMap.getValue().getKey();
				VocabularyTerm relation = relationIsImplicatedIn;

				if (da instanceof GenePhenotypeAnnotation) {
					relation = da.getRelation();
				}

				String key = getConsolidationKey(da);
				GenePhenotypeAnnotationDocument gpad = lookup.computeIfAbsent(key, k -> new GenePhenotypeAnnotationDocument());
				if (gpad.getSubject() == null) {
					gpad.setSubject(gene);
					gpad.setRelation(relation);
					gpad.setPhenotypeStatement(da.getPhenotypeAnnotationObject());
				}
				populateBasePhenotypeAnnotationDocument(gene, da, gpad);
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
		if (CollectionUtils.isEmpty(crossReferences)) {
			return null;
		}
		String[] prefixes = {"PMID", "MGI", "RGD", "ZFIN", "FB", "WB", "MGI"};
		for (String prefix : prefixes) {
			Optional<CrossReference> opt = crossReferences.stream().filter(reference -> reference.getReferencedCurie().startsWith(prefix + ":")).findFirst();
			if (opt.isPresent()) {
				return opt.get().getReferencedCurie();
			}
		}
		return null;
	}

	private void populateBasePhenotypeAnnotationDocument(BiologicalEntity biologicalEntity, PhenotypeAnnotation da, PhenotypeAnnotationDocument dad) {
		dad.addReference(da.getEvidenceItem());
		if (da.getEvidenceItem() instanceof Reference) {
			dad.addPubMedPubModID(getPubmedPubModID((Reference) da.getEvidenceItem()));
		} else if (da.getEvidenceItem() instanceof ExternalDatabaseReference externalReference) {
			dad.addPubMedPubModID(externalReference.getCurie());
		}
		dad.addPrimaryAnnotation(da);
	}

	// Consolidated fields
	// phenotype statement
	private static String getConsolidationKey(PhenotypeAnnotation da) {
		String key = da.getPhenotypeAnnotationObject();
		return key;
	}

	private void indexGenes() {
		List<GenePhenotypeAnnotation> genePhenotypeAnnotations = geneService.getFiltered(indexerConfig.getThreadCount(), indexerConfig.getBufferSize());
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

	private void indexAlleles() {

		List<AllelePhenotypeAnnotation> allelePhenotypeAnnotations = alleleService.getFiltered(indexerConfig.getThreadCount(), indexerConfig.getBufferSize());
		log.info("Filtered Alleles: " + String.format("%,d", allelePhenotypeAnnotations.size()));
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

		List<AGMPhenotypeAnnotation> agmDiseaseAnnotations = agmService.getFiltered(indexerConfig.getThreadCount(), indexerConfig.getBufferSize());
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
			Allele inferredAllele = da.getInferredAllele();
			extractAlleleDiseaseAnnotations(da, inferredAllele);
			if (da.getAssertedAlleles() != null) {
				for(Allele allele: da.getAssertedAlleles()) {
					extractAlleleDiseaseAnnotations(da, allele);
				}
			}
		}
	}

	private void extractAlleleDiseaseAnnotations(PhenotypeAnnotation da, Allele inferredAllele) {
		if (inferredAllele != null && !inferredAllele.getInternal()) {
			Pair<Allele, ArrayList<PhenotypeAnnotation>> pair = alleleMap.computeIfAbsent(inferredAllele.getIdentifier(), k -> Pair.of(inferredAllele, new ArrayList<>()));
			pair.getRight().add(da);
		}
	}

	private List<AllelePhenotypeAnnotationDocument> createAllelePhenotypeAnnotationDocuments() {

		List<AllelePhenotypeAnnotationDocument> ret = new ArrayList<>();

		ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
		ph.startProcess("Creating Allele Disease Annotations", alleleMap.size());

		VocabularyTerm relation = vocabTermService.getDiseaseRelationTerms().get("is_implicated_in");

		for (Entry<String, Pair<Allele, ArrayList<PhenotypeAnnotation>>> pairMap : alleleMap.entrySet()) {
			HashMap<String, AllelePhenotypeAnnotationDocument> lookup = new HashMap<>();

			for (PhenotypeAnnotation pa : pairMap.getValue().getValue()) {

				// use this relation if inherited (inferred or asserted) from an AGM DA.
				if (pa instanceof AllelePhenotypeAnnotation) {
					relation = pa.getRelation();
				}

				String key = getConsolidationKey(pa);
				AllelePhenotypeAnnotationDocument apad = lookup.computeIfAbsent(key, k -> new AllelePhenotypeAnnotationDocument());
				Allele allele = pairMap.getValue().getKey();
				if (apad.getSubject() == null) {
					apad.setSubject(allele);
					apad.setRelation(relation);
					apad.setPhenotypeStatement(pa.getPhenotypeAnnotationObject());
				}
				populateBasePhenotypeAnnotationDocument(allele, pa, apad);
			}
			ph.progressProcess();
			ret.addAll(lookup.values());
			lookup.clear();
		}
		ph.finishProcess();
		return ret;
	}


}
