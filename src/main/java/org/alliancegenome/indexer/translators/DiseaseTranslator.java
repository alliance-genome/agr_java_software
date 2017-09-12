package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.*;
import org.alliancegenome.indexer.entity.SpeciesType;
import org.alliancegenome.indexer.entity.node.*;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class DiseaseTranslator extends EntityDocumentTranslator<DOTerm, DiseaseDocument> {

	private GeneTranslator geneTranslator = new GeneTranslator();
	private Map<String, DOTerm> fullDiseaseListMap = new HashMap<>(12000);

	private Logger log = LogManager.getLogger(getClass());

	@Override
	protected DiseaseDocument entityToDocument(DOTerm entity, int translationDepth) {
		if (fullDiseaseListMap.isEmpty())
			fetchFullList();

		populateParentsAndChildren(entity);
		DiseaseDocument doc = getTermDiseaseDocument(entity);

		doc.setCategory("disease");

		if (entity.getDiseaseGeneJoins() == null) {
			return doc;
		}

		// group by gene
		Map<Gene, List<DiseaseGeneJoin>> geneAssociationMap = entity.getDiseaseGeneJoins().stream()
				.collect(
						groupingBy(DiseaseGeneJoin::getGene,
								Collectors.mapping(association -> association, Collectors.toList())
								)
						);
		
		// generate AnnotationDocument records
		List<AnnotationDocument> annotationDocuments = geneAssociationMap.entrySet().stream()
				// sort by gene symbol
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> {
					AnnotationDocument document = new AnnotationDocument();
					if(translationDepth > 0) {
						document.setGeneDocument(geneTranslator.translate(entry.getKey(), translationDepth - 1)); // This needs to not happen if being call from GeneTranslator
					}
					List<PublicationDoclet> publicationDocuments = entry.getValue().stream()
							.map(association -> {
								document.setAssoicationType(association.getJoinType());
								Publication publication = association.getPublication();
								return getPublicationDocument(association, publication);
							})
							.collect(Collectors.toList());
					document.setPublications(publicationDocuments);
					return document;
				})
				.collect(Collectors.toList());

		doc.setAnnotations(annotationDocuments);

		return doc;
	}

	private void populateParentsAndChildren(DOTerm entity) {
		DOTerm term = fullDiseaseListMap.get(entity.getPrimaryKey());
		if (term == null)
			return;

		entity.setChildren(term.getChildren());
		entity.setParents(term.getParents());
		entity.setSynonyms(term.getSynonyms());
	}

	private void fetchFullList() {
		DiseaseRepository diseaseRepository = new DiseaseRepository();
		fullDiseaseListMap = diseaseRepository.getAllTerms().stream()
				.collect((Collectors.toMap(DOTerm::getPrimaryKey, id -> id)));
	}

	private PublicationDoclet getPublicationDocument(DiseaseGeneJoin association, Publication publication) {
		PublicationDoclet pubDoc = new PublicationDoclet();
		pubDoc.setPrimaryKey(publication.getPrimaryKey());
		pubDoc.setPubMedId(publication.getPubMedId());
		pubDoc.setPubModId(publication.getPubModId());
		pubDoc.setPubModUrl(publication.getPubModUrl());
		List<String> evidencesDocument = association.getEvidenceCodes().stream()
				.map(EvidenceCode::getPrimaryKey)
				.collect(Collectors.toList());
		pubDoc.setEvidenceCodes(evidencesDocument);
		return pubDoc;
	}

	private DiseaseDocument getTermDiseaseDocument(DOTerm doTerm) {
		return getTermDiseaseDocument(doTerm, false);
	}


	private DiseaseDocument getTermDiseaseDocument(DOTerm doTerm, boolean shallow) {
		DiseaseDocument document = new DiseaseDocument();
		if (doTerm.getDoId() != null)
			document.setDoId(doTerm.getDoId());
		document.setPrimaryKey(doTerm.getPrimaryKey());
		document.setPrimaryId(doTerm.getPrimaryKey());
		document.setName(doTerm.getName());
		document.setNameKey(doTerm.getName());
		document.setDefinition(doTerm.getDefinition());
		if (doTerm.getSynonyms() != null) {
			List<String> synonymList = doTerm.getSynonyms().stream()
					.map(Synonym::getPrimaryKey)
					.collect(Collectors.toList());
			document.setSynonyms(synonymList);
		}
		// add External Ids
		if (doTerm.getExternalIds() != null) {
			List<String> externalIds = doTerm.getExternalIds().stream()
					.map(ExternalId::getPrimaryKey)
					.collect(Collectors.toList());
			document.setExternal_ids(externalIds);
		}
		if (shallow)
			return document;

		// set parents
		if (doTerm.getParents() != null) {
			List<DiseaseDocument> parentDocs = doTerm.getParents().stream()
					.map(term -> getTermDiseaseDocument(term, true))
					.collect(Collectors.toList());
			document.setParents(parentDocs);
		}

		// set children
		if (doTerm.getChildren() != null) {
			List<DiseaseDocument> childrenDocs = doTerm.getChildren().stream()
					.map(term -> getTermDiseaseDocument(term, true))
					.collect(Collectors.toList());
			document.setChildren(childrenDocs);
		}

		return document;
	}

	@Override
	protected DOTerm documentToEntity(DiseaseDocument document, int translationDepth) {
		return null;
	}

	public Iterable<DiseaseAnnotationDocument> translateAnnotationEntities(List<DOTerm> geneDiseaseList, int translationDepth) {
		Set<DiseaseAnnotationDocument> diseaseAnnotationDocuments = new HashSet<>();
		geneDiseaseList.forEach(doTerm -> {
			if (doTerm.getDiseaseGeneJoins() == null) {
				DiseaseAnnotationDocument doc = new DiseaseAnnotationDocument();
				doc.setPrimaryKey(doTerm.getPrimaryKey());
				doc.setDiseaseID(doTerm.getPrimaryKey());
				doc.setDiseaseName(doTerm.getName());
				diseaseAnnotationDocuments.add(doc);
			} else {
				Set<DiseaseAnnotationDocument> docSet = doTerm.getDiseaseGeneJoins().stream()
						.map(diseaseGeneJoin -> {
							DiseaseAnnotationDocument doc = new DiseaseAnnotationDocument();
							doc.setPrimaryKey(doTerm.getPrimaryKey() + ":" + diseaseGeneJoin.getGene().getPrimaryKey());
							doc.setDiseaseName(doTerm.getName());
							doc.setDiseaseID(doTerm.getPrimaryKey());
							doc.setParentDiseaseIDs(getParentIdList(doTerm));
							doc.setAssociationType(diseaseGeneJoin.getJoinType());
							doc.setSpecies(getSpeciesDoclet(diseaseGeneJoin));
							doc.setGeneDocument(geneTranslator.entityToDocument(diseaseGeneJoin.getGene(), translationDepth - 1));
							List<PublicationDoclet> pubDocs = new ArrayList<>();
							pubDocs.add(getPublicationDocument(diseaseGeneJoin, diseaseGeneJoin.getPublication()));
							doc.setPublications(pubDocs);
							return doc;
						})
						.collect(Collectors.toSet());
				diseaseAnnotationDocuments.addAll(docSet);
			}
		});
		return diseaseAnnotationDocuments;
	}

	private List<String> getParentIdList(DOTerm doTerm) {
		List<String> idList = new ArrayList<>();
		idList.add(doTerm.getPrimaryKey());
		idList.addAll(doTerm.getParents().stream().map(DOTerm::getPrimaryKey).collect(Collectors.toSet()));
		return idList;
	}

	private SpeciesDoclet getSpeciesDoclet(DiseaseGeneJoin diseaseGeneJoin) {
		Species species = diseaseGeneJoin.getGene().getSpecies();
		SpeciesType type = species.getType();
		SpeciesDoclet doclet = new SpeciesDoclet();
		doclet.setName(species.getName());
		doclet.setTaxonID(species.getPrimaryKey());
		doclet.setOrderID(type.ordinal());
		return doclet;
	}
}
