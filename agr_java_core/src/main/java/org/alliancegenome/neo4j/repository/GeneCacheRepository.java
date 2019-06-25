package org.alliancegenome.neo4j.repository;

import static java.util.stream.Collectors.toSet;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.view.OrthologView;
import org.apache.commons.collections4.MapUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class GeneCacheRepository {

    private static GeneRepository geneRepository = new GeneRepository();

    // cached value
    private static List<Gene> allGenes = null;
    // Map<gene ID, Gene>
    private static Map<String, Gene> allGeneMap = new HashMap<>();
    private static boolean caching;

    private static boolean orthologyCaching;
    private static LocalDateTime startOrthology;
    private static LocalDateTime endOrthology;

    private static Map<String, Set<OrthologView>> orthologViewMap = new HashMap<>();
    
    
    public List<Gene> getAllGenes() {
        checkCache();
        if (caching)
            return null;

        return allGenes;
    }

    public Gene getGene(String geneID) {
        if (geneID == null)
            return null;
        checkCache();
        if (caching)
            return null;

        return allGeneMap.get(geneID);
    }

    private synchronized void checkCache() {
        if (allGenes == null && !caching) {
            caching = true;
            cacheAllGenes();
            caching = false;
        }
    }

    private void cacheAllGenes() {
        long start = System.currentTimeMillis();
        allGenes = geneRepository.getAllGenes();
        if (allGenes == null)
            return;

        allGenes.forEach(gene -> allGeneMap.put(gene.getPrimaryKey(), gene));
        log.info("Retrieved " + allGenes.size() + " genes");
        log.info("Time to retrieve genes " + ((System.currentTimeMillis() - start) / 1000) + " s");
    }

    public List<OrthologView> getAllOrthologyGenes(List<String> geneIDs) {
        orthologyCheckCache();
        if (orthologyCaching)
            return null;

        List<OrthologView> fullOrthologyList = new ArrayList<>();
        geneIDs.forEach(id -> fullOrthologyList.addAll(orthologViewMap.get(id)));

        return fullOrthologyList;

    }

    private synchronized void orthologyCheckCache() {
        if (MapUtils.isEmpty(orthologViewMap) && !orthologyCaching) {
            orthologyCaching = true;
            cacheAllOrthology();
            orthologyCaching = false;
        }
    }

    private void cacheAllOrthology() {
        startOrthology = LocalDateTime.now();
        long start = System.currentTimeMillis();
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
            orthologViewMap.put(gene.getPrimaryKey(), orthologySet);
        });


        log.info("Number of Gene IDs in gene / orthologyView Map: " + orthologViewMap.size());
        log.info("Time to create orthology cache: " + (System.currentTimeMillis() - start) / 1000);
        geneRepository.clearCache();
        endOrthology = LocalDateTime.now();
    }

    public CacheStatus getCacheStatus() {
        CacheStatus status = new CacheStatus("Orthology");
        status.setCaching(orthologyCaching);
        status.setStart(startOrthology);
        status.setEnd(endOrthology);
        if (orthologViewMap != null)
            status.setNumberOfEntities(orthologViewMap.size());
        return status;
    }

}
