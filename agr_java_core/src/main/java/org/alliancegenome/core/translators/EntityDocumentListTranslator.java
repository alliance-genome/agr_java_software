package org.alliancegenome.core.translators;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.neo4j.entity.Neo4jEntity;

public abstract class EntityDocumentListTranslator<E extends Neo4jEntity, D extends ESDocument> {

	public List<D> translate(E entity) {
		return translate(entity, 1);
	}

	public List<D> translate(E entity, int depth) {
		return entityToDocument(entity, depth);
	}

	public Iterable<D> translateEntities(Iterable<E> entities) {
		ArrayList<D> douments = new ArrayList<D>();
		for (E entity : entities) {
			douments.addAll(translate(entity, 1));
		}
		return douments;
	}

	protected abstract List<D> entityToDocument(E entity, int translationDepth);

}
