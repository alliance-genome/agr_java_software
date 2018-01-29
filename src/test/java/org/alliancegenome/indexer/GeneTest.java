package org.alliancegenome.indexer;

import org.alliancegenome.shared.config.ConfigHelper;
import org.alliancegenome.shared.neo4j.entity.node.Gene;
import org.alliancegenome.shared.neo4j.repository.GeneRepository;
import org.alliancegenome.shared.translators.GeneTranslator;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GeneTest {

    public static void main(String[] args) throws Exception {
        ConfigHelper.init();
        
        GeneRepository repo = new GeneRepository();
        GeneTranslator trans = new GeneTranslator();
        
        Gene gene = null;
        gene = repo.getOneGene("ZFIN:ZDB-GENE-000210-7");
        gene = repo.getOneGene("WB:WBGene00015146");
        
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(trans.translate(gene));
        System.out.println(json);
    }

}
