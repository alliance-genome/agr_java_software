package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AgmDiseaseAnnotationInterface;
import org.alliancegenome.neo4j.repository.GeneRepository;

import si.mazi.rescu.RestProxyFactory;

public class AGMDiseaseAnnotationCurationIndexer extends DiseaseAnnotationCurationIndexer {

	private AgmDiseaseAnnotationInterface agmApi = RestProxyFactory.createProxy(AgmDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private static GeneRepository geneRepository = new GeneRepository();

	public AGMDiseaseAnnotationCurationIndexer(IndexerConfig config) {
		super(config);
	}

	public void index() {
		
		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		
		List<AGMDiseaseAnnotation> allAGMDiseaseAnnotations = new ArrayList<>();

		int batchSize = 500;
		int page = 0;
		int pages = 0;
		do {
			SearchResponse<AGMDiseaseAnnotation> response = agmApi.find(page, batchSize, params);
			allAGMDiseaseAnnotations.addAll(response.getResults());
			
			if(page == 0) {
				System.out.println("Total AGM annotations from persistent store: " + String.format("%,d", response.getTotalResults()));
				display.startProcess("Pulling AGM DA's from curation", response.getTotalResults());
			}
			display.progressProcess(response.getReturnedRecords().longValue());
			
			pages = (int) (response.getTotalResults() / batchSize);
			page++;
		} while(page <= pages);
		display.finishProcess();
		
		
		System.out.println("Valid AGM annotations: " + String.format("%,d", allAGMDiseaseAnnotations.size()));
		List<String> agmIds = geneRepository.getAllAgmKeys();
		System.out.println("Valid AGM annotation IDs: " + String.format("%,d", agmIds.size()));
		List<AGMDiseaseAnnotation> filteredAGMAnnotations = allAGMDiseaseAnnotations.stream().filter(agmDiseaseAnnotation -> agmIds.contains(agmDiseaseAnnotation.getSubject().getCurie())).collect(Collectors.toList());
		allAGMDiseaseAnnotations.clear();
		System.out.println("Filtered AGM annotation IDs: " + String.format("%,d", filteredAGMAnnotations.size()));
		
		
		List<GeneDiseaseAnnotation> geneDiseaseAnnotations = expandGeneDiseaseAnnotationsFromAGMDiseaseAnnotations(filteredAGMAnnotations);
		List<AlleleDiseaseAnnotation> alleleDiseaseAnnotations = expandAlleleDiseaseAnnotationsFromAGMDiseaseAnnotations(filteredAGMAnnotations);

		createJsonFile(filteredAGMAnnotations, "agm-disease-annotations.json");
		createJsonFile(geneDiseaseAnnotations, "expanded-gene-annotations-from-agm-disease-annotations.json");
		createJsonFile(alleleDiseaseAnnotations, "expanded-allele-annotations-from-agm-disease-annotations.json");
	}

	public static void main(String[] args) {
		AGMDiseaseAnnotationCurationIndexer indexer = new AGMDiseaseAnnotationCurationIndexer(IndexerConfig.DiseaseAnnotationMlIndexer);
		indexer.index();
		System.exit(0);
	}

}
