package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.time.LocalDateTime;
import java.util.List;

import org.alliancegenome.cache.repository.helper.AlleleESFiltering;
import org.alliancegenome.cache.repository.helper.AlleleFiltering;
import org.alliancegenome.cache.repository.helper.AlleleSorting;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.api.service.ColumnFieldMapping;
import org.alliancegenome.core.api.service.FilterService;
import org.alliancegenome.core.api.service.Table;
import org.alliancegenome.core.api.service.TransgenicAlleleColumnFieldMapping;
import org.alliancegenome.es.index.site.dao.VariantESDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AlleleService {

	private static AlleleRepository alleleRepo = new AlleleRepository();
	private static VariantESDAO variantESDAO = new VariantESDAO();

	public Allele getById(String id) {
		return alleleRepo.getAllele(id);
	}

	static AlleleESFiltering filter = new AlleleESFiltering();

	public JsonResultResponse<Allele> getAllelesByGene(String geneID, Pagination pagination) {

		SearchSourceBuilder searchSourceBuilderFull = new SearchSourceBuilder();
		searchSourceBuilderFull.query(getBaseQueryBuilder(geneID));

		BoolQueryBuilder bool = getBaseQueryBuilder(geneID);
		BaseFilter fieldFilterValueMap = pagination.getFieldFilterValueMap();
		if (fieldFilterValueMap != null) {
			fieldFilterValueMap
					.forEach((fieldFilter, value) -> {
						if (value != null) {
							bool.must(QueryBuilders.wildcardQuery(filter.getFieldName(fieldFilter), "*" + value + "*"));
						}
					});
		}
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(bool);
		JsonResultResponse<Allele> response = variantESDAO.performQuery(searchSourceBuilder, pagination);
		// add distinct values
		response.addDistinctFieldValueSupplementalData(variantESDAO.getDistinctValues(searchSourceBuilderFull));
		return response;

	}

	public BoolQueryBuilder getBaseQueryBuilder(String geneID) {
		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("category", "allele"));
		bool.must(new TermQueryBuilder("variant.gene.id.keyword", geneID));
		return bool;
	}

	public JsonResultResponse<Allele> getTransgenicAlleles(String geneID, Pagination pagination) {
		LocalDateTime startDate = LocalDateTime.now();
		List<Allele> alleles = alleleRepo.getTransgenicAlleles(geneID);
		JsonResultResponse<Allele> result = new JsonResultResponse<>();

		// filter
		if (CollectionUtils.isNotEmpty(alleles)) {
			FilterService<Allele> filterService = new FilterService<>(new AlleleFiltering());
			List<Allele> filteredAlleles = filterService.filterAnnotations(alleles, pagination.getFieldFilterValueMap());
			result.setTotal(filteredAlleles.size());
			result.setResults(filterService.getSortedAndPaginatedAnnotations(pagination, filteredAlleles, new AlleleSorting()));
			ColumnFieldMapping<Allele> mapping = new TransgenicAlleleColumnFieldMapping();
			result.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(alleles,
					mapping.getSingleValuedFieldColumns(Table.TRANSGENIC_ALLELE), mapping));
		}
		// sort
		result.calculateRequestDuration(startDate);
		return result;
	}
}