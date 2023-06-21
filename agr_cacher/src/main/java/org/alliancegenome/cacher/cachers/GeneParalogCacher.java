package org.alliancegenome.cacher.cachers;

import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.HomologView;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.util.*;

import static java.util.stream.Collectors.*;

@Slf4j
public class GeneParalogCacher extends Cacher {

	private static GeneRepository geneRepository;
	private MultiKeyMap<String, Map<String, Set<String>>> geneGeneAlgorithm;
	private List<String> allMethods = new ArrayList<>();

	@Override
	protected void init() {
		geneRepository = new GeneRepository();
	}
	
	@Override
	protected void cache() {

		startProcess("geneRepository.getAllParalogyGenes");

		List<Gene> geneList = geneRepository.getAllParalogyGenes();

		finishProcess();
		if (geneList == null)
			return;

		log.info("Total Number of Genes: ", geneList.size());

		geneGeneAlgorithm = geneRepository.getAllParalogyGeneJoin();
		allMethods = geneRepository.getAllParalogyMethods();
		log.info("Algorithms: " + geneGeneAlgorithm.size());

		int paralogousRecords = geneList.stream().filter(gene -> gene.getParaGenes() != null).map(gene -> gene.getParaGenes().size()).mapToInt(Integer::intValue).sum();
		log.info("Total Number of Para Records: ", paralogousRecords);
		startProcess("create geneList into cache", paralogousRecords);

		List<HomologView> allParalogy = new ArrayList<>();
		geneList.stream().filter(gene -> gene.getParaGenes() != null).forEach(gene -> {
			Set<HomologView> paralogySet = gene.getParaGenes().stream()
					.map(paralogous -> {
						HomologView view = new HomologView();
						view.setGene(gene);
						view.setHomologGene(paralogous.getGene2());
						view.setBest(paralogous.getIsBestScore());
						view.setBestReverse(paralogous.getIsBestRevScore());
						if (paralogous.isStrictFilter()) {
							view.setStringencyFilter("stringent");
						} else if (paralogous.isModerateFilter()) {
							view.setStringencyFilter("moderate");
						}

						progressProcess();
						view.setPredictionMethodsMatched(getPredictionMatches(gene.getPrimaryKey(), paralogous.getGene2().getPrimaryKey()));
						view.setPredictionMethodsNotMatched(getPredictionNotMatches(gene.getPrimaryKey(), paralogous.getGene2().getPrimaryKey()));
						view.setPredictionMethodsNotCalled(getPredictionNotCalled(view));
						return view;
					})
					.collect(toSet());
			allParalogy.addAll(paralogySet);

			cacheService.putCacheEntry(gene.getPrimaryKey(), new ArrayList<>(paralogySet), View.OrthologyCacher.class, CacheAlliance.GENE_PARALOGY);
			progressProcess();
		});
		finishProcess();

		// get homology cache by species
		
		startProcess("allParalogy.stream - group By o.getGene().getTaxonId()");
		Map<String, List<HomologView>> map = allParalogy.stream()
				.collect(groupingBy(o -> o.getGene().getTaxonId()));
		finishProcess();
		
		
		CacheStatus status = new CacheStatus(CacheAlliance.SPECIES_ORTHOLOGY);

		Map<String, Integer> speciesStatsInt = new TreeMap<>();
		map.forEach((speciesID, paralogy) -> speciesStatsInt.put(speciesID, paralogy.size()));

		map.clear();
		
		status.setSpeciesStats(speciesStatsInt);
		setCacheStatus(status);

		geneRepository.clearCache();
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
