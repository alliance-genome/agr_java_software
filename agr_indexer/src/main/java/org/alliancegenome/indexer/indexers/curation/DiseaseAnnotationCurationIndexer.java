package org.alliancegenome.indexer.indexers.curation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.AGMDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.GeneDiseaseAnnotationDocument;
import org.alliancegenome.curation_api.config.RestDefaultObjectMapper;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.es.index.site.doclet.SpeciesDoclet;
import org.alliancegenome.es.util.ProcessDisplayHelper;
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

	public DiseaseAnnotationCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return (new RestDefaultObjectMapper()).getMapper();
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
		log.info("Indexing " + list.size() + " gene documents");
		indexDocuments(list);

		List<AlleleDiseaseAnnotationDocument> alleleList = createAlleleDiseaseAnnotationDocuments();
		log.info("Indexing " + alleleList.size() + " allele documents");
		indexDocuments(alleleList);

		List<AGMDiseaseAnnotationDocument> agmList = createAGMDiseaseAnnotationDocuments();
		log.info("Indexing " + agmList.size() + " agm documents");
		indexDocuments(agmList);

		diseaseRepository.close();
	}

	private List<GeneDiseaseAnnotationDocument> createGeneDiseaseAnnotationDocuments() {

		List<GeneDiseaseAnnotationDocument> ret = new ArrayList<>();
		ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
		ph.startProcess("Creating Gene Disease Annotations", geneMap.size());

		for (Entry<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> entry : geneMap.entrySet()) {
			HashMap<String, GeneDiseaseAnnotationDocument> lookup = new HashMap<>();

			for (DiseaseAnnotation da : entry.getValue().getRight()) {
				String key = da.getDiseaseRelation().getName() + "_" + da.getObject().getName() + "_" + da.getNegated();
				if (da.getWith() != null && da.getWith().size() > 0) {
					key += "_" + da.getSingleReference().getCurie();
				}
				GeneDiseaseAnnotationDocument gdad = lookup.get(key);

				if (gdad == null) {
					gdad = new GeneDiseaseAnnotationDocument();
					gdad.setSubject(entry.getValue().getLeft());
					SpeciesType type = SpeciesType.getTypeByID(entry.getValue().getLeft().getTaxon().getCurie());
					int order = 100;
					if (type != null) {
						order = type.getOrderID();
					}
					gdad.setPhylogeneticSortingIndex(order);
					if (da instanceof AGMDiseaseAnnotation || da instanceof AlleleDiseaseAnnotation) {
						gdad.setDiseaseRelation(vocabService.getVocabularyTerm("is_implicated_in"));
					} else {
						gdad.setDiseaseRelation(da.getDiseaseRelation());
					}
					String diseaseRelationNegation = getDiseaseRelationNegation(gdad.getDiseaseRelation().getName(), da.getNegated());
					gdad.setDiseaseRelationNegation(diseaseRelationNegation);
					gdad.setObject(da.getObject());
					gdad.setParentSlimIDs(closureMap.get(da.getObject().getCurie()));
					lookup.put(key, gdad);
				}
				gdad.setEvidenceCodes(da.getEvidenceCodes());
				// gdad.setDataProvider(da.getDataProvider());
				gdad.addReference(da.getSingleReference());
				gdad.addPubMedPubModID(getPubmedPubModID(da.getSingleReference()));
				gdad.addPrimaryAnnotation(da);
				SpeciesDoclet doc = SpeciesType.fromTaxonId(entry.getValue().getLeft().getTaxon().getCurie());
				if (doc != null) {
					gdad.setPhylogeneticSortingIndex(doc.getOrderID());
				} else {
					gdad.setPhylogeneticSortingIndex(100);
				}
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

	private String getDiseaseRelationNegation(String diseaseRelation, Boolean negated) {
		if (!negated)
			return diseaseRelation;
		return diseaseRelation.replaceFirst("_", "_not_");
	}

	private List<AlleleDiseaseAnnotationDocument> createAlleleDiseaseAnnotationDocuments() {

		List<AlleleDiseaseAnnotationDocument> ret = new ArrayList<>();

		ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
		ph.startProcess("Creating Allele Disease Annotations", alleleMap.size());

		for (Entry<String, Pair<Allele, ArrayList<DiseaseAnnotation>>> entry : alleleMap.entrySet()) {
			HashMap<String, AlleleDiseaseAnnotationDocument> lookup = new HashMap<>();

			for (DiseaseAnnotation da : entry.getValue().getRight()) {
				String key = da.getDiseaseRelation().getName() + "_" + da.getObject().getName() + "_" + da.getNegated();
				AlleleDiseaseAnnotationDocument adad = lookup.get(key);

				if (adad == null) {
					adad = new AlleleDiseaseAnnotationDocument();
					SpeciesType type = SpeciesType.getTypeByID(entry.getValue().getLeft().getTaxon().getCurie());
					int order = 100;
					if (type != null) {
						order = type.getOrderID();
					}
					adad.setPhylogeneticSortingIndex(order);
					adad.setSubject(entry.getValue().getLeft());
					adad.setDiseaseRelation(da.getDiseaseRelation());
					adad.setObject(da.getObject());
					lookup.put(key, adad);
				}
				adad.setEvidenceCodes(da.getEvidenceCodes());
				// gdad.setDataProvider(da.getDataProvider());
				adad.addReference(da.getSingleReference());
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
				String key = da.getDiseaseRelation().getName() + "_" + da.getObject().getName() + "_" + da.getNegated();
				AGMDiseaseAnnotationDocument adad = lookup.get(key);

				if (adad == null) {
					adad = new AGMDiseaseAnnotationDocument();
					SpeciesType type = SpeciesType.getTypeByID(entry.getValue().getLeft().getTaxon().getCurie());
					int order = 100;
					if (type != null) {
						order = type.getOrderID();
					}
					adad.setPhylogeneticSortingIndex(order);
					adad.setSubject(entry.getValue().getLeft());
					adad.setDiseaseRelation(da.getDiseaseRelation());
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

			da.getInferredAllele();

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
