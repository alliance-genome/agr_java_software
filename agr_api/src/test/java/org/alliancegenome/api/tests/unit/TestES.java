package org.alliancegenome.api.tests.unit;

import java.io.IOException;

import org.alliancegenome.api.controller.GeneController;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.es.util.EsClientFactory;

public class TestES {

	private TestES() { }
	
	public static void main(String[] args) throws IOException {
		GeneController ctrl = new GeneController();
		JsonResultResponse<AlleleSummaryDocument> res = ctrl.getAllelesPerGene("HGNC:6190", 10, 1, null, "true", "", "", "", "", "intron_variant", "", "", "allele|allele with multiple associated variants|allele with one associated variant|variant");
		System.out.println("DONE!!" + "RESULTS SIZE:" + res.getResults().size());
		EsClientFactory.getDefaultEsClient().close();

	}

}