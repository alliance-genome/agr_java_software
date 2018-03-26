package org.alliancegenome.shared;

import java.util.HashMap;

import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

public class TestGetGene {

	public static void main(String[] args) {
		GeneRepository repo = new GeneRepository();
		
		//"MGI:97490" OR g.primaryKey = "RGD:3258"
		
		System.out.println("MGI:97490");
		HashMap<String, Gene> geneMap = repo.getGene("RGD:3258");
		System.out.println(geneMap);

	}

}
