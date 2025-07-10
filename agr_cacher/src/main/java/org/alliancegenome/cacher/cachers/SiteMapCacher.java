package org.alliancegenome.cacher.cachers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.VariantRepository;
import org.alliancegenome.neo4j.view.View;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SiteMapCacher extends Cacher {

	private Integer batchSize = 15000;
	private GeneRepository geneRepository;
	private AlleleRepository alleleRepository;
	private DiseaseRepository diseaseRepository;
	private VariantRepository variantRepository;

	@Override
	protected void init() {
		geneRepository = new GeneRepository();
		alleleRepository = new AlleleRepository();
		diseaseRepository = new DiseaseRepository();
		variantRepository = new VariantRepository();
	}

	@Override
	protected void cache() {

		startProcess("geneRepository.getAllGeneKeys");
		List<String> geneKeyList = geneRepository.getAllGeneKeys();
		log.info("Gene List Size: " + geneKeyList.size());
		cacheSiteMap(geneKeyList, CacheAlliance.SITEMAP_GENE);
		finishProcess();

		List<String> alleleKeyList = alleleRepository.getAllAlleleKeys();
		log.info("Allele List Size: " + alleleKeyList.size());
		cacheSiteMap(alleleKeyList, CacheAlliance.SITEMAP_ALLELE);
		finishProcess();

		startProcess("diseaseRepository.getAllDiseaseWithAnnotationsKeys");
		Set<String> diseaseKeyList = diseaseRepository.getAllDiseaseWithAnnotationsKeys();
		log.info("Disease List Size: " + diseaseKeyList.size());
		cacheSiteMap(diseaseKeyList, CacheAlliance.SITEMAP_DISEASE);
		finishProcess();

		startProcess("variantRepository.getAllVariantKeys");
		List<String> variantKeyList = variantRepository.getAllVariantKeys();
		log.info("Variant List Size: " + variantKeyList.size());
		cacheSiteMap(variantKeyList, CacheAlliance.SITEMAP_VARIANT);
		finishProcess();

		startProcess("accession cache");
		cacheAccession("gene", geneKeyList, CacheAlliance.ACCESSION_MAP);
		cacheAccession("allele", alleleKeyList, CacheAlliance.ACCESSION_MAP);
		cacheAccession("disease", new ArrayList<String>(diseaseKeyList), CacheAlliance.ACCESSION_MAP);
		cacheAccession("variant", variantKeyList, CacheAlliance.ACCESSION_MAP);
		finishProcess();
	}

	private void cacheAccession(String type, List<String> keyList, CacheAlliance cache) {
		HashMap<String, String> batchMap = new HashMap<>();
		int totalProcessed = 0;
		int batchCount = 0;
		
		log.info("Starting batched bulk caching for " + type + " with " + keyList.size() + " keys");
		
		for (String key : keyList) {
			String url = "https://www.alliancegenome.org/" + type + "/" + key;
			try {
				String jsonValue = CacheService.mapper.writeValueAsString(url);
				batchMap.put(key, jsonValue);
				
				if (batchMap.size() >= batchSize) {
					cacheService.getCacheSpace(cache).putAll(batchMap);
					totalProcessed += batchMap.size();
					batchCount++;
					log.info("Completed batch {} for {}: {} urls cached. Total processed: {}", batchCount, type, batchMap.size(), totalProcessed);
					batchMap.clear();
				}
			} catch (Exception e) {
				log.error("Error serializing URL for key: " + key, e);
				throw new RuntimeException(e);
			}
		}
		
		if (!batchMap.isEmpty()) {
			cacheService.getCacheSpace(cache).putAll(batchMap);
			totalProcessed += batchMap.size();
			batchCount++;
			log.info("Completed final batch {} for {}: {} urls cached. Total processed: {}", batchCount, type, batchMap.size(), totalProcessed);
		}
		
		log.info("Bulk caching completed for " + type + ": " + totalProcessed + " keys in " + batchCount + " batches");
	}

	private void cacheSiteMap(Iterable<String> list, CacheAlliance cache) {
		List<String> idList = new ArrayList<>();
		int c = 0;
		for (String id : list) {
			idList.add(id);
			if (idList.size() >= batchSize) {
				cacheService.putCacheEntry(String.valueOf(c), idList, View.Default.class, cache);
				idList.clear();
				c++;
			}
		}

		if (idList.size() > 0) {
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
		variantRepository.close();
	}

}
