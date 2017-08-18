package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.disease.DiseaseDocument;
import org.alliancegenome.indexer.document.gene.GeneDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class DiseaseToESDiseaseTranslator extends EntityDocumentTranslator<DOTerm, DiseaseDocument> {

    private GeneTranslator geneTranslator = new GeneTranslator();

    private Logger log = LogManager.getLogger(getClass());

    @Override
    protected DiseaseDocument entityToDocument(DOTerm entity) {

        log.info(entity);

        DiseaseDocument doc = new DiseaseDocument();

        doc.setPrimaryKey(entity.getPrimaryKey());
        doc.setName(entity.getName());

        if (entity.getGenes() != null) {

            List<GeneDocument> geneDocuments = entity.getGenes().stream()
                    .map(gene -> {
                        return geneTranslator.entityToDocument(gene);
                    })
                    .collect(Collectors.toList());
            entity.getGenes().forEach(gene -> {

            });
            doc.setGeneDocuments(geneDocuments);
        }
        return doc;
    }

    @Override
    protected DOTerm doumentToEntity(DiseaseDocument doument) {
        // TODO Auto-generated method stub
        return null;
    }

}
