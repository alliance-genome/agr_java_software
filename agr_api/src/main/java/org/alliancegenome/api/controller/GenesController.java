package org.alliancegenome.api.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.api.rest.interfaces.GenesRESTInterface;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.apache.commons.collections.CollectionUtils;

@RequestScoped
public class GenesController extends BaseController implements GenesRESTInterface {

    @Override
    public JsonResultResponse<Gene> getGenes(List<String> taxonID, Integer rows, Integer start) throws IOException {
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
        response.calculateRequestDuration(startDate);
        return response;
    }

    @Override
    public String getGeneIDs(List<String> taxonID, Integer rows, Integer start) {
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
        geneIDs.forEach(joiner::add);
        return joiner.toString();
    }

}
