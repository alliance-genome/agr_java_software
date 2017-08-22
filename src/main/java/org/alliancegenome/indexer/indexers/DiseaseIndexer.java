package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.disease.DiseaseDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.alliancegenome.indexer.service.Neo4jESService;
import org.alliancegenome.indexer.translators.DiseaseToESDiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {


    private Logger log = LogManager.getLogger(getClass());

    private Neo4jESService<DOTerm> neo4jService = new Neo4jESService<>(DOTerm.class);
    private DiseaseToESDiseaseTranslator diseaseToSI = new DiseaseToESDiseaseTranslator();

    public DiseaseIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {

        Neo4jESService<DOTerm> neo4jService = new Neo4jESService<>(DOTerm.class);
        List<DOTerm> geneDiseaseList = neo4jService.getDiseasesWithGenes();
        List<DOTerm> geneDiseaseInfoList = neo4jService.getDiseaseInfo();
        Map<String, DOTerm> infoMap = geneDiseaseInfoList.stream()
                .collect((Collectors.toMap(DOTerm::getPrimaryKey, id -> id)));
        List<DOTerm> geneDiseaseCompleteList = geneDiseaseList.stream()
                .peek(doTerm -> {
                    if (infoMap.get(doTerm.getPrimaryKey()) != null)
                        doTerm.setParents(infoMap.get(doTerm.getPrimaryKey()).getParents());
                })
                .collect(Collectors.toList());

        int diseaseCount = geneDiseaseCompleteList.size();
        int chunkSize = 1000;
        int pages = diseaseCount / chunkSize;

        log.debug("DiseaseCount: " + diseaseCount);


        if (diseaseCount > 0) {
            startProcess(pages, chunkSize, diseaseCount);
            for (int i = 0; i <= pages; i++) {
                addDocuments(diseaseToSI.translateEntities(geneDiseaseCompleteList));
                progress(i, pages, chunkSize);
            }
            finishProcess(diseaseCount);
        }

    }

}
