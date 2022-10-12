package org.alliancegenome.indexer.indexers.linkml;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections.CollectionUtils;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

@Log4j2
public class DiseaseAnnotationMLIndexer extends Indexer<SearchableItemDocument> {

    private GeneDiseaseAnnotationInterface geneApi = RestProxyFactory.createProxy(GeneDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
    private AlleleDiseaseAnnotationInterface alleleApi = RestProxyFactory.createProxy(AlleleDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
    private AgmDiseaseAnnotationInterface agmApi = RestProxyFactory.createProxy(AgmDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

    private static GeneRepository geneRepository = new GeneRepository();

    private static AlleleRepository alleleRepository = new AlleleRepository();

    public DiseaseAnnotationMLIndexer(IndexerConfig config) {
        super(config);
    }

    public void index() {
        indexAllele();
        indexAGMs();
        indexGenes();
    }

    public void indexGenes() {
        try {
            ProcessDisplayHelper display = new ProcessDisplayHelper(log, 10000);
            HashMap<String, Object> params = new HashMap<>();
            int batchSize = 300;
            SearchResponse<GeneDiseaseAnnotation> response = geneApi.find(0, batchSize, params);
            List<GeneDiseaseAnnotation> annotationBatch = removePrivateData(response.getResults());
            long total = response.getTotalResults();
            log.info("Total Gene annotations from persistent store: " + String.format("%,d", total));
            int pages = (int) (total / batchSize);
            display.startProcess("Starting Gene indexing", total);
            for (int page = 1; page < pages + 1; page++) {
                response = geneApi.find(page, batchSize, params);
                display.progressProcess((long) batchSize * (page + 1));
                annotationBatch.addAll(removePrivateData(response.getResults()));
            }
            display.finishProcess();
            log.info("Valid Gene annotations: " + String.format("%,d", annotationBatch.size()));
            List<String> geneIds = geneRepository.getAllGeneKeys();
            log.info("Valid Gene IDs: " + String.format("%,d", geneIds.size()));
            List<GeneDiseaseAnnotation> filteredAnnotation = annotationBatch.stream()
                .filter(agmDiseaseAnnotation -> geneIds.contains(agmDiseaseAnnotation.getSubject().getCurie()))
                .collect(Collectors.toList());
            log.info("Filtered Gene annotation IDs: " + String.format("%,d", filteredAnnotation.size()));
            log.info("Gene IDs not found in Neo4j:");
            annotationBatch.stream()
                .filter(agmDiseaseAnnotation -> !geneIds.contains(agmDiseaseAnnotation.getSubject().getCurie()))
                .collect(Collectors.toList()).forEach(agmDiseaseAnnotation -> log.info(agmDiseaseAnnotation.getSubject().getCurie()));

            log.info("Number of Disease Annotation: " + response.getTotalResults());
        } catch (Exception e) {
            log.error("Error while indexing...", e);
        }
    }

    public void indexAGMs() {
        try {
            ProcessDisplayHelper display = new ProcessDisplayHelper(log, 10000);
            HashMap<String, Object> params = new HashMap<>();
            int batchSize = 500;
            SearchResponse<AGMDiseaseAnnotation> response = agmApi.find(0, batchSize, params);
            List<AGMDiseaseAnnotation> annotationBatch = removePrivateData(response.getResults());
            long total = response.getTotalResults();
            log.info("Total AGM annotations from persistent store: " + String.format("%,d", total));
            int pages = (int) (total / batchSize);
            display.startProcess("Starting Allele indexing", total);
            for (int page = 1; page < pages + 1; page++) {
                response = agmApi.find(page, batchSize, params);
                display.progressProcess((long) batchSize * (page + 1));
                annotationBatch.addAll(removePrivateData(response.getResults()));
            }
            display.finishProcess();
            log.info("Valid AGM annotations: " + String.format("%,d", annotationBatch.size()));
            List<String> agmIds = geneRepository.getAllAgmKeys();
            log.info("Valid AGM annotation IDs: " + String.format("%,d", agmIds.size()));
            List<AGMDiseaseAnnotation> filteredAnnotation = annotationBatch.stream()
                .filter(agmDiseaseAnnotation -> agmIds.contains(agmDiseaseAnnotation.getSubject().getCurie()))
                .collect(Collectors.toList());
            log.info("Filtered AGM annotation IDs: " + String.format("%,d", filteredAnnotation.size()));
            log.info("AGM IDs not found in Neo4j:");
            annotationBatch.stream()
                .filter(agmDiseaseAnnotation -> !agmIds.contains(agmDiseaseAnnotation.getSubject().getCurie()))
                .collect(Collectors.toList()).forEach(agmDiseaseAnnotation -> log.info(agmDiseaseAnnotation.getSubject().getCurie()));
            log.info("Number of Disease Annotation: " + response.getTotalResults());
        } catch (Exception e) {
            log.error("Error while indexing...", e);
        }
    }

    public void indexAllele() {
        try {
            ProcessDisplayHelper display = new ProcessDisplayHelper(log, 10000);
            HashMap<String, Object> params = new HashMap<>();
            int batchSize = 300;
            SearchResponse<AlleleDiseaseAnnotation> response = alleleApi.find(0, batchSize, params);
            List<AlleleDiseaseAnnotation> annotationBatch = removePrivateData(response.getResults());
            long total = response.getTotalResults();
            log.info("Total Allele annotations from persistent store: " + String.format("%,d", total));
            int pages = (int) (total / batchSize);
            display.startProcess("Starting Allele indexing", total);
            for (int page = 1; page < pages + 1; page++) {
                response = alleleApi.find(page, batchSize, params);
                display.progressProcess((long) batchSize * (page + 1));
                if (CollectionUtils.isNotEmpty(response.getResults())) {
                    annotationBatch.addAll(removePrivateData(response.getResults()));
                }
            }
            display.finishProcess();
            log.info("Valid allele annotations: " + String.format("%,d", annotationBatch.size()));
            List<String> alleleds = alleleRepository.getAllAlleleIDs();
            log.info("Valid Allele IDs: " + String.format("%,d", alleleds.size()));
            List<AlleleDiseaseAnnotation> filteredAnnotation = annotationBatch.stream()
                .filter(agmDiseaseAnnotation -> alleleds.contains(agmDiseaseAnnotation.getSubject().getCurie()))
                .collect(Collectors.toList());
            log.info("Filtered Allele annotation IDs: " + String.format("%,d", filteredAnnotation.size()));
            log.info("Allele IDs not found in Neo4j:");
            annotationBatch.stream()
                .filter(agmDiseaseAnnotation -> !alleleds.contains(agmDiseaseAnnotation.getSubject().getCurie()))
                .collect(Collectors.toList()).forEach(agmDiseaseAnnotation -> log.info(agmDiseaseAnnotation.getSubject().getCurie()));
            log.info("Number of Disease Annotation: " + response.getTotalResults());
        } catch (Exception e) {
            log.error("Error while indexing...", e);
        }
    }

    private static <T extends DiseaseAnnotation> List<T> removePrivateData(List<T> annotations) {
        return annotations.stream()
            .filter(diseaseAnnotation -> !diseaseAnnotation.getInternal())
            .collect(Collectors.toList());
    }

    @Override
    protected void startSingleThread(LinkedBlockingDeque<String> queue) {

    }


    public static void main(String[] args) {
        DiseaseAnnotationMLIndexer indexer = new DiseaseAnnotationMLIndexer(IndexerConfig.DiseaseAnnotationMlIndexer);
        //SearchResponse<GeneDiseaseAnnotation> response = indexer.geneApi.find(0, 100, new HashMap<>());
        //indexer.indexAGMs();
        indexer.indexAllele();
        //indexer.indexGenes();


        log.info("HTTP code: ");
        System.exit(0);
    }

}
