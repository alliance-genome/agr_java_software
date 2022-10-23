package org.alliancegenome.indexer.indexers.linkml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.config.RestDefaultObjectMapper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections.CollectionUtils;
import si.mazi.rescu.RestProxyFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

@Log4j2
public class DiseaseAnnotationMLIndexer extends Indexer<SearchableItemDocument> {

	private GeneDiseaseAnnotationInterface geneApi = RestProxyFactory.createProxy(GeneDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private AlleleDiseaseAnnotationInterface alleleApi = RestProxyFactory.createProxy(AlleleDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private AgmDiseaseAnnotationInterface agmApi = RestProxyFactory.createProxy(AgmDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private static GeneRepository geneRepository = new GeneRepository();

	private static AlleleRepository alleleRepository = new AlleleRepository();

	public DiseaseAnnotationMLIndexer(IndexerConfig config) {
		super(config);
	}

	private List<AGMDiseaseAnnotation> agmDiseaseAnnotations;
	private List<AlleleDiseaseAnnotation> alleleDiseaseAnnotations;
	private List<GeneDiseaseAnnotation> geneDiseaseAnnotations;

	public void index() {
		indexAllele();
		indexAGMs();
		indexGenes();
		log.info("Allele DA before expansion: " + alleleDiseaseAnnotations.size());
		log.info("Gene DA before expansion: " + geneDiseaseAnnotations.size());
		expandAGMAnnotations();
		expandAlleleAnnotations();
		log.info("Allele DA after expansion: " + alleleDiseaseAnnotations.size());
		log.info("Gene DA after expansion: " + geneDiseaseAnnotations.size());

		RestDefaultObjectMapper restDefaultObjectMapper = new RestDefaultObjectMapper();
		ObjectMapper mapper = restDefaultObjectMapper.getMapper();
		mapper.writerWithView(View.FieldsAndLists.class);
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
		String jsonInString = null;
		try {
			jsonInString = writer.writeValueAsString(alleleDiseaseAnnotations);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		try (PrintStream out = new PrintStream(new FileOutputStream("/data/all-disease-annotation.json"))) {
			//try (PrintStream out = new PrintStream(new FileOutputStream("all-disease-annotation.json"))) {
			out.print(jsonInString);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

	}

	private void expandAlleleAnnotations() {
		alleleDiseaseAnnotations.forEach(alleleDiseaseAnnotation -> {
			if (alleleDiseaseAnnotation.getInferredGene() != null) {
				GeneDiseaseAnnotation geneAnnotation = new GeneDiseaseAnnotation();
				geneAnnotation.setSubject(alleleDiseaseAnnotation.getInferredGene());

				createGeneDA(alleleDiseaseAnnotation, geneAnnotation);
				geneDiseaseAnnotations.add(geneAnnotation);
			}
			if (alleleDiseaseAnnotation.getAssertedGene() != null) {
				GeneDiseaseAnnotation geneAnnotation = new GeneDiseaseAnnotation();
				geneAnnotation.setSubject(alleleDiseaseAnnotation.getAssertedGene());

				createGeneDA(alleleDiseaseAnnotation, geneAnnotation);
				geneDiseaseAnnotations.add(geneAnnotation);
			}
		});
	}

	private void expandAGMAnnotations() {
		agmDiseaseAnnotations.forEach(agmDiseaseAnnotation -> {
			if (agmDiseaseAnnotation.getInferredGene() != null) {
				GeneDiseaseAnnotation geneAnnotation = new GeneDiseaseAnnotation();
				geneAnnotation.setSubject(agmDiseaseAnnotation.getInferredGene());

				createGeneDA(agmDiseaseAnnotation, geneAnnotation);
				geneDiseaseAnnotations.add(geneAnnotation);
			}
			if (agmDiseaseAnnotation.getAssertedGene() != null) {
				GeneDiseaseAnnotation geneAnnotation = new GeneDiseaseAnnotation();
				geneAnnotation.setSubject(agmDiseaseAnnotation.getAssertedGene());

				createGeneDA(agmDiseaseAnnotation, geneAnnotation);
				geneDiseaseAnnotations.add(geneAnnotation);
			}
			if (agmDiseaseAnnotation.getInferredAllele() != null) {
				AlleleDiseaseAnnotation alleleDiseaseAnnotation = new AlleleDiseaseAnnotation();
				alleleDiseaseAnnotation.setSubject(agmDiseaseAnnotation.getInferredAllele());

				createGeneDA(agmDiseaseAnnotation, alleleDiseaseAnnotation);
				alleleDiseaseAnnotations.add(alleleDiseaseAnnotation);
			}
			if (agmDiseaseAnnotation.getAssertedAllele() != null) {
				AlleleDiseaseAnnotation alleleDiseaseAnnotation = new AlleleDiseaseAnnotation();
				alleleDiseaseAnnotation.setSubject(agmDiseaseAnnotation.getAssertedAllele());

				createGeneDA(agmDiseaseAnnotation, alleleDiseaseAnnotation);
				alleleDiseaseAnnotations.add(alleleDiseaseAnnotation);
			}
		});
	}

	private static void createGeneDA(DiseaseAnnotation agmDiseaseAnnotation, DiseaseAnnotation geneAnnotation) {
		geneAnnotation.setAnnotationType(agmDiseaseAnnotation.getAnnotationType());
		geneAnnotation.setDiseaseQualifiers(agmDiseaseAnnotation.getDiseaseQualifiers());
		geneAnnotation.setDiseaseGeneticModifier(agmDiseaseAnnotation.getDiseaseGeneticModifier());
		geneAnnotation.setConditionRelations(agmDiseaseAnnotation.getConditionRelations());
		geneAnnotation.setDataProvider(agmDiseaseAnnotation.getDataProvider());
		geneAnnotation.setDiseaseGeneticModifierRelation(agmDiseaseAnnotation.getDiseaseGeneticModifierRelation());
		geneAnnotation.setEvidenceCodes(agmDiseaseAnnotation.getEvidenceCodes());
		geneAnnotation.setGeneticSex(agmDiseaseAnnotation.getGeneticSex());
		geneAnnotation.setDiseaseRelation(agmDiseaseAnnotation.getDiseaseRelation());
		geneAnnotation.setNegated(agmDiseaseAnnotation.getNegated());
		geneAnnotation.setRelatedNotes(agmDiseaseAnnotation.getRelatedNotes());
		geneAnnotation.setObject(agmDiseaseAnnotation.getObject());
		geneAnnotation.setSecondaryDataProvider(agmDiseaseAnnotation.getSecondaryDataProvider());
		geneAnnotation.setSingleReference(agmDiseaseAnnotation.getSingleReference());
		geneAnnotation.setWith(agmDiseaseAnnotation.getWith());
		geneAnnotation.setDateCreated(agmDiseaseAnnotation.getDateCreated());
		geneAnnotation.setDateUpdated(agmDiseaseAnnotation.getDateUpdated());
		geneAnnotation.setInternal(agmDiseaseAnnotation.getInternal());
		geneAnnotation.setObsolete(agmDiseaseAnnotation.getObsolete());
		geneAnnotation.setUpdatedBy(agmDiseaseAnnotation.getUpdatedBy());
	}

	public void indexGenes() {
		ProcessDisplayHelper display = new ProcessDisplayHelper(log, 10000);
		HashMap<String, Object> params = new HashMap<>();
		int batchSize = 300;
		SearchResponse<GeneDiseaseAnnotation> response = geneApi.find(0, batchSize, params);
		List<GeneDiseaseAnnotation> annotationBatch = removePrivateData(response.getResults());
		long total = response.getTotalResults();
		log.info("Total Gene annotations from persistent store: " + String.format("%,d", total));
		int pages = (int) (total / batchSize);
		display.startProcess("Starting Gene indexing", total);
		for (int page = 1; page < pages + 1; page++) {
			response = geneApi.find(page, batchSize, params);
			display.progressProcess((long) response.getReturnedRecords());
			annotationBatch.addAll(removePrivateData(response.getResults()));
		}
		display.finishProcess();
		log.info("Valid Gene annotations: " + String.format("%,d", annotationBatch.size()));
		List<String> geneIds = geneRepository.getAllGeneKeys();
		log.info("Valid Gene IDs: " + String.format("%,d", geneIds.size()));
		List<GeneDiseaseAnnotation> filteredAnnotation = annotationBatch.stream().filter(agmDiseaseAnnotation -> geneIds.contains(agmDiseaseAnnotation.getSubject().getCurie())).collect(Collectors.toList());
		log.info("Filtered Gene annotation IDs: " + String.format("%,d", filteredAnnotation.size()));
		log.info("Gene IDs not found in Neo4j:");
		geneDiseaseAnnotations = filteredAnnotation;
		annotationBatch.stream().filter(agmDiseaseAnnotation -> !geneIds.contains(agmDiseaseAnnotation.getSubject().getCurie())).collect(Collectors.toList()).forEach(agmDiseaseAnnotation -> log.info(agmDiseaseAnnotation.getSubject().getCurie()));

		log.info("Number of Disease Annotation: " + response.getTotalResults());
	}

	public void indexAGMs() {
		ProcessDisplayHelper display = new ProcessDisplayHelper(log, 10000);
		HashMap<String, Object> params = new HashMap<>();
		int batchSize = 400;
		SearchResponse<AGMDiseaseAnnotation> response = agmApi.find(0, batchSize, params);
		List<AGMDiseaseAnnotation> annotationBatch = removePrivateData(response.getResults());
		long total = response.getTotalResults();
		log.info("Total AGM annotations from persistent store: " + String.format("%,d", total));
		int pages = (int) (total / batchSize);
		display.startProcess("Starting AGM indexing", total);
		for (int page = 1; page < pages + 1; page++) {
			response = agmApi.find(page, batchSize, params);
			display.progressProcess((long) response.getReturnedRecords());
			annotationBatch.addAll(removePrivateData(response.getResults()));
		}
		display.finishProcess();
		log.info("Valid AGM annotations: " + String.format("%,d", annotationBatch.size()));
		List<String> agmIds = geneRepository.getAllAgmKeys();
		log.info("Valid AGM annotation IDs: " + String.format("%,d", agmIds.size()));
		List<AGMDiseaseAnnotation> filteredAnnotation = annotationBatch.stream().filter(agmDiseaseAnnotation -> agmIds.contains(agmDiseaseAnnotation.getSubject().getCurie())).collect(Collectors.toList());
		log.info("Filtered AGM annotation IDs: " + String.format("%,d", filteredAnnotation.size()));
		log.info("AGM IDs not found in Neo4j:");
		agmDiseaseAnnotations = filteredAnnotation;
		annotationBatch.stream().filter(agmDiseaseAnnotation -> !agmIds.contains(agmDiseaseAnnotation.getSubject().getCurie())).collect(Collectors.toList()).forEach(agmDiseaseAnnotation -> log.info(agmDiseaseAnnotation.getSubject().getCurie()));
		log.info("Number of Disease Annotation: " + response.getTotalResults());
	}

	public void indexAllele() {
		ProcessDisplayHelper display = new ProcessDisplayHelper(log, 10000);
		HashMap<String, Object> params = new HashMap<>();
		int batchSize = 300;
		SearchResponse<AlleleDiseaseAnnotation> response = alleleApi.find(0, batchSize, params);
		List<AlleleDiseaseAnnotation> annotationBatch = removePrivateData(response.getResults());
		long total = response.getTotalResults();
		log.info("Total Allele annotations from persistent store: " + String.format("%,d", total));
		int pages = (int) (total / batchSize);
		display.startProcess("Starting Allele indexing", total);
		pages = 0;
		for (int page = 1; page < pages + 1; page++) {
			response = alleleApi.find(page, batchSize, params);
			display.progressProcess((long) response.getReturnedRecords());
			if (CollectionUtils.isNotEmpty(response.getResults())) {
				annotationBatch.addAll(removePrivateData(response.getResults()));
			}
		}
		display.finishProcess();
		log.info("Valid allele annotations: " + String.format("%,d", annotationBatch.size()));
		List<String> alleleIDs = alleleRepository.getAllAlleleIDs();
		log.info("Valid Allele IDs: " + String.format("%,d", alleleIDs.size()));
		List<AlleleDiseaseAnnotation> filteredAnnotation = annotationBatch.stream().filter(agmDiseaseAnnotation -> alleleIDs.contains(agmDiseaseAnnotation.getSubject().getCurie())).collect(Collectors.toList());
		log.info("Filtered Allele annotation IDs: " + String.format("%,d", filteredAnnotation.size()));
		log.info("Allele IDs not found in Neo4j:");
		alleleDiseaseAnnotations = filteredAnnotation;
		annotationBatch.stream().filter(agmDiseaseAnnotation -> !alleleIDs.contains(agmDiseaseAnnotation.getSubject().getCurie())).collect(Collectors.toList()).forEach(agmDiseaseAnnotation -> log.info(agmDiseaseAnnotation.getSubject().getCurie()));
		log.info("Number of Disease Annotation: " + response.getTotalResults());
	}

	private static <T extends DiseaseAnnotation> List<T> removePrivateData(List<T> annotations) {
		return annotations.stream().filter(diseaseAnnotation -> !diseaseAnnotation.getInternal()).collect(Collectors.toList());
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {

	}


	public static void main(String[] args) {
		DiseaseAnnotationMLIndexer indexer = new DiseaseAnnotationMLIndexer(IndexerConfig.DiseaseAnnotationMlIndexer);
		//SearchResponse<GeneDiseaseAnnotation> response = indexer.geneApi.find(0, 100, new HashMap<>());
		indexer.index();
		//indexer.indexAllele();
		//indexer.indexGenes();


		log.info("HTTP code: ");
		System.exit(0);
	}

}
