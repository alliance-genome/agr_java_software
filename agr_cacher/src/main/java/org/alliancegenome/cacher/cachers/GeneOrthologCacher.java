package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.util.*;

import static java.util.stream.Collectors.*;

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

        startProcess("create geneList into cache", geneList.size());
        BasicCachingManager manager = new BasicCachingManager();

        List<OrthologView> allOrthology = new ArrayList<>();
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
            allOrthology.addAll(orthologySet);

            manager.setCache(gene.getPrimaryKey(), new ArrayList<>(orthologySet), View.OrthologyCacher.class, CacheAlliance.GENE_ORTHOLOGY);
        });
        finishProcess();

        // get homology cache by species
        Map<String, List<OrthologView>> map = allOrthology.stream()
                .collect(groupingBy(o -> o.getGene().getTaxonId()));

        map.forEach((speciesID, orthologViews) -> {
            manager.setCache(speciesID, orthologViews, View.OrthologyCacher.class, CacheAlliance.SPECIES_ORTHOLOGY);
        });

        CacheStatus status = new CacheStatus(CacheAlliance.SPECIES_ORTHOLOGY);
        //status.setNumberOfEntities(allExpression.size());

        Map<String, Integer> speciesStatsInt = new TreeMap<>();
        map.forEach((speciesID, orthology) -> speciesStatsInt.put(speciesID, orthology.size()));

        status.setSpeciesStats(speciesStatsInt);
        setCacheStatus(status);

        OrthologyService service = new OrthologyService();
        Map<String, List<OrthologView>> speciesToSpeciesMap;
        speciesToSpeciesMap = allOrthology.stream()
                .collect(groupingBy(service::getSpeciesSpeciesID));

        speciesToSpeciesMap.forEach((speciesSpeciesID, orthologViews) -> {
            manager.setCache(speciesSpeciesID, orthologViews, View.OrthologyCacher.class, CacheAlliance.SPECIES_SPECIES_ORTHOLOGY);
        });

        status = new CacheStatus(CacheAlliance.SPECIES_SPECIES_ORTHOLOGY);
        //status.setNumberOfEntities(allExpression.size());

        Map<String, Integer> speciesSpeciesStatsInt = new TreeMap<>();
        speciesToSpeciesMap.forEach((speciesID, orthology) -> speciesSpeciesStatsInt.put(speciesID, orthology.size()));

        status.setSpeciesStats(speciesSpeciesStatsInt);
        setCacheStatus(status);

        geneRepository.clearCache();
    }

    private List<String> getPredictionNotCalled(OrthologView view) {
        List<String> usedNames = new ArrayList<>(view.getPredictionMethodsMatched());
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
