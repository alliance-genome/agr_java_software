package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AlleleDiseaseAnnotationInterface;
import org.alliancegenome.neo4j.repository.AlleleRepository;

import lombok.extern.log4j.Log4j2;
import si.mazi.rescu.RestProxyFactory;

@Log4j2
public class AlleleDiseaseAnnotationCurationIndexer extends DiseaseAnnotationCurationIndexer {

	private AlleleDiseaseAnnotationInterface alleleApi = RestProxyFactory.createProxy(AlleleDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private static AlleleRepository alleleRepository = new AlleleRepository();

	public AlleleDiseaseAnnotationCurationIndexer(IndexerConfig config) {
		super(config);
	}

	public void index() {

		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		
		List<AlleleDiseaseAnnotation> allAlleleDiseaseAnnotations = new ArrayList<>();

		int batchSize = 1000;
		int page = 0;
		int pages = 0;
		do {
			log.info("Running Search with: " + params);
			SearchResponse<AlleleDiseaseAnnotation> response = alleleApi.find(page, batchSize, params);
			allAlleleDiseaseAnnotations.addAll(response.getResults());
			
			if(page == 0) {
				display.startProcess("Pulling Allele DA's from curation", response.getTotalResults());
			}
			display.progressProcess(response.getReturnedRecords().longValue());
			
			pages = (int) (response.getTotalResults() / batchSize);
			page++;
		} while(page <= pages);
		display.finishProcess();

		log.info("Valid allele annotations: " + String.format("%,d", allAlleleDiseaseAnnotations.size()));
		List<String> alleleIDs = alleleRepository.getAllAlleleIDs();
		log.info("Valid Allele IDs: " + String.format("%,d", alleleIDs.size()));
		List<AlleleDiseaseAnnotation> filteredAlleleDiseaseAnnotation = allAlleleDiseaseAnnotations.stream().filter(alleleDiseaseAnnotation -> alleleIDs.contains(alleleDiseaseAnnotation.getSubject().getCurie())).collect(Collectors.toList());
		allAlleleDiseaseAnnotations.clear();
		log.info("Filtered Allele annotation IDs: " + String.format("%,d", filteredAlleleDiseaseAnnotation.size()));
		
		List<GeneDiseaseAnnotation> expandedAlleleDiseaseAnnotations = expandGeneAnnotationsFromAlleleDiseaseAnnotations(filteredAlleleDiseaseAnnotation);

		createJsonFile(filteredAlleleDiseaseAnnotation, "allele-disease-annotation.json");
		createJsonFile(expandedAlleleDiseaseAnnotations, "expanded-gene-annotations-from-allele-disease-annotations.json");
	}

	public static void main(String[] args) {
		log.info("Running Indexer");
		AlleleDiseaseAnnotationCurationIndexer indexer = new AlleleDiseaseAnnotationCurationIndexer(IndexerConfig.DiseaseAnnotationMlIndexer);
		indexer.index();
		System.exit(0);
	}

}
