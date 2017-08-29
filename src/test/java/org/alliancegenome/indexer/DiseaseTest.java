package org.alliancegenome.indexer;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.entity.DOTerm;
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

/*
        Iterable<DOTerm> disease_entities = neo4jService.getPage(0, 1000, 3);

        disease_entities.forEach(entity -> {
            if (entity.getGenes() != null)
                System.out.println(entity);
        });
        Collection<DOTerm> entityt = neo4jService.getEntity("primaryKey", "DOID:9281");
*/

        Neo4jRepository<DOTerm> neo4jService = new Neo4jRepository<>(DOTerm.class);
        String cypher = "match (n:DOTerm), " +
                "(a:Association)-[q:ASSOCIATION]->(n), " +
                "(m:Gene)-[qq:ASSOCIATION]->(a), " +
                "(p:Publication)<-[qqq*]-(a), " +
                "(e:EvidenceCode)<-[ee:EVIDENCE]-(a), " +
                "(n)-[ex:ALSO_KNOWN_AS]->(exx:ExternalId)" +
                "return n, q,a,qq,m,qqq,p, ee, e, ex, exx";
        List<DOTerm> geneDiseaseList = (List<DOTerm>) neo4jService.query(cypher);

        System.out.println("Number of Diseases with Genes: " + geneDiseaseList.size());

        cypher = "match (n:DOTerm)<-[q:IS_A]-(m:DOTerm)<-[r:IS_IMPLICATED_IN]-(g:Gene)," +
                "(m)-[qq:IS_A]->(o:DOTerm), " +
                "(m)-[ss:ALSO_KNOWN_AS]->(s:Synonym)  " +
                "return m,q, n, qq, o, ss, s";
        List<DOTerm> geneDiseaseInfoList = (List<DOTerm>) neo4jService.query(cypher);

        List<DOTerm> fullTerms = geneDiseaseList.stream()
                .filter(doTerm -> !(doTerm.getPrimaryKey().contains("!")))
                .collect(Collectors.toList());
        System.out.println("Number of Diseases with Genes Info: " + geneDiseaseList.size());

    }

}
