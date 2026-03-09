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
			CuratedVariantGenomicLocationAssociation variantLocation = doc.getVariants().get(0).getCuratedVariantGenomicLocations().get(0);
			if (variantLocation == null || variantLocation.getPredictedVariantConsequences() == null) {
				continue;
			}

			for (PredictedVariantConsequence consequence : variantLocation.getPredictedVariantConsequences()) {
				if (consequence.getVariantTranscript() != null && consequence.getVariantTranscript().getTranscriptGeneAssociations() != null) {
					SequenceSummaryDocument ssd = new SequenceSummaryDocument();
					ssd.setAllele(doc.getAllele());
					ssd.setSymbol(doc.getSymbol());
					ssd.setVariant(doc.getVariants().get(0));
					ssd.setConsequence(consequence);
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
