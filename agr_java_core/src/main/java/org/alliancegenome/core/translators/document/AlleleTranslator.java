package org.alliancegenome.core.translators.document;

import java.util.HashSet;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.Allele;

public class AlleleTranslator extends EntityDocumentTranslator<Allele, SearchableItemDocument> {

    @Override
    protected SearchableItemDocument entityToDocument(Allele entity, int translationDepth) {

        SearchableItemDocument document = new SearchableItemDocument();

        document.setCategory("allele");
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

    public void updateDocuments(Iterable<SearchableItemDocument> alleleDocuments) {
        for (SearchableItemDocument document : alleleDocuments) {
            updateDocument(document);
        }
    }

    //This method is for updating/setting fields after fields are populated by AlleleDocumentCache
    public void updateDocument(SearchableItemDocument document) {

        if (document.getVariants() != null && document.getVariants().size() == 1) {
            document.setAlterationType("allele with one variant");
        }

        if (document.getVariants() != null && document.getVariants().size() > 1) {
            document.setAlterationType(("allele with multiple variants"));
        }

    }

}
