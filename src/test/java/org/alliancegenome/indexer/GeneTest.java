package org.alliancegenome.indexer;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.entity.node.Gene;
import org.alliancegenome.indexer.repository.GeneRepository;
import org.alliancegenome.indexer.translators.GeneTranslator;

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
