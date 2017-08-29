package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.Gene;

public class SimpleGeneTranslator extends EntityDocumentTranslator<Gene, GeneDocument> {

    @Override
    protected GeneDocument entityToDocument(Gene entity) {

        GeneDocument document = new GeneDocument();
        document.setPrimaryId(entity.getPrimaryKey());
        document.setSymbol(entity.getSymbol());
        document.setSpecies(entity.getSpecies().getName());
        document.setTaxonId(entity.getTaxonId());
        return document;
    }

    @Override
    protected Gene documentToEntity(GeneDocument document) {

        return null;
    }

}
