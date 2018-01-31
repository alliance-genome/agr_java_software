package org.alliancegenome.indexer.translators;

import java.util.ArrayList;

import org.alliancegenome.indexer.document.FeatureDocument;
import org.alliancegenome.indexer.entity.node.Feature;
import org.alliancegenome.indexer.entity.node.SecondaryId;
import org.alliancegenome.indexer.entity.node.Synonym;

public class AlleleTranslator extends EntityDocumentTranslator<Feature, FeatureDocument> {

    @Override
    protected FeatureDocument entityToDocument(Feature entity, int translationDepth) {

        FeatureDocument allele = new FeatureDocument();

        //allele.setDataProvider(entity.getDataProvider());
        allele.setDateProduced(entity.getDateProduced());
        allele.setGlobalId(entity.getGlobalId());
        allele.setLocalId(entity.getLocalId());
        allele.setPrimaryKey(entity.getPrimaryKey());
        allele.setRelease(entity.getRelease());
        allele.setSpecies(entity.getSpecies().getName());
        allele.setSymbol(entity.getSymbol());

        if(translationDepth > 0) {

            // This code is duplicated in Gene and Feature should be pulled out into its own translator
            ArrayList<String> secondaryIds = new ArrayList<>();
            if (entity.getSecondaryIds() != null) {
                for (SecondaryId secondaryId : entity.getSecondaryIds()) {
                    secondaryIds.add(secondaryId.getName());
                }
            }
            allele.setSecondaryIds(secondaryIds);

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
            allele.setSynonyms(synonyms);

        }

        return allele;
    }

    @Override
    protected Feature documentToEntity(FeatureDocument doument, int translationDepth) {
        // We are not going to the database yet so will implement this when we need to
        return null;
    }

}
