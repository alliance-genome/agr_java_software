package org.alliancegenome.indexer.translators;

import java.util.ArrayList;

import org.alliancegenome.indexer.document.AlleleDocument;
import org.alliancegenome.indexer.entity.node.Allele;
import org.alliancegenome.indexer.entity.node.SecondaryId;
import org.alliancegenome.indexer.entity.node.Synonym;

public class AlleleTranslator extends EntityDocumentTranslator<Allele, AlleleDocument> {

    @Override
    protected AlleleDocument entityToDocument(Allele entity, int translationDepth) {

        AlleleDocument allele = new AlleleDocument();

        allele.setDataProvider(entity.getDataProvider());
        allele.setDateProduced(entity.getDateProduced());
        allele.setGlobalId(entity.getGlobalId());
        allele.setLocalId(entity.getLocalId());
        allele.setPrimaryKey(entity.getPrimaryKey());
        allele.setRelease(entity.getRelease());
        allele.setSpecies(entity.getSpecies().getName());
        allele.setSymbol(entity.getSymbol());

        if(translationDepth > 0) {

            // This code is duplicated in Gene and Allele should be pulled out into its own translator
            ArrayList<String> secondaryIds = new ArrayList<>();
            if (entity.getSecondaryIds() != null) {
                for (SecondaryId secondaryId : entity.getSecondaryIds()) {
                    secondaryIds.add(secondaryId.getName());
                }
            }
            allele.setSecondaryIds(secondaryIds);

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
            allele.setSynonyms(synonyms);

        }

        return allele;
    }

    @Override
    protected Allele documentToEntity(AlleleDocument doument, int translationDepth) {
        // We are not going to the database yet so will implement this when we need to
        return null;
    }

}
