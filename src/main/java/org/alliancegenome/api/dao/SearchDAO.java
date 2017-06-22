package org.alliancegenome.api.dao;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

@RequestScoped
@SuppressWarnings("serial")
public class SearchDAO {

	private List<String> response_fields = new ArrayList<String>() {
		{
			add("name"); add("symbol"); add("synonyms"); add("soTermName"); add("gene_chromosomes"); add("gene_chromosome_starts"); add("gene_chromosome_ends");
			add("description"); add("external_ids"); add("species"); add("gene_biological_process"); add("gene_molecular_function"); add("gene_cellular_component");
			add("go_type"); add("go_genes"); add("go_synonyms"); add("disease_genes"); add("disease_synonyms"); add("homologs"); add("crossReferences"); add("category");
			add("href");
		}
	};
	
	public SearchResponse performQuery(String index, QueryBuilder query, int limit, int offset, HighlightBuilder highlighter, String sort) {

		try {
			PreBuiltTransportClient searchClient = new PreBuiltTransportClient(Settings.EMPTY);
			searchClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));

			SearchRequestBuilder srb = searchClient.prepareSearch();

			srb.setFetchSource(response_fields.toArray(new String[response_fields.size()]), null);

			srb.setIndices(index);
			srb.setQuery(query);
			srb.setSize(limit);
			srb.setFrom(offset);
			if(sort != null && sort.equals("alphabetical")) {
				srb.addSort("name.raw", SortOrder.ASC);
			}
			srb.highlighter(highlighter);
			srb.setPreference("p_" + query);
			System.out.println(srb);
			SearchResponse res = srb.execute().actionGet();

			searchClient.close();
			return res;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}


}
