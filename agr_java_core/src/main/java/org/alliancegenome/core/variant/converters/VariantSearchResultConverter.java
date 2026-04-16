package org.alliancegenome.core.variant.converters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.curation_api.model.entities.PredictedVariantConsequence;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.model.entities.associations.TranscriptGeneAssociation;
import org.alliancegenome.curation_api.model.entities.slotAnnotations.NameSlotAnnotation;
import org.alliancegenome.es.model.VariantSearchResultDocument;

public class VariantSearchResultConverter {

	public List<VariantSearchResultDocument> convertToVariantSearchDocument(List<VariantSummaryDocument> variantSummaryDocuments) {
		List<VariantSearchResultDocument> result = new ArrayList<>();

		for (VariantSummaryDocument doc : variantSummaryDocuments) {
			CuratedVariantGenomicLocationAssociation variantLocation = doc.getVariantList().get(0).getCuratedVariantGenomicLocations().get(0);
			if (variantLocation == null) {
				continue;
			}

			VariantSearchResultDocument vsd = new VariantSearchResultDocument();
			vsd.setSearchable(true);
			vsd.setAlterationType("variant");
			vsd.setPrimaryKey(variantLocation.getHgvs());
			vsd.setName(variantLocation.getHgvs());
			vsd.setNameKey(variantLocation.getHgvs());

			if (variantLocation.getVariantAssociationSubject() != null) {
				if (variantLocation.getVariantAssociationSubject().getTaxon() != null) {
					vsd.setSpecies(variantLocation.getVariantAssociationSubject().getTaxon().getSpecies().getFullName());
				}
				if (variantLocation.getVariantAssociationSubject().getVariantType() != null && variantLocation.getVariantAssociationSubject().getVariantType().getName() != null) {
					vsd.setVariantType(List.of(variantLocation.getVariantAssociationSubject().getVariantType().getName()));
				}
			}

			vsd.setPopularity(0.0);

			if (variantLocation.getPredictedVariantConsequences() != null) {
				List<String> geneNames = variantLocation.getPredictedVariantConsequences().stream()
					.filter(pvc -> pvc.getVariantTranscript() != null && pvc.getVariantTranscript().getTranscriptGeneAssociations() != null)
					.flatMap(pvc -> pvc.getVariantTranscript().getTranscriptGeneAssociations().stream())
					.map(TranscriptGeneAssociation::getTranscriptGeneAssociationObject)
					.filter(Objects::nonNull)
					.map(gene -> {
						String name = null;
						if (gene.getGeneSymbol() != null && gene.getGeneSymbol().getDisplayText() != null) {
							name = gene.getGeneSymbol().getDisplayText();
						} else if (gene.getPrimaryExternalId() != null) {
							name = gene.getPrimaryExternalId();
						} else {
							name = gene.getCurie();
						}
						if (name != null && gene.getTaxon() != null && gene.getTaxon().getSpecies() != null) {
							name = name + " (" + gene.getTaxon().getSpecies().getAbbreviation() + ")";
						}
						return name;
					})
					.filter(Objects::nonNull)
					.distinct()
					.sorted()
					.collect(Collectors.toList());
				if (!geneNames.isEmpty()) {
					vsd.setGenes(geneNames);
				}

				Set<String> geneSynonyms = variantLocation.getPredictedVariantConsequences().stream()
					.filter(pvc -> pvc.getVariantTranscript() != null && pvc.getVariantTranscript().getTranscriptGeneAssociations() != null)
					.flatMap(pvc -> pvc.getVariantTranscript().getTranscriptGeneAssociations().stream())
					.map(TranscriptGeneAssociation::getTranscriptGeneAssociationObject)
					.filter(Objects::nonNull)
					.filter(gene -> gene.getGeneSynonyms() != null)
					.flatMap(gene -> gene.getGeneSynonyms().stream())
					.map(NameSlotAnnotation::getFormatText)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
				if (!geneSynonyms.isEmpty()) {
					vsd.setGeneSynonyms(geneSynonyms);
				}
			}

			if (variantLocation.getVariantAssociationSubject() != null && variantLocation.getVariantAssociationSubject().getCrossReferences() != null) {
				Set<String> crossRefs = new HashSet<>();
				for (var xref : variantLocation.getVariantAssociationSubject().getCrossReferences()) {
					crossRefs.add(xref.getDisplayName());
				}
				if (!crossRefs.isEmpty()) {
					vsd.setCrossReferences(crossRefs);
				}
			}

			if (variantLocation.getPredictedVariantConsequences() != null) {
				Set<String> consequences = new HashSet<>();
				for (PredictedVariantConsequence pvc : variantLocation.getPredictedVariantConsequences()) {
					if (pvc.getVepConsequences() != null) {
						for (var soTerm : pvc.getVepConsequences()) {
							consequences.add(soTerm.getName());
						}
					}
				}
				if (!consequences.isEmpty()) {
					vsd.setMolecularConsequence(consequences);
				}
			}

			result.add(vsd);
		}
		return result;
	}
}
