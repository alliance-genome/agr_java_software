package org.alliancegenome.indexer.indexers.linkml;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.DiseaseAnnotationRESTInterface;
import org.alliancegenome.indexer.RestConfig;
import org.jboss.logging.Logger;
import si.mazi.rescu.RestProxyFactory;


public class DiseaseAnnotationIndexer {

    private Logger log = Logger.getLogger(getClass());
    private DiseaseAnnotationRESTInterface api = RestProxyFactory.createProxy(DiseaseAnnotationRESTInterface.class, ConfigHelper.getCurationApiBaseUrl(), RestConfig.config);

    public DiseaseAnnotationIndexer() {
    }

    public void index() {
        try {
            SearchResponse<AGMDiseaseAnnotation> response = api.getAgmDiseaseAnnotation("", 100);
            log.info("Number of Disease Annotation: " + response.getTotalResults());
        } catch (Exception e) {
            log.error("Error while indexing...", e);
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
/*
        DiseaseAnnotationRESTInterface api = RestManager.getDiseaseAnnotationEndpoints();
        JsonResultResponse<AGMDiseaseAnnotation> annotation = api.getAgmDiseaseAnnotation("Bearer fb0d3a42-aac6-4470-8d66-da2777117598");
*/
        DiseaseAnnotationIndexer indexer = new DiseaseAnnotationIndexer();
        SearchResponse<AGMDiseaseAnnotation> response = indexer.api.getAgmDiseaseAnnotation("", 10);
        SearchResponse<GeneDiseaseAnnotation> response1 = indexer.api.getGeneDiseaseAnnotation("", 10);
        SearchResponse<AlleleDiseaseAnnotation> response2 = indexer.api.getAlleleDiseaseAnnotation("", 10);



/*
        ResteasyClient client = new ResteasyClientBuilder().register(resteasyJacksonProvider).build();
        //ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(path));
        DiseaseAnnotationRESTInterfaceAlliance proxy = target.proxy(DiseaseAnnotationRESTInterfaceAlliance.class);

        AllianceDiseaseAnnotation annotation = proxy.getDiseaseAnnotation(4491701);
*/
        System.out.println("HTTP code: ");
    }

}
