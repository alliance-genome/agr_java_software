package org.alliancegenome.core.translators.document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.CrossReferenceDocletTranslator;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.document.AlleleDocument;
import org.alliancegenome.es.index.site.document.DiseaseDocument;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.Phenotype;
import org.alliancegenome.neo4j.entity.node.SecondaryId;
import org.alliancegenome.neo4j.entity.node.Synonym;
import org.apache.commons.collections4.CollectionUtils;

public class AlleleTranslator extends EntityDocumentTranslator<Allele, AlleleDocument> {

    private static GeneTranslator geneTranslator = new GeneTranslator();
    private static DiseaseTranslator diseaseTranslator = new DiseaseTranslator();
    private static CrossReferenceDocletTranslator crossReferenceDocletTranslator = new CrossReferenceDocletTranslator();

    @Override
    protected AlleleDocument entityToDocument(Allele entity, int translationDepth) {

        AlleleDocument alleleDocument = new AlleleDocument();

        //allele.setDataProvider(entity.getDataProvider());
        alleleDocument.setDateProduced(entity.getDateProduced());
        alleleDocument.setGlobalId(entity.getGlobalId());
        alleleDocument.setLocalId(entity.getLocalId());
        alleleDocument.setPrimaryKey(entity.getPrimaryKey());
        alleleDocument.setRelease(entity.getRelease());
        alleleDocument.setSymbol(entity.getSymbol());
        alleleDocument.setName(entity.getSymbol());
        alleleDocument.setNameKey(entity.getSymbolText());
        if (entity.getSpecies() != null) {
            alleleDocument.setSpecies(entity.getSpecies().getName());
        }

        if (entity.getCrossReferences() != null && entity.getCrossReferences().size() > 0) {
            CrossReference allele = entity.getCrossReferences().stream()
                    .filter(ref -> ref.getCrossRefType().equals("allele"))
                    .findFirst().orElse(null);
            if (allele != null) {
                alleleDocument.setModCrossRefFullUrl(allele.getCrossRefCompleteUrl());
                List<CrossReferenceDoclet> crossRefDoclets = entity.getCrossReferences().stream()
                        .map(crossReference -> crossReferenceDocletTranslator.translate(crossReference))
                        .collect(Collectors.toList());
                alleleDocument.setCrossReferenceList(crossRefDoclets);
            }
        }

        // This code is duplicated in Gene and Allele should be pulled out into its own translator
        ArrayList<String> secondaryIds = new ArrayList<>();
        if (entity.getSecondaryIds() != null) {
            for (SecondaryId secondaryId : entity.getSecondaryIds()) {
                secondaryIds.add(secondaryId.getName());
            }
        }
        alleleDocument.setSecondaryIds(secondaryIds);

        // This code is duplicated in Gene and Allele should be pulled out into its own translator
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
        alleleDocument.setSynonyms(synonyms);

        return alleleDocument;
    }

    @Override
    protected Allele documentToEntity(AlleleDocument doument, int translationDepth) {
        // We are not going to the database yet so will implement this when we need to
        return null;
    }

}
