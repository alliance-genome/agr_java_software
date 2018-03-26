package org.alliancegenome.indexer;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.entity.node.DOTerm;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.repository.Neo4jRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
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
