package org.alliancegenome.core.variant.converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Variant;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Converts AlleleSummaryDocument objects into VariantSummaryDocument objects
 * for LTP (Low Throughput) variants that come through the curation API allele endpoint.
 * Produces one variant_summary document per allele x variant combination.
 */
public class AlleleToVariantSummaryConverter {

	public List<VariantSummaryDocument> convert(List<AlleleSummaryDocument> alleleDocs) {
		List<VariantSummaryDocument> result = new ArrayList<>();

		for (AlleleSummaryDocument doc : alleleDocs) {
			if (CollectionUtils.isEmpty(doc.getVariantList())) {
				continue;
			}

			for (Variant variant : doc.getVariantList()) {
				if (CollectionUtils.isEmpty(variant.getCuratedVariantGenomicLocations())) {
					continue;
				}

				VariantSummaryDocument vsd = new VariantSummaryDocument();
				vsd.setAllele(doc.getAllele());
				vsd.setSymbol(doc.getSymbol());
				vsd.setVariantList(List.of(variant));
				vsd.setHasPhenotype(doc.getHasPhenotype() != null && doc.getHasPhenotype());
				vsd.setHasDisease(doc.getHasDisease() != null && doc.getHasDisease());

				if (doc.getGeneIds() != null) {
					vsd.setGeneIds(new HashSet<>(doc.getGeneIds()));
				}

				result.add(vsd);
			}
		}

		return result;
	}
}
