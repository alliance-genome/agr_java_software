package org.alliancegenome.indexer.translators;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.indexer.document.AnnotationDocument;
import org.alliancegenome.indexer.document.CrossReferenceDoclet;
import org.alliancegenome.indexer.document.DiseaseAnnotationDocument;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.document.PublicationDoclet;
import org.alliancegenome.indexer.document.SourceDoclet;
import org.alliancegenome.indexer.document.SpeciesDoclet;
import org.alliancegenome.indexer.entity.SpeciesType;
import org.alliancegenome.indexer.entity.node.DOTerm;
import org.alliancegenome.indexer.entity.node.DiseaseGeneJoin;
import org.alliancegenome.indexer.entity.node.EvidenceCode;
import org.alliancegenome.indexer.entity.node.Gene;
import org.alliancegenome.indexer.entity.node.Publication;
import org.alliancegenome.indexer.entity.node.Species;
import org.alliancegenome.indexer.entity.node.Synonym;
import org.alliancegenome.indexer.service.SpeciesService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseTranslator extends EntityDocumentTranslator<DOTerm, DiseaseDocument> {

	private GeneTranslator geneTranslator = new GeneTranslator();

	private Logger log = LogManager.getLogger(getClass());

	@Override
	protected DiseaseDocument entityToDocument(DOTerm entity, int translationDepth) {

		DiseaseDocument doc = getTermDiseaseDocument(entity);

		if (entity.getDiseaseGeneJoins() == null)
			return doc;

		// group by gene then by association type
		Map<Gene, Map<String, List<DiseaseGeneJoin>>> geneAssociationMap = entity.getDiseaseGeneJoins().stream()
				.collect(
						groupingBy(DiseaseGeneJoin::getGene,
								groupingBy(DiseaseGeneJoin::getJoinType))
						);

		// sort by gene symbol
		Map<Gene, Map<String, List<DiseaseGeneJoin>>> sortedGeneAssociationMap =
				geneAssociationMap.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

		// generate AnnotationDocument records
		List<AnnotationDocument> annotationDocuments = sortedGeneAssociationMap.entrySet().stream()
				.map(geneMapEntry ->
				geneMapEntry.getValue().entrySet().stream().map(associationEntry -> {
					AnnotationDocument document = new AnnotationDocument();
					if (translationDepth > 0) {
						document.setGeneDocument(geneTranslator.translate(geneMapEntry.getKey(), translationDepth - 1)); // This needs to not happen if being call from GeneTranslator
					}
					document.setAssociationType(associationEntry.getKey());
					List<PublicationDoclet> publicationDocuments = associationEntry.getValue().stream()
							// filter out records that do not have valid pub / evidence code entries
							.filter(diseaseGeneJoin ->
							getPublicationDoclet(diseaseGeneJoin, diseaseGeneJoin.getPublication()) != null
									)
							.map(diseaseGeneJoin -> {
								Publication publication = diseaseGeneJoin.getPublication();
								return getPublicationDoclet(diseaseGeneJoin, publication);
							})
							.collect(Collectors.toList());
					document.setPublications(publicationDocuments);

					return document;
				}).collect(Collectors.toList()))
				// turn List<AnnotationDocument> into stream<AnnotationDocument> so they can be collected into
				// the outer List<AnnotationDocument>
				.flatMap(Collection::stream)
				.collect(Collectors.toList());

		doc.setAnnotations(annotationDocuments);

		return doc;
	}

	private PublicationDoclet getPublicationDoclet(DiseaseGeneJoin association, Publication publication) {
		PublicationDoclet pubDoc = new PublicationDoclet();
		pubDoc.setPrimaryKey(publication.getPrimaryKey());

		pubDoc.setPubMedId(publication.getPubMedId());
		pubDoc.setPubMedUrl(publication.getPubMedUrl());
		pubDoc.setPubModId(publication.getPubModId());
		pubDoc.setPubModUrl(publication.getPubModUrl());

		if (association.getEvidenceCodes() == null) {
			log.error("Could not find any evidence codes for " + association.getGene().getPrimaryKey() + " and publication " + publication.getPrimaryKey());
			return null;
		}

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
		document.setDefinitionLinks(doTerm.getDefLinks());
		if (doTerm.getSynonyms() != null) {
			List<String> synonymList = doTerm.getSynonyms().stream()
					.map(Synonym::getPrimaryKey)
					.collect(Collectors.toList());
			document.setSynonyms(synonymList);
		}
		// add CrossReferences
		if (doTerm.getCrossReferences() != null) {
			List<CrossReferenceDoclet> externalIds = doTerm.getCrossReferences().stream()
					.map(crossReference -> {
						CrossReferenceDoclet doclet = new CrossReferenceDoclet();
						doclet.setLocalId(crossReference.getLocalId());
						doclet.setCrossRefCompleteUrl(crossReference.getCrossRefCompleteUrl());
						doclet.setPrefix(crossReference.getPrefix());
						doclet.setName(crossReference.getPrimaryKey());
						return doclet;
					})
					.collect(Collectors.toList());
			document.setCrossReferences(externalIds);
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

		document.setSourceList(getSourceUrls(doTerm));

		return document;
	}

	private List<SourceDoclet> getSourceUrls(DOTerm doTerm) {
		List<SourceDoclet> sourceDoclets = new ArrayList<>();
		if (doTerm.getFlybaseLink() != null) {
			SourceDoclet doclet = new SourceDoclet();
			doclet.setSpecies(SpeciesService.getSpeciesDoclet(SpeciesType.FLY));
			doclet.setUrl(doTerm.getFlybaseLink());
			sourceDoclets.add(doclet);
		}
		if (doTerm.getRgdLink() != null) {
			SourceDoclet doclet = new SourceDoclet();
			doclet.setSpecies(SpeciesService.getSpeciesDoclet(SpeciesType.RAT));
			doclet.setUrl(doTerm.getRgdLink());
			sourceDoclets.add(doclet);
		}
		if (doTerm.getMgiLink() != null) {
			SourceDoclet doclet = new SourceDoclet();
			doclet.setSpecies(SpeciesService.getSpeciesDoclet(SpeciesType.MOUSE));
			doclet.setUrl(doTerm.getMgiLink());
			sourceDoclets.add(doclet);
		}
		if (doTerm.getZfinLink() != null) {
			SourceDoclet doclet = new SourceDoclet();
			doclet.setSpecies(SpeciesService.getSpeciesDoclet(SpeciesType.ZEBRAFISH));
			doclet.setUrl(doTerm.getZfinLink());
			sourceDoclets.add(doclet);
		}
		if (doTerm.getHumanLink() != null) {
			SourceDoclet doclet = new SourceDoclet();
			doclet.setSpecies(SpeciesService.getSpeciesDoclet(SpeciesType.HUMAN));
			doclet.setUrl(doTerm.getHumanLink());
			sourceDoclets.add(doclet);
		}
		return sourceDoclets;
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
							pubDocs.add(getPublicationDoclet(diseaseGeneJoin, diseaseGeneJoin.getPublication()));
							doc.setPublications(pubDocs);
							return doc;
						})
						.collect(Collectors.toSet());
				diseaseAnnotationDocuments.addAll(docSet);
			}
		});
		return diseaseAnnotationDocuments;
	}

	/**
	 * Get all the parent termIDs compiled.
	 */
	private Set<String> getParentIdList(DOTerm doTerm) {
		Set<String> idList = new LinkedHashSet<>();
		idList.add(doTerm.getPrimaryKey());
		doTerm.getParents().forEach(term -> {
			idList.add(term.getPrimaryKey());
			if (term.getParents() != null)
				idList.addAll(getParentIdList(term));
		});
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
