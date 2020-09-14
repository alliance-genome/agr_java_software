package org.alliancegenome.core.translators.document;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.HTPDataset;

public class HTPDatasetTranslator extends EntityDocumentTranslator<HTPDataset, SearchableItemDocument> {

    @Override
    protected SearchableItemDocument entityToDocument(HTPDataset entity, int translationDepth) {

        SearchableItemDocument document = new SearchableItemDocument();

        document.setCategory("dataset");
        document.setPrimaryKey(entity.getPrimaryKey());
        document.setName(entity.getPrimaryKey());
        document.setSummary(entity.getSummary());

        return document;

    }


}
