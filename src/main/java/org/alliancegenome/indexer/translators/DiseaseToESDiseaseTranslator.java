package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.disease.AnnotationDocument;
import org.alliancegenome.indexer.document.disease.DiseaseDocument;
import org.alliancegenome.indexer.document.disease.PublicationDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.alliancegenome.indexer.entity.EvidenceCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class DiseaseToESDiseaseTranslator extends EntityDocumentTranslator<DOTerm, DiseaseDocument> {

	private GeneTranslator geneTranslator = new GeneTranslator();

	private Logger log = LogManager.getLogger(getClass());

	@Override
	protected DiseaseDocument entityToDocument(DOTerm entity) {
		return entityToDocument(entity, false);
	}

	private DiseaseDocument entityToDocument(DOTerm entity, boolean shallow) {

		log.info(entity);

		DiseaseDocument doc = getTermDiseaseDocument(entity);

		// set parents
		if (entity.getParents() != null) {
			List<DiseaseDocument> parentDocs = entity.getParents().stream()
					.map(this::getTermDiseaseDocument)
					.collect(Collectors.toList());
			doc.setParents(parentDocs);
		}

		// set children
		if (entity.getChildren() != null) {
			List<DiseaseDocument> childrenDocs = entity.getChildren().stream()
					.map(this::getTermDiseaseDocument)
					.collect(Collectors.toList());
			doc.setChildren(childrenDocs);
		}

		if (shallow)
			return doc;

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
		return entityToDocument(doTerm, true);
	}

	@Override
	protected DOTerm doumentToEntity(DiseaseDocument doument) {
		// TODO Auto-generated method stub
		return null;
	}


}
