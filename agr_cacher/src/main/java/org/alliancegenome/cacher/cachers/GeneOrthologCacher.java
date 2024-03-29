package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.HomologView;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.map.MultiKeyMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneOrthologCacher extends Cacher {

	private static GeneRepository geneRepository;
	private MultiKeyMap<String, Map<String, Set<String>>> geneGeneAlgorithm;
	private List<String> allMethods = new ArrayList<>();

	@Override
	protected void init() {
		geneRepository = new GeneRepository();
	}
	
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
		log.info("Algorithms: " + geneGeneAlgorithm.size());

		int orthologousRecords = geneList.stream().filter(gene -> gene.getOrthoGenes() != null).map(gene -> gene.getOrthoGenes().size()).mapToInt(Integer::intValue).sum();
		log.info("Total Number of Ortho Records: ", orthologousRecords);
		startProcess("create geneList into cache", orthologousRecords);

		List<HomologView> allOrthology = new ArrayList<>();
		geneList.stream().filter(gene -> gene.getOrthoGenes() != null).forEach(gene -> {
			Set<HomologView> orthologySet = gene.getOrthoGenes().stream()
					.map(orthologous -> {
						HomologView view = new HomologView();
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

			cacheService.putCacheEntry(gene.getPrimaryKey(), new ArrayList<>(orthologySet), View.OrthologyCacher.class, CacheAlliance.GENE_ORTHOLOGY);
			progressProcess();
		});
		finishProcess();

		// get homology cache by species
		
		startProcess("allOrthology.stream - group By o.getGene().getTaxonId()");
		Map<String, List<HomologView>> map = allOrthology.stream()
				.collect(groupingBy(o -> o.getGene().getTaxonId()));
		finishProcess();
		
		
/*
		startProcess("allOrthology orthologViews into cache", map.size());

		map.forEach((speciesID, orthologViews) -> {
			cacheService.putCacheEntry(speciesID, orthologViews, View.OrthologyCacher.class, CacheAlliance.SPECIES_ORTHOLOGY);
			progressProcess();
		});
		
		finishProcess();
*/

		CacheStatus status = new CacheStatus(CacheAlliance.SPECIES_ORTHOLOGY);
		//status.setNumberOfEntities(allExpression.size());

		Map<String, Integer> speciesStatsInt = new TreeMap<>();
		map.forEach((speciesID, orthology) -> speciesStatsInt.put(speciesID, orthology.size()));

		map.clear();
		
		status.setSpeciesStats(speciesStatsInt);
		setCacheStatus(status);

		startProcess("allOrthology.stream - group By getSpeciesSpeciesID");
		Map<String, List<HomologView>> speciesToSpeciesMap = allOrthology.stream()
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

	public String getSpeciesSpeciesID(HomologView o) {
		return o.getGene().getTaxonId() + ":" + o.getHomologGene().getTaxonId();
	}

	private List<String> getPredictionNotCalled(HomologView view) {
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

	@Override
	public void close() {
		geneRepository.close();
	}

}
