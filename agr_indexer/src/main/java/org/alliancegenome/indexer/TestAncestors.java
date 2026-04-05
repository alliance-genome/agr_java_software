package org.alliancegenome.indexer;

import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.GeneDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.GeneSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.ontology.OntologyTermClosure;
import org.alliancegenome.curation_api.model.entities.ontology.SOTerm;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;

import si.mazi.rescu.RestProxyFactory;

public class TestAncestors {

	public static void main(String[] args) throws Exception {
		long geneId = args.length > 0 ? Long.parseLong(args[0]) : 28821279L;

		GeneDocumentInterface geneApi = RestProxyFactory.createProxy(GeneDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

		System.out.println("=== API URL: " + ConfigHelper.getCurationApiUrl() + " ===");
		System.out.println("=== Gene ID: " + geneId + " ===\n");

		SearchResponse<GeneSummaryDocument> response = geneApi.findByIds(List.of(geneId));

		System.out.println("=== GeneSummaryDocument from API ===");
		for (GeneSummaryDocument doc : response.getResults()) {
			Gene gene = doc.getGene();
			if (gene == null) {
				System.out.println("Gene is null");
				continue;
			}
			System.out.println("Gene symbol: " + (gene.getGeneSymbol() != null ? gene.getGeneSymbol().getDisplayText() : "null"));

			SOTerm geneType = gene.getGeneType();
			if (geneType == null) {
				System.out.println("geneType is null");
				continue;
			}
			System.out.println("geneType curie: " + geneType.getCurie());
			System.out.println("geneType name: " + geneType.getName());

			if (geneType.getAncestors() == null) {
				System.out.println("ancestors: null");
			} else {
				System.out.println("ancestors count: " + geneType.getAncestors().size());
				for (OntologyTermClosure closure : geneType.getAncestors()) {
					System.out.println("  " + closure.getClosureObject().getCurie() + " " + closure.getClosureTypes());
				}
			}
		}
		System.out.println("\n=== Done ===");
	}

}
