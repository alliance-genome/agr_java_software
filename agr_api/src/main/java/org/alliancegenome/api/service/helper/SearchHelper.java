package org.alliancegenome.api.service.helper;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.alliancegenome.es.model.search.AggDocCount;
import org.alliancegenome.es.model.search.AggResult;
import org.alliancegenome.es.model.search.Category;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.jboss.logging.Logger;

@SuppressWarnings("serial")
public class SearchHelper {

    private Logger log = Logger.getLogger(getClass());

    private static String[] SUFFIX_LIST = { ".htmlSmoosh", ".keywordAutocomplete", ".keyword", ".smoosh",
                                            ".synonyms", ".symbols", ".text" };

    
    private HashMap<String, List<String>> category_filters = new HashMap<String, List<String>>() {
        {
            put("gene", new ArrayList<String>() {
                {
                    add("species");
                    add("soTermName");
                    add("diseasesViaExperiment.name");
                    add("biologicalProcessAgrSlim");
                    add("molecularFunctionAgrSlim");
                    add("cellularComponentAgrSlim");
                    add("anatomicalExpression");
                    add("cellularComponentExpressionAgrSlim");
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
                    add("disease_group");
                    add("annotations.geneDocument.name_key");
                    add("annotations.geneDocument.species");
                }
            });
            put("allele", new ArrayList<String>() {
                {
                    add("geneDocument.species");
                    add("diseaseDocuments.name");
                    add("geneDocument.name_key");
                }
            });
        }
    };

    public Map<String, Float> getBoostMap() { return boostMap; }
    private Map<String, Float> boostMap = new HashMap<String, Float>() {
        {
            put("symbol",5.0F);
            put("symbol.autocomplete",2.0F);
            put("name.autocomplete",0.1F);
            put("synonyms.keyword",2.0F);
        }
    };

    public List<String> getSearchFields() { return searchFields; }
    private List<String> searchFields = new ArrayList<String>() {
        {
            add("primaryId");
            add("primaryKey");
            add("id");
            add("localId");
            add("globalId");
            add("secondaryIds");
            add("name_key");
            add("name_key.autocomplete");
            add("name_key.htmlSmoosh");
            add("name_key.keyword");
            add("name_key.standardBigrams");
            add("name_key.keywordAutocomplete");
            add("name");
            add("name.autocomplete");
            add("name.htmlSmoosh");
            add("name.keyword");
            add("name.standardBigrams");
            add("symbol");
            add("symbol.autocomplete");
            add("symbol.keyword");
            add("synonyms");
            add("synonyms.keyword");
            add("synonyms.htmlSmoosh");
            add("synonyms.standardBigrams");
            add("automatedGeneSynopsis");
            add("geneSynopsis");
            add("external_ids");
            add("species"); add("species.synonyms"); add("modLocalId");
            add("biologicalProcessWithParents"); add("molecularFunctionWithParents"); add("cellularComponentWithParents");
            add("go_type"); add("go_genes"); add("go_synonyms");
            add("disease_genes"); add("disease_synonyms"); add("diseasesViaExperiment.name");
            //disease cross references:
            add("crossReferences.ontology_provided_cross_reference.name");
            add("crossReferences.ontology_provided_cross_reference.localId");
            //gene cross references:
            add("crossReferences.generic_cross_reference.name");
            add("crossReferences.generic_cross_reference.localId");
            add("geneDocument.name");
            add("geneDocument.name_key");
            add("diseaseDocuments.name");
            add("alleles.symbol");
            add("annotations.featureDocument.symbol");
            add("annotations.featureDocument.name");
            add("annotations.geneDocument.symbol");
            add("annotations.geneDocument.name");
            add("phenotypeStatements");
            add("strictOrthologySymbols.autocomplete");
            add("strictOrthologySymbols.keyword");
            add("whereExpressed"); add("whereExpressed.keyword");
            add("anatomicalExpression"); add("anatomicalExpression.keyword");
            add("cellularComponentExpression"); add("cellularComponentExpression.keyword");
            add("cellularComponentExpressionWithParents"); add("cellularComponentExpressionWithParents.keyword");
            add("cellularComponentExpressionAgrSlim"); add("cellularComponentExpressionAgrSlim.keyword");
        }
    };


    private List<String> highlight_blacklist_fields = new ArrayList<String>() {
        {
            add("go_genes"); add("name.autocomplete");
        }
    };



    public List<AggregationBuilder> createAggBuilder(String category) {
        List<AggregationBuilder> ret = new ArrayList<>();

        if(category == null || !category_filters.containsKey(category)) {
            TermsAggregationBuilder term = AggregationBuilders.terms("categories");
            term.field("category");
            term.size(50);
            ret.add(term);
        } else {
            for(String item: category_filters.get(category)) {
                TermsAggregationBuilder term = AggregationBuilders.terms(item);
                term.field(item + ".keyword");
                term.size(999);
                ret.add(term);
            }
        }

        return ret;
    }


    public ArrayList<AggResult> formatAggResults(String category, SearchResponse res) {
        ArrayList<AggResult> ret = new ArrayList<>();

        if(category == null) {

            Terms aggs = res.getAggregations().get("categories");

            AggResult ares = new AggResult("category");
            for (Terms.Bucket entry : aggs.getBuckets()) {
                ares.values.add(new AggDocCount(entry.getKeyAsString(), entry.getDocCount()));
            }
            ret.add(ares);

        } else {
            if(category_filters.containsKey(category)) {
                for(String item: category_filters.get(category)) {
                    Terms aggs = res.getAggregations().get(item);

                    AggResult ares = new AggResult(item);
                    for (Terms.Bucket entry : aggs.getBuckets()) {
                        ares.values.add(new AggDocCount(entry.getKeyAsString(), entry.getDocCount()));
                    }
                    ret.add(ares);
                }
            }
        }

        return ret;
    }


    public boolean filterIsValid(String category, String fieldName) {
        if (searchFields.contains(fieldName)) { return true; }

        if (!category_filters.containsKey(category)) { return false; }

        List<String> fields = category_filters.get(category);

        return fields.contains(fieldName);
    }


    public void applyFilters(BoolQueryBuilder bool, String category, UriInfo uriInfo ) {
        if(category_filters.containsKey(category)) {
            for(String item: category_filters.get(category)) {
                if(uriInfo.getQueryParameters().containsKey(item)) {
                    for(String param: uriInfo.getQueryParameters().get(item)) {
                        bool.filter(new TermQueryBuilder(item + ".keyword", param));
                    }
                }
            }
        }
    }


    public ArrayList<Map<String, Object>> formatResults(SearchResponse res, List<String> searchedTerms) {
        log.debug("Formatting Results: ");
        ArrayList<Map<String, Object>> ret = new ArrayList<>();

        for(SearchHit hit: res.getHits()) {
            Map<String, Object> map = new HashMap<>();
            for(String key: hit.getHighlightFields().keySet()) {
                if(key.endsWith(".symbol")) {
                    log.debug("Source as String: " + hit.getSourceAsString());
                    log.debug("Highlights: " + hit.getHighlightFields());
                }

                ArrayList<String> list = new ArrayList<>();
                for(Text t: hit.getHighlightFields().get(key).getFragments()) {
                    list.add(t.string());
                }

                // this may eventually need to be replaced by a more targeted
                // method that just remove .keyword .synonym etc
                String name = hit.getHighlightFields().get(key).getName();
                
                for (int i = 0 ; i < SUFFIX_LIST.length ; i++ ) {
                    name = name.replace(SUFFIX_LIST[i],"");
                }

                map.put(name, list);
            }
            hit.getSourceAsMap().put("highlights", map);
            hit.getSourceAsMap().put("id", hit.getId());
            hit.getSourceAsMap().put("score", hit.getScore());
            if (hit.getExplanation() != null) {
                hit.getSourceAsMap().put("explanation", hit.getExplanation());
            }

            hit.getSourceAsMap().put("missingTerms", findMissingTerms(Arrays.asList(hit.getMatchedQueries()),
                                                                 searchedTerms));
            ret.add(hit.getSourceAsMap());
        }
        log.debug("Finished Formatting Results: ");
        return ret;
    }

    private List<String> findMissingTerms(List<String> matchedTerms, List<String> searchedTerms) {

        List<String> terms = new ArrayList<>();

        //if only one term was searched, just assume it matched
        //(not for efficiency, avoids false negatives - if the document came back, the single term matched)
        if (matchedTerms == null || searchedTerms == null || searchedTerms.size() == 1) {
            return terms; //just give up and return an empty list
        }

        terms.addAll(searchedTerms);
        terms.removeAll(matchedTerms);

        return terms;
    }

    public HighlightBuilder buildHighlights() {

        HighlightBuilder hlb = new HighlightBuilder();

        for(String field: searchFields) {
            if(!highlight_blacklist_fields.contains(field)) {
                hlb.field(field);
            }
        }

        return hlb;
    }
    
    public BoolQueryBuilder limitCategories() {
        BoolQueryBuilder bool = boolQuery();
        Arrays.asList(Category.values()).stream()
                .filter(cat ->  cat.isSearchable() )
                .forEach(cat ->
                        bool.should(termQuery("category", cat.getName()))
                );
        return bool;
    }



}
