package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.TransgenicAlleleDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.es.model.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AlleleESService extends ESService {

	public JsonResultResponse<TransgenicAlleleDocument> getTransgenicAlleles(String alleleId) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);
		// ToDo: Change this class such that the category is public
		// TransgenicAlleleDocument.category
		bool.filter(new TermQueryBuilder("category", "transgenic_allele_summary"));
		bool2.should(new MatchQueryBuilder("allele.primaryExternalId.keyword", alleleId));

		JsonResultResponse<TransgenicAlleleDocument> ret = new JsonResultResponse<>();

		SearchResponse searchResponse = getSearchResponse(bool, new Pagination(), null, false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		List<TransgenicAlleleDocument> list = new ArrayList<>();
		Arrays.stream(searchResponse.getHits().getHits()).forEach(searchHit -> {
			try {
				TransgenicAlleleDocument object = mapper.readValue(searchHit.getSourceAsString(), TransgenicAlleleDocument.class);
				list.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		ret.setResults(list);
		return ret;
	}

	public AlleleSummaryDocument getById(String alleleId) {

		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("allele.primaryExternalId", alleleId));
		bool.filter(new TermQueryBuilder("category", "allele_summary"));
		Pagination pagination = new Pagination();
		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);
		try {
			if (searchResponse.getHits().getTotalHits().value >= 1) {
				return mapper.readValue(searchResponse.getHits().getHits()[0].getSourceAsString(), AlleleSummaryDocument.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	public JsonResultResponse<VariantSummaryDocument> getVariantSummary(String alleleId, Pagination pagination) {

		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("allele.primaryExternalId", alleleId));
		bool.filter(new TermQueryBuilder("category", "variant_summary"));
		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);
		List<VariantSummaryDocument> list = new ArrayList<>();
		for (SearchHit hit : searchResponse.getHits().getHits()) {
			try {
				VariantSummaryDocument object = mapper.readValue(hit.getSourceAsString(), VariantSummaryDocument.class);
				list.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		JsonResultResponse<VariantSummaryDocument> ret = new JsonResultResponse<>();
		ret.setResults(list);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		return ret;
	}

	public JsonResultResponse<AlleleSummaryDocument> getAllelesByGene(String geneId, Pagination pagination) {

		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("alleleOfGene.primaryExternalId", geneId));
		bool.filter(new TermQueryBuilder("category", "allele_summary"));
		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);
		List<AlleleSummaryDocument> list = new ArrayList<>();
		Arrays.stream(searchResponse.getHits().getHits()).forEach(searchHit -> {
			try {
				AlleleSummaryDocument object = mapper.readValue(searchHit.getSourceAsString(), AlleleSummaryDocument.class);
				list.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		JsonResultResponse<AlleleSummaryDocument> ret = new JsonResultResponse<>();
		ret.setResults(list);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		return ret;
	}
}
