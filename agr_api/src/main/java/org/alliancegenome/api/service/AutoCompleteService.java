package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.service.helper.SearchHelper;
import org.alliancegenome.es.index.site.dao.AutoCompleteDAO;
import org.alliancegenome.es.model.search.AutoCompleteResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@RequestScoped
public class AutoCompleteService {

    private static AutoCompleteDAO autoCompleteDAO = new AutoCompleteDAO();
    private SearchHelper searchHelper = new SearchHelper();

    @Inject
    private SearchService searchService;

    public AutoCompleteResult query(String q, String category) {
        QueryBuilder query = buildQuery(q, category);
        SearchResponse res = autoCompleteDAO.performQuery(query);
        AutoCompleteResult result = new AutoCompleteResult();
        result.results = formatResults(res);
        return result;
    }

    public BoolQueryBuilder buildBasicQuery(String q, String category) {

        BoolQueryBuilder bool = new BoolQueryBuilder();

        MultiMatchQueryBuilder multi = QueryBuilders.multiMatchQuery(q);
        multi.field("symbol",5.0F);
        multi.field("name_key.autocomplete",3.0F);
        multi.field("name.keyword", 2.0F);
        multi.field("name.autocomplete");
        multi.field("synonyms.keyword", 2.0F);
        multi.field("synonyms.autocomplete");

        bool.must(multi);

        if (StringUtils.isNotEmpty(category)) {
            bool.filter(termQuery("category", category));
        }

        //include only searchable categories in search results
        bool.filter(searchHelper.limitCategories());

        return bool;
    }


    public QueryBuilder buildQuery(String q, String category) {
        BoolQueryBuilder basicQuery = buildBasicQuery(q, category);
        FunctionScoreQueryBuilder.FilterFunctionBuilder[] boostFunctions = searchService.buildBoostFunctions(q);

        FunctionScoreQueryBuilder builder = new FunctionScoreQueryBuilder(basicQuery,boostFunctions);

        return builder;
    }


    public ArrayList<Map<String, Object>> formatResults(SearchResponse res) {
        ArrayList<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();

        for(SearchHit hit: res.getHits()) {
            String category = (String) hit.getSourceAsMap().get("category");

            //this comes over from the Python code, use symbol for genes,
            //seems like maybe it could also use name_key for everyone...
            if (StringUtils.equals(category,"gene")) {
                hit.getSourceAsMap().put("name", hit.getSourceAsMap().get("symbol"));
            }
            ret.add(hit.getSourceAsMap());
        }
        return ret;
    }

}
