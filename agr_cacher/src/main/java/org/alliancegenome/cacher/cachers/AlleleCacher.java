package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.groupingBy;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.*;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.filedownload.model.*;
import org.alliancegenome.core.filedownload.process.FileDownloadManager;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class AlleleCacher extends Cacher {

    private AlleleRepository alleleRepository;

    private ConcurrentHashMap<String, ConcurrentLinkedDeque<AlleleVariantSequence>> htpAlleleSequenceMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> htpVariantMap = new ConcurrentHashMap<>();

    private Map<String, List<String>> speciesChromosomeMap = new HashMap<>();
    private DownloadFileSet downloadSet;
    // <geneID, List<Allele>>
    private Map<String, List<Allele>> variantMap = new HashMap<>();
    
    
    public AlleleCacher() {
        
    }

    @Override
    protected void init() {
        alleleRepository = new AlleleRepository();
    }

    @Override
    protected void cache() {
        readAllFileMetaData();
        //cacheSpecies(SpeciesType.MOUSE.getTaxonID());
        cacheSpecies(SpeciesType.YEAST.getTaxonID());
        cacheSpecies(SpeciesType.RAT.getTaxonID());
        cacheSpecies(SpeciesType.ZEBRAFISH.getTaxonID());
        cacheSpeciesChromosome(SpeciesType.MOUSE.getTaxonID(), null);
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
        String speciesName = SpeciesType.getNameByID(taxonID);
        
        if (StringUtils.isNotEmpty(chromosome))
            startProcess("Retrieve Alleles for [" + speciesName + ", " + chromosome + "]");
        else
            startProcess("Retrieve Alleles for [" + speciesName + "]");
        
        Set<Allele> allAlleles = alleleRepository.getAlleles(taxonID, chromosome);
        if (allAlleles == null)
            return;
        
        log.info("Number of Alleles: " + String.format("%,d", allAlleles.size()));
        // group by genes. This ignores alleles without gene associations
        Map<String, List<Allele>> map = allAlleles.stream()
                .filter(allele -> allele.getGene() != null)
                .collect(groupingBy(allele -> allele.getGene().getPrimaryKey()));

        // add HTP variants to existing genes in map
        map.forEach((geneID, alleles) -> {
            if (variantMap.get(geneID) != null)
                alleles.addAll(variantMap.get(geneID));
        });
        // add HTP variants to non-existing genes in map
        variantMap.forEach(map::putIfAbsent);
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

        log.info("Number of AlleleVariantSequence records before adding HTP data: " + alleleVariantSequences.size());
        final List<AlleleVariantSequence> collect = htpAlleleSequenceMap.values().stream().flatMap(Collection::parallelStream).collect(Collectors.toList());
        log.info("Number of AlleleVariantSequence records from HTP data: " + collect.size());

        alleleVariantSequences.addAll(collect);
        log.info("Number of AlleleVariantSequence records after adding HTP data: " + alleleVariantSequences.size());

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

    private void readAllFileMetaData() {

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
        if (downloadSet.getDownloadFileSet() != null) {
            downloadSet.getDownloadFileSet()
                    .stream().filter(Objects::nonNull)
                    .forEach(source -> source.getFileList()
                            .forEach(file -> {
                                List<String> chromosomes = speciesChromosomeMap.computeIfAbsent(source.getTaxonId(), k -> new ArrayList<>());
                                chromosomes.add(file.getChromosome());
                            }));
        }
    }

    public void readHtpFiles(String taxonID, String chromosome) {
        htpAlleleSequenceMap = new ConcurrentHashMap<>();
        variantMap = new HashMap<>();
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

            ConcurrentHashMap<String, Long> taxonMap = htpVariantMap.computeIfAbsent(taxonID, s -> new ConcurrentHashMap<>());
            taxonMap.put(chromosome, countAlleleVariants);

            //group by geneID
/*
            Map<String, List<AlleleVariantSequence>> alleleVariantMap = htpAlleleSequenceMap.values().stream()
                    .flatMap(Collection::stream)
                    .collect(groupingBy(sequence -> sequence.getAllele().getGene().getPrimaryKey()));

            // TODO variant ID needs to be agreed on
            alleleVariantMap.forEach((geneID, alleleVariantSequences) -> {
                Map<String, List<AlleleVariantSequence>> varMap = alleleVariantSequences.stream()
                        .collect(groupingBy(sequence -> sequence.getVariant().getHgvsNomenclature()));
                varMap.forEach((variantName, sequences) -> {
                    Allele variantAllele = new Allele("", GeneticEntity.CrossReferenceType.VARIANT);
                    variantAllele.setUrl("");
                    variantAllele.setSymbol(sequences.get(0).getVariant().getHgvsNomenclature());
                    variantAllele.setSymbolText(sequences.get(0).getVariant().getHgvsNomenclature());
                    // adding all of them is not necessary, thus using the first entry
                    //variantAllele.setVariants(sequences.stream().map(AlleleVariantSequence::getVariant).collect(Collectors.toList()));
                    variantAllele.setVariants(List.of(sequences.get(0).getVariant()));
                    List<Allele> list = variantMap.computeIfAbsent(geneID, s -> new ArrayList<>());
                    list.add(variantAllele);
                });
            });
*/
            log.info("Number of Genes: " + variantMap.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        alleleRepository.close();
    }


}
