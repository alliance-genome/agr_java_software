package org.alliancegenome.core.translators.document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.CrossReferenceDocletTranslator;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.doclet.GenomeLocationDoclet;
import org.alliancegenome.es.index.site.document.GeneDocument;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.SecondaryId;
import org.alliancegenome.neo4j.entity.node.Synonym;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;

public class GeneTranslator extends EntityDocumentTranslator<Gene, GeneDocument> {

    //private final Logger log = LogManager.getLogger(getClass());
    
    private static CrossReferenceDocletTranslator crossReferenceTranslator = new CrossReferenceDocletTranslator();

    @Override
    protected GeneDocument entityToDocument(Gene entity, int translationDepth) {
        //log.info(entity);

        GeneDocument document = new GeneDocument();

        document.setCategory("gene");

        document.setDataProvider(entity.getDataProvider());
        document.setDescription(entity.getDescription());

        document.setAutomatedGeneSynopsis(entity.getAutomatedGeneSynopsis());
        document.setGeneSynopsis(entity.getGeneSynopsis());
        document.setGeneSynopsisUrl(entity.getGeneSynopsisUrl());
        document.setGeneticEntityExternalUrl(entity.getGeneticEntityExternalUrl());

        document.setModCrossRefCompleteUrl(entity.getModCrossRefCompleteUrl());
        document.setModLocalId(entity.getModLocalId());
        document.setModGlobalCrossRefId(entity.getModGlobalCrossRefId());
        document.setModGlobalId(entity.getModGlobalId());
        if (entity.getName() == null)
            document.setName(entity.getSymbol());
        else
            document.setName(entity.getName());
        document.setNameKey(entity.getNameKey());
        document.setPrimaryKey(entity.getPrimaryKey());
        document.setDateProduced(entity.getDateProduced());
        document.setTaxonId(entity.getTaxonId());


        if (entity.getCreatedBy() != null) {
            document.setRelease(entity.getCreatedBy().getRelease());
        }
        if (entity.getSpecies() != null) {
            document.setSpecies(entity.getSpecies().getName());
        }

        addSecondaryIds(entity, document);
        addSynonyms(entity, document);

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

        if (entity.getGenomeLocations() != null) {
            List<GenomeLocationDoclet> gllist = new ArrayList<>();
            for (GenomeLocation location : entity.getGenomeLocations()) {
                GenomeLocationDoclet loc = new GenomeLocationDoclet(
                        location.getStart(),
                        location.getEnd(),
                        location.getAssembly(),
                        location.getStrand(),
                        location.getChromosome().getPrimaryKey());

                gllist.add(loc);
            }
            document.setGenomeLocations(gllist);
        }

        if (entity.getCrossReferences() != null) {
            document.setCrossReferencesMap(
                    entity.getCrossReferences().stream()
                            .map(crossReference -> {
                                return crossReferenceTranslator.translate(crossReference);
                            })
                            .collect(Collectors.groupingBy(CrossReferenceDoclet::getType, Collectors.toList())));
        }

        return document;
    }

}
