package org.alliancegenome.api.tests.unit;

import java.io.IOException;

import org.alliancegenome.api.controller.GeneController;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.neo4j.entity.node.Allele;

public class TestES {

	public static void main(String[] args) throws IOException {
		GeneController ctrl = new GeneController();
		JsonResultResponse<Allele> res = ctrl.getAllelesPerGene("HGNC:6190", 10,1, null,"true","",
				"","","intron_variant","","","allele|allele with multiple associated variants|allele with one associated variant|variant");
		System.out.println("DONE!!"+ "RESULTS SIZE:"+ res.getResults().size());
		EsClientFactory.getDefaultEsClient().close();

	}

}