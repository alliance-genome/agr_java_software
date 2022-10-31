package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneDiseaseAnnotationInterface;
import org.alliancegenome.neo4j.repository.GeneRepository;

import si.mazi.rescu.RestProxyFactory;

public class GeneDiseaseAnnotationCurationIndexer extends DiseaseAnnotationCurationIndexer {

	private GeneDiseaseAnnotationInterface geneApi = RestProxyFactory.createProxy(GeneDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private static GeneRepository geneRepository = new GeneRepository();

	public GeneDiseaseAnnotationCurationIndexer(IndexerConfig config) {
		super(config);
	}

	public void index() {

		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);

		List<GeneDiseaseAnnotation> allGeneDiseaseAnnotations = new ArrayList<>();
		
		int batchSize = 1000;
		int page = 0;
		int pages = 0;
		do {
			SearchResponse<GeneDiseaseAnnotation> response = geneApi.find(page, batchSize, params);
			allGeneDiseaseAnnotations.addAll(response.getResults());
			
			if(page == 0) {
				System.out.println("Total Gene annotations from persistent store: " + String.format("%,d", response.getTotalResults()));
				display.startProcess("Pulling Gene DA's from curation", response.getTotalResults());
			}
			display.progressProcess(response.getReturnedRecords().longValue());
			
			pages = (int) (response.getTotalResults() / batchSize);
			page++;
		} while(page <= pages);
		display.finishProcess();


		System.out.println("Valid Gene annotations: " + String.format("%,d", allGeneDiseaseAnnotations.size()));
		List<String> geneIds = geneRepository.getAllGeneKeys();
		System.out.println("Valid Gene IDs: " + String.format("%,d", geneIds.size()));
		List<GeneDiseaseAnnotation> filteredGeneAnnotations = allGeneDiseaseAnnotations.stream().filter(agmDiseaseAnnotation -> geneIds.contains(agmDiseaseAnnotation.getSubject().getCurie())).collect(Collectors.toList());
		System.out.println("Filtered Gene annotation IDs: " + String.format("%,d", filteredGeneAnnotations.size()));
		System.out.println("Gene IDs not found in Neo4j:");
		allGeneDiseaseAnnotations.clear();
		System.out.println("Filtered Gene annotation IDs: " + String.format("%,d", filteredGeneAnnotations.size()));
		
		createJsonFile(filteredGeneAnnotations, "gene-disease-annotations.json");
	}


	public static void main(String[] args) {
		GeneDiseaseAnnotationCurationIndexer indexer = new GeneDiseaseAnnotationCurationIndexer(IndexerConfig.DiseaseAnnotationMlIndexer);
		indexer.index();
		System.exit(0);
	}

}
