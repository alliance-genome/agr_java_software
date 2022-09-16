package org.alliancegenome.cacher.cachers;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.view.View;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ClosureCacher extends Cacher {

	private static DiseaseRepository diseaseRepository;

	@Override
	protected void init() {
		diseaseRepository = new DiseaseRepository();
	}
	
	@Override
	protected void cache() {
	
		Map<String, Set<String>> closure = diseaseRepository.getDOClosureChildMapping();
	
		final Class<View.DiseaseCacher> classView = View.DiseaseCacher.class;
		closure.forEach((parent, children) -> cacheService.putCacheEntry(parent, new ArrayList(children), classView, CacheAlliance.CLOSURE_MAP));
	
		log.info("Retrieved " + String.format("%,d", closure.size()) + " closure parents");
		CacheStatus statusClosure = new CacheStatus(CacheAlliance.CLOSURE_MAP);
		statusClosure.setNumberOfEntities(closure.size());
		setCacheStatus(statusClosure);

	}

	@Override
	public void close() {
		diseaseRepository.close();
	}

}
