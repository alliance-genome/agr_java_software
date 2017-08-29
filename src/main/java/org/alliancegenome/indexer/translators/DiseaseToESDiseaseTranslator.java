package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.disease.AnnotationDocument;
import org.alliancegenome.indexer.document.disease.DiseaseAnnotationDocument;
import org.alliancegenome.indexer.document.disease.DiseaseDocument;
import org.alliancegenome.indexer.document.disease.PublicationDocument;
import org.alliancegenome.indexer.entity.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiseaseToESDiseaseTranslator extends EntityDocumentTranslator<DOTerm, DiseaseDocument> {

    private GeneTranslator geneTranslator = new GeneTranslator();

    private Logger log = LogManager.getLogger(getClass());

    @Override
    protected DiseaseDocument entityToDocument(DOTerm entity) {

        DiseaseDocument doc = getTermDiseaseDocument(entity);

        // group by gene
        Map<Gene, List<DiseaseGeneJoin>> geneAssociationMap = entity.getDiseaseGeneJoins().stream()
                .collect(
                        Collectors.groupingBy(DiseaseGeneJoin::getGene,
                                Collectors.mapping(association -> association, Collectors.toList())
                        )
                );

        // generate AnnotationDocument records
        List<AnnotationDocument> annotationDocuments = geneAssociationMap.entrySet().stream()
                // sort by gene symbol
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    AnnotationDocument document = new AnnotationDocument();
                    document.setGeneDocument(geneTranslator.entityToDocument(entry.getKey()));
                    List<PublicationDocument> publicationDocuments = entry.getValue().stream()
                            .map(association -> {
                                document.setAssoicationType(association.getJoinType());
                                Publication publication = association.getPublication();
                                PublicationDocument pubDoc = new PublicationDocument();
                                pubDoc.setPrimaryKey(publication.getPrimaryKey());
                                pubDoc.setPubMedId(publication.getPubMedId());
                                pubDoc.setPubModId(publication.getPubModId());
                                pubDoc.setPubModUrl(publication.getPubModUrl());
                                List<String> evidencesDocument = association.getEvidenceCodes().stream()
                                        .map(EvidenceCode::getPrimaryKey)
                                        .collect(Collectors.toList());
                                pubDoc.setEvidenceCodes(evidencesDocument);
                                return pubDoc;
                            })
                            .collect(Collectors.toList());
                    document.setPublications(publicationDocuments);
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
        if (doTerm.getSynonyms() != null) {
            List<String> synonymList = doTerm.getSynonyms().stream()
                    .map(Synonym::getPrimaryKey)
                    .collect(Collectors.toList());
            document.setSynonyms(synonymList);
        }
        // add cross references
        if (doTerm.getExternalIds() != null) {
            List<String> crossRefList = doTerm.getExternalIds().stream()
                    .map(ExternalId::getPrimaryKey)
                    .collect(Collectors.toList());
            document.setCrossReferences(crossRefList);
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

    protected List<DiseaseAnnotationDocument> diseaseAnnotationDocument(DiseaseDocument document) {
        List<DiseaseAnnotationDocument> annotations = document.getAnnotations().stream()
                .map(annotationDocument -> {
                    DiseaseAnnotationDocument doc = new DiseaseAnnotationDocument();
                    doc.setDiseaseName(document.getName());
                    doc.setSpecies(annotationDocument.getGeneDocument().getSpecies());
                    doc.setAssociationType(annotationDocument.getAssoicationType());
                    doc.setPublications(annotationDocument.getPublications());
                    return doc;
                })
                .collect(Collectors.toList());
        return annotations;
    }

    @Override
    protected DOTerm documentToEntity(DiseaseDocument document) {
        // TODO Auto-generated method stub
        return null;
    }

}
