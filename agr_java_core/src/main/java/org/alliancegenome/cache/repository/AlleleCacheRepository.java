package org.alliancegenome.cache.repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;
import org.alliancegenome.cache.repository.helper.AlleleFiltering;
import org.alliancegenome.cache.repository.helper.AlleleSorting;
import org.alliancegenome.cache.repository.helper.AlleleVariantSequenceFiltering;
import org.alliancegenome.cache.repository.helper.AlleleVariantSequenceSorting;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.cache.repository.helper.SortingField;
import org.alliancegenome.core.api.service.AlleleColumnFieldMapping;
import org.alliancegenome.core.api.service.AlleleVariantSequenceColumnFieldMapping;
import org.alliancegenome.core.api.service.ColumnFieldMapping;
import org.alliancegenome.core.api.service.FilterService;
import org.alliancegenome.core.api.service.Table;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.apache.commons.collections.CollectionUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class AlleleCacheRepository {

	@Inject CacheService cacheService;

	public JsonResultResponse<Allele> getAllelesBySpecies(String taxonID, Pagination pagination) {
		List<Allele> allAlleles = cacheService.getCacheEntries(taxonID, CacheAlliance.ALLELE_SPECIES);
		if (CollectionUtils.isEmpty(allAlleles)) {
			return JsonResultResponse.getEmptyInstance();
		}
		return getAlleleJsonResultResponse(pagination, allAlleles);
	}

	public JsonResultResponse<Allele> getAllelesByGene(String geneID, Pagination pagination) {
		List<Allele> allAlleles = cacheService.getCacheEntries(geneID, CacheAlliance.ALLELE_GENE);
		if (allAlleles == null)
			return null;
		return getAlleleJsonResultResponse(pagination, allAlleles);
	}

	public JsonResultResponse<AlleleVariantSequence> getAllelesAndVariantsByGene(String geneID, Pagination pagination) {
		List<AlleleVariantSequence> allAlleles = cacheService.getCacheEntries(geneID, CacheAlliance.ALLELE_VARIANT_SEQUENCE_GENE);
		if (allAlleles == null)
			return null;
		return getAlleleAndVariantJsonResultResponse(pagination, allAlleles);
	}

	public List<Allele> getSortedAndPaginatedAlleles(List<Allele> alleleList, Pagination pagination) {
		// sorting
		SortingField sortingField = null;
		String sortBy = pagination.getSortBy();
		if (sortBy != null && !sortBy.isEmpty())
			sortingField = SortingField.getSortingField(sortBy.toUpperCase());

		AlleleSorting sorting = new AlleleSorting();
		alleleList.sort(sorting.getComparator(sortingField, pagination.getAsc()));

		// paginating
		return alleleList.stream()
				.skip(pagination.getStart())
				.limit(pagination.getLimit())
				.collect(Collectors.toList());
	}

  public JsonResultResponse<Allele> getAlleleJsonResultResponse(Pagination pagination, List<Allele> allAlleles) {
		JsonResultResponse<Allele> response = new JsonResultResponse<>();
		FilterService<Allele> filterService = new FilterService<>(new AlleleFiltering());
		ColumnFieldMapping<Allele> mapping = new AlleleColumnFieldMapping();

		if(pagination.getLimit()+pagination.getPage()>150000){
			List<Allele> filteredAlleleList = filterService.filterAnnotations(allAlleles, pagination.getFieldFilterValueMap());
			response.setResults(getSortedAndPaginatedAlleles(filteredAlleleList, pagination));
			response.setTotal(filteredAlleleList.size());
			// add distinct values
			response.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(allAlleles, mapping.getSingleValuedFieldColumns(Table.ALLELE_GENE), mapping));

		}else {
			response.setResults(allAlleles);
			response.setTotal((int) pagination.getTotalHits());
			response.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(allAlleles, mapping.getSingleValuedFieldColumns(Table.ALLELE_GENE), mapping));
		}
		return response;
	}
 /*public JsonResultResponse<Allele> getAlleleJsonResultResponse(Pagination pagination, List<Allele> allAlleles) {
	 JsonResultResponse<Allele> response = new JsonResultResponse<>();
	 FilterService<Allele> filterService = new FilterService<>(new AlleleFiltering());
	 ColumnFieldMapping<Allele> mapping = new AlleleColumnFieldMapping();


		 List<Allele> filteredAlleleList = filterService.filterAnnotations(allAlleles, pagination.getFieldFilterValueMap());
		 response.setResults(getSortedAndPaginatedAlleles(filteredAlleleList, pagination));
		 response.setTotal(filteredAlleleList.size());
		 // add distinct values
		 response.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(allAlleles, mapping.getSingleValuedFieldColumns(Table.ALLELE_GENE), mapping));


	 return response;
 }*/

	public JsonResultResponse<AlleleVariantSequence> getAlleleAndVariantJsonResultResponse(Pagination pagination, List<AlleleVariantSequence> allAlleles) {
		JsonResultResponse<AlleleVariantSequence> response = new JsonResultResponse<>();
		log.info("BEFORE Filter:"+new Date());

		FilterService<AlleleVariantSequence> filterService = new FilterService<>(new AlleleVariantSequenceFiltering());
		ColumnFieldMapping<AlleleVariantSequence> mapping = new AlleleVariantSequenceColumnFieldMapping();

			List<AlleleVariantSequence> filteredAlleleList = filterService.filterAnnotations(allAlleles, pagination.getFieldFilterValueMap());
			response.setResults(filterService.getSortedAndPaginatedAnnotations(pagination, filteredAlleleList, new AlleleVariantSequenceSorting()));
			response.setTotal(filteredAlleleList.size());

			// add distinct values
			response.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(allAlleles,
					mapping.getSingleValuedFieldColumns(Table.ALLELE_VARIANT_GENE), mapping));
		log.info("After Filter:"+new Date());

		return response;
	}


	private void printTaxonMap() {
		log.info("Taxon / Allele map: ");
		StringBuilder builder = new StringBuilder();
		// TODO taxonAlleleMap.forEach((key, value) -> builder.append(SpeciesType.fromTaxonId(key).getDisplayName() + ": " + value.size() + ", "));
		log.info(builder.toString());

	}

	public List<PhenotypeAnnotation> getPhenotype(String alleleId) {
		//BasicCachingManager<PhenotypeAnnotation> manager = new BasicCachingManager<>(PhenotypeAnnotation.class);
		List<PhenotypeAnnotation> phenotypeAnnotations = cacheService.getCacheEntries(alleleId, CacheAlliance.ALLELE_PHENOTYPE);
		if (phenotypeAnnotations == null)
			return null;
		return phenotypeAnnotations;
	}

	public List<DiseaseAnnotation> getDisease(String alleleId) {

		List<DiseaseAnnotation> diseaseAnnotations = cacheService.getCacheEntries(alleleId, CacheAlliance.ALLELE_DISEASE);
		if (diseaseAnnotations == null)
			return null;
		return diseaseAnnotations;
	}
}
