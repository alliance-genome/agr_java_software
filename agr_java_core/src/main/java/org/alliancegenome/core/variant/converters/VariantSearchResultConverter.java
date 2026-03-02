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
			CuratedVariantGenomicLocationAssociation curatedVariantGenomicLocationAssociation = doc.getVariantLocation();
			if (curatedVariantGenomicLocationAssociation == null) {
				continue;
			}

			VariantSearchResultDocument vsd = new VariantSearchResultDocument();
			vsd.setSearchable(true);
			vsd.setAlterationType("variant");
			vsd.setPrimaryKey(curatedVariantGenomicLocationAssociation.getHgvs());
			vsd.setName(curatedVariantGenomicLocationAssociation.getHgvs());
			vsd.setNameKey(curatedVariantGenomicLocationAssociation.getHgvs());

			if (curatedVariantGenomicLocationAssociation.getVariantAssociationSubject() != null) {
				if (curatedVariantGenomicLocationAssociation.getVariantAssociationSubject().getTaxon() != null) {
					vsd.setSpecies(curatedVariantGenomicLocationAssociation.getVariantAssociationSubject().getTaxon().getName());
				}
				if (curatedVariantGenomicLocationAssociation.getVariantAssociationSubject().getVariantType() != null && curatedVariantGenomicLocationAssociation.getVariantAssociationSubject().getVariantType().getName() != null) {
					vsd.setVariantType(List.of(curatedVariantGenomicLocationAssociation.getVariantAssociationSubject().getVariantType().getName()));
				}
			}

			vsd.setPopularity(0.0);

			if (curatedVariantGenomicLocationAssociation.getPredictedVariantConsequences() != null) {
				List<String> geneNames = curatedVariantGenomicLocationAssociation.getPredictedVariantConsequences().stream()
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

			if (curatedVariantGenomicLocationAssociation.getVariantAssociationSubject() != null && curatedVariantGenomicLocationAssociation.getVariantAssociationSubject().getCrossReferences() != null) {
				List<String> crossRefs = new ArrayList<>();
				for (var xref : curatedVariantGenomicLocationAssociation.getVariantAssociationSubject().getCrossReferences()) {
					crossRefs.add(xref.getDisplayName());
				}
				if (!crossRefs.isEmpty()) {
					vsd.setCrossReferences(crossRefs);
				}
			}

			if (curatedVariantGenomicLocationAssociation.getPredictedVariantConsequences() != null) {
				HashSet<String> consequences = new HashSet<>();
				for (PredictedVariantConsequence pvc : curatedVariantGenomicLocationAssociation.getPredictedVariantConsequences()) {
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
