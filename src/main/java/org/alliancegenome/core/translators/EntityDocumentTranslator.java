package org.alliancegenome.core.translators;

import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.document.ESDocument;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.node.CrossReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class EntityDocumentTranslator<E extends Neo4jEntity, D extends ESDocument> {

    public E translate(D doument) {
        return translate(doument, 1);
    }

    public D translate(E entity) {
        return translate(entity, 1);
    }

    public D translate(E entity, int depth) {
        return entityToDocument(entity, depth);
    }

    public E translate(D document, int depth) {
        return documentToEntity(document, depth);
    }

    public Iterable<E> translateDocuments(Iterable<D> douments) {
        ArrayList<E> entities = new ArrayList<E>();
        for (D document : douments) {
            entities.add(translate(document, 1));
        }
        return entities;
    }

    public Iterable<D> translateEntities(Iterable<E> entities) {
        ArrayList<D> douments = new ArrayList<D>();
        for (E entity : entities) {
            douments.add(translate(entity, 1));
        }
        return douments;
    }

    protected abstract D entityToDocument(E entity, int translationDepth);
    protected abstract E documentToEntity(D doument, int translationDepth);

}
