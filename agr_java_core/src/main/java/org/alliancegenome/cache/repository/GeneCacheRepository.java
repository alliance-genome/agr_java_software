package org.alliancegenome.cache.repository;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.HomologView;

import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.neo4j.view.ParalogView;

@Slf4j
@RequestScoped
public class GeneCacheRepository {

	@Inject CacheService cacheService;

	public List<HomologView> getAllOrthologyGenes(List<String> geneIDs) {
		List<HomologView> fullOrthologyList = new ArrayList<>();
		geneIDs.forEach(id -> {
			final List<HomologView> orthology = cacheService.getCacheEntries(id, CacheAlliance.GENE_ORTHOLOGY);
			if (orthology != null)
				fullOrthologyList.addAll(orthology);
		});

		return fullOrthologyList;
	}

	public List<ParalogView> getAllParalogyGenes(List<String> geneIDs) {
		List<ParalogView> fullOrthologyList = new ArrayList<>();
		geneIDs.forEach(id -> {
			final List<ParalogView> orthology = cacheService.getCacheEntries(id, CacheAlliance.GENE_PARALOGY);
			if (orthology != null)
				fullOrthologyList.addAll(orthology);
		});

		return fullOrthologyList;
	}

	public List<HomologView> getOrthologyBySpeciesSpecies(String taxonOne, String taxonTwo) {
		List<HomologView> fullOrthologyList = new ArrayList<>();
		final List<HomologView> orthology = cacheService.getCacheEntries(taxonOne + ":" + taxonTwo, CacheAlliance.SPECIES_SPECIES_ORTHOLOGY);
		if (orthology != null)
			fullOrthologyList.addAll(orthology);

		return fullOrthologyList;
	}

	public List<HomologView> getOrthologyBySpecies(List<String> taxonIDs) {

		List<HomologView> fullOrthologyList = new ArrayList<>();
		taxonIDs.forEach(id -> {
			final List<HomologView> orthology = cacheService.getCacheEntries(id, CacheAlliance.SPECIES_ORTHOLOGY);
			if (orthology != null)
				fullOrthologyList.addAll(orthology);
		});

		return fullOrthologyList;
	}
	
	/**
	 * retrieve InteractionGeneJoin list from cache directly by gene primary key, could be use to get DistinctFieldValueSupplementalData
	 * @param geneID
	 * @return list of InteractionGeneJoin
	 */
	public List<InteractionGeneJoin> getInteractions(String geneID){
		List<InteractionGeneJoin> interactionAnnotationList = cacheService.getCacheEntries(geneID, CacheAlliance.GENE_INTERACTION);
		return interactionAnnotationList ;
	}
}
