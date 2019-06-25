package org.alliancegenome.cacher.cachers.db;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;

import org.alliancegenome.cacher.cachers.DBCacher;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologView;

public class GeneOrthologDBCacher extends DBCacher<Set<OrthologView>> {

    private static GeneRepository geneRepository = new GeneRepository();
    
    public GeneOrthologDBCacher(String cacheName) {
        super(cacheName);
    }
    
    @Override
    protected void cache() {
        
        List<Gene> geneList = geneRepository.getAllOrthologyGenes();
        if (geneList == null)
            return;

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
            cache.put(gene.getPrimaryKey(), orthologySet);
        });
        
    }
}
