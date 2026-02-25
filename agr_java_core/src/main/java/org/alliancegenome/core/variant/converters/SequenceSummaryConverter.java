package org.alliancegenome.core.variant.converters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.alliancegenome.curation_api.model.document.es.SequenceSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.PredictedVariantConsequence;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.model.entities.associations.TranscriptGeneAssociation;
import org.alliancegenome.es.model.VariantSearchDocument;

/**
 * Flattens VariantSummaryDocument objects into SequenceSummaryDocument objects,
 * producing one document per allele x variant x consequence combination.
 */
public class SequenceSummaryConverter {

	public List<SequenceSummaryDocument> convertToSequenceSummary(List<VariantSummaryDocument> docs) {
		List<SequenceSummaryDocument> result = new ArrayList<>();

		for (VariantSummaryDocument doc : docs) {
			CuratedVariantGenomicLocationAssociation variant = doc.getVariant();
			if (variant == null || variant.getPredictedVariantConsequences() == null) {
				continue;
			}

			for (PredictedVariantConsequence consequence : variant.getPredictedVariantConsequences()) {
				if (consequence.getVariantTranscript() != null && consequence.getVariantTranscript().getTranscriptGeneAssociations() != null) {
					SequenceSummaryDocument ssd = new SequenceSummaryDocument();
					ssd.setAllele(doc.getAllele());
					ssd.setVariant(variant);
					ssd.setConsequence(consequence);
					ssd.setSequenceSummaryCategory("variant");

					HashSet<String> geneIds = new HashSet<>();
					for (TranscriptGeneAssociation assoc : consequence.getVariantTranscript().getTranscriptGeneAssociations()) {
						geneIds.add(assoc.getTranscriptGeneAssociationObject().getCurie());
					}
					for (Gene gene : variant.getOverlapGenes()) {
						geneIds.add(gene.getCurie());
					}
					ssd.setGeneIds(geneIds);
					result.add(ssd);
				}
			}
		}
		return result;
	}

	public List<VariantSearchDocument> convertToVariantSearchDocument(List<VariantSummaryDocument> variantSummaryDocuments) {
		List<VariantSearchDocument> result = new ArrayList<>();

		for (VariantSummaryDocument doc : variantSummaryDocuments) {
			CuratedVariantGenomicLocationAssociation variant = doc.getVariant();
			if (variant == null) {
				continue;
			}

			VariantSearchDocument vsd = new VariantSearchDocument();
			vsd.setSearchable(true);
			vsd.setAlterationType("variant");
			vsd.setPrimaryKey(variant.getHgvs());
			vsd.setName(variant.getHgvs());
			vsd.setNameKey(variant.getHgvs());

			if (variant.getVariantAssociationSubject() != null) {
				if (variant.getVariantAssociationSubject().getTaxon() != null) {
					vsd.setSpecies(variant.getVariantAssociationSubject().getTaxon().getName());
				}
				if (variant.getVariantAssociationSubject().getVariantType() != null) {
					vsd.setVariantType(List.of(variant.getVariantAssociationSubject().getVariantType().getName()));
				}
			}

			vsd.setPopularity(0.0);

			if (variant.getOverlapGenes() != null && !variant.getOverlapGenes().isEmpty()) {
				List<String> geneNames = new ArrayList<>();
				for (Gene gene : variant.getOverlapGenes()) {
					geneNames.add(gene.getPrimaryExternalId());
				}
				vsd.setGenes(geneNames);
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
