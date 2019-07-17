package org.alliancegenome.cacher.cachers.db;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.AllianceCacheManager;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cacher.cachers.Cacher;
import org.alliancegenome.core.service.DiseaseAnnotationSorting;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.ehcache.Cache;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Log4j2
public class DiseaseDBCacher extends Cacher {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();

    protected void cache() {
        Set<DiseaseEntityJoin> joinList = diseaseRepository.getAllDiseaseEntityJoins();
        if (joinList == null)
            return;

        // grouping orthologous records
        List<DiseaseAnnotation> summaryList = new ArrayList<>();

///        DiseaseRibbonService diseaseRibbonService = new DiseaseRibbonService();

        List<DiseaseAnnotation> allDiseaseAnnotations = joinList.stream()
                .map(diseaseEntityJoin -> {
                    DiseaseAnnotation document = new DiseaseAnnotation();
                    document.setGene(diseaseEntityJoin.getGene());
                    document.setFeature(diseaseEntityJoin.getAllele());
                    document.setDisease(diseaseEntityJoin.getDisease());
                    document.setSource(diseaseEntityJoin.getSource());
                    document.setAssociationType(diseaseEntityJoin.getJoinType());
                    document.setSortOrder(diseaseEntityJoin.getSortOrder());
                    Gene orthologyGene = diseaseEntityJoin.getOrthologyGene();
                    if (orthologyGene != null) {
                        document.setOrthologyGene(orthologyGene);
                        document.addOrthologousGene(orthologyGene);
// for memory savings reason use cached gene objects.
//                        document.setOrthologyGene(geneCacheRepository.getGene(orthologyGene.getPrimaryKey()));
                    }
                    List<Publication> publicationList = diseaseEntityJoin.getPublicationEvidenceCodeJoin().stream()
                            .map(PublicationEvidenceCodeJoin::getPublication).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
                    document.setPublications(publicationList.stream().distinct().collect(Collectors.toList()));
                    // work around as I cannot figure out how to include the ECOTerm in the overall query without slowing down the performance.
/*
                    Set<ECOTerm> evidences = diseaseEntityJoin.getPublicationEvidenceCodeJoin().stream()
                            .map(PublicationEvidenceCodeJoin::getEcoCode)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
*/
                    List<ECOTerm> evidences = diseaseRepository.getEcoTerm(diseaseEntityJoin.getPublicationEvidenceCodeJoin());
                    document.setEcoCodes(evidences);
/*
                    Set<String> slimId = diseaseRibbonService.getSlimId(diseaseEntityJoin.getDisease().getPrimaryKey());
                    document.setParentIDs(slimId);
*/
                    return document;
                })
                .collect(toList());

        // default sorting
        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        allDiseaseAnnotations.sort(sorting.getComparator(SortingField.DEFAULT, Boolean.TRUE));

        int currentHashCode = 0;
        for (DiseaseAnnotation document : allDiseaseAnnotations) {
            int hash = document.hashCode();
            if (currentHashCode == hash) {
                summaryList.get(summaryList.size() - 1).addOrthologousGene(document.getOrthologyGene());
            } else {
                summaryList.add(document);
            }
            currentHashCode = hash;
        }


        log.info("Retrieved " + String.format("%,d", allDiseaseAnnotations.size()) + " annotations");
        long startCreateHistogram = System.currentTimeMillis();
        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureMapping();
        log.info("Number of Disease IDs: " + closureMapping.size());
        final Set<String> allIDs = closureMapping.keySet();

        long start = System.currentTimeMillis();
        // loop over all disease IDs (termID)
        // and store the annotations in a map for quick retrieval
        Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = new HashMap<>();
        Map<String, List<DiseaseAnnotation>> diseaseAnnotationSummaryMap = new HashMap<>();
        allIDs.forEach(termID -> {
            Set<String> allDiseaseIDs = closureMapping.get(termID);
            List<DiseaseAnnotation> joins = allDiseaseAnnotations.stream()
                    .filter(join -> allDiseaseIDs.contains(join.getDisease().getPrimaryKey()))
                    .collect(Collectors.toList());
            diseaseAnnotationMap.put(termID, joins);

            List<DiseaseAnnotation> summaryJoins = summaryList.stream()
                    .filter(join -> allDiseaseIDs.contains(join.getDisease().getPrimaryKey()))
                    .collect(Collectors.toList());
            diseaseAnnotationSummaryMap.put(termID, summaryJoins);
        });

        System.out.println("Time populating diseaseAnnotationMap:  " + ((System.currentTimeMillis() - start) / 1000) + " s");

        // group by gene IDs
        Map<String, List<DiseaseAnnotation>> diseaseAnnotationExperimentGeneMap = new HashMap<>();
        // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
        Map<String, List<DiseaseAnnotation>> diseaseAnnotationOrthologGeneMap = new HashMap<>();

        diseaseAnnotationExperimentGeneMap = allDiseaseAnnotations.stream()
                .filter(annotation -> annotation.getSortOrder() < 10)
                .collect(groupingBy(o -> o.getGene().getPrimaryKey(), Collectors.toList()));

        diseaseAnnotationOrthologGeneMap = allDiseaseAnnotations.stream()
                .filter(annotation -> annotation.getSortOrder() == 10)
                .collect(groupingBy(o -> o.getGene().getPrimaryKey(), Collectors.toList()));

        log.info("Number of Disease IDs in disease Map: " + diseaseAnnotationMap.size());
        log.info("Time to create annotation  list: " + (System.currentTimeMillis() - startCreateHistogram) / 1000);
        diseaseRepository.clearCache();

        Cache<String, ArrayList> cache = AllianceCacheManager.getCacheSpace(CacheAlliance.DISEASE_ANNOTATION);
        for (Map.Entry<String, List<DiseaseAnnotation>> entry : diseaseAnnotationMap.entrySet()) {
            cache.put(entry.getKey(), new ArrayList(entry.getValue()));
        }

        Cache<String, ArrayList> cacheGene = AllianceCacheManager.getCacheSpace(CacheAlliance.GENE_DISEASE_ANNOTATION);
        for (Map.Entry<String, List<DiseaseAnnotation>> entry : diseaseAnnotationExperimentGeneMap.entrySet()) {
            cacheGene.put(entry.getKey(), new ArrayList(entry.getValue()));
        }


    }

}

