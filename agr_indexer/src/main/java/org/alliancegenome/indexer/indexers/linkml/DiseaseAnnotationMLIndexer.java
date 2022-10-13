package org.alliancegenome.indexer.indexers.linkml;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

@Log4j2
public class DiseaseAnnotationMLIndexer extends Indexer<SearchableItemDocument> {

    private DiseaseAnnotationInterface api = RestProxyFactory.createProxy(DiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

    public DiseaseAnnotationMLIndexer(IndexerConfig config) {
        super(config);
    }

    public void index() {
        try {
            HashMap<String, Object> params = new HashMap<>();
            SearchResponse<DiseaseAnnotation> response = api.find(0, 1000000, params);
            log.info("Number of Disease Annotation: " + response.getTotalResults());
        } catch (Exception e) {
            log.error("Error while indexing...", e);
            System.exit(-1);
        }
    }

    @Override
    protected void startSingleThread(LinkedBlockingDeque<String> queue) {

    }


    public static void main(String[] args) {
        DiseaseAnnotationMLIndexer indexer = new DiseaseAnnotationMLIndexer(IndexerConfig.DiseaseAnnotationMlIndexer);
        SearchResponse<DiseaseAnnotation> response = indexer.api.find(0, 10, new HashMap<>());


        System.out.println("HTTP code: " + response);
    }

}
