package org.alliancegenome.core.tests.unit;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import com.fasterxml.jackson.core.JsonProcessingException;

public class TestGetGene {

    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();
    }

    public static void main(String[] args) throws JsonProcessingException {
        Pagination pagination = new Pagination(1, 20, "gene", "true");
        //pagination.addFieldFilter(FieldFilter.GENE_NAME, "p");
        //pagination.addFieldFilter(FieldFilter.DISEASE, "fat");
        //pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, "is_");
        //pagination.addFieldFilter(FieldFilter.REFERENCE, "PMID");
        //pagination.addFieldFilter(FieldFilter.SPECIES, "Homo");
        //pagination.addFieldFilter(FieldFilter.SOURCE, "Mgi");
        //pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, "TA");
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, "allel");
        //SearchApiResponse response = service.getDiseaseAnnotations("DOID:655", pagination);

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

}
