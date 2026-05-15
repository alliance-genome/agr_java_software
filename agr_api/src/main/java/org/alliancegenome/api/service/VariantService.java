package org.alliancegenome.api.service;

import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.es.index.site.dao.VariantESDAO;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class VariantService {

	@Inject
	private VariantESDAO variantDAO;

	public VariantSummaryDocument getVariantById(String id) {
		VariantSummaryDocument variant = variantDAO.getVariant(id);
		return variant;
	}
}
