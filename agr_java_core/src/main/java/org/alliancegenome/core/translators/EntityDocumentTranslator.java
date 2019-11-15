package org.alliancegenome.core.translators;

import java.util.ArrayList;
import java.util.HashSet;

import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.SecondaryId;
import org.alliancegenome.neo4j.entity.node.Synonym;

public abstract class EntityDocumentTranslator<E extends Neo4jEntity, D extends ESDocument> {

    public D translate(E entity) {
        return translate(entity, 1);
    }

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

    protected void addSecondaryIds(GeneticEntity entity, SearchableItemDocument document) {
        ArrayList<String> secondaryIds = new ArrayList<>();
        if (entity.getSecondaryIds() != null) {
            for (SecondaryId secondaryId : entity.getSecondaryIds()) {
                secondaryIds.add(secondaryId.getName());
            }
        }
        document.setSecondaryIds(new HashSet<>(entity.getSecondaryIdsList()));
    }

    protected void addSynonyms(GeneticEntity entity, SearchableItemDocument document) {
        ArrayList<String> synonyms = new ArrayList<>();
        if (entity.getSynonyms() != null) {
            for (Synonym synonym : entity.getSynonyms()) {
                if (synonym.getPrimaryKey() != null) {
                    synonyms.add(synonym.getPrimaryKey());
                } else {
                    synonyms.add(synonym.getName());
                }
            }
        }
        document.setSynonyms(new HashSet<>(entity.getSynonymList()));
    }
}
