package org.alliancegenome.neo4j.repository;

import org.alliancegenome.core.service.DiseaseAnnotationSorting;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.entity.node.PublicationEvidenceCodeJoin;
import org.alliancegenome.neo4j.view.OrthologView;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class GeneCacheRepository {

    private Log log = LogFactory.getLog(getClass());
    private static GeneRepository geneRepository = new GeneRepository();


    // cached value
    private static List<Gene> allGenes = null;
    // Map<gene ID, Gene>
    private static Map<String, Gene> allGeneMap = new HashMap<>();
    private static boolean caching;

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

    private static boolean orthologyCaching;
    private List<OrthologView> orthologViewList;
    private Map<Gene, Set<OrthologView>> orthologViewMap = new HashMap<>();

    public List<Gene> getAllOrthologyGenes() {
        orthologyCheckCache();
        if (orthologyCaching)
            return null;

//        List<DiseaseAnnotation> fullDiseaseAnnotationList = diseaseAnnotationSummaryMap.get(diseaseID);

        return null;

    }

    private synchronized void orthologyCheckCache() {
        if (orthologViewList == null && !caching) {
            orthologyCaching = true;
            cacheAllOrthology();
            orthologyCaching = false;
        }
    }

    private void cacheAllOrthology() {
        List<Gene> geneList = geneRepository.getAllOrthologyGenes();
        if (geneList == null)
            return;

//        List<OrthologView> orthologViewList =

        orthologViewMap = geneList.stream()
/*
                .map(gene -> {
                    return gene.getOrthoGenes().stream()
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
                            .collect(Collectors.toSet()); })
*/
                .collect(groupingBy(Function.identity(),  Collectors.mapping(gene -> gene.)))


/*
        orthologViewList = orthologViewList.stream()
                .skip(filter.getStart() - 1)
                .limit(filter.getRows())
                .collect(Collectors.toList());
*/

        log.info("Number of Disease IDs in disease Map: " + orthologViewMap.size());
//        log.info("Time to create annotation histogram: " + (System.currentTimeMillis() - startCreateHistogram) / 1000);

    }

}
