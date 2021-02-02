package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.*;

import java.util.*;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

        log.info("Total Number of Genes: ", geneList.size());

        geneGeneAlgorithm = geneRepository.getAllOrthologyGeneJoin();
        allMethods = geneRepository.getAllMethods();
        log.info(geneGeneAlgorithm.size());

        int orthologousRecords = geneList.stream().map(gene -> gene.getOrthoGenes().size()).mapToInt(Integer::intValue).sum();
        log.info("Total Number of Ortho Records: ", orthologousRecords);
        startProcess("create geneList into cache", orthologousRecords);

        List<OrthologView> allOrthology = new ArrayList<>();
        geneList.forEach(gene -> {
            Set<OrthologView> orthologySet = gene.getOrthoGenes().stream()
                    .map(orthologous -> {
                        OrthologView view = new OrthologView();
                        view.setGene(gene);
                        view.setHomologGene(orthologous.getGene2());
                        view.setBest(orthologous.getIsBestScore());
                        view.setBestReverse(orthologous.getIsBestRevScore());
                        if (orthologous.isStrictFilter() && !orthologous.isModerateFilter()) {
                            view.setStringencyFilter("stringent");
                        } else if (orthologous.isModerateFilter() && !orthologous.isStrictFilter()) {
                            view.setStringencyFilter("moderate");
                        }
                        else
                        	view.setStringencyFilter("all");
                        progressProcess();
                        view.setPredictionMethodsMatched(getPredictionMatches(gene.getPrimaryKey(), orthologous.getGene2().getPrimaryKey()));
                        view.setPredictionMethodsNotMatched(getPredictionNotMatches(gene.getPrimaryKey(), orthologous.getGene2().getPrimaryKey()));
                        view.setPredictionMethodsNotCalled(getPredictionNotCalled(view));
                        return view;
                    })
                    .collect(toSet());
            allOrthology.addAll(orthologySet);

            cacheService.putCacheEntry(gene.getPrimaryKey(), new ArrayList<>(orthologySet), View.OrthologyCacher.class, CacheAlliance.GENE_ORTHOLOGY);
            progressProcess();
        });
        finishProcess();

        // get homology cache by species
        
        startProcess("allOrthology.stream - group By o.getGene().getTaxonId()");
        Map<String, List<OrthologView>> map = allOrthology.stream()
                .collect(groupingBy(o -> o.getGene().getTaxonId()));
        finishProcess();
        
        
        startProcess("allOrthology orthologViews into cache", map.size());
        
        map.forEach((speciesID, orthologViews) -> {
            cacheService.putCacheEntry(speciesID, orthologViews, View.OrthologyCacher.class, CacheAlliance.SPECIES_ORTHOLOGY);
            progressProcess();
        });
        
        finishProcess();
        
        CacheStatus status = new CacheStatus(CacheAlliance.SPECIES_ORTHOLOGY);
        //status.setNumberOfEntities(allExpression.size());

        Map<String, Integer> speciesStatsInt = new TreeMap<>();
        map.forEach((speciesID, orthology) -> speciesStatsInt.put(speciesID, orthology.size()));

        map.clear();
        
        status.setSpeciesStats(speciesStatsInt);
        setCacheStatus(status);

        startProcess("allOrthology.stream - group By getSpeciesSpeciesID");
        Map<String, List<OrthologView>> speciesToSpeciesMap = allOrthology.stream()
                .collect(groupingBy(this::getSpeciesSpeciesID));
        finishProcess();
        
        startProcess("Cache speciesToSpeciesMap into cache", speciesToSpeciesMap.size());
        
        speciesToSpeciesMap.forEach((speciesSpeciesID, orthologViews) -> {
            cacheService.putCacheEntry(speciesSpeciesID, orthologViews, View.OrthologyCacher.class, CacheAlliance.SPECIES_SPECIES_ORTHOLOGY);
            progressProcess();
        });
        finishProcess();

        status = new CacheStatus(CacheAlliance.SPECIES_SPECIES_ORTHOLOGY);
        //status.setNumberOfEntities(allExpression.size());

        Map<String, Integer> speciesSpeciesStatsInt = new TreeMap<>();
        speciesToSpeciesMap.forEach((speciesID, orthology) -> speciesSpeciesStatsInt.put(speciesID, orthology.size()));

        status.setSpeciesStats(speciesSpeciesStatsInt);
        setCacheStatus(status);

        geneRepository.clearCache();
    }

    public String getSpeciesSpeciesID(OrthologView o) {
        return o.getGene().getTaxonId() + ":" + o.getHomologGene().getTaxonId();
    }

    private List<String> getPredictionNotCalled(OrthologView view) {
        List<String> usedNames = view.getPredictionMethodsMatched() != null ? new ArrayList<>(view.getPredictionMethodsMatched()) : new ArrayList<>();
        if (view.getPredictionMethodsNotMatched() != null)
            usedNames.addAll(view.getPredictionMethodsNotMatched());
        return allMethods.stream()
                .filter(method -> !usedNames.contains(method))
                .sorted(Comparator.naturalOrder())
                .collect(toList());
    }

    private List<String> getPredictionMatches(String primaryKey, String primaryKey1) {
        if (primaryKey == null || primaryKey1 == null)
            return null;

        Map<String, Set<String>> lists = geneGeneAlgorithm.get(primaryKey, primaryKey1);
        if (lists == null) {
            log.debug("No algorithm found for " + primaryKey + " and " + primaryKey1);
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
            log.debug("No algorithm found for " + primaryKey + " and " + primaryKey1);
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
