package org.alliancegenome.api;

import org.alliancegenome.api.config.ConfigHelper;
import org.alliancegenome.api.dao.DiseaseDAO;
import org.alliancegenome.api.model.SearchResult;
import org.alliancegenome.api.service.helper.Pagination;
import org.alliancegenome.api.translator.DiseaseAnnotationToTdfTranslator;
import org.jboss.logging.Logger;

import java.util.Map;

public class Disease1Test {

    private static Logger log = Logger.getLogger(Disease1Test.class);

    public static void main(String[] args) {
        ConfigHelper helper = new ConfigHelper();
        helper.init();

        DiseaseDAO service = new DiseaseDAO();
        service.setConfig(helper);
        service.init();
        System.out.println("Number of Diseases with Genes Info: ");

        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        String str = translator.getAllRows(service.getDiseaseAnnotationsDownload("DOID:9351", Pagination.getDownloadPagination()));
        Pagination pagination = new Pagination(1, 20, "disease", null);
        SearchResult response = service.getDiseaseAnnotations("DOID:655", pagination);
        if (response.results != null) {
            response.results.forEach(entry -> {
                Map<String, Object> map1 = (Map<String, Object>) entry.get("geneDocument");
                if (map1 != null)
                    log.info(entry.get("diseaseID") + "\t" + entry.get("diseaseName") + ": "+ "\t" + map1.get("species") + ": " + map1.get("symbol") + ": " + map1.get("primaryId"));

            });
        }
        System.out.println("Number of results " + response.total);

    }


}