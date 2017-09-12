package org.alliancegenome.indexer.indexers;


import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.DiseaseAnnotationDocument;
import org.alliancegenome.indexer.entity.node.DOTerm;
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

        List<DOTerm> diseaseTermsWithAnnotations = repo.getDiseaseTermsWithAnnotations();
        log.info("Disease Records with annotations: " + diseaseTermsWithAnnotations.size());
        addDocuments(diseaseTrans.translateAnnotationEntities(diseaseTermsWithAnnotations, 1));

    }

}
