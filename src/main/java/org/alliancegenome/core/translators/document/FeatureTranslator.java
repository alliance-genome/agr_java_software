package org.alliancegenome.core.translators.document;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.es.index.site.document.DiseaseDocument;
import org.alliancegenome.es.index.site.document.FeatureDocument;
import org.alliancegenome.neo4j.entity.node.Feature;
import org.alliancegenome.neo4j.entity.node.SecondaryId;
import org.alliancegenome.neo4j.entity.node.Synonym;
import org.apache.commons.collections4.CollectionUtils;

public class FeatureTranslator extends EntityDocumentTranslator<Feature, FeatureDocument> {

    private static GeneTranslator geneTranslator = new GeneTranslator();
    private static DiseaseTranslator diseaseTranslator = new DiseaseTranslator();

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
        featureDocument.setName(entity.getSymbol());
        featureDocument.setModCrossRefFullUrl(entity.getModCrossRefCompleteUrl());

        if (translationDepth > 0) {
            if (entity.getGene().getSpecies() != null)
                featureDocument.setNameKeyWithSpecies(entity.getSymbol(), entity.getGene().getSpecies().getType().getAbbreviation());

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
            if (CollectionUtils.isNotEmpty(entity.getDiseaseEntityJoins())) {
                List<DiseaseDocument> diseaseList = diseaseTranslator.getDiseaseDocuments(entity.getGene(), entity.getDiseaseEntityJoins(), translationDepth);
                featureDocument.setDiseaseDocuments(diseaseList);
            }
        }

        return featureDocument;
    }

    @Override
    protected Feature documentToEntity(FeatureDocument doument, int translationDepth) {
        // We are not going to the database yet so will implement this when we need to
        return null;
    }

}
