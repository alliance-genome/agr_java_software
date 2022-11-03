package org.alliancegenome.core.translators;

import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.neo4j.entity.Neo4jEntity;

public abstract class EntityDocumentTranslator<E extends Neo4jEntity, D extends ESDocument> extends Translator<E, D> {

	@Override
	public D translate(E entity, int depth) {
		return entityToDocument(entity, depth);
	}

	public Iterable<D> translateEntities(Iterable<E> entities) {
		ArrayList<D> douments = new ArrayList<D>();
		for (E entity : entities) {
			douments.add(translate(entity, 1));
		}
		return douments;
	}

	protected abstract D entityToDocument(E entity, int translationDepth);

}
