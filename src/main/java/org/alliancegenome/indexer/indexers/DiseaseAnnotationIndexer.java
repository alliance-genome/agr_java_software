package org.alliancegenome.indexer.indexers;


import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.DiseaseAnnotationDocument;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseAnnotationIndexer extends Indexer<DiseaseAnnotationDocument> {

    private Logger log = LogManager.getLogger(getClass());

    private DiseaseRepository repo = new DiseaseRepository();
    private DiseaseTranslator diseaseTrans = new DiseaseTranslator();

    public DiseaseAnnotationIndexer(String currentIndex, TypeConfig config) {
        super(currentIndex, config);
    }

    @Override
    public void index() {

        int diseaseCount = repo.getCount();
        int chunkSize = typeConfig.getFetchChunkSize();
        int pages = diseaseCount / chunkSize;

        if (diseaseCount > 0) {
            startProcess(pages, chunkSize, diseaseCount);
            for (int i = 0; i <= pages; i++) {
                addDocuments(diseaseTrans.translateAnnotationEntities(repo.getDiseaseTermsWithAnnotations(i, chunkSize)));
                progress(i, pages, chunkSize);
            }
            finishProcess(diseaseCount);
        }

    }

}
