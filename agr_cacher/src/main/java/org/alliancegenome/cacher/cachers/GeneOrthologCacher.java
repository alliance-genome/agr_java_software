package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.OrthologyAllianceCacheManager;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.map.MultiKeyMap;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class GeneOrthologCacher extends Cacher {

    private static GeneRepository geneRepository = new GeneRepository();
    private MultiKeyMap<String, Map<String, Set<String>>> geneGeneAlgorithm;
    private List<String> allMethods = new ArrayList<>();

    @Override
    protected void cache() {

        startProcess("geneRepository.getAllOrthologyGenes");

        List<Gene> geneList = geneRepository.getAllOrthologyGenes();

        finishProcess();

        if (geneList == null)
            return;

        geneGeneAlgorithm = geneRepository.getAllOrthologyGeneJoin();
        allMethods = geneRepository.getAllMethods();
        log.info(geneGeneAlgorithm.size());

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
                        view.setPredictionMethodsMatched(getPredictionMatches(gene.getPrimaryKey(), orthologous.getGene2().getPrimaryKey()));
                        view.setPredictionMethodsNotMatched(getPredictionNotMatches(gene.getPrimaryKey(), orthologous.getGene2().getPrimaryKey()));
                        view.setPredictionMethodsNotCalled(getPredictionNotCalled(view));
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

    private List<String> getPredictionNotCalled(OrthologView view) {
        List<String> usedNames = new ArrayList<>(view.getPredictionMethodsMatched());
        if (view.getPredictionMethodsNotMatched() != null)
            usedNames.addAll(view.getPredictionMethodsNotMatched());
        return allMethods.stream()
                .filter(method -> !usedNames.contains(method))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    private List<String> getPredictionMatches(String primaryKey, String primaryKey1) {
        if (primaryKey == null || primaryKey1 == null)
            return null;

        Map<String, Set<String>> lists = geneGeneAlgorithm.get(primaryKey, primaryKey1);
        if (lists == null) {
            log.warn("No algorithm found for " + primaryKey + " and " + primaryKey1);
            return null;
        }
        Set<String> algorithmSet = lists.get("match");
        ArrayList<String> strings = new ArrayList<>(algorithmSet);
        strings.sort(Comparator.naturalOrder());
        return strings;
    }

    private List<String> getPredictionNotMatches(String primaryKey, String primaryKey1) {
        if (primaryKey == null || primaryKey1 == null)
            return null;

        Map<String, Set<String>> lists = geneGeneAlgorithm.get(primaryKey, primaryKey1);
        if (lists == null) {
            log.warn("No algorithm found for " + primaryKey + " and " + primaryKey1);
            return null;
        }
        // Always return non-null list
        ArrayList<String> strings = new ArrayList<>();
        Set<String> algorithmSet = lists.get("notMatch");
        if (CollectionUtils.isNotEmpty(algorithmSet)) {
            strings = new ArrayList<>(algorithmSet);
            strings.sort(Comparator.naturalOrder());
        }
        return strings;
    }
}
