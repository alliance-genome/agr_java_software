package org.alliancegenome.core.variant.converters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.alliancegenome.curation_api.model.document.es.SequenceSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.curation_api.model.entities.PredictedVariantConsequence;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.model.entities.associations.TranscriptGeneAssociation;


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
					ssd.setAlterationType("variant");

					HashSet<String> geneIds = new HashSet<>();
					for (TranscriptGeneAssociation assoc : consequence.getVariantTranscript().getTranscriptGeneAssociations()) {
						geneIds.add(assoc.getTranscriptGeneAssociationObject().getCurie());
					}
					ssd.setGeneIds(geneIds);
					result.add(ssd);
				}
			}
		}
		return result;
	}

}
