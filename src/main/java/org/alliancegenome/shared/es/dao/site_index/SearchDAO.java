package org.alliancegenome.shared.es.dao.site_index;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.alliancegenome.shared.es.dao.ESDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequest;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

public class SearchDAO extends ESDAO {

	private Log log = LogFactory.getLog(getClass());

	public void init() {
		super.init();
		checkIndex(config.getEsIndex());
	}
	
	private final List<String> response_fields = new ArrayList<String>() {
		{
			add("name"); add("symbol"); add("synonyms"); add("soTermName"); add("gene_chromosomes"); add("gene_chromosome_starts"); add("gene_chromosome_ends");
			add("description"); add("definition"); add("external_ids"); add("species"); add("gene_biological_process"); add("gene_molecular_function"); add("gene_cellular_component");
			add("go_type"); add("go_genes"); add("go_synonyms"); add("disease_genes"); add("disease_synonyms"); add("homologs"); add("crossReferences"); add("category");
			add("href"); add("name_key");
		}
	};

	public SearchResponse performQuery(QueryBuilder query,
			List<AggregationBuilder> aggBuilders,
			int limit, int offset,
			HighlightBuilder highlighter,
			String sort, Boolean debug) {

		SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();

		searchRequestBuilder.setFetchSource(response_fields.toArray(new String[response_fields.size()]), null);

		if (debug) {
			searchRequestBuilder.setExplain(true);
		}

		searchRequestBuilder.setIndices(config.getEsIndex());
		searchRequestBuilder.setQuery(query);
		searchRequestBuilder.setSize(limit);
		searchRequestBuilder.setFrom(offset);

		if(sort != null && sort.equals("alphabetical")) {
			searchRequestBuilder.addSort("name.keyword", SortOrder.ASC);
		}
		searchRequestBuilder.highlighter(highlighter);
		searchRequestBuilder.setPreference("p_" + query);

		for(AggregationBuilder aggBuilder: aggBuilders) {
			searchRequestBuilder.addAggregation(aggBuilder);
		}

		log.debug(searchRequestBuilder);

		return	searchRequestBuilder.execute().actionGet();

	}


	public List<String> analyze(String query) {
		AnalyzeRequest request = (new AnalyzeRequest()).analyzer("standard").text(query);
		List<AnalyzeResponse.AnalyzeToken> tokens = searchClient.admin().indices().analyze(request).actionGet().getTokens();
		return tokens.stream().map(token -> token.getTerm()).collect(Collectors.toList());
	}


	public List<SearchHit> getAllIds(QueryBuilder query, Integer size) {

		ArrayList<SearchHit> hits = new ArrayList<SearchHit>();
		SearchResponse scrollResp = searchClient.prepareSearch()
				.setScroll(new TimeValue(60000))
				.setQuery(query)
				.setIndices(config.getEsIndex())
				.setSize(size)
				.execute().actionGet();

		do {
			for (SearchHit hit : scrollResp.getHits().getHits()) {
				hits.add(hit);
			}
			scrollResp = searchClient.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
		} while(scrollResp.getHits().getHits().length != 0);
		return hits;
	}
}
