package org.alliancegenome.indexer;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.entity.node.DOTerm;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.repository.Neo4jRepository;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.List;
import java.util.stream.Collectors;

public class DiseaseTest {



    public static void main(String[] args) {
        Configurator.setRootLevel(Level.WARN);
        //Configurator.setLevel("org.neo4j",Level.DEBUG);
        Logger log = LogManager.getLogger(DiseaseTest.class);
        log.info("Hallo");
        ConfigHelper.init();

        DiseaseRepository diseaseRepository = new DiseaseRepository();
/*
        Iterable<DOTerm> disease_entities = neo4jService.getPage(0, 1000, 3);

        disease_entities.forEach(entity -> {
            if (entity.getGenes() != null)
                System.out.println(entity);
        });
        Collection<DOTerm> entityt = neo4jService.getEntity("primaryKey", "DOID:9281");
*/

        Neo4jRepository<DOTerm> neo4jService = new Neo4jRepository<>(DOTerm.class);

        List<DOTerm> geneDiseaseList = diseaseRepository.getAllDiseaseTerms(0, 100);
        geneDiseaseList = diseaseRepository.getAllTerms();

        System.out.println("Number of Diseases with Genes: " + geneDiseaseList.size());


        String cypher = "match (n:Gene)-[*]->(d:DOTerm) return n, d";
        //geneDiseaseList = (List<DOTerm>) neo4jService.query(cypher);



        List<DOTerm> fullTerms = geneDiseaseList.stream()
                .filter(doTerm -> !(doTerm.getPrimaryKey().contains("!")))
                .collect(Collectors.toList());
        System.out.println("Number of Diseases with Genes Info: " + geneDiseaseList.size());

    }

}
