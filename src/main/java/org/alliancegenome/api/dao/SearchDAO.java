package org.alliancegenome.api.dao;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

@ApplicationScoped
@SuppressWarnings("serial")
public class SearchDAO extends ESDAO {

	//private Logger log = Logger.getLogger(getClass());

	private List<String> response_fields = new ArrayList<String>() {
		{
			add("name"); add("symbol"); add("synonyms"); add("soTermName"); add("gene_chromosomes"); add("gene_chromosome_starts"); add("gene_chromosome_ends");
			add("description"); add("external_ids"); add("species"); add("gene_biological_process"); add("gene_molecular_function"); add("gene_cellular_component");
			add("go_type"); add("go_genes"); add("go_synonyms"); add("disease_genes"); add("disease_synonyms"); add("homologs"); add("crossReferences"); add("category");
			add("href");
		}
	};

	public SearchResponse[] performQuery(QueryBuilder query, List<AggregationBuilder> aggBuilders, int limit, int offset, HighlightBuilder highlighter, String sort) {

		SearchResponse[] ret = new SearchResponse[2];

		SearchRequestBuilder srb1 = searchClient.prepareSearch();

		srb1.setFetchSource(response_fields.toArray(new String[response_fields.size()]), null);

		srb1.setIndices(config.getEsIndex());
		srb1.setQuery(query);
		srb1.setSize(limit);
		srb1.setFrom(offset);
		if(sort != null && sort.equals("alphabetical")) {
			srb1.addSort("name.raw", SortOrder.ASC);
		}
		srb1.highlighter(highlighter);
		srb1.setPreference("p_" + query);
		//log.info(srb1);
		ret[0] = srb1.execute().actionGet();

		SearchRequestBuilder srb2 = searchClient.prepareSearch();


		srb2.setQuery(query);
		srb2.setSize(0);
		for(AggregationBuilder aggBuilder: aggBuilders) {
			srb2.addAggregation(aggBuilder);
		}
		//log.info(srb2);
		ret[1] = srb2.execute().actionGet();

		//log.info(ret[1]);
		return ret;

	}

}
