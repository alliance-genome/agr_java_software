package org.alliancegenome.core.translators;

import java.util.ArrayList;

import org.alliancegenome.es.index.ESDoclet;
import org.alliancegenome.neo4j.entity.Neo4jEntity;

public abstract class EntityDocletTranslator<E extends Neo4jEntity, D extends ESDoclet> {

    public E translate(D doclet) {
        return translate(doclet, 1);
    }
    public D translate(E entity) {
        return translate(entity, 1);
    }
    
    public D translate(E entity, int depth) {
        return entityToDocument(entity, depth);
    }
    public E translate(D doclet, int depth) {
        return documentToEntity(doclet, depth);
    }

    public Iterable<E> translateDoclets(Iterable<D> doclets) {
        ArrayList<E> entities = new ArrayList<E>();
        for(D doclet: doclets) {
            entities.add(translate(doclet, 1));
        }
        return entities;
    }

    public Iterable<D> translateEntities(Iterable<E> entities) {
        ArrayList<D> doclets = new ArrayList<D>();
        for(E entity: entities) {
            doclets.add(translate(entity, 1));
        }
        return doclets;
    }

    protected abstract D entityToDocument(E entity, int translationDepth);
    protected abstract E documentToEntity(D doument, int translationDepth);

}
