package org.alliancegenome.core.variant.converters;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.es.model.AlleleSearchResultDocument;
import org.apache.commons.collections.CollectionUtils;

public class AlleleSearchResultConverter {

	public List<AlleleSearchResultDocument> convert(List<AlleleSummaryDocument> alleleSummaryDocuments) {
		List<AlleleSearchResultDocument> result = new ArrayList<>();

		for (AlleleSummaryDocument doc : alleleSummaryDocuments) {
			Allele allele = doc.getAllele();
			if (allele == null) {
				continue;
			}

			AlleleSearchResultDocument searchDoc = new AlleleSearchResultDocument();
			searchDoc.setSearchable(true);
			searchDoc.setAlterationType(doc.getAlterationType());

			searchDoc.setPrimaryKey(allele.getPrimaryExternalId());
			searchDoc.setGlobalId(allele.getPrimaryExternalId());
			searchDoc.setLocalId(allele.getPrimaryExternalId());

			if (allele.getAlleleSymbol() != null) {
				searchDoc.setSymbol(allele.getAlleleSymbol().getDisplayText());
				searchDoc.setSymbolText(allele.getAlleleSymbol().getFormatText());
				searchDoc.setName(allele.getAlleleSymbol().getDisplayText());
			}

			if (allele.getTaxon() != null) {
				searchDoc.setSpecies(allele.getTaxon().getName());
				if (allele.getAlleleSymbol() != null) {
					searchDoc.setNameKey(allele.getAlleleSymbol().getFormatText() + " (" + allele.getTaxon().getName() + ")");
				}
			} else if (allele.getAlleleSymbol() != null) {
				searchDoc.setNameKey(allele.getAlleleSymbol().getFormatText());
			}

			if (CollectionUtils.isNotEmpty(allele.getAlleleSynonyms())) {
				List<String> synonyms = allele.getAlleleSynonyms().stream()
					.map(syn -> syn.getDisplayText())
					.filter(s -> s != null)
					.collect(Collectors.toList());
				if (!synonyms.isEmpty()) {
					searchDoc.setSynonyms(synonyms);
				}
			}

			if (doc.getAlleleOfGene() != null && doc.getAlleleOfGene().getGeneSymbol() != null) {
				searchDoc.setGenes(List.of(doc.getAlleleOfGene().getGeneSymbol().getDisplayText()));
			}

			if (doc.getCrossReference() != null && doc.getCrossReference().getReferencedCurie() != null) {
				searchDoc.setModCrossRefCompleteUrl(doc.getCrossReference().getReferencedCurie());
			}

			if (allele.getPopularity() != null) {
				searchDoc.setPopularity(allele.getPopularity());
			} else {
				searchDoc.setPopularity(0.0);
			}

			result.add(searchDoc);
		}
		return result;
	}

}
