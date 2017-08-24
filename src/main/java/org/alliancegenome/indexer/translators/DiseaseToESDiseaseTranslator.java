package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.disease.AnnotationDocument;
import org.alliancegenome.indexer.document.disease.DiseaseDocument;
import org.alliancegenome.indexer.document.disease.PublicationDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.alliancegenome.indexer.entity.EvidenceCode;
import org.alliancegenome.indexer.entity.Synonym;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class DiseaseToESDiseaseTranslator extends EntityDocumentTranslator<DOTerm, DiseaseDocument> {

    private GeneTranslator geneTranslator = new GeneTranslator();

    private Logger log = LogManager.getLogger(getClass());

    @Override
    protected DiseaseDocument entityToDocument(DOTerm entity) {

        log.info(entity);

        DiseaseDocument doc = getTermDiseaseDocument(entity);

        // generate AnnotationDocument records
        List<AnnotationDocument> annotationDocuments = entity.getAnnotations()
                .stream().map(annotation -> {
                    AnnotationDocument document = new AnnotationDocument();
                    document.setGeneDocument(geneTranslator.entityToDocument(annotation.getGene()));

                    List<PublicationDocument> pubDocuments = annotation.getPublications().stream()
                            .map(publication -> {
                                PublicationDocument pubDoc = new PublicationDocument();
                                pubDoc.setPrimaryKey(publication.getPrimaryKey());
                                pubDoc.setPubMedId(publication.getPubMedId());
                                pubDoc.setPubModId(publication.getPubModId());
                                pubDoc.setPubModUrl(publication.getPubModUrl());
                                List<String> evidencesDocument = publication.getEvidence().stream()
                                        .map(EvidenceCode::getPrimaryKey)
                                        .collect(Collectors.toList());
                                pubDoc.setEvidenceCodes(evidencesDocument);
                                return pubDoc;
                            })
                            .collect(Collectors.toList());
                    document.setPublications(pubDocuments);
                    return document;
                })
                .collect(Collectors.toList());
        doc.setAnnotations(annotationDocuments);

        return doc;
    }

    private DiseaseDocument getTermDiseaseDocument(DOTerm doTerm) {
        return getTermDiseaseDocument(doTerm, false);
    }


    private DiseaseDocument getTermDiseaseDocument(DOTerm doTerm, boolean shallow) {
        DiseaseDocument document = new DiseaseDocument();
        if (doTerm.getDoId() != null)
            document.setDoId(doTerm.getDoId());
        document.setPrimaryKey(doTerm.getPrimaryKey());
        document.setName(doTerm.getName());
        document.setDefinition(doTerm.getDefinition());
        if(doTerm.getSynonyms() != null) {
            List<String> synonymList = doTerm.getSynonyms().stream()
                    .map(Synonym::getPrimaryKey)
                    .collect(Collectors.toList());
            document.setSynonyms(synonymList);
        }
        if (shallow)
            return document;

        // set parents
        if (doTerm.getParents() != null) {
            List<DiseaseDocument> parentDocs = doTerm.getParents().stream()
                    .map(term -> getTermDiseaseDocument(doTerm, true))
                    .collect(Collectors.toList());
            document.setParents(parentDocs);
        }

        // set children
        if (doTerm.getChildren() != null) {
            List<DiseaseDocument> childrenDocs = doTerm.getChildren().stream()
                    .map(term -> getTermDiseaseDocument(doTerm, true))
                    .collect(Collectors.toList());
            document.setChildren(childrenDocs);
        }

        return document;
    }

    @Override
    protected DOTerm documentToEntity(DiseaseDocument document) {
        // TODO Auto-generated method stub
        return null;
    }

}
