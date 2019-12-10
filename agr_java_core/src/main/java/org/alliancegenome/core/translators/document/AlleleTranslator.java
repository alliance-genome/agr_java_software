package org.alliancegenome.core.translators.document;

import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.CrossReferenceDocletTranslator;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.document.AlleleDocument;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.CrossReference;

public class AlleleTranslator extends EntityDocumentTranslator<Allele, AlleleDocument> {

    private static GeneTranslator geneTranslator = new GeneTranslator();
    private static DiseaseTranslator diseaseTranslator = new DiseaseTranslator();
    private static CrossReferenceDocletTranslator crossReferenceDocletTranslator = new CrossReferenceDocletTranslator();

    @Override
    protected AlleleDocument entityToDocument(Allele entity, int translationDepth) {

        AlleleDocument document = new AlleleDocument();

        //allele.setDataProvider(entity.getDataProvider());
        document.setDateProduced(entity.getDateProduced());
        document.setGlobalId(entity.getGlobalId());
        document.setLocalId(entity.getLocalId());
        document.setPrimaryKey(entity.getPrimaryKey());
        document.setRelease(entity.getRelease());
        document.setSymbol(entity.getSymbol());
        document.setSymbolText(entity.getSymbolText());
        document.setName(entity.getSymbol());
        document.setNameKey(entity.getSymbolTextWithSpecies());
        if (entity.getSpecies() != null) {
            document.setSpecies(entity.getSpecies().getName());
        }

        if (entity.getCrossReferences() != null && entity.getCrossReferences().size() > 0) {
            CrossReference allele = entity.getCrossReferences().stream()
                    .filter(ref -> ref.getCrossRefType().equals("allele"))
                    .findFirst().orElse(null);
            if (allele != null) {
                document.setModCrossRefCompleteUrl(allele.getCrossRefCompleteUrl());
                List<CrossReferenceDoclet> crossRefDoclets = entity.getCrossReferences().stream()
                        .map(crossReference -> crossReferenceDocletTranslator.translate(crossReference))
                        .collect(Collectors.toList());
                document.setCrossReferenceList(crossRefDoclets);
            }
        }


        addSecondaryIds(entity, document);
        addSynonyms(entity, document);


        return document;
    }


}
