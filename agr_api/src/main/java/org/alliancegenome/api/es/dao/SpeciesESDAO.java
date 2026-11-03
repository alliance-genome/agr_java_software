package org.alliancegenome.api.es.dao;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.document.SpeciesSummaryDocument;
import org.alliancegenome.core.es.util.EsClientFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class SpeciesESDAO extends ESDAO {

	private static final String SITE_INDEX = ConfigHelper.getEsIndex();
	private static final String CATEGORY = "species_summary";
	private static final int FETCH_SIZE = 1000;

	private static volatile List<SpeciesSummaryDocument> allSpecies;
	private static volatile Map<String, SpeciesSummaryDocument> byTaxonID;
	private static volatile Map<String, SpeciesSummaryDocument> byName;

	@Inject
	ObjectMapper mapper;

	public List<SpeciesSummaryDocument> getAll() {
		ensureLoaded();
		return allSpecies;
	}

	public SpeciesSummaryDocument byTaxonID(String taxonID) {
		ensureLoaded();
		if (taxonID == null || byTaxonID == null) {
			return null;
		}
		return byTaxonID.get(taxonID);
	}

	public SpeciesSummaryDocument byScientificName(String scientificName) {
		ensureLoaded();
		if (scientificName == null || byName == null) {
			return null;
		}
		return byName.get(scientificName);
	}

	public String getAllTaxonIDs() {
		ensureLoaded();
		if (allSpecies == null) {
			return "";
		}
		return allSpecies.stream()
			.map(SpeciesSummaryDocument::getTaxonID)
			.collect(Collectors.joining(","));
	}

	private void ensureLoaded() {
		if (allSpecies != null) {
			return;
		}
		synchronized (SpeciesESDAO.class) {
			if (allSpecies != null) {
				return;
			}
			List<SpeciesSummaryDocument> fetched = fetchAll();
			if (fetched.isEmpty()) {
				log.warn("No {} documents found in {}; will retry on next call", CATEGORY, SITE_INDEX);
				return;
			}
			Map<String, SpeciesSummaryDocument> tx = new LinkedHashMap<>();
			Map<String, SpeciesSummaryDocument> nm = new LinkedHashMap<>();
			for (SpeciesSummaryDocument d : fetched) {
				if (d.getTaxonID() != null) {
					tx.put(d.getTaxonID(), d);
				}
				if (d.getName() != null) {
					nm.put(d.getName(), d);
				}
			}
			byTaxonID = Collections.unmodifiableMap(tx);
			byName = Collections.unmodifiableMap(nm);
			allSpecies = Collections.unmodifiableList(fetched);
		}
	}

	private List<SpeciesSummaryDocument> fetchAll() {
		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("category", CATEGORY));

		SearchSourceBuilder ssb = new SearchSourceBuilder();
		ssb.query(bool);
		ssb.size(FETCH_SIZE);
		ssb.trackTotalHits(true);

		SearchRequest searchRequest = new SearchRequest(SITE_INDEX);
		searchRequest.source(ssb);

		List<SpeciesSummaryDocument> result = new ArrayList<>();
		try {
			SearchResponse response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
			for (SearchHit hit : response.getHits().getHits()) {
				try {
					result.add(mapper.readValue(hit.getSourceAsString(), SpeciesSummaryDocument.class));
				} catch (Exception parseEx) {
					log.error("Failed to parse {} hit", CATEGORY, parseEx);
				}
			}
			if (result.size() >= FETCH_SIZE) {
				log.warn("{} fetch returned {} docs — at FETCH_SIZE cap, results may be truncated", CATEGORY, result.size());
			}
			result.sort(Comparator.comparing(
				SpeciesSummaryDocument::getPhylogeneticOrder,
				Comparator.nullsLast(Comparator.naturalOrder())));
		} catch (IOException e) {
			log.error("Failed to load {} documents from {}", CATEGORY, SITE_INDEX, e);
		}
		return result;
	}
}
