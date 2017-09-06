package org.alliancegenome.indexer;

import java.util.List;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.node.Gene;
import org.alliancegenome.indexer.repository.GeneRepository;
import org.alliancegenome.indexer.translators.GeneTranslator;

public class Main2 {

	public static void main(String[] args) {
		ConfigHelper.init();
		
		GeneTranslator geneTrans = new GeneTranslator();
		GeneRepository repo = new GeneRepository();
		//Iterable<Gene> genes = repo.getOneGene("MGI:1343498");
		
		List<String> ids = repo.getAllGeneIds();
		
		for(String id: ids) {
			Iterable<Gene> genes = repo.getOneGene(id);
			Gene g = genes.iterator().next();
			GeneDocument gd = geneTrans.translate(g);
		}
		
		
		
//		
//        try {
//        	ObjectMapper objectMapper = new ObjectMapper();
//            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(gd);
//            System.out.print(jsonString);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }

	}

}
