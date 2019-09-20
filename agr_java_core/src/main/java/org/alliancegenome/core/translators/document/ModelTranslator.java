package org.alliancegenome.core.translators.document;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.es.index.site.document.ModelDocument;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;

public class ModelTranslator extends EntityDocumentTranslator<AffectedGenomicModel, ModelDocument> {

    @Override
    protected ModelDocument entityToDocument(AffectedGenomicModel entity, int translationDepth ) {

        ModelDocument document = new ModelDocument();

        document.setPrimaryId(entity.getPrimaryKey());
        document.setName(entity.getName());
        document.setLocalId(entity.getLocalId());
        document.setGlobalId(entity.getGlobalId());
        document.setModCrossRefCompleteUrl(entity.getModCrossRefCompleteUrl());
        if (entity.getSpecies() != null) {
            document.setSpecies(entity.getSpecies().getName());
        }

        addSecondaryIds(entity, document);
        addSynonyms(entity, document);

        return document;
    }

}
