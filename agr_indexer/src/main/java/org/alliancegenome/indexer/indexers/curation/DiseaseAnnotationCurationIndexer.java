package org.alliancegenome.indexer.indexers.curation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.curation_api.config.RestDefaultObjectMapper;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.curation_api.view.View;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.document.AGMDiseaseAnnotationDocument;
import org.alliancegenome.indexer.indexers.curation.document.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.indexer.indexers.curation.document.GeneDiseaseAnnotationDocument;
import org.alliancegenome.indexer.indexers.curation.service.AGMDiseaseAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.AlleleDiseaseAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.GeneDiseaseAnnotationService;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingDeque;

@Log4j2
public class DiseaseAnnotationCurationIndexer extends Indexer<SearchableItemDocument> {

	private GeneDiseaseAnnotationService geneService = new GeneDiseaseAnnotationService();
	private AlleleDiseaseAnnotationService alleleService = new AlleleDiseaseAnnotationService();
	private AGMDiseaseAnnotationService agmService = new AGMDiseaseAnnotationService();


	private Map<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> geneMap = new HashMap<>();
	private Map<String, Pair<Allele, ArrayList<DiseaseAnnotation>>> alleleMap = new HashMap<>();
	private Map<String, Pair<AffectedGenomicModel, ArrayList<DiseaseAnnotation>>> agmMap = new HashMap<>();

	// TODO implement these in the future when we switch to the other tables.
	//private Map<String, Pair<Allele, ArrayList<DiseaseAnnotation>>> alleleMap = new HashMap<String, Pair<Allele, ArrayList<DiseaseAnnotation>>>();
	//private Map<String, Pair<AffectedGenomicModel, ArrayList<DiseaseAnnotation>>> agmMap = new HashMap<String, Pair<AffectedGenomicModel, ArrayList<DiseaseAnnotation>>>();


	public DiseaseAnnotationCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	protected void createJsonFile(List<GeneDiseaseAnnotationDocument> gdaList, String fileName) {
		RestDefaultObjectMapper restDefaultObjectMapper = new RestDefaultObjectMapper();
		ObjectMapper mapper = restDefaultObjectMapper.getMapper();
		mapper.writerWithView(View.FieldsAndLists.class);
		ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
		try (PrintStream out = new PrintStream(new FileOutputStream(fileName))) {
			out.print(writer.writeValueAsString(gdaList));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {

	}

	@Override
	protected void index() {
		indexGenes();
		indexAlleles();
		indexAGMs();

		List<GeneDiseaseAnnotationDocument> list = createGeneDiseaseAnnotationDocuments();

		createJsonFile(list, "gdaList.json");

		List<AlleleDiseaseAnnotationDocument> alleleList = createAlleleDiseaseAnnotationDocuments();
		createJsonFile(list, "adaList.json");

		List<AGMDiseaseAnnotationDocument> agmList = createAGMDiseaseAnnotationDocuments();
		createJsonFile(list, "agmaList.json");

		//createAGMDiseaseAnnotationDocuments();

	}

	private List<GeneDiseaseAnnotationDocument> createGeneDiseaseAnnotationDocuments() {

		List<GeneDiseaseAnnotationDocument> ret = new ArrayList<>();

		for (Entry<String, Pair<Gene, ArrayList<DiseaseAnnotation>>> entry : geneMap.entrySet()) {
			HashMap<String, GeneDiseaseAnnotationDocument> lookup = new HashMap<>();

			for (DiseaseAnnotation da : entry.getValue().getRight()) {
				String key = da.getDiseaseRelation().getName() + "_" + da.getObject().getName();
				GeneDiseaseAnnotationDocument gdad = lookup.get(key);
				GeneDiseaseAnnotationDocument gdad = lookup.get(key);

				if (gdad == null) {
					gdad = new GeneDiseaseAnnotationDocument();
					gdad.setSubject(entry.getValue().getLeft());
					gdad.setDiseaseRelation(da.getDiseaseRelation());
					gdad.setObject(da.getObject());
					lookup.put(key, gdad);
				}
				gdad.setEvidenceCodes(da.getEvidenceCodes());
				//gdad.setDataProvider(da.getDataProvider());
				gdad.addReference(da.getSingleReference());
				if (da instanceof AlleleDiseaseAnnotation || da instanceof AGMDiseaseAnnotation) {
					gdad.addPrimaryAnnotation(da);
				}
			}
			ret.addAll(lookup.values());
			lookup.clear();
		}

		return ret;
	}

	private List<AlleleDiseaseAnnotationDocument> createAlleleDiseaseAnnotationDocuments() {

		List<AlleleDiseaseAnnotationDocument> ret = new ArrayList<>();

		for (Entry<String, Pair<Allele, ArrayList<DiseaseAnnotation>>> entry : alleleMap.entrySet()) {
			HashMap<String, AlleleDiseaseAnnotationDocument> lookup = new HashMap<>();

			for (DiseaseAnnotation da : entry.getValue().getRight()) {
				String key = da.getDiseaseRelation().getName() + "_" + da.getObject().getName();
				AlleleDiseaseAnnotationDocument gdad = lookup.get(key);

				if (gdad == null) {
					gdad = new AlleleDiseaseAnnotationDocument();
					gdad.setSubject(entry.getValue().getLeft());
					gdad.setDiseaseRelation(da.getDiseaseRelation());
					gdad.setObject(da.getObject());
					lookup.put(key, gdad);
				}
				gdad.setEvidenceCodes(da.getEvidenceCodes());
				//gdad.setDataProvider(da.getDataProvider());
				gdad.addReference(da.getSingleReference());
				if (da instanceof AlleleDiseaseAnnotation || da instanceof AGMDiseaseAnnotation) {
					gdad.addPrimaryAnnotation(da);
				}
			}
			ret.addAll(lookup.values());
			lookup.clear();
		}

		return ret;
	}

	private List<AGMDiseaseAnnotationDocument> createAGMDiseaseAnnotationDocuments() {

		List<AGMDiseaseAnnotationDocument> ret = new ArrayList<>();

		for (Entry<String, Pair<AffectedGenomicModel, ArrayList<DiseaseAnnotation>>> entry : agmMap.entrySet()) {
			HashMap<String, AGMDiseaseAnnotationDocument> lookup = new HashMap<>();

			for (DiseaseAnnotation da : entry.getValue().getRight()) {
				String key = da.getDiseaseRelation().getName() + "_" + da.getObject().getName();
				AGMDiseaseAnnotationDocument gdad = lookup.get(key);

				if (gdad == null) {
					gdad = new AGMDiseaseAnnotationDocument();
					gdad.setSubject(entry.getValue().getLeft());
					gdad.setDiseaseRelation(da.getDiseaseRelation());
					gdad.setObject(da.getObject());
					lookup.put(key, gdad);
				}
				gdad.setEvidenceCodes(da.getEvidenceCodes());
				//gdad.setDataProvider(da.getDataProvider());
				gdad.addReference(da.getSingleReference());
			}
			ret.addAll(lookup.values());
			lookup.clear();
		}

		return ret;
	}

	private void indexGenes() {
		List<GeneDiseaseAnnotation> geneDiseaseAnnotations = geneService.getFiltered();
		for (GeneDiseaseAnnotation da : geneDiseaseAnnotations) {
			Gene gene = da.getSubject();
			Pair<Gene, ArrayList<DiseaseAnnotation>> pair = geneMap.computeIfAbsent(gene.getCurie(), geneCurie -> Pair.of(gene, new ArrayList<>()));
			pair.getRight().add(da);
		}
	}

	private void indexAlleles() {

		List<AlleleDiseaseAnnotation> alleleDiseaseAnnotations = alleleService.getFiltered();

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

	private void indexAGMs() {

		List<AGMDiseaseAnnotation> agmDiseaseAnnotations = agmService.getFiltered();

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
		}
	}

	public static void main(String[] args) {
		DiseaseAnnotationCurationIndexer indexer = new DiseaseAnnotationCurationIndexer(IndexerConfig.DiseaseAnnotationMlIndexer);
		indexer.indexGenes();
		System.exit(0);
	}


}
