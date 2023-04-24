package org.alliancegenome.cache.repository;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;
import org.alliancegenome.cache.repository.helper.PaginationResult;
import org.alliancegenome.cache.repository.helper.PhenotypeAnnotationFiltering;
import org.alliancegenome.cache.repository.helper.PhenotypeAnnotationSorting;
import org.alliancegenome.cache.repository.helper.SortingField;
import org.alliancegenome.core.api.service.FilterService;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.apache.commons.collections.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class PhenotypeCacheRepository {

	@Inject
	CacheService cacheService;

	public PaginationResult<PhenotypeAnnotation> getPhenotypeAnnotationList(String geneID, Pagination pagination) {

		List<PhenotypeAnnotation> fullPhenotypeAnnotationList = getPhenotypeAnnotationList(geneID);

		// remove GENE annotations from PAE list
		// filtering
		PaginationResult<PhenotypeAnnotation> result = new PaginationResult<>();
		FilterService<PhenotypeAnnotation> filterService = new FilterService<>(new PhenotypeAnnotationFiltering());
		if (CollectionUtils.isNotEmpty(fullPhenotypeAnnotationList)) {
			List<PhenotypeAnnotation> filteredAnnotations = filterService.filterAnnotations(fullPhenotypeAnnotationList, pagination.getFieldFilterValueMap());
			filterService.getSortedAndPaginatedAnnotations(pagination, filteredAnnotations, new PhenotypeAnnotationSorting());
			result.setTotalNumber(filteredAnnotations.size());
			result.setResult(filterService.getPaginatedAnnotations(pagination, filteredAnnotations));
		}
		return result;
	}

	private List<PhenotypeAnnotation> getSortedAndPaginatedDiseaseAnnotations(Pagination pagination, List<PhenotypeAnnotation> fullDiseaseAnnotationList) {
		// sorting
		SortingField sortingField = null;
		String sortBy = pagination.getSortBy();
		if (sortBy != null && !sortBy.isEmpty())
			sortingField = SortingField.getSortingField(sortBy.toUpperCase());

		PhenotypeAnnotationSorting sorting = new PhenotypeAnnotationSorting();
		fullDiseaseAnnotationList.sort(sorting.getComparator(sortingField, pagination.getAsc()));

		// paginating
		return fullDiseaseAnnotationList.stream().skip(pagination.getStart()).limit(pagination.getLimit()).collect(toList());
	}

	public List<PhenotypeAnnotation> getPhenotypeAnnotationList(String geneID) {

		List<PhenotypeAnnotation> cache = cacheService.getCacheEntries(geneID, CacheAlliance.GENE_PHENOTYPE);
		Optional<List<PhenotypeAnnotation>> optional = Optional.ofNullable(cache);
		return optional.orElse(new ArrayList<>());
	}

	public List<PrimaryAnnotatedEntity> getPhenotypeAnnotationPureModeList(String geneID) {
		return cacheService.getCacheEntries(geneID, CacheAlliance.GENE_PURE_AGM_PHENOTYPE);
	}
}
