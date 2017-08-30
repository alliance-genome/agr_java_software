package org.alliancegenome.indexer.indexers;


import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {

    private Logger log = LogManager.getLogger(getClass());

    private DiseaseRepository repo = new DiseaseRepository();
    private DiseaseTranslator diseaseTrans = new DiseaseTranslator();

    public DiseaseIndexer(String currnetIndex, TypeConfig config) {
        super(currnetIndex, config);
    }

    @Override
    public void index() {

        List<DOTerm> geneDiseaseList = repo.getAllDiseaseTermsWithAnnotations();

        int diseaseCount = geneDiseaseList.size();
        int chunkSize = typeConfig.getFetchChunkSize();
        int pages = diseaseCount / chunkSize;

        log.debug("DiseaseCount: " + diseaseCount);


        if (diseaseCount > 0) {
            startProcess(pages, chunkSize, diseaseCount);
            for (int i = 0; i <= pages; i++) {
                addDocuments(diseaseTrans.translateEntities(geneDiseaseList));
                progress(i, pages, chunkSize);
            }
            finishProcess(diseaseCount);
        }

    }

}
