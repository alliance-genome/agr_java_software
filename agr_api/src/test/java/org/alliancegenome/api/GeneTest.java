package org.alliancegenome.api;

import org.alliancegenome.api.controller.GeneController;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.site.dao.GeneDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchResult;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class GeneTest {

    private GeneService geneService;

    private static Logger log = Logger.getLogger(GeneTest.class);


    public static void main(String[] args) {
        GeneDAO service = new GeneDAO();

        service.init();
        System.out.println("Number of Diseases with Genes Info: ");

        //DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();
        //String str = translator.getAllRows(service.getDiseaseAnnotationsDownload("DOID:9351", Pagination.getDownloadPagination()));
        Pagination pagination = new Pagination(1, 20, "gene", "true");
        pagination.addFieldFilter(FieldFilter.GENE_NAME, "l");
        SearchResult response = service.getAllelesByGene("ZFIN:ZDB-GENE-051127-5", pagination);
        if (response.results != null) {
            response.results.forEach(entry -> {
                Map<String, Object> map1 = (Map<String, Object>) entry.get("geneDocument");
                if (map1 != null)
                    log.info(entry.get("diseaseID") + "\t" + entry.get("diseaseName") + ": " + "\t" + map1.get("species") + ": " + map1.get("symbol") + ": " + map1.get("primaryId"));

            });
        }
        System.out.println("Number of results " + response.total);

    }

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();
        geneService = new GeneService();
    }


    @Test
    @Ignore
    public void checkForSecondaryId() {
        // ZFIN:ZDB-GENE-030131-3355 is a secondary ID for ZFIN:ZDB-LINCRNAG-160518-1
        Map<String, Object> result = geneService.getById("ZFIN:ZDB-GENE-030131-3355");
        assertNotNull(result);
        assertThat(result.get("primaryId"), equalTo("ZFIN:ZDB-LINCRNAG-160518-1"));
        assertThat(result.get("species"), equalTo("Danio rerio"));
    }

    @Test
    public void checkOrthologyAPIWithSpecies() throws IOException {

        GeneController controller = new GeneController();
        String json = controller.getGeneOrthology("MGI:109583", null, "NCBITaxon:10115", null, null, null);
        assertThat(json, equalTo("[]"));
        json = controller.getGeneOrthology("MGI:109583", null, "NCBITaxon:10116", null, null, null);
        assertThat(json, startsWith("[{\"gene"));
        json = controller.getGeneOrthology("MGI:109583", null, "NCBITaxon:10116,NCBITaxon:7955", null, null, null);
        assertThat(json, startsWith("[{\"gene"));
/*
        json = controller.getGeneOrthology("MGI:109583", "stringENT", null, null, null, null);
        assertNotNull(json);
*/
    }

    @Test
    public void checkOrthologyAPIWithMethods() throws IOException {

        GeneController controller = new GeneController();
        String json = controller.getGeneOrthology("MGI:109583", null, null, "ZFIN", null, null);
        assertThat(json, equalTo("[]"));
        json = controller.getGeneOrthology("MGI:109583", null, null, "OrthoFinder", null, null);
        assertThat(json, startsWith("[{\"gene"));
        json = controller.getGeneOrthology("MGI:109583", null, null, "OrthoFinder,ZFIN", null, null);
        assertThat(json, equalTo("[]"));
        json = controller.getGeneOrthology("MGI:109583", null, null, "OrthoFinder,Panther", null, null);
        assertThat(json, startsWith("[{\"gene"));
    }

    @Test
    public void checkOrthologyAPINoFilters() throws IOException {

        GeneController controller = new GeneController();
        String json = controller.getGeneOrthology("MGI:109583", null, null, null, null, null);
        assertNotNull(json);
    }

}