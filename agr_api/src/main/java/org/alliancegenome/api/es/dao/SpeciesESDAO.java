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
	private static final int FETCH_SIZE = 100;

	private static volatile List<SpeciesSummaryDocument> all;
	private static volatile Map<String, SpeciesSummaryDocument> byTaxonID;
	private static volatile Map<String, SpeciesSummaryDocument> byName;

	@Inject
	ObjectMapper mapper;

	public List<SpeciesSummaryDocument> getAll() {
		ensureLoaded();
		return all;
	}

	public SpeciesSummaryDocument byTaxonID(String taxonID) {
		ensureLoaded();
		return taxonID == null ? null : byTaxonID.get(taxonID);
	}

	public SpeciesSummaryDocument byScientificName(String scientificName) {
		ensureLoaded();
		return scientificName == null ? null : byName.get(scientificName);
	}

	public String getAllTaxonIDs() {
		ensureLoaded();
		return all.stream()
			.map(SpeciesSummaryDocument::getTaxonID)
			.collect(Collectors.joining(","));
	}

	private void ensureLoaded() {
		if (all != null) {
			return;
		}
		synchronized (SpeciesESDAO.class) {
			if (all != null) {
				return;
			}
			List<SpeciesSummaryDocument> fetched = fetchAll();
			Map<String, SpeciesSummaryDocument> tx = new LinkedHashMap<>();
			Map<String, SpeciesSummaryDocument> nm = new LinkedHashMap<>();
			for (SpeciesSummaryDocument d : fetched) {
				if (d.getTaxonID() != null) tx.put(d.getTaxonID(), d);
				if (d.getName() != null) nm.put(d.getName(), d);
			}
			all = Collections.unmodifiableList(fetched);
			byTaxonID = Collections.unmodifiableMap(tx);
			byName = Collections.unmodifiableMap(nm);
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
					log.error("Failed to parse species_summary hit", parseEx);
				}
			}
			result.sort(Comparator.comparing(
				SpeciesSummaryDocument::getPhylogeneticOrder,
				Comparator.nullsLast(Comparator.naturalOrder())));
		} catch (IOException e) {
			log.error("Failed to load species_summary documents from " + SITE_INDEX, e);
		}
		return result;
	}
}
