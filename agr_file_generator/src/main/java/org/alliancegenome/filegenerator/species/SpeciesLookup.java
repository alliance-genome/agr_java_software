package org.alliancegenome.filegenerator.species;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.Species;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.filegenerator.curation.SpeciesInterface;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class SpeciesLookup {

	private final Map<String, String> taxonToMod = new LinkedHashMap<>();
	private final Map<String, String> taxonToName = new LinkedHashMap<>();
	private final Map<String, String> taxonToFullName = new LinkedHashMap<>();
	private final Map<String, Integer> taxonToOrder = new LinkedHashMap<>();
	private final Map<String, String> nameToTaxon = new LinkedHashMap<>();

	public SpeciesLookup() {
		load();
	}

	private void load() {
		SpeciesInterface api = RestProxyFactory.createProxy(SpeciesInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
		List<Species> all = api.findForPublic(0, 100, "FieldsOnly", new HashMap<>()).getResults();
		for (Species s : all) {
			if (s.getTaxon() == null || s.getTaxon().getCurie() == null) {
				continue;
			}
			String curie = s.getTaxon().getCurie();
			String mod = s.getDisplayName();
			String taxonName = s.getTaxon().getName();
			Integer order = s.getPhylogeneticOrder();
			if (mod != null) {
				taxonToMod.put(curie, mod);
			}
			if (taxonName != null) {
				taxonToName.put(curie, taxonName);
				nameToTaxon.put(taxonName, curie);
			}
			if (s.getFullName() != null) {
				taxonToFullName.put(curie, s.getFullName());
			}
			// gene_search_result emits species without strain suffix (e.g. "Saccharomyces cerevisiae"
			// vs taxon name "Saccharomyces cerevisiae S288C"). Register every name variant so the
			// reverse name -> curie lookup resolves regardless of which form the indexer used.
			registerAlias(s.getFullName(), curie);
			registerAlias(s.getDisplayName(), curie);
			registerAlias(s.getAbbreviation(), curie);
			if (s.getCommonNames() != null) {
				for (String common : s.getCommonNames()) {
					registerAlias(common, curie);
				}
			}
			if (order != null) {
				taxonToOrder.put(curie, order);
			}
		}
		log.info("Loaded {} species from curation API", taxonToMod.size());
	}

	public String modFor(String taxonCurie) {
		return taxonToMod.get(taxonCurie);
	}

	public String nameFor(String taxonCurie) {
		return taxonToName.get(taxonCurie);
	}

	public String fullNameFor(String taxonCurie) {
		String full = taxonToFullName.get(taxonCurie);
		if (full != null && !full.isEmpty()) {
			return full;
		}
		return taxonToName.get(taxonCurie);
	}

	public String taxonForName(String name) {
		return nameToTaxon.get(name);
	}

	public Integer orderFor(String taxonCurie) {
		return taxonToOrder.get(taxonCurie);
	}

	public Map<String, String> taxonToMod() {
		return taxonToMod;
	}

	public Map<String, String> taxonToName() {
		return taxonToName;
	}

	private void registerAlias(String alias, String curie) {
		if (alias == null || alias.isEmpty()) {
			return;
		}
		nameToTaxon.putIfAbsent(alias, curie);
	}
}
