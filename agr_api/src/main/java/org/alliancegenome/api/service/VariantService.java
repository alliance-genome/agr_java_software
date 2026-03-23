package org.alliancegenome.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.api.service.FilterService;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.es.index.site.dao.VariantESDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.repository.VariantRepository;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class VariantService {

	private static VariantRepository variantRepo = new VariantRepository();
	@Inject
	private VariantESDAO variantDAO;

	public JsonResultResponse<Variant> getVariants(String id, Pagination pagination) {
		LocalDateTime startDate = LocalDateTime.now();

		List<Variant> variants = variantRepo.getVariantsOfAllele(id);

		JsonResultResponse<Variant> result = new JsonResultResponse<>();

		FilterService<Variant> filterService = new FilterService<>(null);
		result.setTotal(variants.size());
		result.setResults(filterService.getPaginatedAnnotations(pagination, variants));
		result.calculateRequestDuration(startDate);
		return result;
	}

	public VariantSummaryDocument getVariantById(String id) {
		VariantSummaryDocument variant = variantDAO.getVariant(id);
		return variant;
	}
}
