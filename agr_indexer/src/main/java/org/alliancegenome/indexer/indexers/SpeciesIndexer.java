package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.core.document.SpeciesSummaryDocument;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.curation_api.interfaces.crud.SpeciesCrudInterface;
import org.alliancegenome.curation_api.model.entities.Species;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class SpeciesIndexer extends Indexer {

	private static final int FETCH_LIMIT = 1000;

	private final SpeciesCrudInterface speciesApi = RestProxyFactory.createProxy(
		SpeciesCrudInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public SpeciesIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index(ProcessDisplayHelper display) {
		try {
			SearchResponse<Species> response = speciesApi.find(0, FETCH_LIMIT, new HashMap<>());
			List<Species> all = response != null && response.getResults() != null
				? response.getResults()
				: new ArrayList<>();
			if (all.size() >= FETCH_LIMIT) {
				log.warn("Species fetch returned {} rows — at FETCH_LIMIT cap, results may be truncated", all.size());
			}
			List<SpeciesSummaryDocument> docs = all.stream()
				.filter(s -> s != null
					&& !Boolean.TRUE.equals(s.getInternal())
					&& !Boolean.TRUE.equals(s.getObsolete()))
				.map(SpeciesIndexer::toDocument)
				.collect(Collectors.toList());
			log.info("Indexing {} species_summary documents", docs.size());
			indexDocuments(docs);
		} catch (Exception e) {
			log.error("Error while indexing species", e);
			ExceptionCatcher.report(e);
			System.exit(-1);
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
	}

	private static SpeciesSummaryDocument toDocument(Species s) {
		SpeciesSummaryDocument d = new SpeciesSummaryDocument();
		d.setName(s.getFullName());
		d.setDisplayName(s.getDisplayName());
		d.setAbbreviation(s.getAbbreviation());
		d.setPhylogeneticOrder(s.getPhylogeneticOrder());
		if (s.getTaxon() != null) {
			String curie = s.getTaxon().getCurie();
			d.setTaxonID(curie);
			d.setTaxonIDPart(taxonIDPart(curie));
		}
		return d;
	}

	private static String taxonIDPart(String curie) {
		if (curie == null) {
			return null;
		}
		int idx = curie.lastIndexOf(':');
		return idx >= 0 ? curie.substring(idx + 1) : curie;
	}
}
