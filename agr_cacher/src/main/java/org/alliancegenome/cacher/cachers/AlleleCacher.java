package org.alliancegenome.cacher.cachers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.filedownload.model.DownloadFileSet;
import org.alliancegenome.core.filedownload.model.DownloadableFile;
import org.alliancegenome.core.filedownload.process.FileDownloadManager;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Log4j2
public class AlleleCacher extends Cacher {

    private static final AlleleRepository alleleRepository = new AlleleRepository();

    @Override
    protected void cache() {
        readAllFileMetaData();
        cacheSpeciesChromosome(SpeciesType.ZEBRAFISH.getTaxonID(), null);
        cacheSpeciesChromosome(SpeciesType.MOUSE.getTaxonID(), null);
        cacheSpeciesChromosome(SpeciesType.YEAST.getTaxonID(), null);
        cacheSpecies(SpeciesType.RAT.getTaxonID());
        cacheSpecies(SpeciesType.WORM.getTaxonID());
        cacheSpecies(SpeciesType.FLY.getTaxonID());
//        cacheSpecies(SpeciesType.HUMAN.getTaxonID());
        log.info(htpVariantMap);
    }

    private void cacheSpecies(String taxonID) {
        if (speciesChromosomeMap.get(taxonID) == null)
            return;
        speciesChromosomeMap.get(taxonID).forEach(s -> {
            cacheSpeciesChromosome(taxonID, s);
        });
    }


    private void cacheSpeciesChromosome(String taxonID, String chromosome) {
        readHtpFiles(taxonID, chromosome);
        if (StringUtils.isNotEmpty(chromosome))
            startProcess("get Alleles for: [" + taxonID + ", " + chromosome + "]");
        else
            startProcess("get Alleles for: [" + taxonID + "]");
        Set<Allele> allAlleles = alleleRepository.getAlleles(taxonID, chromosome);
        if (allAlleles == null)
            return;
        log.info("Number of Alleles: " + String.format("%,d", allAlleles.size()));
        // group by genes. This ignores alleles without gene associations
        Map<String, List<Allele>> map = allAlleles.stream()
                .filter(allele -> allele.getGene() != null)
                .collect(groupingBy(allele -> allele.getGene().getPrimaryKey()));

        allAlleles.forEach(allele -> allele.setPhenotypes(allele.getPhenotypes().stream()
                .sorted(Comparator.comparing(phenotype -> phenotype.getPhenotypeStatement().toLowerCase()))
                .collect(Collectors.toList())));
        finishProcess();

        populateCacheFromMap(map, View.GeneAllelesAPI.class, CacheAlliance.ALLELE_GENE);

        CacheStatus status = new CacheStatus(CacheAlliance.ALLELE_GENE);
        status.setNumberOfEntities(allAlleles.size());

        Map<String, List<Species>> speciesStats = allAlleles.stream()
                .map(GeneticEntity::getSpecies)
                .collect(groupingBy(Species::getName));

        Map<String, Integer> entityStats = new TreeMap<>();
        map.forEach((geneID, alleles) -> entityStats.put(geneID, alleles.size()));

        populateStatisticsOnStatus(status, entityStats, speciesStats);
        status.setCollectionEntity(Allele.class.getSimpleName());
        status.setJsonViewClass(View.GeneAllelesAPI.class.getSimpleName());
        setCacheStatus(status);

        // generate Allele detail records

        List<AlleleVariantSequence> alleleVariantSequences = allAlleles.stream()
                .map(allele -> {
                    if (CollectionUtils.isEmpty(allele.getVariants())) {
                        return List.of(new AlleleVariantSequence(allele, null, null));
                    } else {
                        return allele.getVariants().stream()
                                .map(variant -> {
                                    if (CollectionUtils.isEmpty(variant.getTranscriptLevelConsequence())) {
                                        return List.of(new AlleleVariantSequence(allele, variant, null));
                                    } else {
                                        return variant.getTranscriptLevelConsequence().stream()
                                                .map(transcriptLevelConsequence -> new AlleleVariantSequence(allele, variant, transcriptLevelConsequence))
                                                .collect(Collectors.toList());
                                    }
                                })
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList());
                    }
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        alleleVariantSequences.addAll(htpAlleleSequenceMap.values().stream().flatMap(Collection::parallelStream).collect(Collectors.toList()));
//        alleleVariantSequences = alleleVariantSequences.stream().filter(sequence -> sequence.getAllele().getPrimaryKey().equals("ZFIN:ZDB-ALT-130411-1942")).collect(Collectors.toList());

        Map<String, List<AlleleVariantSequence>> allRecordsMap = alleleVariantSequences.stream()
                .filter(sequence -> sequence.getAllele().getGene() != null)
                .collect(groupingBy(sequence -> sequence.getAllele().getGene().getPrimaryKey()));

        populateCacheFromMap(allRecordsMap, View.GeneAlleleVariantSequenceAPI.class, CacheAlliance.ALLELE_VARIANT_SEQUENCE_GENE);

/*
        status = new CacheStatus(CacheAlliance.ALLELE_VARIANT_SEQUENCE_GENE);
        status.setNumberOfEntities(alleleVariantSequences.size());

        speciesStats = alleleVariantSequences.stream()
                .map(AlleleVariantSequence::getAllele)
                .map(GeneticEntity::getSpecies)
                .collect(groupingBy(Species::getName));

        Map<String, Integer> entityStatsDetail = new TreeMap<>();
        speciesStats.forEach((geneID, alleles) -> entityStatsDetail.put(geneID, alleles.size()));

        populateStatisticsOnStatus(status, entityStatsDetail, speciesStats);
        status.setCollectionEntity(Allele.class.getSimpleName());
        status.setJsonViewClass(View.GeneAllelesAPI.class.getSimpleName());
        setCacheStatus(status);
*/

        // create allele-species index
        // <taxonID, List<Allele>>
        // include alleles without gene associations
        Map<String, List<Allele>> speciesMap = allAlleles.stream()
                .collect(groupingBy(allele -> allele.getSpecies().getPrimaryKey()));
        populateCacheFromMap(speciesMap, View.GeneAlleleVariantSequenceAPI.class, CacheAlliance.ALLELE_VARIANT_SEQUENCE_GENE);

        alleleRepository.clearCache();

    }

    private ConcurrentHashMap<String, ConcurrentLinkedDeque<AlleleVariantSequence>> htpAlleleSequenceMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> htpVariantMap = new ConcurrentHashMap<>();

    private Map<String, List<String>> speciesChromosomeMap = new HashMap<>();
    private DownloadFileSet downloadSet;

    private void readAllFileMetaData() {
        ConfigHelper.init();
        VariantConfigHelper.init();
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            downloadSet = mapper.readValue(getClass().getClassLoader().getResourceAsStream(VariantConfigHelper.getVariantCacherConfigFile()), DownloadFileSet.class);
            downloadSet.setDownloadPath(VariantConfigHelper.getVariantFileDownloadPath());

            if (VariantConfigHelper.isDownloading()) {
                FileDownloadManager fdm = new FileDownloadManager(downloadSet);
                fdm.start();
                fdm.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        downloadSet.getDownloadFileSet()
                .forEach(source -> source.getFileList()
                        .forEach(file -> {
                            List<String> chromosomes = speciesChromosomeMap.computeIfAbsent(source.getTaxonId(), k -> new ArrayList<>());
                            chromosomes.add(file.getChromosome());
                        }));
    }

    public void readHtpFiles(String taxonID, String chromosome) {
        htpAlleleSequenceMap = new ConcurrentHashMap<>();
        if (StringUtils.isEmpty(chromosome))
            return;
        try {
            ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getSourceDocumentCreatorThreads());
            DownloadableFile file = downloadSet.getDownloadFileSet().stream()
                    .filter(source -> source.getTaxonId().equals(taxonID))
                    .findAny().get()
                    .getFileList().stream()
                    .filter(downloadableFile -> downloadableFile.getChromosome().equals(chromosome))
                    .findAny().get();

            HtpVariantCreation creator = new HtpVariantCreation(taxonID, chromosome, file, htpAlleleSequenceMap);
            executor.submit(creator);
            Thread.sleep(10000);

            executor.shutdown();
            while (!executor.isTerminated()) {
                Thread.sleep(1000);
            }
            log.info("Size of HTP Gene with AlleleVariantSequence: " + String.format("%,d", htpAlleleSequenceMap.size()));
            long countAlleleVariants = htpAlleleSequenceMap.values().stream().flatMap(Collection::parallelStream).count();
            log.info("Size of HTP AlleleVariantSequence records: " +
                    String.format("%,d", (int) countAlleleVariants));

            ConcurrentHashMap<String, Long> taxonMap = htpVariantMap.get(taxonID);
            if (taxonMap == null) {
                taxonMap = new ConcurrentHashMap<>();
                htpVariantMap.put(taxonID, taxonMap);
            }
            taxonMap.put(chromosome, countAlleleVariants);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
