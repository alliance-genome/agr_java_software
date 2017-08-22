package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.gene.GeneDocument;
import org.alliancegenome.indexer.entity.Gene;

public class GeneTranslator extends EntityDocumentTranslator<Gene, GeneDocument> {

    @Override
    protected GeneDocument entityToDocument(Gene entity) {

        GeneDocument document = new GeneDocument();
        document.setPrimaryId(entity.getPrimaryKey());
        document.setSymbol(entity.getSymbol());
        return document;
    }

    @Override
    protected Gene doumentToEntity(GeneDocument doument) {

        return null;
    }

}
