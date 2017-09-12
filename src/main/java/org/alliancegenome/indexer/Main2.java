package org.alliancegenome.indexer;

import java.util.ArrayList;
import java.util.List;

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



		//List<String> list = new ArrayList<String>();
		//list.add("MGI:2178429");
		//list.add("MGI:96969");
		//list.add("MGI:95294");
		//list.add("WB:WBGene00001650");  
		//list.add("ZFIN:ZDB-GENE-000210-7");

		// MGI:5594564 No Disease or Ortho
		// MGI:96217
		
		// Mapping Exception:
		// MGI:4439048
		// MGI:1915146
		// MGI:98327
		// MGI:2384300
		// MGI:97960
		
		
		Gene gene = repo.getOneGene("MGI:5594564");

	

		if(gene != null) {

			GeneDocument gd = geneTrans.translate(gene);
			try {
				ObjectMapper objectMapper = new ObjectMapper();
				String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(gd);
				System.out.print(jsonString);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}






	}

}
