package org.alliancegenome.core.translators.document;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.CrossReferenceDocletTranslator;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Synonym;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseTranslator extends EntityDocumentTranslator<DOTerm, SearchableItemDocument> {

    private static CrossReferenceDocletTranslator crossReferenceTranslator = new CrossReferenceDocletTranslator();

    private final Logger log = LogManager.getLogger(getClass());

    @Override
    protected SearchableItemDocument entityToDocument(DOTerm entity, int translationDepth) {
        return getTermDiseaseDocument(entity);
    }

    private SearchableItemDocument getTermDiseaseDocument(DOTerm doTerm) {
        SearchableItemDocument document = new SearchableItemDocument();

        document.setCategory("disease");

        document.setPrimaryKey(doTerm.getPrimaryKey());
        document.setName(doTerm.getName());
        document.setNameKey(doTerm.getName());
        document.setDefinition(doTerm.getDefinition());

        if (doTerm.getSynonyms() != null) {
            List<String> synonymList = doTerm.getSynonyms().stream()
                    .map(Synonym::getPrimaryKey)
                    .collect(Collectors.toList());
            document.setSynonyms(new HashSet<>(synonymList));
        }
        // add CrossReferences
        if (doTerm.getCrossReferences() != null) {
            document.setCrossReferencesMap(doTerm.getCrossReferences()
                    .stream()
                    .map(crossReference -> crossReferenceTranslator.translate(crossReference))
                    .collect(Collectors.groupingBy(CrossReferenceDoclet::getType, Collectors.toList())));
        }

        return document;
    }

}
