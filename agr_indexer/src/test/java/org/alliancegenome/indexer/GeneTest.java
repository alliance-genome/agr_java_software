package org.alliancegenome.indexer;

import java.util.Date;

import org.alliancegenome.core.translators.document.GeneTranslator;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GeneTest {

    public static void main(String[] args) throws Exception {

        GeneRepository repo = new GeneRepository();
        GeneTranslator trans = new GeneTranslator();
        
        Gene gene = null;
        gene = repo.getOneGene("ZFIN:ZDB-GENE-000210-7");
        gene = repo.getOneGene("WB:WBGene00015146");

        //for(GOTerm go: gene.getGoParentTerms()) {
        //  System.out.println(go.getName());
        //}
        
        Date start = new Date();
        gene = repo.getOneGene("MGI:97490");
        Date end = new Date();
        
        System.out.println("Time: " + (end.getTime() - start.getTime()));
        
        //ObjectMapper mapper = new ObjectMapper();
        //String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(trans.translate(gene));
        //System.out.println(json);
    }

}
