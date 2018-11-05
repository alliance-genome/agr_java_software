package org.alliancegenome.api;

import java.util.Map;

import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.es.index.site.dao.DiseaseDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jboss.logging.Logger;
import org.junit.Before;

public class DiseaseTest {

    private static Logger log = Logger.getLogger(DiseaseTest.class);

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();
    }


    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        ConfigHelper.init();

        DiseaseDAO service = new DiseaseDAO();

        service.init();
        System.out.println("Number of Diseases with Genes Info: ");

        //DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        //String str = translator.getAllRows(service.getDiseaseAnnotationsDownload("DOID:9351", Pagination.getDownloadPagination()));
        Pagination pagination = new Pagination(1, 20, "gene", "true");
        pagination.addFieldFilter(FieldFilter.GENE_NAME, "l");
        SearchApiResponse response = service.getDiseaseAnnotations("DOID:655", pagination);
        if (response.resultMapList != null) {
            response.resultMapList.forEach(entry -> {
                Map<String, Object> map1 = (Map<String, Object>) entry.get("geneDocument");
                if (map1 != null)
                    log.info(entry.get("diseaseID") + "\t" + entry.get("diseaseName") + ": " + "\t" + map1.get("species") + ": " + map1.get("symbol") + ": " + map1.get("primaryId"));

            });
        }
        System.out.println("Number of resultMapList " + response.total);

        pagination = new Pagination(1, Integer.MAX_VALUE, null, null);
        DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        DiseaseService diseaseService = new DiseaseService();
        System.out.println(translator.getAllRows(diseaseService.getDiseaseAnnotationsDownload("DOID:655", pagination)));

    }

}