package org.alliancegenome.indexer.translators;

import java.util.ArrayList;

import org.alliancegenome.indexer.document.Document;
import org.alliancegenome.indexer.entity.Entity;

public abstract class EntityDocumentTranslator<E extends Entity, D extends Document> {

	public E translate(D doument) {
		return doumentToEntity(doument);
	}

	public D translate(E entity) {
		return entityToDocument(entity);
	}

	public Iterable<E> translateDouments(Iterable<D> douments) {
		ArrayList<E> entities = new ArrayList<E>();
		for(D document: douments) {
			entities.add(doumentToEntity(document));
		}
		return entities;
	}

	public Iterable<D> translateEntities(Iterable<E> entities) {
		ArrayList<D> douments = new ArrayList<D>();
		for(E entity: entities) {
			douments.add(entityToDocument(entity));
		}
		return douments;
	}

	protected abstract D entityToDocument(E entity);
	protected abstract E doumentToEntity(D doument);

}
