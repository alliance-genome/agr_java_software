package org.alliancegenome.indexer.translators;

import java.util.ArrayList;

import org.alliancegenome.indexer.document.ESDocument;
import org.alliancegenome.indexer.entity.Neo4jNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class EntityDocumentTranslator<E extends Neo4jNode, D extends ESDocument> {

	private Logger log = LogManager.getLogger(getClass());
	
	public E translate(D doument) {
		return documentToEntity(doument);
	}

	public D translate(E entity) {
		return entityToDocument(entity);
	}

	public Iterable<E> translateDocuments(Iterable<D> douments) {
		ArrayList<E> entities = new ArrayList<E>();
		for(D document: douments) {
			entities.add(documentToEntity(document));
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
	protected abstract E documentToEntity(D doument);

}
