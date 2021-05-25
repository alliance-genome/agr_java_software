package org.alliancegenome.core.translators.document;

import java.util.*;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.core.translators.EntityDocumentListTranslator;
import org.alliancegenome.neo4j.entity.node.*;

public class AlleleTranslator extends EntityDocumentListTranslator<Allele, AlleleVariantSequence> {

    @Override
    protected List<AlleleVariantSequence> entityToDocument(Allele entity, int depth) {

        List<AlleleVariantSequence> docs = new ArrayList<>();
        
        for(Variant v: entity.getVariants()) {  
            AlleleVariantSequence document = new AlleleVariantSequence(entity, v, null);
            
            document.setCategory("allele");
            document.setAlterationType("allele");
            document.setGlobalId(entity.getGlobalId());
            document.setLocalId(entity.getLocalId());
            document.setPrimaryKey(entity.getPrimaryKey());
            document.setSymbol(entity.getSymbol());
            document.setSymbolText(entity.getSymbolText());
            document.setName(entity.getSymbol());
            document.setNameKey(entity.getSymbolTextWithSpecies());
            if (entity.getSpecies() != null) {
                document.setSpecies(entity.getSpecies().getName());
            }

            document.setSecondaryIds(new HashSet<>(entity.getSecondaryIdsList()));
            document.setSynonyms(new HashSet<>(entity.getSynonymList()));
            
            docs.add(document);
        }

        return docs;
    }

    public void updateDocuments(Iterable<AlleleVariantSequence> alleleDocuments) {
        for (AlleleVariantSequence document : alleleDocuments) {
            updateDocument(document);
        }
    }

    //This method is for updating/setting fields after fields are populated by AlleleDocumentCache
    public void updateDocument(AlleleVariantSequence document) {

        if (document.getVariants() != null && document.getVariants().size() == 1) {
            document.setAlterationType("allele with one variant");
        }

        if (document.getVariants() != null && document.getVariants().size() > 1) {
            document.setAlterationType(("allele with multiple variants"));
        }

    }


}
