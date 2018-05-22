package org.alliancegenome.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.site.dao.DiseaseDAO;
import org.alliancegenome.es.index.site.dao.GeneDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchResult;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class TestGetGene {

    private GeneDAO geneService;

    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();
        geneService = new GeneDAO();
    }

    public static void main(String[] args) throws JsonProcessingException {
        GeneRepository repo = new GeneRepository();

        DiseaseDAO service = new DiseaseDAO();

        Pagination pagination = new Pagination(1, 20, "gene", "true");
        //pagination.addFieldFilter(FieldFilter.GENE_NAME, "p");
        //pagination.addFieldFilter(FieldFilter.DISEASE, "fat");
        //pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, "is_");
        //pagination.addFieldFilter(FieldFilter.REFERENCE, "PMID");
        //pagination.addFieldFilter(FieldFilter.SPECIES, "Homo");
        //pagination.addFieldFilter(FieldFilter.SOURCE, "Mgi");
        //pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, "TA");
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, "allel");
        SearchResult response = service.getDiseaseAnnotations("DOID:655", pagination);
        System.out.println("Result size: " + response.results.size());
        if (response.results != null) {
            response.results.forEach(entry -> {
                Map<String, Object> map1 = (Map<String, Object>) entry.get("geneDocument");
                if (map1 != null)
                    System.out.println(entry.get("diseaseID") + "\t" + entry.get("diseaseName") + ": " + "\t" + map1.get("species") + ": " + map1.get("symbol") + ": " + map1.get("primaryId")+ ": " + map1.get("associationType"));

            });
        }

        //"MGI:97490" OR g.primaryKey = "RGD:3258"

/*
        System.out.println("MGI:97490");
        HashMap<String, Gene> geneMap = repo.getGene("RGD:3258");
        System.out.println(geneMap);

        Gene gene = null;
        gene = repo.getOneGene("ZFIN:ZDB-GENE-990415-270");
        //gene = repo.getOneGene("FB:FBgn0036309");

        GeneTranslator trans = new GeneTranslator();
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(trans.translate(gene));
        System.out.println(json);
*/

    }

    public void checkSecondaryId() {
        Map<String, Object> result = geneService.getGeneBySecondary("ZFIN:ZDB-GENE-030131-3355");
        assertNotNull(result);
    }
}
