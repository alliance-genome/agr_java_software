package org.alliancegenome.api.translators.tdf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.AGMDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.DiseaseAnnotationDocument;
import org.alliancegenome.api.entity.GeneDiseaseAnnotationDocument;
import org.alliancegenome.core.helpers.DiseaseAnnotationHelper;
import org.alliancegenome.core.translators.tdf.DiseaseDownloadRow;
import org.alliancegenome.core.translators.tdf.DownloadHeader;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.BiologicalEntity;
import org.alliancegenome.curation_api.model.entities.ConditionRelation;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Organization;
import org.alliancegenome.curation_api.model.entities.base.SubmittedObject;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.apache.commons.collections.CollectionUtils;

public class DiseaseAnnotationToTdfTranslator extends BaseToTdfTranslator {

	public String getAllRowsForGeneDiseaseAnnotations(List<GeneDiseaseAnnotationDocument> diseaseAnnotations) {

		// convert collection of DiseaseAnnotation records to DiseaseDownloadRow records
		List<DiseaseDownloadRow> list = getDownloadRowsFromGeneDiseaseAnnotations(diseaseAnnotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Species ID", DiseaseDownloadRow::getSpeciesID),
			new DownloadHeader<>("Species Name", DiseaseDownloadRow::getSpeciesName),
			new DownloadHeader<>("Gene ID", DiseaseDownloadRow::getMainEntityID),
			new DownloadHeader<>("Gene Symbol", DiseaseDownloadRow::getMainEntitySymbol),
			new DownloadHeader<>("Additional Implicated Gene ID", DiseaseDownloadRow::getAssertedGeneID),
			new DownloadHeader<>("Additional Implicated Gene Symbol", DiseaseDownloadRow::getAssertedGeneName),
			new DownloadHeader<>("Gene Association", DiseaseDownloadRow::getAssociation),
			new DownloadHeader<>("Genetic Entity ID", DiseaseDownloadRow::getGeneticEntityID),
			new DownloadHeader<>("Genetic Entity Name", DiseaseDownloadRow::getGeneticEntityName),
			new DownloadHeader<>("Genetic Entity Type", DiseaseDownloadRow::getGeneticEntityType),
			new DownloadHeader<>("Genetic Entity Association", DiseaseDownloadRow::getGeneticEntityAssociation),
			new DownloadHeader<>("Disease Qualifier", DiseaseDownloadRow::getDiseaseQualifier),
			new DownloadHeader<>("Disease ID", DiseaseDownloadRow::getDiseaseID),
			new DownloadHeader<>("Disease Name", DiseaseDownloadRow::getDiseaseName),
			new DownloadHeader<>("Evidence Code", DiseaseDownloadRow::getEvidenceCode),
			new DownloadHeader<>("Evidence Code Abbreviation", DiseaseDownloadRow::getEvidenceAbbreviation),
			new DownloadHeader<>("Evidence Code Name", DiseaseDownloadRow::getEvidenceCodeName),
			new DownloadHeader<>("Experimental Conditions", DiseaseDownloadRow::getExperimentalCondition),
			new DownloadHeader<>("Genetic Modifier Relation", DiseaseDownloadRow::getDiseaseGeneticModifierRelation),
			new DownloadHeader<>("Genetic Modifier IDs", DiseaseDownloadRow::getDiseaseGeneticModifierID),
			new DownloadHeader<>("Genetic Modifier Names", DiseaseDownloadRow::getDiseaseGeneticModifierName),
			new DownloadHeader<>("Strain Background ID", DiseaseDownloadRow::getStrainBackgroundID),
			new DownloadHeader<>("Strain Background Name", DiseaseDownloadRow::getStrainBackgroundName),
			new DownloadHeader<>("Genetic Sex", DiseaseDownloadRow::getGeneticSex),
			new DownloadHeader<>("Notes", DiseaseDownloadRow::getNote),
			new DownloadHeader<>("Annotation Type", DiseaseDownloadRow::getAnnotationType),
			new DownloadHeader<>("Based On ID", DiseaseDownloadRow::getBasedOnID),
			new DownloadHeader<>("Based On Symbol", DiseaseDownloadRow::getBasedOnName),
			new DownloadHeader<>("Source", DiseaseDownloadRow::getSource),
			new DownloadHeader<>("Source URL", DiseaseDownloadRow::getSourceUrl),
			new DownloadHeader<>("Reference", DiseaseDownloadRow::getReference),
			new DownloadHeader<>("Date", DiseaseDownloadRow::getDateAssigned)
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public List<DiseaseDownloadRow> getDownloadRowsFromGeneDiseaseAnnotations(List<? extends DiseaseAnnotationDocument> diseaseAnnotations) {
		return diseaseAnnotations.stream()
			.map(this::getGeneDiseaseDownloadRow)
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

	private static void extracted(DiseaseAnnotationDocument annotation, DiseaseAnnotation primaryAnnotation, DiseaseDownloadRow row) {
		String subjectTaxonCurie = null;
		String subjectTaxonName = null;
		final String subjectID;
		String subjectSymbol = null;
		if (annotation instanceof GeneDiseaseAnnotationDocument document) {
			org.alliancegenome.curation_api.model.entities.Gene subject = document.getSubject();
			subjectTaxonCurie = subject.getTaxon().getCurie();
			subjectTaxonName = subject.getTaxon().getSpecies().getFullName();
			subjectID = subject.getIdentifier();
			subjectSymbol = subject.getGeneSymbol().getDisplayText();
		} else if (annotation instanceof AlleleDiseaseAnnotationDocument document) {
			org.alliancegenome.curation_api.model.entities.Allele subject = document.getSubject();
			subjectTaxonCurie = subject.getTaxon().getCurie();
			subjectTaxonName = subject.getTaxon().getSpecies().getFullName();
			subjectID = subject.getIdentifier();
			subjectSymbol = subject.getAlleleSymbol().getDisplayText();
		} else if (annotation instanceof AGMDiseaseAnnotationDocument document) {
			org.alliancegenome.curation_api.model.entities.AffectedGenomicModel subject = document.getSubject();
			subjectTaxonCurie = subject.getTaxon().getCurie();
			subjectTaxonName = subject.getTaxon().getSpecies().getFullName();
			subjectID = subject.getIdentifier();
			subjectSymbol = subject.getAgmFullName().getDisplayText();
			row.setEntityType(subject.getSubtype().getName());
		} else {
			subjectID = null;
		}
		row.setSpeciesID(subjectTaxonCurie);
		row.setSpeciesName(subjectTaxonName);
		row.setMainEntityID(subjectID);
		row.setMainEntitySymbol(subjectSymbol);
		// needs better generics or have subject attribute on the parent class (DiseaseAnnotation)
		if (primaryAnnotation instanceof AGMDiseaseAnnotation pAnnotation) {
			row.setGeneticEntityID(pAnnotation.getDiseaseAnnotationSubject().getIdentifier());
			row.setGeneticEntityName(pAnnotation.getDiseaseAnnotationSubject().getAgmFullName().getDisplayText());
			row.setGeneticEntityType(pAnnotation.getDiseaseAnnotationSubject().getSubtype().getName());
			List<org.alliancegenome.curation_api.model.entities.Gene> assertedGenes = pAnnotation.getAssertedGenes();
			if (CollectionUtils.isNotEmpty(assertedGenes)) {
				row.setAssertedGeneID(assertedGenes.stream().filter(gene -> !gene.getIdentifier().equals(subjectID))
					.map(SubmittedObject::getIdentifier).collect(Collectors.joining("|")));
				row.setAssertedGeneName(assertedGenes.stream().filter(gene -> !gene.getIdentifier().equals(subjectID))
					.map(gene -> gene.getGeneSymbol().getDisplayText()).collect(Collectors.joining("|")));
			}
			List<org.alliancegenome.curation_api.model.entities.Allele> assertedAlleles = pAnnotation.getAssertedAlleles();
			// Only include if the output main annotation is of type allele
			if (CollectionUtils.isNotEmpty(assertedAlleles) && annotation instanceof AlleleDiseaseAnnotationDocument alleleAnnot) {
				String primaryExternalId = alleleAnnot.getSubject().getPrimaryExternalId();
				// exclude the allele of the annotation.getSubject() object
				row.setAssertedAlleleID(assertedAlleles.stream().filter(allele -> !allele.getIdentifier().equals(primaryExternalId))
					.map(SubmittedObject::getIdentifier).collect(Collectors.joining("|")));
				row.setAssertedAlleleName(assertedAlleles.stream().filter(allele -> !allele.getIdentifier().equals(primaryExternalId))
					.map(allele -> allele.getAlleleSymbol().getDisplayText()).collect(Collectors.joining("|")));
			}

		}
		if (annotation.getGeneratedRelationString().contains("via_orthology")) {
			row.setGeneticEntityID(subjectID);
			row.setGeneticEntityName(subjectSymbol);
			row.setGeneticEntityType("gene");
		} else {
			if (primaryAnnotation instanceof GeneDiseaseAnnotation pAnnotation) {
				row.setGeneticEntityID(pAnnotation.getDiseaseAnnotationSubject().getIdentifier());
				row.setGeneticEntityName(pAnnotation.getDiseaseAnnotationSubject().getGeneSymbol().getDisplayText());
				if (pAnnotation.getSgdStrainBackground() != null) {
					row.setStrainBackgroundID(pAnnotation.getSgdStrainBackground().getIdentifier());
					if (pAnnotation.getSgdStrainBackground().getAgmFullName() != null) {
						row.setStrainBackgroundName(pAnnotation.getSgdStrainBackground().getAgmFullName().getDisplayText());
					}
				}
				row.setGeneticEntityType("gene");
			}
			if (primaryAnnotation instanceof AlleleDiseaseAnnotation pAnnotation) {
				row.setGeneticEntityID(pAnnotation.getDiseaseAnnotationSubject().getIdentifier());
				row.setGeneticEntityName(pAnnotation.getDiseaseAnnotationSubject().getAlleleSymbol().getDisplayText());
				row.setGeneticEntityType("Allele");
			}
		}
		if (annotation.getCategory().equals("gene_disease_annotation") || annotation.getCategory().equals("allele_disease_annotation")) {
			if (CollectionUtils.isNotEmpty(primaryAnnotation.getConditionRelations())) {
				List<ConditionRelation> conditionModifiers = primaryAnnotation.getConditionRelations().stream()
					.filter(conditionRelation -> conditionRelation.getConditionRelationType() != null)
					.filter(conditionRelation -> conditionRelation.getConditionRelationType().getName().contains("has_condition")
						|| conditionRelation.getConditionRelationType().getName().contains("induced")
						|| conditionRelation.getConditionRelationType().getName().contains("ameliorated")
						|| conditionRelation.getConditionRelationType().getName().contains("exacerbated")).toList();
				List<String> experimentalConditionComponents = new ArrayList<>(conditionModifiers.stream().map(conditionRelation -> conditionRelation.getConditionRelationType().getName()).toList());
				conditionModifiers.forEach(conditionRelation -> conditionRelation.getConditions().forEach(experimentalCondition -> {
					experimentalConditionComponents.add(experimentalCondition.getConditionSummary());
				}));
				row.setExperimentalCondition(String.join(",", experimentalConditionComponents));
			}
		}
		if (primaryAnnotation.getDiseaseGeneticModifierRelation() != null) {
			row.setDiseaseGeneticModifierRelation(primaryAnnotation.getDiseaseGeneticModifierRelation().getName());
		}
		if (annotation.getExperimentalConditionList() != null) {
			row.setExperimentalCondition(annotation.getExperimentalConditionsAggregated());
		}
		if (annotation.getConditionModifierAggregated() != null) {
			row.setConditionModifier(annotation.getConditionModifierAggregated());
		}
		row.setReference(getReferenceString(primaryAnnotation.getEvidenceItem()));
		row.setSource(primaryAnnotation.getDataProviderString());
		List<String> urlExceptionHandler = List.of("MGI", "SGD", "OMIM");
		Organization organization = primaryAnnotation.getDataProvider();
		org.alliancegenome.curation_api.model.entities.CrossReference crossRef = primaryAnnotation.getDataProviderCrossReference();
		if (organization != null && crossRef != null) {
			String urlTemplate = crossRef.getResourceDescriptorPage().getUrlTemplate();
			if (urlExceptionHandler.contains(organization.getAbbreviation())) {
				// remove the prefix in the template as the prefix is already in the curie.
				urlTemplate = urlTemplate.replace(organization.getAbbreviation() + ":", "");
			}
			urlTemplate = urlTemplate.replace("[%s]", crossRef.getReferencedCurie());
			row.setSourceUrl(urlTemplate);
		}
		if (primaryAnnotation.getDateCreated() != null) {
			row.setDateAssigned(primaryAnnotation.getDateCreated().toString());
		}
		if (CollectionUtils.isNotEmpty(primaryAnnotation.getWith())) {
			row.setBasedOnID(primaryAnnotation.getWith().stream().map(SubmittedObject::getIdentifier).collect(Collectors.joining("|")));
			row.setBasedOnName(primaryAnnotation.getWith().stream().map(gene -> gene.getGeneSymbol().getDisplayText()).collect(Collectors.joining("|")));
		}
		if (primaryAnnotation.getGeneticSex() != null) {
			row.setGeneticSex(primaryAnnotation.getGeneticSex().getName());
		}
		row.setGeneticEntityAssociation(primaryAnnotation.getFullRelationString());
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
		List<BiologicalEntity> geneticModifiers = new ArrayList<>();
		if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(primaryAnnotation.getDiseaseGeneticModifierAlleles())) {
			geneticModifiers.addAll(primaryAnnotation.getDiseaseGeneticModifierAlleles().stream().filter(Objects::nonNull).toList());
		}
		if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(primaryAnnotation.getDiseaseGeneticModifierGenes())) {
			geneticModifiers.addAll(primaryAnnotation.getDiseaseGeneticModifierGenes().stream().filter(Objects::nonNull).toList());
		}
		if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(primaryAnnotation.getDiseaseGeneticModifierAgms())) {
			geneticModifiers.addAll(primaryAnnotation.getDiseaseGeneticModifierAgms().stream().filter(Objects::nonNull).toList());
		}
		if (CollectionUtils.isNotEmpty(geneticModifiers)) {
			row.setDiseaseGeneticModifierID(geneticModifiers.stream().map(SubmittedObject::getIdentifier).collect(Collectors.joining("|")));
			StringJoiner joiner = new StringJoiner("|");
			geneticModifiers.forEach(entity -> joiner.add(DiseaseAnnotationHelper.getEntityName(entity)));
			row.setDiseaseGeneticModifierName(joiner.toString());
		}
	}

	private DiseaseDownloadRow getBaseDiseaseDownloadRow(DiseaseAnnotationDocument annotation, org.alliancegenome.curation_api.model.entities.Gene homologousGene, DiseaseAnnotation primaryAnnotation) {
		DiseaseDownloadRow row = new DiseaseDownloadRow();
		row.setAssociation(annotation.getGeneratedRelationString());
		row.setDiseaseID(annotation.getObject().getCurie());
		row.setDiseaseName(annotation.getObject().getName());
		row.setSource(primaryAnnotation.getDataProviderString());
		if (homologousGene != null) {
			row.setBasedOnID(homologousGene.getCurie());
			row.setBasedOnName(homologousGene.getGeneSymbol().getDisplayText());
		}

		StringJoiner evidenceJoiner = getStringJoiner(primaryAnnotation, ECOTerm::getCurie);
		row.setEvidenceCode(evidenceJoiner.toString());

		StringJoiner evidenceJoinerName = getStringJoiner(primaryAnnotation, ECOTerm::getName);
		row.setEvidenceCodeName(evidenceJoinerName.toString());

		StringJoiner evidenceJoinerAbbreviation = getStringJoiner(primaryAnnotation, ECOTerm::getAbbreviation);
		row.setEvidenceAbbreviation(evidenceJoinerAbbreviation.toString());

		return row;
	}

	private static StringJoiner getStringJoiner(DiseaseAnnotation annotation, Function<ECOTerm, String> function) {
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

	public String getAllRowsForModel(List<AGMDiseaseAnnotationDocument> diseaseAnnotations) {

		List<DiseaseDownloadRow> list = getDownloadRowsFromGeneDiseaseAnnotations(diseaseAnnotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Species ID", DiseaseDownloadRow::getSpeciesID),
			new DownloadHeader<>("Species Name", DiseaseDownloadRow::getSpeciesName),
			new DownloadHeader<>("Model ID", DiseaseDownloadRow::getMainEntityID),
			new DownloadHeader<>("Model Symbol", DiseaseDownloadRow::getMainEntitySymbol),
			new DownloadHeader<>("Model Type", DiseaseDownloadRow::getEntityType),
			new DownloadHeader<>("Model Association", DiseaseDownloadRow::getAssociation),
			new DownloadHeader<>("Disease Qualifier", DiseaseDownloadRow::getDiseaseQualifier),
			new DownloadHeader<>("Disease ID", DiseaseDownloadRow::getDiseaseID),
			new DownloadHeader<>("Disease Name", DiseaseDownloadRow::getDiseaseName),
			new DownloadHeader<>("Evidence Code", DiseaseDownloadRow::getEvidenceCode),
			new DownloadHeader<>("Evidence Code Abbreviation", DiseaseDownloadRow::getEvidenceAbbreviation),
			new DownloadHeader<>("Evidence Code Name", DiseaseDownloadRow::getEvidenceCodeName),
			new DownloadHeader<>("Experimental Conditions", DiseaseDownloadRow::getExperimentalCondition),
			new DownloadHeader<>("Condition Modifiers", DiseaseDownloadRow::getConditionModifier),
			new DownloadHeader<>("Genetic Modifier Relation", DiseaseDownloadRow::getDiseaseGeneticModifierRelation),
			new DownloadHeader<>("Genetic Modifier IDs", DiseaseDownloadRow::getDiseaseGeneticModifierID),
			new DownloadHeader<>("Genetic Modifier Names", DiseaseDownloadRow::getDiseaseGeneticModifierName),
			new DownloadHeader<>("Genetic Sex", DiseaseDownloadRow::getGeneticSex),
			new DownloadHeader<>("Notes", DiseaseDownloadRow::getNote),
			new DownloadHeader<>("Annotation Type", DiseaseDownloadRow::getAnnotationType),
			new DownloadHeader<>("Source", DiseaseDownloadRow::getSource),
			new DownloadHeader<>("Source URL", DiseaseDownloadRow::getSourceUrl),
			new DownloadHeader<>("Reference", DiseaseDownloadRow::getReference),
			new DownloadHeader<>("Date", DiseaseDownloadRow::getDateAssigned)
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public String getAllRowsForAlleleDiseaseAnnotations(List<AlleleDiseaseAnnotationDocument> diseaseAnnotations) {

		// convert collection of DiseaseAnnotation records to DiseaseDownloadRow records
		List<DiseaseDownloadRow> list = getDownloadRowsFromGeneDiseaseAnnotations(diseaseAnnotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Species ID", DiseaseDownloadRow::getSpeciesID),
			new DownloadHeader<>("Species Name", DiseaseDownloadRow::getSpeciesName),
			new DownloadHeader<>("Allele ID", DiseaseDownloadRow::getMainEntityID),
			new DownloadHeader<>("Allele Symbol", DiseaseDownloadRow::getMainEntitySymbol),
			new DownloadHeader<>("Allele Association", DiseaseDownloadRow::getAssociation),
			new DownloadHeader<>("Additional Implicated Allele IDs", DiseaseDownloadRow::getAssertedAlleleID),
			new DownloadHeader<>("Additional Implicated Allele Symbols", DiseaseDownloadRow::getAssertedAlleleName),
			new DownloadHeader<>("Genetic Entity ID", DiseaseDownloadRow::getGeneticEntityID),
			new DownloadHeader<>("Genetic Entity Name", DiseaseDownloadRow::getGeneticEntityName),
			new DownloadHeader<>("Genetic Entity Type", DiseaseDownloadRow::getGeneticEntityType),
			new DownloadHeader<>("Genetic Entity Association", DiseaseDownloadRow::getGeneticEntityAssociation),
			new DownloadHeader<>("Disease Qualifier", DiseaseDownloadRow::getDiseaseQualifier),
			new DownloadHeader<>("Disease ID", DiseaseDownloadRow::getDiseaseID),
			new DownloadHeader<>("Disease Name", DiseaseDownloadRow::getDiseaseName),
			new DownloadHeader<>("Evidence Code", DiseaseDownloadRow::getEvidenceCode),
			new DownloadHeader<>("Evidence Code Abbreviation", DiseaseDownloadRow::getEvidenceAbbreviation),
			new DownloadHeader<>("Evidence Code Name", DiseaseDownloadRow::getEvidenceCodeName),
			new DownloadHeader<>("Experimental Conditions", DiseaseDownloadRow::getExperimentalCondition),
			new DownloadHeader<>("Genetic Modifier Relation", DiseaseDownloadRow::getDiseaseGeneticModifierRelation),
			new DownloadHeader<>("Genetic Modifier IDs", DiseaseDownloadRow::getDiseaseGeneticModifierID),
			new DownloadHeader<>("Genetic Modifier Names", DiseaseDownloadRow::getDiseaseGeneticModifierName),
			new DownloadHeader<>("Genetic Sex", DiseaseDownloadRow::getGeneticSex),
			new DownloadHeader<>("Notes", DiseaseDownloadRow::getNote),
			new DownloadHeader<>("Annotation Type", DiseaseDownloadRow::getAnnotationType),
			new DownloadHeader<>("Source", DiseaseDownloadRow::getSource),
			new DownloadHeader<>("Source URL", DiseaseDownloadRow::getSourceUrl),
			new DownloadHeader<>("Reference", DiseaseDownloadRow::getReference),
			new DownloadHeader<>("Date", DiseaseDownloadRow::getDateAssigned)
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}


	public String getAllRowsForAssociatedGenes(List<GeneDiseaseAnnotationDocument> diseaseAnnotations) {
		// convert collection of DiseaseAnnotation records to DiseaseDownloadRow records
		List<DiseaseDownloadRow> list = getDownloadRowsFromGeneDiseaseAnnotations(diseaseAnnotations);

		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Species ID", DiseaseDownloadRow::getSpeciesID),
			new DownloadHeader<>("Species Name", DiseaseDownloadRow::getSpeciesName),
			new DownloadHeader<>("Gene ID", DiseaseDownloadRow::getMainEntityID),
			new DownloadHeader<>("Gene Symbol", DiseaseDownloadRow::getMainEntitySymbol),
			new DownloadHeader<>("Additional Implicated Gene ID", DiseaseDownloadRow::getAssertedGeneID),
			new DownloadHeader<>("Additional Implicated Gene Symbol", DiseaseDownloadRow::getAssertedGeneName),
			new DownloadHeader<>("Gene Association", DiseaseDownloadRow::getAssociation),
			new DownloadHeader<>("Genetic Entity ID", DiseaseDownloadRow::getGeneticEntityID),
			new DownloadHeader<>("Genetic Entity Name", DiseaseDownloadRow::getGeneticEntityName),
			new DownloadHeader<>("Genetic Entity Type", DiseaseDownloadRow::getGeneticEntityType),
			new DownloadHeader<>("Genetic Entity Association", DiseaseDownloadRow::getGeneticEntityAssociation),
			new DownloadHeader<>("Disease Qualifier", DiseaseDownloadRow::getDiseaseQualifier),
			new DownloadHeader<>("Disease ID", DiseaseDownloadRow::getDiseaseID),
			new DownloadHeader<>("Disease Name", DiseaseDownloadRow::getDiseaseName),
			new DownloadHeader<>("Evidence Code", DiseaseDownloadRow::getEvidenceCode),
			new DownloadHeader<>("Evidence Code Abbreviation", DiseaseDownloadRow::getEvidenceAbbreviation),
			new DownloadHeader<>("Evidence Code Name", DiseaseDownloadRow::getEvidenceCodeName),
			new DownloadHeader<>("Experimental Conditions", DiseaseDownloadRow::getExperimentalCondition),
			new DownloadHeader<>("Genetic Modifier Relation", DiseaseDownloadRow::getDiseaseGeneticModifierRelation),
			new DownloadHeader<>("Genetic Modifier IDs", DiseaseDownloadRow::getDiseaseGeneticModifierID),
			new DownloadHeader<>("Genetic Modifier Names", DiseaseDownloadRow::getDiseaseGeneticModifierName),
			new DownloadHeader<>("Strain Background ID", DiseaseDownloadRow::getStrainBackgroundID),
			new DownloadHeader<>("Strain Background Name", DiseaseDownloadRow::getStrainBackgroundName),
			new DownloadHeader<>("Genetic Sex", DiseaseDownloadRow::getGeneticSex),
			new DownloadHeader<>("Notes", DiseaseDownloadRow::getNote),
			new DownloadHeader<>("Annotation Type", DiseaseDownloadRow::getAnnotationType),
			new DownloadHeader<>("Based On ID", DiseaseDownloadRow::getBasedOnID),
			new DownloadHeader<>("Based On Symbol", DiseaseDownloadRow::getBasedOnName),
			new DownloadHeader<>("Source", DiseaseDownloadRow::getSource),
			new DownloadHeader<>("Source URL", DiseaseDownloadRow::getSourceUrl),
			new DownloadHeader<>("Reference", DiseaseDownloadRow::getReference),
			new DownloadHeader<>("Date", DiseaseDownloadRow::getDateAssigned)
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}
}
