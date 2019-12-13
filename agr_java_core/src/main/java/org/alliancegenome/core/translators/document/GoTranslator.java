package org.alliancegenome.core.translators.document;

import java.util.HashSet;
import java.util.Set;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.entity.node.Synonym;

public class GoTranslator extends EntityDocumentTranslator<GOTerm, SearchableItemDocument> {


    @Override
    protected SearchableItemDocument entityToDocument(GOTerm entity, int translationDepth) {
        //log.info(entity);
        SearchableItemDocument doc = new SearchableItemDocument();

        doc.setCategory("go");

        doc.setName(entity.getName());
        doc.setId(entity.getPrimaryKey());
        doc.setPrimaryKey(entity.getPrimaryKey());
        doc.setNameKey(entity.getNameKey());
        doc.setBranch(entity.getType());
        doc.setHref(entity.getHref());
        doc.setDefinition(entity.getDefinition());

        Set<String> synonyms = new HashSet<>();
        for(Synonym s: entity.getSynonyms()) {
            synonyms.add(s.getPrimaryKey());
        }
        doc.setSynonyms(synonyms);
        doc.setGenes(entity.getGeneNameKeys());
        doc.setAssociatedSpecies(entity.getSpeciesNames());

        return doc;
    }

}
