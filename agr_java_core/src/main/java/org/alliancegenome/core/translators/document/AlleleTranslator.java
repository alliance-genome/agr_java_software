package org.alliancegenome.core.translators.document;

import java.util.HashSet;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.CrossReferenceDocletTranslator;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.Allele;

public class AlleleTranslator extends EntityDocumentTranslator<Allele, SearchableItemDocument> {

    private static CrossReferenceDocletTranslator crossReferenceDocletTranslator = new CrossReferenceDocletTranslator();

    @Override
    protected SearchableItemDocument entityToDocument(Allele entity, int translationDepth) {

        SearchableItemDocument document = new SearchableItemDocument();

        document.setCategory("alteration");
        document.setAlterationType("allele");
        document.setGlobalId(entity.getGlobalId());
        document.setLocalId(entity.getLocalId());
        document.setPrimaryKey(entity.getPrimaryKey());
        document.setSymbol(entity.getSymbol());
        document.setSymbolText(entity.getSymbolText());
        document.setName(entity.getSymbol());
        document.setNameKey(entity.getSymbolTextWithSpecies());
        if (entity.getSpecies() != null) {
            document.setSpecies(entity.getSpecies().getName());
        }

        document.setSecondaryIds(new HashSet<>(entity.getSecondaryIdsList()));
        document.setSynonyms(new HashSet<>(entity.getSynonymList()));

        return document;
    }

}
