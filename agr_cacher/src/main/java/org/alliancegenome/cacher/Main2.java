package org.alliancegenome.cacher;

import org.alliancegenome.cache.*;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.View;

import java.io.IOException;
import java.util.List;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.CacheMode;
import org.alliancegenome.cache.AllianceCacheManager;
import org.alliancegenome.cache.CacheAlliance;
import org.apache.commons.collections4.CollectionUtils;
import org.ehcache.Cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        System.out.println("Allele: " + resultsAllele.size());
*/

/*
        AllianceCacheManager<OrthologView> resultOrtho = new AllianceCacheManager<>();
        List<OrthologView> results = resultOrtho.getResultList(id, CacheAlliance.ORTHOLOGY);
        System.out.println("Ortho: " + results.size());

*/

        try {

            ExpressionAllianceCacheManager managerExp = new ExpressionAllianceCacheManager();
            List<ExpressionDetail> resultsExp = managerExp.getExpressions("MGI:109583", View.Expression.class);
            System.out.println("Expression: " + resultsExp.size());

            OrthologyAllianceCacheManager managerOrtho = new OrthologyAllianceCacheManager();
            List<OrthologView> resultsOrtho = managerOrtho.getOrthology("MGI:109583", View.Orthology.class);
            System.out.println("Orthology: " + resultsOrtho.size());

            InteractionAllianceCacheManager managerInter = new InteractionAllianceCacheManager();
            List<InteractionGeneJoin> resultsInter = managerInter.getInteractions("MGI:109583", View.Interaction.class);
            System.out.println("Interactions: " + resultsInter.size());


            PhenotypeCacheManager managerPheno = new PhenotypeCacheManager();
            List<PhenotypeAnnotation> resultsPheno = managerPheno.getPhenotypeAnnotations("MGI:109583", View.PhenotypeAPI.class);
            System.out.println("Phenotype: " + resultsPheno.size());

            DiseaseAllianceCacheManager resultDisease = new DiseaseAllianceCacheManager();
            List<DiseaseAnnotation> resultsDisease = resultDisease.getDiseaseAnnotations("DOID:9952", View.DiseaseAnnotationSummary.class);
            System.out.println("Disease Annotations: " + resultsDisease.size());

            AlleleAllianceCacheManager managerAllele = new AlleleAllianceCacheManager();
            List<Allele> resultAllele = managerAllele.getAlleles("MGI:109583", View.GeneAllelesAPI.class);
            System.out.println("Alleles: " + resultAllele.size());
        } catch (Exception ignored) {
        } finally {
            AllianceCacheManager.close();
        }
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
