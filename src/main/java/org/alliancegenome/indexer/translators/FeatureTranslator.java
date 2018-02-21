package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.FeatureDocument;
import org.alliancegenome.indexer.entity.node.Feature;
import org.alliancegenome.indexer.entity.node.SecondaryId;
import org.alliancegenome.indexer.entity.node.Synonym;

import java.util.ArrayList;

public class FeatureTranslator extends EntityDocumentTranslator<Feature, FeatureDocument> {

    private final GeneTranslator geneTranslator = new GeneTranslator();

    @Override
    protected FeatureDocument entityToDocument(Feature entity, int translationDepth) {

        FeatureDocument featureDocument = new FeatureDocument();

        //allele.setDataProvider(entity.getDataProvider());
        featureDocument.setDateProduced(entity.getDateProduced());
        featureDocument.setGlobalId(entity.getGlobalId());
        featureDocument.setLocalId(entity.getLocalId());
        featureDocument.setPrimaryKey(entity.getPrimaryKey());
        featureDocument.setRelease(entity.getRelease());
        featureDocument.setSymbol(entity.getSymbol());

        if (translationDepth > 0) {

            // This code is duplicated in Gene and Feature should be pulled out into its own translator
            ArrayList<String> secondaryIds = new ArrayList<>();
            if (entity.getSecondaryIds() != null) {
                for (SecondaryId secondaryId : entity.getSecondaryIds()) {
                    secondaryIds.add(secondaryId.getName());
                }
            }
            featureDocument.setSecondaryIds(secondaryIds);

            // This code is duplicated in Gene and Feature should be pulled out into its own translator
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
            featureDocument.setSynonyms(synonyms);
            featureDocument.setGeneDocument(geneTranslator.translate(entity.getGene(), translationDepth - 1));

        }


        return featureDocument;
    }

    @Override
    protected Feature documentToEntity(FeatureDocument doument, int translationDepth) {
        // We are not going to the database yet so will implement this when we need to
        return null;
    }

}
