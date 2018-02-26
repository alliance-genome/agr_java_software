package org.alliancegenome.shared.translators;

import org.alliancegenome.shared.es.document.site_index.AnnotationDocument;
import org.alliancegenome.shared.es.document.site_index.CrossReferenceDoclet;
import org.alliancegenome.shared.es.document.site_index.DiseaseAnnotationDocument;
import org.alliancegenome.shared.es.document.site_index.DiseaseDocument;
import org.alliancegenome.shared.es.document.site_index.PublicationDoclet;
import org.alliancegenome.shared.es.document.site_index.SourceDoclet;
import org.alliancegenome.shared.es.document.site_index.SpeciesDoclet;
import org.alliancegenome.shared.es.util.SpeciesDocletUtil;
import org.alliancegenome.shared.neo4j.entity.SpeciesType;
import org.alliancegenome.shared.neo4j.entity.node.DOTerm;
import org.alliancegenome.shared.neo4j.entity.node.DiseaseEntityJoin;
import org.alliancegenome.shared.neo4j.entity.node.EvidenceCode;
import org.alliancegenome.shared.neo4j.entity.node.Feature;
import org.alliancegenome.shared.neo4j.entity.node.Gene;
import org.alliancegenome.shared.neo4j.entity.node.Publication;
import org.alliancegenome.shared.neo4j.entity.node.Species;
import org.alliancegenome.shared.neo4j.entity.node.Synonym;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class DiseaseTranslator extends EntityDocumentTranslator<DOTerm, DiseaseDocument> {

	private final GeneTranslator geneTranslator = new GeneTranslator();
	private final FeatureTranslator featureTranslator = new FeatureTranslator();

	private final Logger log = LogManager.getLogger(getClass());

	@Override
	protected DiseaseDocument entityToDocument(DOTerm entity, int translationDepth) {
		return entityToDocument(entity, null, translationDepth);
	}


	protected DiseaseDocument entityToDocument(DOTerm entity, Gene gene, int translationDepth) {
		DiseaseDocument doc = getTermDiseaseDocument(entity);

		if (entity.getDiseaseEntityJoins() == null)
			return doc;

		Map<Gene, Map<String, List<DiseaseEntityJoin>>> sortedGeneAssociationMap = getGeneAnnotationMap(entity, gene);
		List<AnnotationDocument> annotationDocuments = generateAnnotationDocument(entity, translationDepth, sortedGeneAssociationMap);
		doc.setAnnotations(annotationDocuments);
		return doc;
	}

	protected DiseaseDocument entityToDocument(DOTerm entity, Gene gene, List<DiseaseEntityJoin> dejList, int translationDepth) {
		DiseaseDocument doc = getTermDiseaseDocument(entity);

		if (dejList == null)
			return doc;

		Map<String, List<DiseaseEntityJoin>> associationMap = dejList.stream()
				.collect(Collectors.groupingBy(diseaseEntityJoin -> diseaseEntityJoin.getJoinType()));
		Map<Gene, Map<String, List<DiseaseEntityJoin>>> map = new HashMap<>();
		map.put(gene, associationMap);
		// create AnnotationDocument objects per
		// disease, gene, Feature, association type
		List<AnnotationDocument> annotationDocuments = generateAnnotationDocument(entity, translationDepth, map);
		doc.setAnnotations(annotationDocuments);
		return doc;
	}

	List<DiseaseDocument> getDiseaseDocuments(Gene entity, List<DiseaseEntityJoin> diseaseJoins, int translationDepth) {
		// group by disease
		Map<DOTerm, List<DiseaseEntityJoin>> diseaseMap = diseaseJoins.stream()
				.collect(Collectors.groupingBy(DiseaseEntityJoin::getDisease));
		List<DiseaseDocument> diseaseList = new ArrayList<>();
		// for each disease create annotation doc
		// diseaseEntityJoin list turns into AnnotationDocument objects
		diseaseMap.forEach((doTerm, diseaseEntityJoins) -> {
			if (translationDepth > 0) {
				try {
					DiseaseDocument doc = entityToDocument(doTerm, entity, diseaseEntityJoins, translationDepth - 1); // This needs to not happen if being called from DiseaseTranslator
					if (!diseaseList.contains(doc))
						diseaseList.add(doc);
				} catch (Exception e) {
					log.error("Exception Creating Disease Document: " + e.getMessage());
				}
			}

		});
		return diseaseList;
	}

	private List<AnnotationDocument> generateAnnotationDocument(DOTerm entity, int translationDepth, Map<Gene, Map<String, List<DiseaseEntityJoin>>> sortedGeneAssociationMap) {
		// generate AnnotationDocument records
		return sortedGeneAssociationMap.entrySet().stream()
				.map(geneMapEntry ->
						geneMapEntry.getValue().entrySet().stream()
								.map(associationEntry -> {
									List<DiseaseEntityJoin> featureJoins = associationEntry.getValue().stream()
											.filter(join -> join.getFeature() != null)
											.collect(toList());
									List<DiseaseEntityJoin> featurelessJoins = associationEntry.getValue().stream()
											.filter(join -> join.getFeature() == null)
											.collect(toList());

									Map<Feature, List<DiseaseEntityJoin>> featureMap = featureJoins.stream()
											.filter(entry -> entity != null)
											.collect(Collectors.groupingBy(DiseaseEntityJoin::getFeature
											));
									featureMap.put(null, featurelessJoins);
									return featureMap.entrySet().stream()
											.map(featureMapEntry -> {

												AnnotationDocument document = new AnnotationDocument();
												document.setGeneDocument(geneTranslator.translate(geneMapEntry.getKey(), 0));
												Feature feature = featureMapEntry.getKey();
												if (feature != null) {
													document.setFeatureDocument(featureTranslator.entityToDocument(feature, 0));
												}
												document.setAssociationType(associationEntry.getKey());
												document.setSource(getSourceUrls(entity, geneMapEntry.getKey().getSpecies()));
												document.setPublications(getPublicationDoclets(featureMapEntry.getValue()));
												return document;
											})
											.collect(Collectors.toList());
								})
								.flatMap(Collection::stream)
								.collect(Collectors.toList()))
				// turn List<AnnotationDocument> into stream<AnnotationDocument> so they can be collected into
				// the outer List<AnnotationDocument>
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

//	private PublicationDoclet getPublicationDoclets(DiseaseEntityJoin diseaseEntityJoin) {
//		if (diseaseEntityJoin == null)
//			return null;
//		Publication publication = diseaseEntityJoin.getPublication();
//		PublicationDoclet pubDoc = new PublicationDoclet();
//		pubDoc.setPrimaryKey(publication.getPrimaryKey());
//		pubDoc.setPubMedId(publication.getPubMedId());
//		pubDoc.setPubMedUrl(publication.getPubMedUrl());
//		pubDoc.setPubModId(publication.getPubModId());
//		pubDoc.setPubModUrl(publication.getPubModUrl());
//
//		if (diseaseEntityJoin.getEvidenceCodes() == null) {
//			log.error("Could not find any evidence codes for " + diseaseEntityJoin.getGene().getPrimaryKey() + " and publication " + publication.getPrimaryKey());
//			return null;
//		}
//
//		Set<String> evidencesDocument = diseaseEntityJoin.getEvidenceCodes().stream()
//				.map(EvidenceCode::getPrimaryKey)
//				.collect(Collectors.toSet());
//		pubDoc.setEvidenceCodes(evidencesDocument);
//		return pubDoc;
//	}

	private Map<Gene, Map<String, List<DiseaseEntityJoin>>> getGeneAnnotationMap(DOTerm entity, Gene gene) {
		// group by gene then by association type
		Map<Gene, Map<String, List<DiseaseEntityJoin>>> geneAssociationMap = entity.getDiseaseEntityJoins().stream()
				.filter(diseaseEntityJoin -> gene == null || diseaseEntityJoin.getGene().equals(gene))
				.collect(
						groupingBy(DiseaseEntityJoin::getGene,
								groupingBy(DiseaseEntityJoin::getJoinType))
				);

		// sort by gene symbol
		return geneAssociationMap.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private PublicationDoclet getPublicationDoclet(DiseaseEntityJoin association, Publication publication) {
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

		Set<String> evidencesDocument = association.getEvidenceCodes().stream()
				.map(EvidenceCode::getPrimaryKey)
				.collect(Collectors.toSet());
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
		document.setName_key(doTerm.getName());
		document.setDefinition(doTerm.getDefinition());
		document.setDefinitionLinks(doTerm.getDefLinks());
		document.setDateProduced(doTerm.getDateProduced());
		//document.setParentDiseaseNames(getParentNameList(doTerm));
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

		// set highLevelSlim values
		if (CollectionUtils.isNotEmpty(doTerm.getHighLevelTermList())) {
			doTerm.getHighLevelTermList().forEach(slimTerm ->
					document.getHighLevelSlimTermNames().add(slimTerm.getName()));
		}

		// set all parent Names
		if (CollectionUtils.isNotEmpty(doTerm.getHighLevelTermList())) {
			doTerm.getHighLevelTermList().forEach(slimTerm ->
					document.getHighLevelSlimTermNames().add(slimTerm.getName()));
		}

		// set all sources except Human
		document.setSourceList(getSourceUrls(doTerm).stream()
				.filter(sourceDoclet -> !sourceDoclet.getSpecies().getTaxonID().equals(SpeciesType.HUMAN.getTaxonID()))
				.collect(Collectors.toList()));

		return document;
	}

	private SourceDoclet getSourceUrls(DOTerm doTerm, Species species) {
		List<SourceDoclet> sources;
		sources = getSourceUrls(doTerm).stream().
				filter(sourceUrl ->
						sourceUrl.getSpecies().getTaxonID().equals(species.getType().getTaxonID())
				)
				.collect(Collectors.toList());
		if (sources.isEmpty())
			return null;
		return sources.get(0);
	}

	private List<SourceDoclet> getSourceUrls(DOTerm doTerm) {

		List<SourceDoclet> sourceDoclets = Arrays.stream(SpeciesType.values())
				.map(speciesType -> {
					SourceDoclet doclet = new SourceDoclet();
					doclet.setSpecies(SpeciesDocletUtil.getSpeciesDoclet(speciesType));
					doclet.setName(speciesType.getDisplayName());
					if (speciesType.equals(SpeciesType.HUMAN)) {
						doclet.setName(SpeciesType.RAT.getDisplayName());
						doclet.setDiseaseUrl(doTerm.getHumanOnlyRgdLink());
					}
					if (speciesType == SpeciesType.FLY && doTerm.getFlybaseLink() != null) {
						doclet.setUrl(doTerm.getFlybaseLink());
						doclet.setDiseaseUrl(doTerm.getFlybaseLink());
					}
					if (speciesType == SpeciesType.RAT && doTerm.getRgdLink() != null) {
						doclet.setUrl(doTerm.getRgdLink());
						doclet.setDiseaseUrl(doTerm.getRatOnlyRgdLink());
					}
					if (speciesType == SpeciesType.MOUSE && doTerm.getMgiLink() != null) {
						doclet.setUrl(doTerm.getMgiLink());
						doclet.setDiseaseUrl(doTerm.getMgiLink());
					}
					if (speciesType == SpeciesType.ZEBRAFISH && doTerm.getZfinLink() != null) {
						doclet.setUrl(doTerm.getZfinLink());
						doclet.setDiseaseUrl(doTerm.getZfinLink());
					}
					if (speciesType == SpeciesType.HUMAN && doTerm.getHumanLink() != null) {
						doclet.setUrl(doTerm.getHumanLink());
					}
					if (speciesType == SpeciesType.WORM && doTerm.getWormbaseLink() != null) {
						doclet.setUrl(doTerm.getWormbaseLink());
						doclet.setDiseaseUrl(doTerm.getWormbaseLink());
					}
					return doclet;
				})
				.collect(toList());

		return sourceDoclets;
	}

	@Override
	protected DOTerm documentToEntity(DiseaseDocument document, int translationDepth) {
		return null;
	}

	public Iterable<DiseaseAnnotationDocument> translateAnnotationEntities(List<DOTerm> geneDiseaseList, int translationDepth) {
		Set<DiseaseAnnotationDocument> diseaseAnnotationDocuments = new HashSet<>();
		geneDiseaseList.forEach(doTerm -> {
			Map<Gene, Map<String, List<DiseaseEntityJoin>>> sortedGeneAssociationMap = getGeneAnnotationMap(doTerm, null);
			List<DiseaseAnnotationDocument> docSet = sortedGeneAssociationMap.entrySet().stream()
					.map(geneMapEntry ->
							geneMapEntry.getValue().entrySet().stream().map(associationEntry ->
									associationEntry.getValue().stream()
											.map(diseaseEntityJoin -> {
												Gene gene = geneMapEntry.getKey();
												DiseaseAnnotationDocument document = new DiseaseAnnotationDocument();
												if (translationDepth > 0) {
													document.setGeneDocument(geneTranslator.translate(gene, translationDepth - 1)); // This needs to not happen if being call from GeneTranslator
												}
												String primaryKey = doTerm.getPrimaryKey() + ":" + gene.getPrimaryKey();
												document.setDiseaseName(doTerm.getName());
												document.setDiseaseID(doTerm.getPrimaryKey());
												document.setParentDiseaseIDs(getParentIdList(doTerm));
												document.setAssociationType(associationEntry.getKey());
												document.setSpecies(getSpeciesDoclet(gene));
												document.setSource(getSourceUrls(doTerm, gene.getSpecies()));
												document.setPublications(getPublicationDoclets(associationEntry.getValue()));
												Feature feature = diseaseEntityJoin.getFeature();
												if (feature != null) {
													primaryKey += ":" + feature.getPrimaryKey();
													document.setFeatureDocument(featureTranslator.entityToDocument(feature, 0));
												}
												document.setPrimaryKey(primaryKey);
												return document;
											})
											.collect(Collectors.toList()))
									.flatMap(Collection::stream)
									.collect(Collectors.toList()))
					.flatMap(Collection::stream)
					.collect(Collectors.toList());
			diseaseAnnotationDocuments.addAll(docSet);
		});
		return diseaseAnnotationDocuments;
	}

	private List<PublicationDoclet> getPublicationDoclets(List<DiseaseEntityJoin> diseaseEntityJoinList) {
		Set<PublicationDoclet> publicationDocuments = diseaseEntityJoinList.stream()
				// filter out records that do not have valid pub / evidence code entries
				.filter(diseaseGeneJoin ->
						getPublicationDoclet(diseaseGeneJoin, diseaseGeneJoin.getPublication()) != null
				)
				.map(diseaseGeneJoin -> {
					Publication publication = diseaseGeneJoin.getPublication();
					return getPublicationDoclet(diseaseGeneJoin, publication);
				})
				.collect(Collectors.toSet());
		List<PublicationDoclet> pubDocletListRaw = new ArrayList<>(publicationDocuments);
		pubDocletListRaw.sort(PublicationDoclet::compareTo);

		// get evidence codes for same pub onto s
		List<PublicationDoclet> pubDocletList = new ArrayList<>();
		for (PublicationDoclet doclet : pubDocletListRaw) {
			PublicationDoclet existingDoclet = null;
			for (PublicationDoclet finalDoclet : pubDocletList) {
				if (doclet.compareTo(finalDoclet) == 0) {
					existingDoclet = finalDoclet;
				}
			}
			if (existingDoclet == null) {
				pubDocletList.add(doclet);
			} else {
				existingDoclet.getEvidenceCodes().addAll(doclet.getEvidenceCodes());
			}
		}
		return pubDocletList;
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

	/**
	 * Get all the parent term names compiled.
	 */
//	private Set<String> getParentNameList(DOTerm doTerm) {
//		Set<String> nameList = new LinkedHashSet<>();
//		nameList.add(doTerm.getName());
//		doTerm.getParents().forEach(term -> {
//			nameList.add(term.getName());
//			if (term.getParents() != null)
//				nameList.addAll(getParentNameList(term));
//		});
//		return nameList;
//	}
//
//	private SpeciesDoclet getSpeciesDoclet(DiseaseEntityJoin diseaseGeneJoin) {
//		return getSpeciesDoclet(diseaseGeneJoin.getGene());
//	}

	private SpeciesDoclet getSpeciesDoclet(Gene gene) {
		Species species = gene.getSpecies();
		SpeciesType type = species.getType();
		SpeciesDoclet doclet = new SpeciesDoclet();
		doclet.setName(species.getName());
		doclet.setTaxonID(species.getPrimaryKey());
		doclet.setOrderID(type.ordinal());
		return doclet;
	}
}
