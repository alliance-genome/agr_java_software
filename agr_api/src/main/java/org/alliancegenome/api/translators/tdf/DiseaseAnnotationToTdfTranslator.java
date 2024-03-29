package org.alliancegenome.api.translators.tdf;

import org.alliancegenome.api.entity.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.DiseaseAnnotationDocument;
import org.alliancegenome.api.entity.GeneDiseaseAnnotationDocument;
import org.alliancegenome.core.translators.tdf.DiseaseDownloadRow;
import org.alliancegenome.core.translators.tdf.DownloadHeader;
import org.alliancegenome.curation_api.model.entities.ExperimentalCondition;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.curation_api.model.entities.base.CurieAuditedObject;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DiseaseAnnotationToTdfTranslator {

	public String getAllRowsForGeneDiseaseAnnotations(List<GeneDiseaseAnnotationDocument> diseaseAnnotations) {

		// convert collection of DiseaseAnnotation records to DiseaseDownloadRow records
		List<DiseaseDownloadRow> list = getDownloadRowsFromGeneDiseaseAnnotations(diseaseAnnotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Species ID", (DiseaseDownloadRow::getSpeciesID)),
			new DownloadHeader<>("Species Name", (DiseaseDownloadRow::getSpeciesName)),
			new DownloadHeader<>("Gene ID", (DiseaseDownloadRow::getMainEntityID)),
			new DownloadHeader<>("Gene Symbol", (DiseaseDownloadRow::getMainEntitySymbol)),
			new DownloadHeader<>("Additional Implicated Gene ID", (DiseaseDownloadRow::getAssertedGeneID)),
			new DownloadHeader<>("Additional Implicated Gene Symbol", (DiseaseDownloadRow::getAssertedGeneName)),
			new DownloadHeader<>("Gene Association", (DiseaseDownloadRow::getAssociation)),
			new DownloadHeader<>("Genetic Entity ID", (DiseaseDownloadRow::getGeneticEntityID)),
			new DownloadHeader<>("Genetic Entity Name", (DiseaseDownloadRow::getGeneticEntityName)),
			new DownloadHeader<>("Genetic Entity Type", (DiseaseDownloadRow::getGeneticEntityType)),
			new DownloadHeader<>("Genetic Entity Association", (DiseaseDownloadRow::getGeneticEntityAssociation)),
			new DownloadHeader<>("Disease Qualifier", (DiseaseDownloadRow::getDiseaseQualifier)),
			new DownloadHeader<>("Disease ID", (DiseaseDownloadRow::getDiseaseID)),
			new DownloadHeader<>("Disease Name", (DiseaseDownloadRow::getDiseaseName)),
			new DownloadHeader<>("Evidence Code", (DiseaseDownloadRow::getEvidenceCode)),
			new DownloadHeader<>("Evidence Code Abbreviation", (DiseaseDownloadRow::getEvidenceAbbreviation)),
			new DownloadHeader<>("Evidence Code Name", (DiseaseDownloadRow::getEvidenceCodeName)),
			new DownloadHeader<>("Experimental Conditions", (DiseaseDownloadRow::getExperimentalCondition)),
			new DownloadHeader<>("Genetic Modifier Relation", (DiseaseDownloadRow::getDiseaseGeneticModifierRelation)),
			new DownloadHeader<>("Genetic Modifier IDs", (DiseaseDownloadRow::getDiseaseGeneticModifierID)),
			new DownloadHeader<>("Genetic Modifier Names", (DiseaseDownloadRow::getDiseaseGeneticModifierName)),
			new DownloadHeader<>("Strain Background ID", (DiseaseDownloadRow::getStrainBackgroundID)),
			new DownloadHeader<>("Strain Background Name", (DiseaseDownloadRow::getStrainBackgroundName)),
			new DownloadHeader<>("Genetic Sex", (DiseaseDownloadRow::getGeneticSex)),
			new DownloadHeader<>("Notes", (DiseaseDownloadRow::getNote)),
			new DownloadHeader<>("Annotation Type", (DiseaseDownloadRow::getAnnotationType)),
			new DownloadHeader<>("Based On ID", (DiseaseDownloadRow::getBasedOnID)),
			new DownloadHeader<>("Based On Symbol", (DiseaseDownloadRow::getBasedOnName)),
			new DownloadHeader<>("Source", (DiseaseDownloadRow::getSource)),
			new DownloadHeader<>("Source URL", (DiseaseDownloadRow::getSourceUrl)),
			new DownloadHeader<>("Reference", (DiseaseDownloadRow::getReference)),
			new DownloadHeader<>("Date", (DiseaseDownloadRow::getDateAssigned))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public String getAllRowsForGenes(List<DiseaseAnnotation> diseaseAnnotations) {

		// convert collection of DiseaseAnnotation records to DiseaseDownloadRow records
		List<DiseaseDownloadRow> list = getDownloadRowsFromGenes(diseaseAnnotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Species ID", (DiseaseDownloadRow::getSpeciesID)),
			new DownloadHeader<>("Species Name", (DiseaseDownloadRow::getSpeciesName)),
			new DownloadHeader<>("Gene ID", (DiseaseDownloadRow::getMainEntityID)),
			new DownloadHeader<>("Gene Symbol", (DiseaseDownloadRow::getMainEntitySymbol)),
			new DownloadHeader<>("Genetic Entity ID", (DiseaseDownloadRow::getGeneticEntityID)),
			new DownloadHeader<>("Genetic Entity Name", (DiseaseDownloadRow::getGeneticEntityName)),
			new DownloadHeader<>("Genetic Entity Type", (DiseaseDownloadRow::getGeneticEntityType)),
			new DownloadHeader<>("Association", (DiseaseDownloadRow::getAssociation)),
			new DownloadHeader<>("Disease ID", (DiseaseDownloadRow::getDiseaseID)),
			new DownloadHeader<>("Disease Name", (DiseaseDownloadRow::getDiseaseName)),
			new DownloadHeader<>("Evidence Code", (DiseaseDownloadRow::getEvidenceCode)),
			new DownloadHeader<>("Evidence Code Name", (DiseaseDownloadRow::getEvidenceCodeName)),
			new DownloadHeader<>("Based On ID", (DiseaseDownloadRow::getBasedOnID)),
			new DownloadHeader<>("Based On Name", (DiseaseDownloadRow::getBasedOnName)),
			new DownloadHeader<>("Source", (DiseaseDownloadRow::getSource)),
			new DownloadHeader<>("Reference", (DiseaseDownloadRow::getReference)),
			new DownloadHeader<>("Date", (DiseaseDownloadRow::getDateAssigned))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public String getAllRowsForGenesAndAlleles(List<DiseaseAnnotation> diseaseAnnotationsGenes,
											   List<DiseaseAnnotation> diseaseAnnotationsAlleles,
											   List<DiseaseAnnotation> diseaseAnnotationsModels) {

		// convert collection of DiseaseAnnotation records to DiseaseDownloadRow records
		List<DiseaseDownloadRow> list = new ArrayList<>();
		if (!diseaseAnnotationsGenes.isEmpty())
			list = getDownloadRowsFromGenes(diseaseAnnotationsGenes);
		if (!diseaseAnnotationsAlleles.isEmpty())
			list.addAll(getDiseaseDownloadRowsFromAlleles(diseaseAnnotationsAlleles));
		if (!diseaseAnnotationsModels.isEmpty())
			list.addAll(getDiseaseModelDownloadRows(diseaseAnnotationsModels));

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Species ID", (DiseaseDownloadRow::getSpeciesID)),
			new DownloadHeader<>("Species Name", (DiseaseDownloadRow::getSpeciesName)),
			new DownloadHeader<>("Entity Type", (DiseaseDownloadRow::getEntityType)),
			new DownloadHeader<>("Entity ID", (DiseaseDownloadRow::getMainEntityID)),
			new DownloadHeader<>("Entity Symbol", (DiseaseDownloadRow::getMainEntitySymbol)),
			new DownloadHeader<>("Association Type", (DiseaseDownloadRow::getAssociation)),
			new DownloadHeader<>("Disease ID", (DiseaseDownloadRow::getDiseaseID)),
			new DownloadHeader<>("Disease Name", (DiseaseDownloadRow::getDiseaseName)),
			new DownloadHeader<>("Based On ID", (DiseaseDownloadRow::getBasedOnID)),
			new DownloadHeader<>("Based On Name", (DiseaseDownloadRow::getBasedOnName)),
			new DownloadHeader<>("Inferred from Entity ID", (DiseaseDownloadRow::getGeneticEntityID)),
			new DownloadHeader<>("Inferred from Entity Name", (DiseaseDownloadRow::getGeneticEntityName)),
//				  new DownloadHeader<>("Genetic Entity Type", (DiseaseDownloadRow::getGeneticEntityType)),
			new DownloadHeader<>("Evidence Code", (DiseaseDownloadRow::getEvidenceCode)),
			new DownloadHeader<>("Evidence Code Name", (DiseaseDownloadRow::getEvidenceCodeName)),
			new DownloadHeader<>("Source", (DiseaseDownloadRow::getSource)),
			new DownloadHeader<>("Reference", (DiseaseDownloadRow::getReference)),
			new DownloadHeader<>("Date", (DiseaseDownloadRow::getDateAssigned))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public List<DiseaseDownloadRow> getDownloadRowsFromGeneDiseaseAnnotations(List<? extends DiseaseAnnotationDocument> diseaseAnnotations) {
		return diseaseAnnotations.stream()
			.map(this::getGeneDiseaseDownloadRow)
			.flatMap(Collection::stream)
			.collect(Collectors.toList());
	}

	public List<DiseaseDownloadRow> getDownloadRowsFromGenes(List<DiseaseAnnotation> diseaseAnnotations) {
		denormalizeAnnotations(diseaseAnnotations);
		return diseaseAnnotations.stream()
			.map(annotation -> annotation.getPrimaryAnnotatedEntities().stream()
				.map(entity -> entity.getPublicationEvidenceCodes().stream()
					.map(join -> {
						if (CollectionUtils.isNotEmpty(annotation.getOrthologyGenes()))
							return annotation.getOrthologyGenes().stream()
								.map(gene -> getDiseaseDownloadRow(annotation, entity, join, gene))
								.collect(Collectors.toList());
						else
							return List.of(getDiseaseDownloadRow(annotation, entity, join, null));
					})
					.flatMap(Collection::stream)
					.collect(Collectors.toList()))
				.flatMap(Collection::stream)
				.collect(Collectors.toList()))
			.flatMap(Collection::stream)
			.collect(Collectors.toList());
	}

	private List<DiseaseDownloadRow> getGeneDiseaseDownloadRow(DiseaseAnnotationDocument annotation) {
		List<DiseaseDownloadRow> list = new ArrayList<>();
		annotation.getPrimaryAnnotations().forEach(primaryAnnotation -> {
			DiseaseDownloadRow row = getBaseDiseaseDownloadRow(annotation, null, primaryAnnotation);
			extracted(annotation, primaryAnnotation, row);
			list.add(row);
		});
		return list;
	}

	private static void extracted(DiseaseAnnotationDocument annotation, org.alliancegenome.curation_api.model.entities.DiseaseAnnotation primaryAnnotation, DiseaseDownloadRow row) {
		String subjectTaxonCurie = null;
		String subjectTaxonName = null;
		final String subjectCurie;
		String subjectSymbol = null;
		if (annotation instanceof GeneDiseaseAnnotationDocument document) {
			org.alliancegenome.curation_api.model.entities.Gene subject = document.getSubject();
			subjectTaxonCurie = subject.getTaxon().getCurie();
			subjectTaxonName = subject.getTaxon().getName();
			subjectCurie = subject.getCurie();
			subjectSymbol = subject.getGeneSymbol().getDisplayText();
		} else if (annotation instanceof AlleleDiseaseAnnotationDocument document) {
			org.alliancegenome.curation_api.model.entities.Allele subject = document.getSubject();
			subjectTaxonCurie = subject.getTaxon().getCurie();
			subjectTaxonName = subject.getTaxon().getName();
			subjectCurie = subject.getCurie();
			subjectSymbol = subject.getAlleleSymbol().getDisplayText();
		} else {
			subjectCurie = null;
		}
		row.setSpeciesID(subjectTaxonCurie);
		row.setSpeciesName(subjectTaxonName);
		row.setMainEntityID(subjectCurie);
		row.setMainEntitySymbol(subjectSymbol);
		// needs better generics or have subject attribute on the parent class (DiseaseAnnotation)
		if (primaryAnnotation instanceof AGMDiseaseAnnotation pAnnotation) {
			row.setGeneticEntityID(pAnnotation.getSubject().getCurie());
			row.setGeneticEntityName(pAnnotation.getSubject().getName());
			row.setGeneticEntityType(pAnnotation.getSubject().getSubtype().getName());
			List<org.alliancegenome.curation_api.model.entities.Gene> assertedGenes = pAnnotation.getAssertedGenes();
			if (CollectionUtils.isNotEmpty(assertedGenes)) {
				row.setAssertedGeneID(assertedGenes.stream().filter(gene -> !gene.getCurie().equals(subjectCurie))
					.map(CurieAuditedObject::getCurie).collect(Collectors.joining("|")));
				row.setAssertedGeneName(assertedGenes.stream().filter(gene -> !gene.getCurie().equals(subjectCurie))
					.map(gene -> gene.getGeneSymbol().getDisplayText()).collect(Collectors.joining("|")));
			}

		}
		if (annotation.getGeneratedRelationString().contains("via_orthology")) {
			row.setGeneticEntityID(subjectCurie);
			row.setGeneticEntityName(subjectSymbol);
			row.setGeneticEntityType("gene");
		} else {
			if (primaryAnnotation instanceof GeneDiseaseAnnotation pAnnotation) {
				row.setGeneticEntityID(pAnnotation.getSubject().getCurie());
				row.setGeneticEntityName(pAnnotation.getSubject().getGeneSymbol().getDisplayText());
				if (pAnnotation.getSgdStrainBackground() != null) {
					row.setStrainBackgroundID(pAnnotation.getSgdStrainBackground().getCurie());
					row.setStrainBackgroundName(pAnnotation.getSgdStrainBackground().getName());
				}
				row.setGeneticEntityType("gene");
			}
			if (primaryAnnotation instanceof AlleleDiseaseAnnotation pAnnotation) {
				row.setGeneticEntityID(pAnnotation.getSubject().getCurie());
				row.setGeneticEntityName(pAnnotation.getSubject().getAlleleSymbol().getDisplayText());
				row.setGeneticEntityType("Allele");
			}
		}
		if (primaryAnnotation.getDiseaseGeneticModifierRelation() != null) {
			row.setDiseaseGeneticModifierRelation(primaryAnnotation.getDiseaseGeneticModifierRelation().getName());
		}
		row.setReference(primaryAnnotation.getSingleReference().getReferenceID());
		row.setSource(primaryAnnotation.getDataProviderString());
		List<String> urlExpceptionHandler = List.of("MGI", "SGD", "OMIM");
		DataProvider dataProvider = primaryAnnotation.getDataProvider();
		if (dataProvider != null && dataProvider.getCrossReference() != null) {
			String urlTemplate = dataProvider.getCrossReference().getResourceDescriptorPage().getUrlTemplate();
			if (urlExpceptionHandler.contains(dataProvider.getSourceOrganization().getAbbreviation())) {
				// remove the prefix in the template as the prefix is already in the curie.
				urlTemplate = urlTemplate.replace(dataProvider.getSourceOrganization().getAbbreviation() + ":", "");
			}
			urlTemplate = urlTemplate.replace("[%s]", dataProvider.getCrossReference().getReferencedCurie());
			row.setSourceUrl(urlTemplate);
		}
		if (primaryAnnotation.getDateCreated() != null) {
			row.setDateAssigned(primaryAnnotation.getDateCreated().toString());
		}
		if (CollectionUtils.isNotEmpty(primaryAnnotation.getWith())) {
			row.setBasedOnID(primaryAnnotation.getWith().stream().map(CurieAuditedObject::getCurie).collect(Collectors.joining("|")));
			row.setBasedOnName(primaryAnnotation.getWith().stream().map(gene -> gene.getGeneSymbol().getDisplayText()).collect(Collectors.joining("|")));
		}
		if (primaryAnnotation.getGeneticSex() != null) {
			row.setGeneticSex(primaryAnnotation.getGeneticSex().getName());
		}
		row.setGeneticEntityAssociation(getGeneratedRelationString(primaryAnnotation.getRelation().getName(), primaryAnnotation.getNegated()));
		if (CollectionUtils.isNotEmpty(primaryAnnotation.getRelatedNotes())) {
			row.setNote(primaryAnnotation.getRelatedNotes().stream().map(note -> {
				String freeNote = note.getFreeText().replace("\n", " ");
				String noteType = note.getNoteType().getName();
				return switch (noteType) {
					case "disease_note" -> "Note: " + freeNote;
					case "disease_summary" -> "Summary: " + freeNote;
					default -> "";
				};
			}).collect(Collectors.joining("|")));
		}
		if (primaryAnnotation.getAnnotationType() != null) {
			row.setAnnotationType(primaryAnnotation.getAnnotationType().getName());
		}
		if (CollectionUtils.isNotEmpty(primaryAnnotation.getDiseaseQualifiers())) {
			row.setDiseaseQualifier(primaryAnnotation.getDiseaseQualifiers().stream()
				.map(term -> term.getName().replace("_", " ")).collect(Collectors.joining("|")));
		}
		List<BiologicalEntity> diseaseGeneticModifiers = primaryAnnotation.getDiseaseGeneticModifiers();
		if (CollectionUtils.isNotEmpty(diseaseGeneticModifiers)) {
			row.setDiseaseGeneticModifierID(diseaseGeneticModifiers.stream().map(CurieAuditedObject::getCurie).collect(Collectors.joining("|")));
			StringJoiner joiner = new StringJoiner("|");
			diseaseGeneticModifiers.forEach(entity -> {
				if (entity instanceof org.alliancegenome.curation_api.model.entities.Gene gene) {
					joiner.add(gene.getGeneSymbol().getFormatText());
				}
				if (entity instanceof org.alliancegenome.curation_api.model.entities.Allele allele) {
					joiner.add(allele.getAlleleSymbol().getFormatText());
				}
				if (entity instanceof org.alliancegenome.curation_api.model.entities.AffectedGenomicModel model) {
					joiner.add(model.getName());
				}
			});
			row.setDiseaseGeneticModifierName(joiner.toString());
		}
		if (CollectionUtils.isNotEmpty(primaryAnnotation.getConditionRelations())) {
			String condition = primaryAnnotation.getConditionRelations().stream().map(conditionRelation -> {
				return conditionRelation.getConditionRelationType().getName() + ": " + conditionRelation.getConditions().stream().map(ExperimentalCondition::getConditionSummary).collect(Collectors.joining(";"));
			}).collect(Collectors.joining("|"));
			row.setExperimentalCondition(condition);
		}
	}


	private static String getGeneratedRelationString(String relation, Boolean negated) {
		if (!negated)
			return relation;
		return relation.replaceFirst("_", "_not_");
	}

	private DiseaseDownloadRow getDiseaseDownloadRow(DiseaseAnnotation annotation, PrimaryAnnotatedEntity entity, PublicationJoin join, Gene homologousGene) {
		DiseaseDownloadRow row = getBaseDownloadRow(annotation, join, homologousGene);

		row.setEntityType(annotation.getGeneticEntityType());
		row.setMainEntityID(annotation.getGene().getPrimaryKey());
		row.setMainEntitySymbol(annotation.getGene().getSymbol());
		row.setGeneticEntityID(entity.getId());
		row.setGeneticEntityName(entity.getDisplayName());
		row.setGeneticEntityType(entity.getType());
		row.setSpeciesID(annotation.getGene().getSpecies().getPrimaryKey());
		row.setSpeciesName(annotation.getGene().getSpecies().getName());

		return row;
	}

	private DiseaseDownloadRow getBaseDiseaseDownloadRow(DiseaseAnnotationDocument annotation, org.alliancegenome.curation_api.model.entities.Gene homologousGene, org.alliancegenome.curation_api.model.entities.DiseaseAnnotation primaryAnnotation) {
		DiseaseDownloadRow row = new DiseaseDownloadRow();
		row.setAssociation(annotation.getGeneratedRelationString());
		row.setDiseaseID(annotation.getObject().getCurie());
		row.setDiseaseName(annotation.getObject().getName());
		row.setSource(primaryAnnotation.getDataProviderString());
		if (homologousGene != null) {
			row.setBasedOnID(homologousGene.getCurie());
			row.setBasedOnName(homologousGene.getGeneSymbol().getDisplayText());
		}
/*
		if (CollectionUtils.isNotEmpty(annotation.getProviders()) &&
				annotation.getProviders().size() == 1) {
			final Map<String, CrossReference> crossReferenceMap = annotation.getProviders().get(0);
			String sourceProvider = crossReferenceMap.get("sourceProvider").getName();
			if (crossReferenceMap.size() == 2) {
				sourceProvider += " via ";
				sourceProvider += crossReferenceMap.get("loadProvider").getName();
			}
			row.setSource(sourceProvider);
		}
*/
		StringJoiner evidenceJoiner = getStringJoiner(primaryAnnotation, org.alliancegenome.curation_api.model.entities.ontology.ECOTerm::getCurie);
		row.setEvidenceCode(evidenceJoiner.toString());

		StringJoiner evidenceJoinerName = getStringJoiner(primaryAnnotation, org.alliancegenome.curation_api.model.entities.ontology.ECOTerm::getName);
		row.setEvidenceCodeName(evidenceJoinerName.toString());

		StringJoiner evidenceJoinerAbbreviation = getStringJoiner(primaryAnnotation, org.alliancegenome.curation_api.model.entities.ontology.ECOTerm::getAbbreviation);
		row.setEvidenceAbbreviation(evidenceJoinerAbbreviation.toString());

		return row;
	}

	private static StringJoiner getStringJoiner(GeneDiseaseAnnotationDocument annotation, Function<org.alliancegenome.curation_api.model.entities.ontology.ECOTerm, String> function) {
		StringJoiner evidenceJoiner = new StringJoiner("|");
		if (CollectionUtils.isNotEmpty(annotation.getEvidenceCodes())) {
			Set<String> evidenceCodes = annotation.getEvidenceCodes()
				.stream()
				.map(function)
				.collect(Collectors.toSet());

			evidenceCodes.forEach(evidenceJoiner::add);
		}
		return evidenceJoiner;
	}

	private static StringJoiner getStringJoiner(org.alliancegenome.curation_api.model.entities.DiseaseAnnotation annotation, Function<org.alliancegenome.curation_api.model.entities.ontology.ECOTerm, String> function) {
		StringJoiner evidenceJoiner = new StringJoiner("|");
		if (CollectionUtils.isNotEmpty(annotation.getEvidenceCodes())) {
			Set<String> evidenceCodes = annotation.getEvidenceCodes()
				.stream()
				.map(function)
				.collect(Collectors.toSet());

			evidenceCodes.forEach(evidenceJoiner::add);
		}
		return evidenceJoiner;
	}

	private DiseaseDownloadRow getBaseDownloadRow(DiseaseAnnotation annotation, PublicationJoin join, Gene homologousGene) {
		DiseaseDownloadRow row = new DiseaseDownloadRow();
		row.setAssociation(annotation.getAssociationType());
		row.setDiseaseID(annotation.getDisease().getPrimaryKey());
		row.setDiseaseName(annotation.getDisease().getName());
		row.setSource(annotation.getSource().getName());
		if (CollectionUtils.isNotEmpty(annotation.getProviders()) &&
			annotation.getProviders().size() == 1) {
			final Map<String, CrossReference> crossReferenceMap = annotation.getProviders().get(0);
			String sourceProvider = crossReferenceMap.get("sourceProvider").getName();
			if (crossReferenceMap.size() == 2) {
				sourceProvider += " via ";
				sourceProvider += crossReferenceMap.get("loadProvider").getName();
			}
			row.setSource(sourceProvider);
		}
		if (homologousGene != null) {
			row.setBasedOnID(homologousGene.getPrimaryKey());
			row.setBasedOnName(homologousGene.getSymbol());
		}
		StringJoiner evidenceJoiner = new StringJoiner("|");
		if (CollectionUtils.isNotEmpty(join.getEcoCode())) {
			Set<String> evidenceCodes = join.getEcoCode()
				.stream()
				.map(ECOTerm::getPrimaryKey)
				.collect(Collectors.toSet());

			evidenceCodes.forEach(evidenceJoiner::add);
		}
		row.setEvidenceCode(evidenceJoiner.toString());

		StringJoiner evidenceJoinerName = new StringJoiner("|");
		if (CollectionUtils.isNotEmpty(join.getEcoCode())) {
			Set<String> evidenceCodes = join.getEcoCode()
				.stream()
				.map(ECOTerm::getName)
				.collect(Collectors.toSet());

			evidenceCodes.forEach(evidenceJoinerName::add);
		}
		row.setEvidenceCodeName(evidenceJoinerName.toString());
		row.setReference(join.getPublication().getPubId());
		row.setDateAssigned(join.getDateAssigned());
		return row;
	}

	public String getAllRowsForModel(List<DiseaseAnnotation> diseaseAnnotations) {

		List<DiseaseDownloadRow> list = getDiseaseModelDownloadRows(diseaseAnnotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Model ID", (DiseaseDownloadRow::getMainEntityID)),
			new DownloadHeader<>("Model Symbol", (DiseaseDownloadRow::getMainEntitySymbol)),
			new DownloadHeader<>("Species ID", (DiseaseDownloadRow::getSpeciesID)),
			new DownloadHeader<>("Species Name", (DiseaseDownloadRow::getSpeciesName)),
			new DownloadHeader<>("Disease ID", (DiseaseDownloadRow::getDiseaseID)),
			new DownloadHeader<>("Disease Name", (DiseaseDownloadRow::getDiseaseName)),
			new DownloadHeader<>("Evidence Code", (DiseaseDownloadRow::getEvidenceCode)),
			new DownloadHeader<>("Evidence Code Name", (DiseaseDownloadRow::getEvidenceCodeName)),
			new DownloadHeader<>("Source", (DiseaseDownloadRow::getSource)),
			new DownloadHeader<>("Reference", (DiseaseDownloadRow::getReference))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public List<DiseaseDownloadRow> getDiseaseModelDownloadRows(List<DiseaseAnnotation> diseaseAnnotations) {
		return diseaseAnnotations.stream()
			.map(annotation -> annotation.getPublicationJoins().stream()
				.map(join -> {
					DiseaseDownloadRow row = getBaseDownloadRow(annotation, join, null);
					row.setMainEntityID(annotation.getModel().getPrimaryKey());
					row.setMainEntitySymbol(annotation.getModel().getNameText());
					row.setSpeciesID(annotation.getModel().getSpecies().getPrimaryKey());
					row.setSpeciesName(annotation.getModel().getSpecies().getName());
					row.setEntityType(annotation.getModel().getSubtype());
					return row;
				})
				.collect(Collectors.toList()))
			.flatMap(Collection::stream)
			.collect(Collectors.toList());
	}

	public String getAllRowsForAllele(List<DiseaseAnnotation> diseaseAnnotations) {
		List<DiseaseDownloadRow> list = getDiseaseDownloadRowsFromAlleles(diseaseAnnotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Allele ID", (DiseaseDownloadRow::getMainEntityID)),
			new DownloadHeader<>("Allele Symbol", (DiseaseDownloadRow::getMainEntitySymbol)),
			new DownloadHeader<>("Genetic Entity ID", (DiseaseDownloadRow::getGeneticEntityID)),
			new DownloadHeader<>("Genetic Entity Name", (DiseaseDownloadRow::getGeneticEntityName)),
			new DownloadHeader<>("Genetic Entity Type", (DiseaseDownloadRow::getGeneticEntityType)),
			new DownloadHeader<>("Species ID", (DiseaseDownloadRow::getSpeciesID)),
			new DownloadHeader<>("Species Name", (DiseaseDownloadRow::getSpeciesName)),
			new DownloadHeader<>("Association", (DiseaseDownloadRow::getAssociation)),
			new DownloadHeader<>("Disease ID", (DiseaseDownloadRow::getDiseaseID)),
			new DownloadHeader<>("Disease Name", (DiseaseDownloadRow::getDiseaseName)),
			new DownloadHeader<>("Evidence Code", (DiseaseDownloadRow::getEvidenceCode)),
			new DownloadHeader<>("Evidence Code Name", (DiseaseDownloadRow::getEvidenceCodeName)),
			new DownloadHeader<>("Source", (DiseaseDownloadRow::getSource)),
			new DownloadHeader<>("Reference", (DiseaseDownloadRow::getReference))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public List<DiseaseDownloadRow> getDiseaseDownloadRowsFromAlleles(List<DiseaseAnnotation> diseaseAnnotations) {
		denormalizeAnnotations(diseaseAnnotations);

		return diseaseAnnotations.stream()
			.map(annotation -> annotation.getPrimaryAnnotatedEntities().stream()
				.map(entity -> entity.getPublicationEvidenceCodes().stream()
					.map(join -> {
						DiseaseDownloadRow row = getBaseDownloadRow(annotation, join, null);
						row.setMainEntityID(annotation.getFeature().getPrimaryKey());
						row.setMainEntitySymbol(annotation.getFeature().getSymbolText());
						row.setEntityType(annotation.getGeneticEntityType());
						if (!entity.getType().equals(GeneticEntity.CrossReferenceType.GENE.getDisplayName())) {
							row.setGeneticEntityID(entity.getId());
							row.setGeneticEntityName(entity.getDisplayName());
							row.setGeneticEntityType(entity.getType());
							row.setSpeciesID(annotation.getFeature().getSpecies().getPrimaryKey());
							row.setSpeciesName(annotation.getFeature().getSpecies().getName());
						} else {
							row.setSpeciesID(annotation.getGene().getSpecies().getPrimaryKey());
							row.setSpeciesName(annotation.getGene().getSpecies().getName());
						}
						return row;
					})
					.collect(Collectors.toList()))
				.flatMap(Collection::stream)
				.collect(Collectors.toList()))
			.flatMap(Collection::stream)
			.collect(Collectors.toList());
	}

	private void denormalizeGeneDiseaseAnnotations(List<GeneDiseaseAnnotationDocument> diseaseAnnotations) {
		// add genetic entity info for annotations with pure genes
		diseaseAnnotations.stream()
			.filter(annotation -> CollectionUtils.isEmpty(annotation.getPrimaryAnnotations()))
			.forEach(annotation -> {
				org.alliancegenome.curation_api.model.entities.DiseaseAnnotation entity = createNewPrimaryDiseaseAnnotatedEntity(annotation, null);
				annotation.addPrimaryAnnotation(entity);
			});

		// add genetic entity info for annotations that are not accounted in PAE
/*
		diseaseAnnotations.forEach(annotation -> annotation.getPublicationJoins().stream()
				// filter out the ones that are not found in an individual PAE
				.filter(join -> annotation.getPrimaryAnnotatedEntities().stream()
						.noneMatch(entity -> String.join(":", entity.getPublicationEvidenceCodes().stream()
								.map(PublicationJoin::toString).collect(Collectors.toList())).contains(join.toString())))
				.forEach(join -> {
					PrimaryAnnotatedEntity entity = createNewPrimaryAnnotatedEntity(annotation, join);
					annotation.addPrimaryAnnotatedEntityDuplicate(entity);
				}));
*/
	}

	private void denormalizeAnnotations(List<? extends DiseaseAnnotation> diseaseAnnotations) {
		// add genetic entity info for annotations with pure genes
		diseaseAnnotations.stream()
			.filter(annotation -> CollectionUtils.isEmpty(annotation.getPrimaryAnnotatedEntities()))
			.forEach(annotation -> {
				PrimaryAnnotatedEntity entity = createNewPrimaryAnnotatedEntity(annotation, null);
				annotation.addPrimaryAnnotatedEntity(entity);
			});

		// add genetic entity info for annotations that are not accounted in PAE
		diseaseAnnotations.forEach(annotation -> {
				if (annotation.getPublicationJoins() != null) {
					annotation.getPublicationJoins().stream()
						// filter out the ones that are not found in an individual PAE
						.filter(join -> annotation.getPrimaryAnnotatedEntities().stream()
							.noneMatch(entity -> String.join(":", entity.getPublicationEvidenceCodes().stream()
								.map(PublicationJoin::toString).collect(Collectors.toList())).contains(join.toString())))
						.forEach(join -> {
							PrimaryAnnotatedEntity entity = createNewPrimaryAnnotatedEntity(annotation, join);
							annotation.addPrimaryAnnotatedEntityDuplicate(entity);
						});
				}
			}

		);
	}

	private org.alliancegenome.curation_api.model.entities.DiseaseAnnotation createNewPrimaryDiseaseAnnotatedEntity(GeneDiseaseAnnotationDocument annotation, PublicationJoin join) {
/*
		org.alliancegenome.curation_api.model.entities.DiseaseAnnotation entity = new org.alliancegenome.curation_api.model.entities.DiseaseAnnotation();
		if (annotation.getGene() != null) {
			entity.setId(annotation.getGene().getPrimaryKey());
			entity.setName(annotation.getGene().getSymbol());
			entity.setType(GeneticEntity.CrossReferenceType.GENE);
		} else {
			entity.setId(annotation.getFeature().getPrimaryKey());
			entity.setName(annotation.getFeature().getSymbolText());
			entity.setType(GeneticEntity.CrossReferenceType.ALLELE);
		}
		if (join == null)
			entity.setPublicationEvidenceCodes(annotation.getPublicationJoins());
		else {
			entity.addPublicationEvidenceCode(join);
		}
		return entity;
*/
		return null;
	}

	private PrimaryAnnotatedEntity createNewPrimaryAnnotatedEntity(DiseaseAnnotation annotation, PublicationJoin join) {
		PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
		if (annotation.getGene() != null) {
			entity.setId(annotation.getGene().getPrimaryKey());
			entity.setName(annotation.getGene().getSymbol());
			entity.setType(GeneticEntity.CrossReferenceType.GENE.getDisplayName());
		} else {
			entity.setId(annotation.getFeature().getPrimaryKey());
			entity.setName(annotation.getFeature().getSymbolText());
			entity.setType(GeneticEntity.CrossReferenceType.ALLELE.getDisplayName());
		}
		if (join == null)
			entity.setPublicationEvidenceCodes(annotation.getPublicationJoins());
		else {
			entity.addPublicationEvidenceCode(join);
		}
		return entity;
	}

	public String getEmpiricalDiseaseByGene(List<DiseaseAnnotation> diseaseAnnotations) {

		denormalizeAnnotations(diseaseAnnotations);

		List<DiseaseDownloadRow> list = diseaseAnnotations.stream()
			.map(annotation -> annotation.getPrimaryAnnotatedEntities().stream()
				.map(entity -> entity.getPublicationEvidenceCodes().stream()
					.map(join -> {
						DiseaseDownloadRow row = getBaseDownloadRow(annotation, join, null);
						row.setMainEntityID(annotation.getGene().getPrimaryKey());
						row.setMainEntitySymbol(annotation.getGene().getSymbol());
						row.setGeneticEntityID(entity.getId());
						row.setGeneticEntityName(entity.getName());
						row.setGeneticEntityType(entity.getType());
						row.setSpeciesID(annotation.getGene().getSpecies().getPrimaryKey());
						row.setSpeciesName(annotation.getGene().getSpecies().getName());
						return row;
					})
					.collect(Collectors.toList()))
				.flatMap(Collection::stream)
				.collect(Collectors.toList()))
			.flatMap(Collection::stream)
			.collect(Collectors.toList());

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Species ID", (DiseaseDownloadRow::getSpeciesID)),
			new DownloadHeader<>("Species Name", (DiseaseDownloadRow::getSpeciesName)),
			new DownloadHeader<>("Gene ID", (DiseaseDownloadRow::getMainEntityID)),
			new DownloadHeader<>("Gene Symbol", (DiseaseDownloadRow::getMainEntitySymbol)),
			new DownloadHeader<>("Genetic Entity ID", (DiseaseDownloadRow::getGeneticEntityID)),
			new DownloadHeader<>("Genetic Entity Name", (DiseaseDownloadRow::getGeneticEntityName)),
			new DownloadHeader<>("Genetic Entity Type", (DiseaseDownloadRow::getGeneticEntityType)),
			new DownloadHeader<>("Disease ID", (DiseaseDownloadRow::getDiseaseID)),
			new DownloadHeader<>("Disease Name", (DiseaseDownloadRow::getDiseaseName)),
			new DownloadHeader<>("Association", (DiseaseDownloadRow::getAssociation)),
			new DownloadHeader<>("Evidence Code", (DiseaseDownloadRow::getEvidenceCode)),
			new DownloadHeader<>("Evidence Code Name", (DiseaseDownloadRow::getEvidenceCodeName)),
			new DownloadHeader<>("Source", (DiseaseDownloadRow::getSource)),
			new DownloadHeader<>("Based On ID", (DiseaseDownloadRow::getBasedOnID)),
			new DownloadHeader<>("Based On Name", (DiseaseDownloadRow::getBasedOnName)),
			new DownloadHeader<>("Reference", (DiseaseDownloadRow::getReference))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public String getAllRowsForAlleleDiseaseAnnotations(List<AlleleDiseaseAnnotationDocument> diseaseAnnotations) {

		// convert collection of DiseaseAnnotation records to DiseaseDownloadRow records
		List<DiseaseDownloadRow> list = getDownloadRowsFromGeneDiseaseAnnotations(diseaseAnnotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Allele ID", (DiseaseDownloadRow::getMainEntityID)),
			new DownloadHeader<>("Allele Symbol", (DiseaseDownloadRow::getMainEntitySymbol)),
			new DownloadHeader<>("Genetic Entity ID", (DiseaseDownloadRow::getGeneticEntityID)),
			new DownloadHeader<>("Genetic Entity Name", (DiseaseDownloadRow::getGeneticEntityName)),
			new DownloadHeader<>("Genetic Entity Type", (DiseaseDownloadRow::getGeneticEntityType)),
			new DownloadHeader<>("Species ID", (DiseaseDownloadRow::getSpeciesID)),
			new DownloadHeader<>("Species Name", (DiseaseDownloadRow::getSpeciesName)),
			new DownloadHeader<>("Allele Association", (DiseaseDownloadRow::getAssociation)),
			new DownloadHeader<>("Genetic Entity Association", (DiseaseDownloadRow::getGeneticEntityAssociation)),
			new DownloadHeader<>("Disease Qualifier", (DiseaseDownloadRow::getDiseaseQualifier)),
			new DownloadHeader<>("Disease ID", (DiseaseDownloadRow::getDiseaseID)),
			new DownloadHeader<>("Disease Name", (DiseaseDownloadRow::getDiseaseName)),
			new DownloadHeader<>("Evidence Code", (DiseaseDownloadRow::getEvidenceCode)),
			new DownloadHeader<>("Evidence Code Abbreviation", (DiseaseDownloadRow::getEvidenceAbbreviation)),
			new DownloadHeader<>("Evidence Code Name", (DiseaseDownloadRow::getEvidenceCodeName)),
			new DownloadHeader<>("Experimental Conditions", (DiseaseDownloadRow::getExperimentalCondition)),
			new DownloadHeader<>("Genetic Modifier Relation", (DiseaseDownloadRow::getDiseaseGeneticModifierRelation)),
			new DownloadHeader<>("Genetic Modifier IDs", (DiseaseDownloadRow::getDiseaseGeneticModifierID)),
			new DownloadHeader<>("Genetic Modifier Names", (DiseaseDownloadRow::getDiseaseGeneticModifierName)),
			new DownloadHeader<>("Genetic Sex", (DiseaseDownloadRow::getGeneticSex)),
			new DownloadHeader<>("Notes", (DiseaseDownloadRow::getNote)),
			new DownloadHeader<>("Annotation Type", (DiseaseDownloadRow::getAnnotationType)),
			new DownloadHeader<>("Source", (DiseaseDownloadRow::getSource)),
			new DownloadHeader<>("Source URL", (DiseaseDownloadRow::getSourceUrl)),
			new DownloadHeader<>("Reference", (DiseaseDownloadRow::getReference)),
			new DownloadHeader<>("Date", (DiseaseDownloadRow::getDateAssigned))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}


	public String getAllRowsForAssociatedGenes(List<GeneDiseaseAnnotationDocument> diseaseAnnotations) {
		// convert collection of DiseaseAnnotation records to DiseaseDownloadRow records
		List<DiseaseDownloadRow> list = getDownloadRowsFromGeneDiseaseAnnotations(diseaseAnnotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Species ID", (DiseaseDownloadRow::getSpeciesID)),
			new DownloadHeader<>("Species Name", (DiseaseDownloadRow::getSpeciesName)),
			new DownloadHeader<>("Gene ID", (DiseaseDownloadRow::getMainEntityID)),
			new DownloadHeader<>("Gene Symbol", (DiseaseDownloadRow::getMainEntitySymbol)),
			new DownloadHeader<>("Additional Implicated Gene ID", (DiseaseDownloadRow::getAssertedGeneID)),
			new DownloadHeader<>("Additional Implicated Gene Symbol", (DiseaseDownloadRow::getAssertedGeneName)),
			new DownloadHeader<>("Gene Association", (DiseaseDownloadRow::getAssociation)),
			new DownloadHeader<>("Genetic Entity ID", (DiseaseDownloadRow::getGeneticEntityID)),
			new DownloadHeader<>("Genetic Entity Name", (DiseaseDownloadRow::getGeneticEntityName)),
			new DownloadHeader<>("Genetic Entity Type", (DiseaseDownloadRow::getGeneticEntityType)),
			new DownloadHeader<>("Genetic Entity Association", (DiseaseDownloadRow::getGeneticEntityAssociation)),
			new DownloadHeader<>("Disease Qualifier", (DiseaseDownloadRow::getDiseaseQualifier)),
			new DownloadHeader<>("Disease ID", (DiseaseDownloadRow::getDiseaseID)),
			new DownloadHeader<>("Disease Name", (DiseaseDownloadRow::getDiseaseName)),
			new DownloadHeader<>("Evidence Code", (DiseaseDownloadRow::getEvidenceCode)),
			new DownloadHeader<>("Evidence Code Abbreviation", (DiseaseDownloadRow::getEvidenceAbbreviation)),
			new DownloadHeader<>("Evidence Code Name", (DiseaseDownloadRow::getEvidenceCodeName)),
			new DownloadHeader<>("Experimental Conditions", (DiseaseDownloadRow::getExperimentalCondition)),
			new DownloadHeader<>("Genetic Modifier Relation", (DiseaseDownloadRow::getDiseaseGeneticModifierRelation)),
			new DownloadHeader<>("Genetic Modifier IDs", (DiseaseDownloadRow::getDiseaseGeneticModifierID)),
			new DownloadHeader<>("Genetic Modifier Names", (DiseaseDownloadRow::getDiseaseGeneticModifierName)),
			new DownloadHeader<>("Strain Background ID", (DiseaseDownloadRow::getStrainBackgroundID)),
			new DownloadHeader<>("Strain Background Name", (DiseaseDownloadRow::getStrainBackgroundName)),
			new DownloadHeader<>("Genetic Sex", (DiseaseDownloadRow::getGeneticSex)),
			new DownloadHeader<>("Notes", (DiseaseDownloadRow::getNote)),
			new DownloadHeader<>("Annotation Type", (DiseaseDownloadRow::getAnnotationType)),
			new DownloadHeader<>("Based On ID", (DiseaseDownloadRow::getBasedOnID)),
			new DownloadHeader<>("Based On Symbol", (DiseaseDownloadRow::getBasedOnName)),
			new DownloadHeader<>("Source", (DiseaseDownloadRow::getSource)),
			new DownloadHeader<>("Source URL", (DiseaseDownloadRow::getSourceUrl)),
			new DownloadHeader<>("Reference", (DiseaseDownloadRow::getReference)),
			new DownloadHeader<>("Date", (DiseaseDownloadRow::getDateAssigned))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}
}
