package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.OrthologyAllianceCacheManager;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.core.JsonProcessingException;

public class GeneOrthologCacher extends Cacher {

    private static GeneRepository geneRepository = new GeneRepository();

    @Override
    protected void cache() {

        startProcess("geneRepository.getAllOrthologyGenes");
        
        List<Gene> geneList = geneRepository.getAllOrthologyGenes();
        
        finishProcess();
        
        if (geneList == null)
            return;

        OrthologyAllianceCacheManager manager = new OrthologyAllianceCacheManager();

        startProcess("create geneList into cache");
        
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
                        progressProcess();
                        return view;
                    })
                    .collect(toSet());

            JsonResultResponse<OrthologView> result = new JsonResultResponse<>();
            result.setResults(new ArrayList<>(orthologySet));
            try {
                manager.putCache(gene.getPrimaryKey(), result, View.Orthology.class, CacheAlliance.GENE_ORTHOLOGY);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        
        finishProcess();
        setCacheStatus(geneList.size(), CacheAlliance.GENE_ORTHOLOGY.getCacheName());
        geneRepository.clearCache();

    }
}
