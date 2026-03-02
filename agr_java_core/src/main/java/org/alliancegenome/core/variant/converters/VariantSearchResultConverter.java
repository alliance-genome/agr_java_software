package org.alliancegenome.core.variant.converters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.curation_api.model.entities.PredictedVariantConsequence;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.model.entities.associations.TranscriptGeneAssociation;
import org.alliancegenome.es.model.VariantSearchResultDocument;

public class VariantSearchResultConverter {

	public List<VariantSearchResultDocument> convertToVariantSearchDocument(List<VariantSummaryDocument> variantSummaryDocuments) {
		List<VariantSearchResultDocument> result = new ArrayList<>();

		for (VariantSummaryDocument doc : variantSummaryDocuments) {
			CuratedVariantGenomicLocationAssociation variant = doc.getVariantLocation();
			if (variant == null) {
				continue;
			}

			VariantSearchResultDocument vsd = new VariantSearchResultDocument();
			vsd.setSearchable(true);
			vsd.setAlterationType("variant");
			vsd.setPrimaryKey(variant.getHgvs());
			vsd.setName(variant.getHgvs());
			vsd.setNameKey(variant.getHgvs());

			if (variant.getVariantAssociationSubject() != null) {
				if (variant.getVariantAssociationSubject().getTaxon() != null) {
					vsd.setSpecies(variant.getVariantAssociationSubject().getTaxon().getName());
				}
				if (variant.getVariantAssociationSubject().getVariantType() != null && variant.getVariantAssociationSubject().getVariantType().getName() != null) {
					vsd.setVariantType(List.of(variant.getVariantAssociationSubject().getVariantType().getName()));
				}
			}

			vsd.setPopularity(0.0);

			if (variant.getPredictedVariantConsequences() != null) {
				List<String> geneNames = variant.getPredictedVariantConsequences().stream()
					.filter(pvc -> pvc.getVariantTranscript() != null && pvc.getVariantTranscript().getTranscriptGeneAssociations() != null)
					.flatMap(pvc -> pvc.getVariantTranscript().getTranscriptGeneAssociations().stream())
					.map(TranscriptGeneAssociation::getTranscriptGeneAssociationObject)
					.filter(Objects::nonNull)
					.map(gene -> {
						if (gene.getGeneSymbol() != null && gene.getGeneSymbol().getDisplayText() != null) {
							return gene.getGeneSymbol().getDisplayText();
						}
						if (gene.getPrimaryExternalId() != null) {
							return gene.getPrimaryExternalId();
						}
						return gene.getCurie();
					})
					.filter(Objects::nonNull)
					.distinct()
					.sorted()
					.collect(Collectors.toList());
				if (!geneNames.isEmpty()) {
					vsd.setGenes(geneNames);
				}
			}

			if (variant.getVariantAssociationSubject() != null && variant.getVariantAssociationSubject().getCrossReferences() != null) {
				List<String> crossRefs = new ArrayList<>();
				for (var xref : variant.getVariantAssociationSubject().getCrossReferences()) {
					crossRefs.add(xref.getDisplayName());
				}
				if (!crossRefs.isEmpty()) {
					vsd.setCrossReferences(crossRefs);
				}
			}

			if (variant.getPredictedVariantConsequences() != null) {
				HashSet<String> consequences = new HashSet<>();
				for (PredictedVariantConsequence pvc : variant.getPredictedVariantConsequences()) {
					if (pvc.getVepConsequences() != null) {
						for (var soTerm : pvc.getVepConsequences()) {
							consequences.add(soTerm.getName());
						}
					}
				}
				if (!consequences.isEmpty()) {
					vsd.setMolecularConsequence(new ArrayList<>(consequences));
				}
			}

			result.add(vsd);
		}
		return result;
	}
}
