package org.alliancegenome.indexer.indexers;

import java.util.List;

import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.entity.node.DOTerm;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {

    private Logger log = LogManager.getLogger(getClass());

    private DiseaseRepository repo = new DiseaseRepository();
    private DiseaseTranslator diseaseTrans = new DiseaseTranslator();

    public DiseaseIndexer(String currnetIndex, TypeConfig config) {
        super(currnetIndex, config);
    }

    @Override
    public void index() {


        int diseaseCount = repo.getCount();
        int chunkSize = typeConfig.getFetchChunkSize();
        int pages = diseaseCount / chunkSize;

        log.debug("DiseaseCount: " + diseaseCount);

        if (diseaseCount > 0) {
            startProcess(pages, chunkSize, diseaseCount);
            for (int i = 0; i <= pages; i++) {
                List<DOTerm> geneDiseaseList = repo.getAllDiseaseTerms(i * chunkSize, chunkSize);
                addDocuments(diseaseTrans.translateEntities(geneDiseaseList));
                progress(i, pages, chunkSize);
            }
            finishProcess(diseaseCount);
        }

    }

}
