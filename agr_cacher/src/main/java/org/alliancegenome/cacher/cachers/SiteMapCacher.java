package org.alliancegenome.cacher.cachers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.View;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SiteMapCacher extends Cacher {
	
	private Integer batchSize = 15000;
	private GeneRepository geneRepository;
	private AlleleRepository alleleRepository;
	private DiseaseRepository diseaseRepository;


	@Override
	protected void init() {
		geneRepository = new GeneRepository();
		alleleRepository = new AlleleRepository();
		diseaseRepository = new DiseaseRepository();
	}
	
	@Override
	protected void cache() {
		
		startProcess("geneRepository.getAllGeneKeys");
		List<String> geneKeyList = geneRepository.getAllGeneKeys();
		log.debug("Gene List Size: " + geneKeyList.size());
		cacheSiteMap(geneKeyList, CacheAlliance.SITEMAP_GENE);
		finishProcess();
		
		List<String> alleleKeyList = alleleRepository.getAllAlleleKeys();
		log.debug("Allele List Size: " + alleleKeyList.size());
		cacheSiteMap(alleleKeyList, CacheAlliance.SITEMAP_ALLELE);
		finishProcess();

		startProcess("diseaseRepository.getAllDiseaseWithAnnotationsKeys");
		Set<String> diseaseKeyList = diseaseRepository.getAllDiseaseWithAnnotationsKeys();
		log.debug("Disease List Size: " + diseaseKeyList.size());
		cacheSiteMap(diseaseKeyList, CacheAlliance.SITEMAP_DISEASE);
		finishProcess();

	}
	
	private void cacheSiteMap(Iterable<String> list, CacheAlliance cache) {
		List<String> idList = new ArrayList<>();
		int c = 0;
		for(String id: list) {
			idList.add(id);
			if(idList.size() >= batchSize) {
				cacheService.putCacheEntry(String.valueOf(c), idList, View.Default.class, cache);
				idList.clear();
				c++;
			}
		}

		if(idList.size() > 0) {
			JsonResultResponse<String> result = new JsonResultResponse<>();
			result.setResults(new ArrayList<>(idList));
			cacheService.putCacheEntry(String.valueOf(c), idList, View.Default.class, cache);
			idList.clear();
		}
		
	}

	@Override
	public void close() {
		geneRepository.close();
		alleleRepository.close();
		diseaseRepository.close();
	}

}
