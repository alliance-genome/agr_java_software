package org.alliancegenome.indexer;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.translators.document.DiseaseTranslator;
import org.alliancegenome.es.index.site.document.DiseaseDocument;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.Neo4jRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DiseaseCrossRefTest {

    public static void main(String[] args) throws Exception {
        ConfigHelper.init();
        
        //Configurator.setLevel("org.neo4j",Level.DEBUG);
        Logger log = LogManager.getLogger(DiseaseTest.class);
        log.info("Hallo");

        DiseaseRepository diseaseRepository = new DiseaseRepository();
        
        Neo4jRepository<DOTerm> neo4jService = new Neo4jRepository<DOTerm>(DOTerm.class);

        DiseaseTranslator translator = new DiseaseTranslator();
        
        String disease = "DOID:0060335";
        
        DOTerm diseaseTerm = diseaseRepository.getDiseaseTerm(disease);
        
        
        DOTerm diseaseTerm1 = diseaseRepository.getDiseaseTermWithAnnotations(disease);
        DiseaseDocument doc = translator.translate(diseaseTerm, 1);
        
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
        System.out.println(json);

        
    }

}
