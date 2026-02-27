package org.alliancegenome.core.variant.converters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.SequenceSummaryDocument;
import org.alliancegenome.curation_api.model.entities.PredictedVariantConsequence;
import org.alliancegenome.curation_api.model.entities.Variant;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.apache.commons.collections4.CollectionUtils;

public class AlleleSequenceSummaryConverter {

	public List<SequenceSummaryDocument> convert(List<AlleleSummaryDocument> alleleDocs) {
		List<SequenceSummaryDocument> result = new ArrayList<>();

		for (AlleleSummaryDocument doc : alleleDocs) {
			if (CollectionUtils.isEmpty(doc.getVariants())) {
				continue;
			}

			HashSet<String> geneIds = new HashSet<>();
			if (doc.getAlleleOfGene() != null) {
				geneIds.add(doc.getAlleleOfGene().getPrimaryExternalId());
			}

			for (Variant variant : doc.getVariants()) {
				if (CollectionUtils.isEmpty(variant.getCuratedVariantGenomicLocations())) {
					result.add(buildDocument(doc, null, null, geneIds));
					continue;
				}

				for (CuratedVariantGenomicLocationAssociation location : variant.getCuratedVariantGenomicLocations()) {
					if (CollectionUtils.isEmpty(location.getPredictedVariantConsequences())) {
						result.add(buildDocument(doc, location, null, geneIds));
						continue;
					}

					for (PredictedVariantConsequence consequence : location.getPredictedVariantConsequences()) {
						result.add(buildDocument(doc, location, consequence, geneIds));
					}
				}
			}
		}

		return result;
	}

	private SequenceSummaryDocument buildDocument(AlleleSummaryDocument doc, CuratedVariantGenomicLocationAssociation location, PredictedVariantConsequence consequence, HashSet<String> geneIds) {
		SequenceSummaryDocument ssd = new SequenceSummaryDocument();
		ssd.setAllele(doc.getAllele());
		ssd.setGeneIds(doc.getGeneIds());
		ssd.setSequenceSummaryCategory("allele");
		ssd.setHasPhenotype(doc.getHasPhenotype() != null && doc.getHasPhenotype());
		ssd.setHasDisease(doc.getHasDisease() != null && doc.getHasDisease());
		ssd.setAlterationType(doc.getAlterationType());
		ssd.setAlterationTypeSortOrder(doc.getAlterationTypeSortOrder());
		ssd.setVariant(location);
		ssd.setConsequence(consequence);
		ssd.setGeneIds(geneIds);
		return ssd;
	}

}
