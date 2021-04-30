package org.alliancegenome.es.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class VariantESDAO {

    private Log log = LogFactory.getLog(getClass());

    protected static RestHighLevelClient searchClient = null; // Make sure to only have 1 of these clients to save on resources

    public VariantESDAO() {
        init();
    }

    public void init() {
        searchClient = EsClientFactory.getDefaultEsClient();
    }

    public void close() {
        log.info("Closing Down ES Client");
        try {
            searchClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ObjectMapper mapper = new ObjectMapper();


    public Integer performQueryCount(QueryBuilder query, Pagination pagination) {

        // index name needs to come from configuration
        CountRequest countRequest = new CountRequest("variant_index");
        countRequest.query(query);

        CountResponse response = null;
        try {
            response = searchClient.count(countRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (int) response.getCount();
    }

    public JsonResultResponse<Allele> performQuery(SearchSourceBuilder searchSourceBuilder, Pagination pagination) {

        SearchRequest searchRequest = new SearchRequest("variant_index");
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.size(pagination.getLimit());
        searchSourceBuilder.sort("primaryKey.keyword");
        SearchResponse response = null;

        try {
            response = searchClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (response == null || response.getHits() == null)
            return null;

        SearchHit[] searchHits = response.getHits().getHits();
        List<AlleleVariantSequence> results =
                Arrays.stream(searchHits)
                        .map(hit -> {
                            try {
                                return mapper.readValue(hit.getSourceAsString(), AlleleVariantSequence.class);
                            } catch (IOException e) {
                                log.error("Error during deserialization ", e);
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(toList());
        List<Allele> alleles = results.stream()
                .map(alleleVariantSequence -> {
                    Allele allele;
                    if (alleleVariantSequence.getAllele() == null) {
                        allele = new Allele(alleleVariantSequence.getPrimaryKey(), GeneticEntity.CrossReferenceType.VARIANT);
                        alleleVariantSequence.getTranscriptLevelConsequences().forEach(transcriptLevelConsequence -> {
                            // populate the first GLC
                            // todo: needs to be adjusted to curator input
                            if (alleleVariantSequence.getVariant().getGeneLevelConsequence() == null) {
                                GeneLevelConsequence consequence = new GeneLevelConsequence();
                                consequence.setGeneLevelConsequence(transcriptLevelConsequence.getGeneLevelConsequence());
                                alleleVariantSequence.getVariant().setGeneLevelConsequence(consequence);
                            }
                        });
                        Variant variant = alleleVariantSequence.getVariant();
                        allele.setVariants(List.of(variant));
                        allele.setSymbol(alleleVariantSequence.getPrimaryKey());
                        Map<String, CrossReference> crossRefs = new HashMap<>();
                        CrossReference ref = new CrossReference();
                        ref.setName("");
                        crossRefs.put("primary", ref);
                        allele.setCrossReferenceMap(Map.copyOf(crossRefs));
                    } else {
                        allele = alleleVariantSequence.getAllele();
                    }
                    return allele;
                })
                .collect(toList());
        JsonResultResponse resultResponse = new JsonResultResponse();
        resultResponse.setResults(alleles);
        resultResponse.setTotal(performQueryCount(searchSourceBuilder.query(), pagination));


        return resultResponse;
    }

    public Map<String, List<String>> getDistinctValues(SearchSourceBuilder searchSourceBuilder) {

        SearchRequest searchRequest = new SearchRequest("variant_index");
        searchRequest.source(searchSourceBuilder);
        SearchResponse response = null;

        Map<FieldFilter, String> distinctFields = new HashMap<>();
        distinctFields.put(FieldFilter.MOLECULAR_CONSEQUENCE, "transcriptLevelConsequences.geneLevelConsequence.keyword");
        distinctFields.put(FieldFilter.VARIANT_TYPE, "variant.variantType.name.keyword");
        distinctFields.put(FieldFilter.ALLELE_CATEGORY, "alterationType.keyword");
        distinctFields.forEach((fieldFilter, esFieldName) -> {
            searchSourceBuilder.aggregation(AggregationBuilders.terms(fieldFilter.getName()).field(esFieldName));
        });

        try {
            response = searchClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // get Distinct values
        // for now on the filtered result set. This needs to be done on the full, unfiltered result set.

        Map<String, List<String>> distinctValueMap = new HashMap<>();
        Iterator<FieldFilter> iter = distinctFields.keySet().iterator();
        while (iter.hasNext()) {
            final FieldFilter filter = iter.next();
            List<String> list = ((ParsedStringTerms) response.getAggregations().get(filter.getName())).getBuckets()
                    .stream()
                    .map(bucket -> (String) bucket.getKey())
                    .collect(toList());
            distinctValueMap.put(filter.getName(), list);
        }
        return distinctValueMap;
    }
}
