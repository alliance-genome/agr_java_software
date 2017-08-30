package org.alliancegenome.indexer.indexers;


import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.DiseaseAnnotationDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DiseaseAnnotationIndexer extends Indexer<DiseaseAnnotationDocument> {

    private Logger log = LogManager.getLogger(getClass());

    private DiseaseRepository repo = new DiseaseRepository();
    private DiseaseTranslator diseaseTrans = new DiseaseTranslator();

    public DiseaseAnnotationIndexer(String currentIndex, TypeConfig config) {
        super(currentIndex, config);
    }

    @Override
    public void index() {

        List<DOTerm> geneDiseaseList = repo.getDiseaseTermsWithAnnotations();

        int diseaseCount = geneDiseaseList.size();
        int chunkSize = typeConfig.getFetchChunkSize();
        int pages = diseaseCount / chunkSize;

        if (diseaseCount > 0) {
            startProcess(pages, chunkSize, diseaseCount);
            for (int i = 0; i <= pages; i++) {
                addDocuments(diseaseTrans.translateAnnotationEntities(geneDiseaseList));
                progress(i, pages, chunkSize);
            }
            finishProcess(diseaseCount);
        }

    }

}
