package org.alliancegenome.core.translators.document;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.HTPDataset;
import org.apache.commons.collections.CollectionUtils;

public class HTPDatasetTranslator extends EntityDocumentTranslator<HTPDataset, SearchableItemDocument> {

    @Override
    protected SearchableItemDocument entityToDocument(HTPDataset entity, int translationDepth) {

        SearchableItemDocument document = new SearchableItemDocument();

        document.setCategory("dataset");
        document.setPrimaryKey(entity.getPrimaryKey());
        document.setName(entity.getTitle());
        document.setSummary(entity.getSummary());

        if (CollectionUtils.isNotEmpty(entity.getCrossReferences())) {
            document.setHref(entity.getCrossReferences().get(0).getCrossRefCompleteUrl());
        }

        return document;

    }


}
