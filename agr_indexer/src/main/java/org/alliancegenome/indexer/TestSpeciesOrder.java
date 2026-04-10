package org.alliancegenome.indexer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.Species;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.SpeciesInterface;

import si.mazi.rescu.RestProxyFactory;

public class TestSpeciesOrder {

	public static void main(String[] args) throws Exception {
		SpeciesInterface speciesApi = RestProxyFactory.createProxy(SpeciesInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

		System.out.println("=== API URL: " + ConfigHelper.getCurationApiUrl() + " ===\n");

		System.out.println("=== Fetching all species via findForPublic ===");
		SearchResponse<Species> response = speciesApi.findForPublic(0, 100, "FieldsOnly", new HashMap<>());
		List<Species> allSpecies = response.getResults();
		System.out.println("Species count: " + allSpecies.size() + "\n");

		// Build the lookup: taxonIdPart -> phylogeneticOrder
		Map<String, Integer> speciesOrderLookup = new HashMap<>();
		for (Species species : allSpecies) {
			String taxonCurie = species.getTaxon() != null ? species.getTaxon().getCurie() : "null";
			Integer order = species.getPhylogeneticOrder();
			String fullName = species.getFullName();
			System.out.println(fullName + " | taxon: " + taxonCurie + " | phylogeneticOrder: " + order);

			if (species.getTaxon() != null && order != null) {
				String taxonIdPart = taxonCurie.replace("NCBITaxon:", "");
				speciesOrderLookup.put(taxonIdPart, order);
			}
		}

		System.out.println("\n=== Species order lookup map ===");
		speciesOrderLookup.entrySet().stream()
			.sorted(Map.Entry.comparingByValue())
			.forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));

		// Test buildSpeciesOrder for a specific taxon (e.g. Xenopus laevis 8355)
		String testTaxon = args.length > 0 ? args[0] : "NCBITaxon:8355";
		System.out.println("\n=== buildSpeciesOrder for " + testTaxon + " ===");
		HashMap<String, Integer> speciesOrder = buildSpeciesOrder(speciesOrderLookup, testTaxon);
		speciesOrder.entrySet().stream()
			.sorted(Map.Entry.comparingByValue())
			.forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));

		System.out.println("\nDone.");
	}

	private static HashMap<String, Integer> buildSpeciesOrder(Map<String, Integer> speciesOrderLookup, String taxonCurie) {
		HashMap<String, Integer> order = new HashMap<>();
		String subjectTaxonIdPart = taxonCurie.replace("NCBITaxon:", "");
		Integer subjectOrder = speciesOrderLookup.getOrDefault(subjectTaxonIdPart, 0);
		for (String key : speciesOrderLookup.keySet()) {
			order.put(key, subjectOrder);
		}
		order.put(subjectTaxonIdPart, 0);
		return order;
	}

}
