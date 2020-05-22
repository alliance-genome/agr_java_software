package org.alliancegenome.core.translators.document;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.Variant;

public class VariantTranslator extends EntityDocumentTranslator<Variant, SearchableItemDocument> {


    @Override
    protected SearchableItemDocument entityToDocument(Variant entity, int translationDepth) {
        SearchableItemDocument document = new SearchableItemDocument();

        document.setCategory("variant");

        document.setPrimaryKey(entity.getPrimaryKey());
        document.setName(entity.getName());
        document.setNameKey(entity.getHgvsNomenclature());

        //todo: populating species through allele or gene seems awkward, maybe we need a
        //      direct relationship?

        return document;

    }

}
