package org.alliancegenome.cacher;

import org.alliancegenome.cache.AllianceCacheManager;
import org.alliancegenome.cache.CacheAlliance;
import org.apache.commons.collections4.CollectionUtils;
import org.ehcache.Cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main2 {

    public static void main(String[] args) {


/*
        CacheManagerBuilder<PersistentCacheManager> with = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(new File(".", "ehcache-data")));
        with = with
                .withCache("genePhenotype", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, ArrayList.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder().disk(4, MemoryUnit.GB, true))
                );
        with = with
                .withCache("mycache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, ArrayList.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder().disk(4, MemoryUnit.GB, true))
                );
        PersistentCacheManager persistentCacheManager = with.build(true);
*/


/*
        PersistentCacheManager persistentCacheManager =
                with
                        .withCache("genePhenotype", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, ArrayList.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder().disk(4, MemoryUnit.GB, true))
                        )
                        .withCache("mycache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, ArrayList.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder().disk(4, MemoryUnit.GB, true))
                        )
                        .build(true);
*/

        //Cache<String, ArrayList> cache = persistentCacheManager.getCache("genePhenotype", String.class, ArrayList.class);
        Cache<String, ArrayList> cache = AllianceCacheManager.getCacheSpace(CacheAlliance.PHENOTYPE);
        Cache<String, ArrayList> cacheAllele = AllianceCacheManager.getCacheSpace(CacheAlliance.ALLELE);
        Cache<String, ArrayList> cacheInteraction = AllianceCacheManager.getCacheSpace(CacheAlliance.INTERACTION);
        Cache<String, ArrayList> cacheExpression = AllianceCacheManager.getCacheSpace(CacheAlliance.EXPRESSION);
        Cache<String, ArrayList> cacheDisease = AllianceCacheManager.getCacheSpace(CacheAlliance.DISEASE_ANNOTATION);
        Cache<String, ArrayList> cacheGeneDisease = AllianceCacheManager.getCacheSpace(CacheAlliance.GENE_DISEASE_ANNOTATION);
        Cache<String, ArrayList> cacheOrtho = AllianceCacheManager.getCacheSpace(CacheAlliance.ORTHOLOGY);
        List listPheno = cache.get("MGI:109583");
        List listAllel = cacheAllele.get("MGI:109583");
        List listInt = cacheInteraction.get("MGI:109583");
        List listExp = cacheExpression.get("MGI:109583");
        List listGeneDis = cacheGeneDisease.get("RGD:1593249");
        List listDisease = cacheDisease.get("DOID:0080120");

        System.out.println("Pheno: "+ CollectionUtils.size(listPheno));
        System.out.println("Allele: "+ CollectionUtils.size(listAllel));
        System.out.println("Interactions: "+ CollectionUtils.size(listInt));
        System.out.println("Expression: "+ CollectionUtils.size(listExp));
        System.out.println("Disease: "+ CollectionUtils.size(listDisease));
        System.out.println("Gene Disease: "+ CollectionUtils.size(listGeneDis));
        System.out.println("Gene Ortho: "+ CollectionUtils.size(cacheOrtho.get("MGI:109583")));

        //cacheOrtho.forEach(entry -> System.out.println(entry.getKey()));



//        Cache<String, ArrayList> cache1 = persistentCacheManager.getCache("mycache", String.class, ArrayList.class);

/*
        Cache<String, HashMap> myCache = persistentCacheManager.createCache("mycache",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, HashMap.class,
                        ResourcePoolsBuilder.heap(100)).build());

*/

/*
        HashMap<String, List<String>> map = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        list.add("Hellaodin");
        list.add("Werner");
        map.put("11231", list);
        map.put("MGI:109583", list);
        cache.put("genotype", list);
        Object value = cache.get("genotype");
*/

        AllianceCacheManager.close();


    }

}
