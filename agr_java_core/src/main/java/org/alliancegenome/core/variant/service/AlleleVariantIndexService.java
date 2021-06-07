package org.alliancegenome.core.variant.service;

import static org.alliancegenome.core.config.Constants.ES_INDEX;

import java.io.IOException;
import java.util.*;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.neo4j.entity.node.*;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import lombok.extern.jbosslog.JBossLog;

import javax.enterprise.context.RequestScoped;

@JBossLog
@RequestScoped
public class AlleleVariantIndexService {

    private ObjectMapper mapper = new ObjectMapper();

    public AlleleVariantIndexService() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION,false);
    }

    public List<AlleleVariantSequence> getAllelesNVariants(String geneId)  {
        SearchResponse searchResponce = null;
        try {
            searchResponce = getSearchResponse(geneId, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<AlleleVariantSequence> avsList = new ArrayList<>();

        if (searchResponce != null) {
            for(SearchHit searchHit: searchResponce.getHits()) {
                AlleleVariantSequence avsDocument = null;
                Allele allele = null;
                try {
                    avsDocument = mapper.readValue(searchHit.getSourceAsString(), AlleleVariantSequence.class);
                    allele = avsDocument.getAllele();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                if (avsDocument != null && avsDocument.getAlterationType().equalsIgnoreCase("variant")) {
                    allele.setCategory("variant");

                }

                if (allele != null) {

                    if (allele.getUrl() == null) {
                        allele.setUrl(" ");
                    }
                    if (allele.getId() == null || (allele.getId() != null && allele.getId().equals("null"))) {
                        allele.setId(0L);
                    }

                    for (Variant variant : allele.getVariants()) {
                        if (variant.getTranscriptLevelConsequence() != null && variant.getTranscriptLevelConsequence().size() > 0) {
                            for (TranscriptLevelConsequence consequence: variant.getTranscriptLevelConsequence()) {
                                AlleleVariantSequence seq = new AlleleVariantSequence(allele, variant, consequence);
                                avsList.add(seq);
                            }
                        } else {
                            AlleleVariantSequence seq = new AlleleVariantSequence(allele, variant, null);
                            avsList.add(seq);
                        }
                    }
                }



            }
        }
        
        log.debug("TOTAL HITS:" + searchResponce.getHits().getTotalHits());
        log.debug("Allele Variant Sequences:" + avsList.size());

        return avsList;
    }
    
    public List<Allele> getAlleles(String geneId)  {
        SearchResponse searchResponce = null;
        try {
            searchResponce = getSearchResponse(geneId, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Allele> alleles = new ArrayList<>();

        if (searchResponce != null) {
            for(SearchHit searchHit: searchResponce.getHits()) {
                AlleleVariantSequence avsDocument = null;
                Allele allele = null;
                try {
                    avsDocument = mapper.readValue(searchHit.getSourceAsString(), AlleleVariantSequence.class);
                    allele = avsDocument.getAllele();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                if (allele != null) {
                    if(allele.getUrl()==null){
                        allele.setUrl(" ");
                    }
                    alleles.add(allele);
                }

            }
        }

        return alleles;
    }
    
    public SearchResponse getSearchResponse(String id, boolean includeHtp) throws IOException {
        SearchSourceBuilder srb = new SearchSourceBuilder();
        srb.query(buildBoolQuery(id, includeHtp));
        srb.size(10000);

        SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());

        searchRequest.source(srb);

        return EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
    }
    
    public BoolQueryBuilder buildBoolQuery(String id, boolean includeHtp){
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
        queryBuilder.must(QueryBuilders.termQuery("geneIds.keyword", id)).filter(QueryBuilders.termQuery("category.keyword", "allele"));
        if(!includeHtp){
            queryBuilder.mustNot(QueryBuilders.termQuery("alterationType.keyword", "variant"));
        }
        return queryBuilder;
    }
    
}
