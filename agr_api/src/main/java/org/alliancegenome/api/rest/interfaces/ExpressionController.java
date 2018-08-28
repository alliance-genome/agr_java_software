package org.alliancegenome.api.rest.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.search.SearchResult;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.View;

import java.time.LocalDateTime;

import static org.alliancegenome.api.controller.GeneController.API_VERSION;

public class ExpressionController implements ExpressionRESTInterface {

    @Override
    public String getExpressionAnnotations(String id, int limit, int page) throws JsonProcessingException {

        LocalDateTime startDate = LocalDateTime.now();
        GeneRepository geneRepository = new GeneRepository();
        Gene gene = geneRepository.getOneGene(id);
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);

        JsonResultResponse response = null;
        response.setRequestDuration(startDate);
        response.setApiVersion(API_VERSION);
        return mapper.writerWithView(View.OrthologyView.class).writeValueAsString(response);
    }
}
