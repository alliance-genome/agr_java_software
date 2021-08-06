package org.alliancegenome.core.variant.service;

import static org.alliancegenome.core.config.Constants.ES_INDEX;

import java.io.IOException;
import java.util.*;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.lucene.search.SortField;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import lombok.extern.jbosslog.JBossLog;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;


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
/** DETAIL PAGE */
    public List<AlleleVariantSequence> getAllelesNVariants(String geneId, Pagination pagination)  {
       SearchResponse searchResponse=null;
        try {
            log.info("BEFORE QUERY:"+new Date());

            searchResponse = getSearchResponse(geneId);
         log.info(searchResponse.getHits().getHits().length);
            log.info("AFTER QUERY:"+new Date()+ "\tTOOK:"+searchResponse.getTook());

            //  searchHits = getSearchResponse(geneId,pagination);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<AlleleVariantSequence> avsList = new ArrayList<>();
        for(SearchHit searchHit: searchResponse.getHits()) {
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
                    if(allele.getVariants()!=null)
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
                    else{
                        AlleleVariantSequence seq = new AlleleVariantSequence(allele, null, null);
                        avsList.add(seq);
                    }
                }
            }

            log.info("TOTAL HITS:" + searchResponse.getHits().getTotalHits().value);
            log.info("Allele Variant Sequences:" + avsList.size());
        return avsList;
    }
/**
 * GENE PAGE*/
    public List<Allele> getAlleles(String geneId, Pagination pagination)  {
        List<SearchHit> searchHits=new ArrayList<>();
        SearchResponse searchResponse=null;
        try {
           searchHits = getSearchResponse(geneId, pagination);
          //  searchResponse = getSearchResponse(geneId);

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Allele> alleles = new ArrayList<>();
        for(SearchHit searchHit: searchHits) {
                AlleleVariantSequence avsDocument = null;
                Allele allele = null;
                try {
                    avsDocument = mapper.readValue(searchHit.getSourceAsString(), AlleleVariantSequence.class);
                    allele = avsDocument.getAllele();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                if (allele != null) {
                    if (avsDocument.getAlterationType().equalsIgnoreCase("variant")) {
                        allele.setCategory("variant");

                    }
                    if(allele.getUrl()==null){
                        allele.setUrl(" ");
                    }
                    if(allele.getCrossReferenceMap()==null){
                        Map<String, Object> crossReferenceMap=new HashMap<>();
                        CrossReference cr=new CrossReference();
                        cr.setCrossRefCompleteUrl("");
                        crossReferenceMap.put("primary", cr);
                        allele.setCrossReferenceMap(crossReferenceMap);
                    }
                    alleles.add(allele);
                }

            }

        log.info("TOTAL HITS:" + searchHits.size());
        log.info("Alleles :" + alleles.size());
        return alleles;
    }

   public SearchResponse getSearchResponse(String id) throws IOException {
        SearchSourceBuilder srb = new SearchSourceBuilder();
        srb.query(buildBoolQuery(id));
        srb.size(150000);
        srb.trackTotalHits(true);

        SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());

        searchRequest.source(srb);
        log.info(searchRequest);
        return EsClientFactory.getDefaultEsClient().search(searchRequest, EsClientFactory.LARGE_RESPONSE_REQUEST_OPTIONS);
    }

    public List<SearchHit> getSearchResponse(String id, Pagination pagination) throws IOException {
        SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
        SearchResponse searchResponse=null;
        List<SearchHit> searchHits= new ArrayList<>();

        SearchSourceBuilder srb = new SearchSourceBuilder();
        int from =0;
        if(pagination.getPage()>1) {
          from=  pagination.getLimit() * (pagination.getPage()-1);
        }
        srb.query(buildBoolQuery(id, pagination));
        System.out.println("SORT FIELD:"+getSortFields(pagination)[0].getField());
        srb.sort(new FieldSortBuilder(getSortFields(pagination)[0].getField()).order(SortOrder.ASC));
        srb.size(pagination.getLimit());
        srb.trackTotalHits(true);

        if(from+pagination.getLimit()<=150000) {
            srb.from(from);
            searchRequest.source(srb);
            searchResponse=  EsClientFactory.getDefaultEsClient().search(searchRequest, EsClientFactory.LARGE_RESPONSE_REQUEST_OPTIONS);
            searchHits.addAll(Arrays.asList(searchResponse.getHits().getHits()));
            pagination.setTotalHits(searchResponse.getHits().getTotalHits().value);
            System.out.println("TOTAL HITS IN pagination object:"+ pagination.getTotalHits());
        }else{
            srb.size(10000);
            searchRequest.source(srb);
            searchRequest.scroll(TimeValue.timeValueSeconds(60));
            searchResponse=EsClientFactory.getDefaultEsClient().search(searchRequest, EsClientFactory.LARGE_RESPONSE_REQUEST_OPTIONS);
            String scrollId = searchResponse.getScrollId();
            searchHits.addAll(Arrays.asList(searchResponse.getHits().getHits()));
            pagination.setTotalHits(searchResponse.getHits().getTotalHits().value);

            while (searchResponse.getHits().getHits().length >0){
                SearchScrollRequest scrollRequest=new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueSeconds(60));
                searchResponse=EsClientFactory.getDefaultEsClient().scroll(scrollRequest, EsClientFactory.LARGE_RESPONSE_REQUEST_OPTIONS);
                scrollId = searchResponse.getScrollId();
                searchHits.addAll(Arrays.asList(searchResponse.getHits().getHits()));
            };
            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            ClearScrollResponse clearScrollResponse = EsClientFactory.getDefaultEsClient().clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        }

        return searchHits;
    }
    public SortField[] getSortFields(Pagination pagination){
        SortField[] sortField = new SortField[2];

        if(pagination.getSortBy()!=null && !pagination.getSortBy().equalsIgnoreCase("default")){
            if(pagination.getSortBy().equalsIgnoreCase("variantType"))
            sortField[0]=new SortField("variantType.keyword", SortField.Type.STRING);
            if(pagination.getSortBy().equalsIgnoreCase("molecularConsequence"))
                sortField[0]=new SortField("molecularConsequence.keyword", SortField.Type.STRING);
            if(pagination.getSortBy().equalsIgnoreCase("VARIANT"))
                sortField[0]=new SortField("allele.variants.displayName.keyword", SortField.Type.STRING);

            if(pagination.getSortBy().equalsIgnoreCase("transcript"))
                sortField[0]=new SortField("allele.variants.transcriptLevelConsequence.transcript.name.keyword", SortField.Type.STRING);
            if(pagination.getSortBy().equalsIgnoreCase("VariantHgvsName"))
                sortField[0]=new SortField("allele.variants.hgvsG.keyword", SortField.Type.STRING);
        }else{
            sortField[0]=  new SortField("alterationType.keyword", SortField.Type.STRING);
           // sortField[1]=new SortField("symbol.keyword", SortField.Type.STRING);
        }
        return sortField;
    }
    public BoolQueryBuilder buildBoolQuery(String id){
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
        queryBuilder.must(QueryBuilders.termQuery("geneIds.keyword", id)).filter(QueryBuilders.termQuery("category.keyword", "allele"));

        return queryBuilder;
    }
    public BoolQueryBuilder buildBoolQuery(String id, Pagination pagination){
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
        queryBuilder.must(QueryBuilders.termQuery("geneIds.keyword", id)).filter(QueryBuilders.termQuery("category.keyword", "allele"));
        HashMap<FieldFilter, String> filterValueMap= pagination.getFieldFilterValueMap();

//    if(filterValueMap!=null){
//          for(Map.Entry e: filterValueMap.entrySet()){
//            System.out.println (e.getKey()+"\t"+ e.getValue());
//
//            if(e.getKey().toString().equalsIgnoreCase("allele_category")){
//                queryBuilder.filter(QueryBuilders.termsQuery("alterationType.keyword", e.getValue().toString().split("\\|")));
//            }
//              if(e.getKey().toString().equalsIgnoreCase("symbol")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("symbol.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("synonym")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("synonym.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("variant_type")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("variantType.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("has_disease")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("hasDisease.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("molecular_consequence")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("allele.variants.transcriptLevelConsequence.molecularConsequences.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("HAS_PHENOTYPE")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("allele.hasPhenotype.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("VARIANT_IMPACT")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("allele.variants.transcriptLevelConsequence.impact.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("VARIANT_POLYPHEN")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("allele.variants.transcriptLevelConsequence.polyphenPrediction.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("VARIANT_SIFT")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("allele.variants.transcriptLevelConsequence.siftPrediction.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("SEQUENCE_FEATURE_TYPE")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("allele.variants.transcriptLevelConsequence.sequenceFeatureType.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("SEQUENCE_FEATURE")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("allele.variants.transcriptLevelConsequence.transcript.name.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("ASSOCIATED_GENE")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("allele.gene.symbol.keyword", e.getValue().toString().split("\\|")));
//              }
//              if(e.getKey().toString().equalsIgnoreCase("VARIANT_LOCATION")){
//                  queryBuilder.filter(QueryBuilders.termsQuery("allele.variants.transcriptLevelConsequence.location.keyword", e.getValue().toString().split("\\|")));
//              }
//
//          }
//      }
        return queryBuilder;
    }
}
