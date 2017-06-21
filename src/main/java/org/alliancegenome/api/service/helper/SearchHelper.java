package org.alliancegenome.api.service.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.elasticsearch.index.search.MatchQuery;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

@RequestScoped
@SuppressWarnings("serial")
public class SearchHelper {

	private HashMap<String, List<String>> category_filters = new HashMap<String, List<String>>() {
		{
			put("gene", new ArrayList<String>() {
				{
					add("soTermName");
					add("gene_biological_process");
					add("gene_molecular_function");
					add("gene_cellular_component");
					add("species");
				}
			});
			put("go", new ArrayList<String>() {
				{
					add("go_type");
					add("go_species");
					add("go_genes");
				}
			});
			put("disease", new ArrayList<String>() {
				{
					add("disease_species");
					add("disease_genes");
				}
			});
		}
	};

	private HashMap<String, Integer> custom_boosts = new HashMap<String, Integer>() {
		{
			put("primaryId", 400);
			put("secondaryIds", 100);
			put("symbol", 500);
			put("symbol.raw", 1000);
			put("synonyms", 120);
			put("synonyms.raw", 200);
			put("name", 100);
			put("name.symbol", 200);
			put("gene_biological_process.symbol", 50);
			put("gene_molecular_function.symbol", 50);
			put("gene_cellular_component.symbol", 50);
			put("diseases.do_name", 50);
		}
	};

	private List<String> search_fields = new ArrayList<String>() {
		{
			add("primaryId"); add("secondaryIds"); add("name"); add("symbol"); add("symbol.raw"); add("synonyms"); add("synonyms.raw");
			add("description"); add("external_ids"); add("species"); add("gene_biological_process"); add("gene_molecular_function");
			add("gene_cellular_component"); add("go_type"); add("go_genes"); add("go_synonyms"); add("disease_genes"); add("disease_synonyms");
			add("diseases.do_name");
		}
	};
	
	private List<String> special_search_fields = new ArrayList<String>() {
		{
			add("name.symbol");
			add("gene_biological_process.symbol");
			add("gene_molecular_function.symbol");
			add("gene_cellular_component.symbol");
		}
	};

	private List<String> highlight_blacklist_fields = new ArrayList<String>() {
		{
			add("go_genes");
		}
	};

	
	


	public QueryBuilder buildQuery(String q, String category, UriInfo uriInfo) {

		QueryBuilder query = QueryBuilders.matchAllQuery();
		
		if((q == null || q.equals("") || q.length() == 0) && (category == null || category.equals("") || category.length() == 0)) {
			RandomScoreFunctionBuilder rsfb = new RandomScoreFunctionBuilder();
			rsfb.seed(12345);
			return new FunctionScoreQueryBuilder(query, rsfb);
		}
		
		
		if(q == null || q.equals("") || q.length() == 0) {
			query = QueryBuilders.matchAllQuery();
		} else {
			query = buildSearchParams(q);
		}

		if(category == null || category.equals("") || category.length() == 0) {
			return query;
		} else {
			return buildCategoryQuery(query, category, uriInfo);
		}
	}
	

	public QueryBuilder buildSearchParams(String q) {

		q = q.replaceAll("\"", "");

		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		DisMaxQueryBuilder dis_max = new DisMaxQueryBuilder();
		bool.must(dis_max);
		ExistsQueryBuilder categoryExists = new ExistsQueryBuilder("category");
		bool.must(categoryExists);

		ArrayList<String> final_search_fields = new ArrayList<String>();
		final_search_fields.addAll(search_fields);
		final_search_fields.addAll(special_search_fields);
		
		for(String field: final_search_fields) {
			MatchQueryBuilder boostedMatchQuery = new MatchQueryBuilder(field, q);
			if(custom_boosts.containsKey(field)) {
				boostedMatchQuery.boost(custom_boosts.get(field));
			} else {
				boostedMatchQuery.boost(50);
			}
			dis_max.add(boostedMatchQuery);

			if(field.contains(".")) {
				dis_max.add(new MatchPhrasePrefixQueryBuilder(field.split("\\.")[0], q));
			} else {
				dis_max.add(new MatchPhrasePrefixQueryBuilder(field, q));
			}

		}

		return bool;
	}
	
	public QueryBuilder buildCategoryQuery(QueryBuilder query, String category, UriInfo uriInfo) {
		
		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		bool.must(query);
		TermQueryBuilder termQueryBuilder = new TermQueryBuilder("category", category);
		bool.must(termQueryBuilder);

//		for(Entry<String, List<String>> e: uriInfo.getQueryParameters().entrySet()) {
//			System.out.println(e.getKey());
//			System.out.println(e.getValue());
//			System.out.println();
//		}

		if(category_filters.containsKey(category)) {
			for(String item: category_filters.get(category)) {
				if(uriInfo.getQueryParameters().containsKey(item)) {
					for(String param: uriInfo.getQueryParameters().get(item)) {
						TermQueryBuilder termQuery = new TermQueryBuilder(item + ".raw", param);
						bool.must(termQuery);
					}
				}
			}
		}

		return bool;
	}


	public HighlightBuilder buildHighlights() {

		HighlightBuilder hlb = new HighlightBuilder();
		
		for(String field: search_fields) {
			if(!highlight_blacklist_fields.contains(field)) {
				hlb.field(field);
			}
		}

		return hlb;
	}

}
