package org.alliancegenome.indexer.variant.es.managers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.filedownload.model.DownloadFileSet;
import org.alliancegenome.core.filedownload.model.DownloadSource;
import org.alliancegenome.core.filedownload.model.DownloadableFile;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class SourceDocumentCreationManager extends Thread {

    private DownloadFileSet downloadSet;
    AlleleRepository alleleRepository=new AlleleRepository();
    public SourceDocumentCreationManager(DownloadFileSet downloadSet) {
        this.downloadSet = downloadSet;
    }

    public void run() {

        try {
            
            ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getSourceDocumentCreatorThreads());
            for(DownloadSource source: downloadSet.getDownloadFileSet()) {
               // SourceDocumentCreationNew creator = new SourceDocumentCreationNew(source);
                Map<String, Map<String, List<Allele>>> chromosomeAllelesMap=new HashMap<>();
                for(DownloadableFile df: source.getFileList()) {
                    Map<String, List<Allele>> alleleMap = alleleRepository.getAllAllelesByTaxonNChromosome(source.getTaxonId(), df.getChromosome());
                    chromosomeAllelesMap.put(df.getChromosome(), alleleMap);
                }
            //    SourceDocumentCreation creator = new SourceDocumentCreation(source, chromosomeAllelesMap);
                  SourceDocumentCreationNew creator = new SourceDocumentCreationNew(source, chromosomeAllelesMap);
               //     creator.getLTPIndexObjects();
                executor.execute(creator);
            }
            
            log.info("SourceDocumentCreationManager shuting down executor: ");
            executor.shutdown();  
            while (!executor.isTerminated()) {
                Thread.sleep(1000);
            }
            log.info("SourceDocumentCreationManager executor shut down: ");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
