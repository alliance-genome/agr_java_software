package org.alliancegenome.indexer.indexers.linkml;

import java.util.HashMap;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;

import lombok.extern.jbosslog.JBossLog;
import si.mazi.rescu.RestProxyFactory;

@JBossLog
public class DiseaseAnnotationIndexer {

	private DiseaseAnnotationInterface api = RestProxyFactory.createProxy(DiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public DiseaseAnnotationIndexer() {
	}

	public void index() {
		try {
			HashMap<String, Object> params = new HashMap<>();
			SearchResponse<DiseaseAnnotation> response = api.find(0, 10, params);
			log.info("Number of Disease Annotation: " + response.getTotalResults());
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			System.exit(-1);
		}
	}

	public static void main(String[] args) {
		DiseaseAnnotationIndexer indexer = new DiseaseAnnotationIndexer();
		SearchResponse<DiseaseAnnotation> response = indexer.api.find(0, 10, new HashMap<>());


		System.out.println("HTTP code: " + response);
	}

}
