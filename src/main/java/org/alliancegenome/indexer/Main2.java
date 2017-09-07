package org.alliancegenome.indexer;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.node.Gene;
import org.alliancegenome.indexer.repository.GeneRepository;
import org.alliancegenome.indexer.translators.GeneTranslator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main2 {

	public static void main(String[] args) {
		ConfigHelper.init();
		
		GeneTranslator geneTrans = new GeneTranslator();
		GeneRepository repo = new GeneRepository();
		//Iterable<Gene> genes = repo.getOneGene("MGI:1343498");
		
		//List<String> keys = repo.getAllGeneKeys();
		
		//for(String key: keys) {
		//String key = "MGI:2178429";
		String key = "MGI:95294";
			System.out.println("Key: " + key);
			Iterable<Gene> genes = repo.getOneGene(key);
			if(genes.iterator().hasNext()) {
				Gene g = genes.iterator().next();
				//if(g.getOrthoGenes() != null) {
				//	System.out.println(g.getOrthologyGeneJoins().size());
				//	System.out.println(g.getOrthologyGeneJoins().get(0).getMatched().get(0).getName());
					
					//List<OrthologyGeneJoin> joins = (List<OrthologyGeneJoin>)orepo.getOrthology(g.getPrimaryKey());
					//System.out.println(joins.size());
					//System.out.println(joins.get(0).getNotMatched().size());
					
					//for(OrthologyGeneJoin ogj: g.getOrthologyGeneJoins()) {
						//System.out.println(ogj);
						//List<OrthologyGeneJoin> joins = (List<OrthologyGeneJoin>)orepo.getOrthology(ogj.getPrimaryKey());
						//System.out.println(joins.size());
						//System.out.println(ogj.getMatched() + " " + ogj.getNotCalled() + " " + ogj.getNotMatched());
						//for(OrthologyGeneJoin j: joins) {
						//	System.out.println(j.getPrimaryKey());
						//}
					//}
				//}
				
				GeneDocument gd = geneTrans.translate(g);
		        try {
		        	ObjectMapper objectMapper = new ObjectMapper();
		            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(gd);
		            System.out.print(jsonString);
		        } catch (JsonProcessingException e) {
		            e.printStackTrace();
		        }
			}
			
			
		//}
		
		
		
		


	}

}
