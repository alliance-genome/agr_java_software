package org.alliancegenome.indexer;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.document.DiseaseAnnotationDocument;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.entity.node.DOTerm;
import org.alliancegenome.indexer.entity.node.Feature;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.repository.FeatureRepository;
import org.alliancegenome.indexer.repository.Neo4jRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DiseaseTest {

    public static void main(String[] args) throws Exception {
        Configurator.setRootLevel(Level.WARN);
        //Configurator.setLevel("org.neo4j",Level.DEBUG);
        Logger log = LogManager.getLogger(DiseaseTest.class);
        log.info("Hallo");
        ConfigHelper.init();

        DiseaseRepository diseaseRepository = new DiseaseRepository();
        FeatureRepository featureRepository = new FeatureRepository();
/*
        Iterable<DOTerm> disease_entities = neo4jService.getPage(0, 1000, 3);

        disease_entities.forEach(entity -> {
            if (entity.getGenes() != null)
                System.out.println(entity);
        });
        Collection<DOTerm> entityt = neo4jService.getEntity("primaryKey", "DOID:9281");
*/

        Neo4jRepository<DOTerm> neo4jService = new Neo4jRepository<>(DOTerm.class);

        DiseaseTranslator translator = new DiseaseTranslator();
        //Feature feature = featureRepository.getFeature("ZFIN:ZDB-ALT-980203-985");

        DOTerm diseaseTerm = diseaseRepository.getDiseaseTerm("DOID:0050700");
        DiseaseDocument doc = translator.translate(diseaseTerm, 1);
        List<DOTerm> doList = new ArrayList<>(Collections.singletonList(diseaseTerm));
        Iterable<DiseaseAnnotationDocument> annotDoc = translator.translateAnnotationEntities(doList, 1);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
        System.out.println(json);

        //List<DOTerm> geneDiseaseList = diseaseRepository.getAllDiseaseTerms(0, 10);
/*
        List<DOTerm> geneDiseaseList = diseaseRepository.getAllTerms();
        System.out.println("Number of all terms: " + geneDiseaseList.size());
*/
        //List<DOTerm>  geneDiseaseList1 = diseaseRepository.getDiseaseTermsWithAnnotations();

        //System.out.println("Number of Diseases with Genes: " + geneDiseaseList1.size());


        String cypher = "match (n:Gene)-[*]->(d:DOTerm) return n, d";
        //geneDiseaseList = (List<DOTerm>) neo4jService.query(cypher);



        //List<DOTerm> fullTerms = geneDiseaseList1.stream()
        //        .filter(doTerm -> !(doTerm.getPrimaryKey().contains("!")))
        //        .collect(Collectors.toList());
        //System.out.println("Number of Diseases with Genes Info: " + geneDiseaseList1.size());

    }

}
