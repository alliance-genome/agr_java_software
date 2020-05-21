package org.alliancegenome.core.translators.document;

import java.util.HashSet;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;

public class ModelTranslator extends EntityDocumentTranslator<AffectedGenomicModel, SearchableItemDocument> {

    @Override
    protected SearchableItemDocument entityToDocument(AffectedGenomicModel entity, int translationDepth ) {

        SearchableItemDocument document = new SearchableItemDocument();

        document.setCategory("model");

        document.setPrimaryKey(entity.getPrimaryKey());
        document.setName(entity.getName());
        document.setNameKey(entity.getNameTextWithSpecies());
        document.setNameText(entity.getNameText());
        document.setLocalId(entity.getLocalId());
        document.setGlobalId(entity.getGlobalId());
        document.setModCrossRefCompleteUrl(entity.getModCrossRefCompleteUrl());
        if (entity.getSpecies() != null) {
            document.setSpecies(entity.getSpecies().getName());
        }

        document.setSecondaryIds(new HashSet<>(entity.getSecondaryIdsList()));
        document.setSynonyms(new HashSet<>(entity.getSynonymList()));

        return document;
    }

}
