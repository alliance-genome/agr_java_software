package org.alliancegenome.es.index.site.dao;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.document.FeatureTranslator;
import org.alliancegenome.es.index.ESDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.Feature;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.PhenotypeRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.sort.SortBuilders;
import org.neo4j.ogm.model.Result;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class GeneDAO extends ESDAO {

    private Log log = LogFactory.getLog(getClass());

    private GeneRepository geneRepository = new GeneRepository();
    private PhenotypeRepository phenotypeRepository = new PhenotypeRepository();

    private FeatureTranslator featureTranslator = new FeatureTranslator();

    public Map<String, Object> getById(String id) {
        try {
            GetRequest request = new GetRequest();
            request.id(id);
            request.type("gene");
            request.index(ConfigHelper.getEsIndex());
            GetResponse res = searchClient.get(request).get();
            //log.info(res);
            return res.getSource();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Object> getGeneBySecondary(String id) {
        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();

        //searchRequestBuilder.setExplain(true);
        searchRequestBuilder.setIndices(ConfigHelper.getEsIndex());

        // match on secondary IDs
        MatchQueryBuilder query = QueryBuilders.matchQuery("secondaryIds", id);
        searchRequestBuilder.setQuery(query);
        org.elasticsearch.action.search.SearchResponse response = searchRequestBuilder.execute().actionGet();
        long total = response.getHits().totalHits;
        if (total > 0)
            return formatResults(response).get(0);
        else
            return null;
    }

    public SearchApiResponse getAllelesByGene(String geneId, Pagination pagination) {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder(geneId);
        if (pagination != null) {
            searchRequestBuilder.setSize(pagination.getLimit());
            int fromIndex = pagination.getIndexOfFirstElement();
            searchRequestBuilder.setFrom(fromIndex);
        }
        org.elasticsearch.action.search.SearchResponse response = searchRequestBuilder.execute().actionGet();
        SearchApiResponse result = new SearchApiResponse();

        result.total = response.getHits().totalHits;
        result.results = formatResults(response);
        return result;
    }

    private SearchRequestBuilder getSearchRequestBuilder(String geneID) {
        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();

        //searchRequestBuilder.setExplain(true);
        searchRequestBuilder.setIndices(ConfigHelper.getEsIndex());

        TermQueryBuilder builder = QueryBuilders.termQuery("geneDocument.primaryId", geneID);
        BoolQueryBuilder query = QueryBuilders.boolQuery().must(builder);
        MultiMatchQueryBuilder multiBuilder = QueryBuilders.multiMatchQuery("allele", "category")
                .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX);
        query.must(multiBuilder);
        searchRequestBuilder.addSort(SortBuilders.fieldSort("symbol.sort"));
        searchRequestBuilder.setQuery(query);
        log.debug(searchRequestBuilder);
        return searchRequestBuilder;
    }

    public JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotations(String geneID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        List<PhenotypeAnnotation> list = getPhenotypeAnnotationList(geneID, pagination);
        JsonResultResponse<PhenotypeAnnotation> response = new JsonResultResponse<>();
        response.calculateRequestDuration(startDate);
        response.setResults(list);
        Long count = phenotypeRepository.getTotalPhenotypeCount(geneID, pagination);
        response.setTotal((int) (long) count);
        return response;
    }

    private List<PhenotypeAnnotation> getPhenotypeAnnotationList(String geneID, Pagination pagination) {

        Result result = phenotypeRepository.getPhenotype(geneID, pagination);
        List<PhenotypeAnnotation> annotationDocuments = new ArrayList<>();
        result.forEach(objectMap -> {
            PhenotypeAnnotation document = new PhenotypeAnnotation();
            Gene gene = new Gene();
            gene.setPrimaryKey(geneID);
            document.setGene(gene);
            document.setPhenotype((String) objectMap.get("phenotype"));
            Feature feature = (Feature) objectMap.get("feature");
            if (feature != null) {
                List<CrossReference> ref = (List<CrossReference>) objectMap.get("crossReferences");
                feature.setCrossReferences(ref);
                document.setFeature(feature);
            }
            document.setPublications((List<Publication>) objectMap.get("publications"));
            annotationDocuments.add(document);
        });

        return annotationDocuments;
    }


    private static Map<FieldFilter, String> diseaseFieldFilterSortingMap = new HashMap<>(10);

    static {
        diseaseFieldFilterSortingMap.put(FieldFilter.PHENOTYPE, "phenotype.sort");
        diseaseFieldFilterSortingMap.put(FieldFilter.GENETIC_ENTITY, "featureDocument.symbol.sort");
    }

}