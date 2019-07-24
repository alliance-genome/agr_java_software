package org.alliancegenome.cacher.cachers.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.alliancegenome.cache.AllianceCacheManager;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.OrthologyAllianceCacheManager;
import org.alliancegenome.cacher.cachers.Cacher;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class GeneOrthologCacher extends Cacher {

    private static GeneRepository geneRepository = new GeneRepository();

    @Override
    protected void cache() {

        List<Gene> geneList = geneRepository.getAllOrthologyGenes();
        if (geneList == null)
            return;

        OrthologyAllianceCacheManager manager = new OrthologyAllianceCacheManager();

        geneList.forEach(gene -> {
            Set<OrthologView> orthologySet = gene.getOrthoGenes().stream()
                    .map(orthologous -> {
                        OrthologView view = new OrthologView();
                        view.setGene(gene);
                        view.setHomologGene(orthologous.getGene2());
                        view.setBest(orthologous.getIsBestScore());
                        view.setBestReverse(orthologous.getIsBestRevScore());
                        if (orthologous.isStrictFilter()) {
                            view.setStringencyFilter("stringent");
                        } else if (orthologous.isModerateFilter()) {
                            view.setStringencyFilter("moderate");
                        }
                        return view;
                    })
                    .collect(toSet());

            JsonResultResponse<OrthologView> result = new JsonResultResponse<>();
            result.setResults(new ArrayList<>(orthologySet));
            try {
                manager.putCache(gene.getPrimaryKey(), result, View.Orthology.class, CacheAlliance.ORTHOLOGY);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

    }
}
