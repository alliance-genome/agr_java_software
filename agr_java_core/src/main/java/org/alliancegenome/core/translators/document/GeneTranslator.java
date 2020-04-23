package org.alliancegenome.core.translators.document;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.CrossReferenceDocletTranslator;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.doclet.GenomeLocationDoclet;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;

public class GeneTranslator extends EntityDocumentTranslator<Gene, SearchableItemDocument> {

    //private final Logger log = LogManager.getLogger(getClass());
    
    private static CrossReferenceDocletTranslator crossReferenceTranslator = new CrossReferenceDocletTranslator();

    @Override
    protected SearchableItemDocument entityToDocument(Gene entity, int translationDepth) {
        //log.info(entity);

        SearchableItemDocument document = new SearchableItemDocument();

        document.setCategory("gene");

        document.setDescription(entity.getDescription());

        document.setAutomatedGeneSynopsis(entity.getAutomatedGeneSynopsis());
        document.setGeneSynopsis(entity.getGeneSynopsis());
        document.setGeneSynopsisUrl(entity.getGeneSynopsisUrl());

        document.setModCrossRefCompleteUrl(entity.getModCrossRefCompleteUrl());
        document.setModLocalId(entity.getModLocalId());
        if (entity.getName() == null)
            document.setName(entity.getSymbol());
        else
            document.setName(entity.getName());
        document.setNameKey(entity.getNameKey());
        document.setPrimaryKey(entity.getPrimaryKey());

        if (entity.getSpecies() != null) {
            document.setSpecies(entity.getSpecies().getName());
        }


        if (entity.getSoTerm() != null) {
            document.setSoTermId(entity.getSoTerm().getPrimaryKey());
            document.setSoTermName(entity.getSoTerm().getName());
        }
        document.setSymbol(entity.getSymbol());

        document.setStrictOrthologySymbols(
                entity.getOrthoGenes().stream()
                        .filter(Orthologous::isStrictFilter)
                        .map(Orthologous::getGene2)
                        .map(Gene::getSymbol)
                        .collect(Collectors.toSet())
        );


        return document;
    }

}
