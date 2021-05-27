package org.alliancegenome.core.variant.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.neo4j.entity.node.*;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.alliancegenome.core.config.Constants.ES_INDEX;

public class AlleleVariantIndexService {
    ObjectMapper mapper=new ObjectMapper();

    public List<AlleleVariantSequence> getAllelesNVariants(String geneId)  {
        SearchResponse sr= null;
        try {
            //    sr = getSearchResponse("RGD:2219");
            sr = getSearchResponse(geneId, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<AlleleVariantSequence> avs=new ArrayList<>();

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION,false);
        if (sr != null) {
            for(SearchHit searchHit:sr.getHits()) {
                AlleleVariantSequence av= null;
                Allele a=null;
                try {
                    av = mapper.readValue(searchHit.getSourceAsString(), AlleleVariantSequence.class);
                    a=av.getAllele();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                if (av != null && av.getAlterationType().equalsIgnoreCase("variant")) {
                    a.setCategory("variant");

                }

                if (a != null) {

                    if (a.getUrl() == null) {
                        a.setUrl(" ");
                    }
                    if (a.getId() == null || (a.getId() != null && a.getId().equals("null"))) {
                        a.setId(0L);
                    }

                    for (Variant v : a.getVariants()) {
                        if (v.getTranscriptLevelConsequence() != null && v.getTranscriptLevelConsequence().size() > 0) {
                            for (TranscriptLevelConsequence c : v.getTranscriptLevelConsequence()) {
                                AlleleVariantSequence seq = new AlleleVariantSequence(a, v, c);
                                avs.add(seq);
                            }
                        } else {
                            AlleleVariantSequence seq = new AlleleVariantSequence(a, v, null);
                            avs.add(seq);
                        }
                    }
                }



            }
        }
        System.out.println("TOTAL HITS:"+sr.getHits().getTotalHits());
        System.out.println("Allele Variant Sequences:" +avs.size());


        return avs;
    }
    public List<Allele> getAlleles(String geneId)  {
        SearchResponse sr= null;
        try {
            //    sr = getSearchResponse("RGD:2219");
            sr = getSearchResponse(geneId, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Allele> alleles=new ArrayList<>();

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION,false);
        if (sr != null) {
            for(SearchHit searchHit:sr.getHits()) {
                AlleleVariantSequence av= null;
                Allele a=null;
                try {
                    av = mapper.readValue(searchHit.getSourceAsString(), AlleleVariantSequence.class);
                    a=av.getAllele();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                if (a != null) {
                    if(a.getUrl()==null){
                        a.setUrl(" ");
                    }
                    alleles.add(a);
                }

            }
        }


        return alleles;
    }
    public SearchResponse getSearchResponse(String id, boolean includeHtp) throws IOException {
        SearchSourceBuilder srb=new SearchSourceBuilder();
        srb.query(buildBoolQuery(id, includeHtp));
        srb.size(10000);

        SearchRequest searchRequest=new SearchRequest(ES_INDEX);

        searchRequest.source(srb);


        return EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
    }
    public BoolQueryBuilder buildBoolQuery(String id, boolean includeHtp){
        BoolQueryBuilder qb=new BoolQueryBuilder();
        qb.must(QueryBuilders.termQuery("geneIds.keyword", id))
                .filter(QueryBuilders.termQuery("category.keyword", "allele"));
        if(!includeHtp){
            qb.mustNot(QueryBuilders.termQuery("alterationType.keyword", "variant"));
        }
        return qb;

    }
}
