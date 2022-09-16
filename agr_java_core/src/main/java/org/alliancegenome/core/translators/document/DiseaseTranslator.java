package org.alliancegenome.core.translators.document;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Synonym;

public class DiseaseTranslator extends EntityDocumentTranslator<DOTerm, SearchableItemDocument> {

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
			if(document.getCrossReferences() == null) {
				document.setCrossReferences(new HashSet<String>());
			}
			document.getCrossReferences().addAll(doTerm.getCrossReferences().stream().map(CrossReference::getName).collect(Collectors.toSet()));
		}

		return document;
	}

}
