package org.alliancegenome.api.controller;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.api.rest.interfaces.GenesRESTInterface;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@RequestScoped
public class GenesController extends BaseController implements GenesRESTInterface {

    public static final String API_VERSION = "0.9";
    @Inject
    private GeneService geneService;
    private final PhenotypeAnnotationToTdfTranslator translator = new PhenotypeAnnotationToTdfTranslator();
    private ObjectMapper mapper = new ObjectMapper();

    @Context
    private HttpServletResponse response;

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Context
    private HttpServletRequest request;

    @Override
    public String getGenes(List<String> taxonID, Integer rows, Integer start) throws IOException {
        LocalDateTime startDate = LocalDateTime.now();
        GeneRepository repo = new GeneRepository();
        List<String> taxonList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(taxonID)) {
            taxonList.addAll(taxonID);
        }
        OrthologyFilter orthologyFilter = new OrthologyFilter(null, taxonList, null);
        orthologyFilter.setRows(rows);
        orthologyFilter.setStart(start);
        List<Gene> genes = repo.getGenes(orthologyFilter);
        JsonResultResponse<Gene> response = new JsonResultResponse<>();
        response.setResults(genes);
        response.setTotal(repo.getGeneCount(orthologyFilter));

        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        response.calculateRequestDuration(startDate);
        response.setApiVersion(API_VERSION);
        response.setHttpServletRequest(request);
        return mapper.writerWithView(View.OrthologyView.class).writeValueAsString(response);
    }

    @Override
    public String getGeneIDs(List<String> taxonID, Integer rows, Integer start) throws IOException {
        LocalDateTime startDate = LocalDateTime.now();
        GeneRepository repo = new GeneRepository();
        List<String> taxonList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(taxonID)) {
            taxonList.addAll(taxonID);
        }
        OrthologyFilter orthologyFilter = new OrthologyFilter(null, taxonList, null);
        orthologyFilter.setRows(rows);
        orthologyFilter.setStart(start);
        List<String> geneIDs = repo.getGeneIDs(orthologyFilter);
        StringJoiner joiner = new StringJoiner(",");
        geneIDs.forEach(s -> joiner.add(s));
        return joiner.toString();
    }

    public String getPhenotypeAnnotationsDownload(String id) {
        Pagination pagination = new Pagination(1, Integer.MAX_VALUE, "termName", null);
        // retrieve all records
        return translator.getAllRows(geneService.getPhenotypeAnnotationsDownload(id, pagination));
    }

}
