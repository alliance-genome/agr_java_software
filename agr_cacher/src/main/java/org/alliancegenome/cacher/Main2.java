package org.alliancegenome.cacher;

import java.io.IOException;


public class Main2 {

    public static void main(String[] args) throws IOException {

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
/*
        String id = "ZFIN:ZDB-GENE-001103-1";
        AllianceCacheManager<Allele> resultAllele = new AllianceCacheManager<>();
        List<Allele> resultsAllele = resultAllele.getResultList(id, CacheAlliance.ALLELE);
        log.info("Allele: " + resultsAllele.size());
*/

/*
        AllianceCacheManager<OrthologView> resultOrtho = new AllianceCacheManager<>();
        List<OrthologView> results = resultOrtho.getResultList(id, CacheAlliance.ORTHOLOGY);
        log.info("Ortho: " + results.size());

*/

/*
        try {

            ExpressionAllianceCacheManager managerExp = new ExpressionAllianceCacheManager();
            List<ExpressionDetail> resultsExp = managerExp.getExpressions("MGI:109583", View.Expression.class);
            log.info("Expression: " + resultsExp.size());

        } catch (Exception ignored) {
        } finally {
            AllianceCacheManager.close();
        }
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


    }

}
