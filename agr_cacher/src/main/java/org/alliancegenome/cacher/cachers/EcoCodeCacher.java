package org.alliancegenome.cacher.cachers;

import java.util.List;
import java.util.Map;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.view.View;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EcoCodeCacher extends Cacher {

	private static DiseaseRepository diseaseRepository;

	@Override
	protected void init() {
		diseaseRepository = new DiseaseRepository();
	}
	
	@Override
	protected void cache() {

		// dej primary key, list of ECO terms
		Map<String, List<ECOTerm>> allEcos = diseaseRepository.getEcoTermMap();
		
		final Class<View.DiseaseCacher> classView = View.DiseaseCacher.class;

		allEcos.forEach((key, ecoTerms) -> cacheService.putCacheEntry(key, ecoTerms, classView, CacheAlliance.ECO_MAP));
		log.info("Retrieved " + String.format("%,d", allEcos.size()) + " EcoTerm mappings");

		CacheStatus status = new CacheStatus(CacheAlliance.ECO_MAP);
		status.setNumberOfEntities(allEcos.size());
		setCacheStatus(status);

	}

	@Override
	public void close() {
		diseaseRepository.close();
	}

}