package org.alliancegenome.api.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.alliancegenome.cache.repository.helper.AnnotationFiltering;
import org.alliancegenome.cache.repository.helper.FilterFunction;
import org.alliancegenome.cache.repository.helper.SortingField;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.view.BaseFilter;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class FilterService<T> {

	private AnnotationFiltering<T> filtering;

	public FilterService(AnnotationFiltering<T> filtering) {
		super();
		this.filtering = filtering;
	}

	public List<T> filterAnnotations(List<T> annotationList, BaseFilter fieldFilterValueMap) {
		if (annotationList == null)
			return null;
		if (fieldFilterValueMap == null || fieldFilterValueMap.size()==0)
			return annotationList;
		return annotationList.stream()
				.filter(annotation -> containsFilterValue(annotation, fieldFilterValueMap))
				.collect(toList());
	}

	public boolean containsFilterValue(T annotation, BaseFilter fieldFilterValueMap) {
		// remove entries with null values.
		fieldFilterValueMap.values().removeIf(Objects::isNull);

		Set<Boolean> filterResults = fieldFilterValueMap.entrySet().stream()
				.map((entry) -> {
					FilterFunction<T, String> filterFunction = filtering.filterFieldMap.get(entry.getKey());
					if (filterFunction == null)
						return null;
					return filterFunction.containsFilterValue(annotation, entry.getValue());
				})
				.collect(Collectors.toSet());

		return !filterResults.contains(false);
	}

	public List<T> getSortedAndPaginatedAnnotations(Pagination pagination, List<T> entityList, Sorting<T> sorting) {
		// sorting
		if (sorting != null) {
			SortingField sortingField = null;
			String sortBy = pagination.getSortBy();
			if (sortBy != null && !sortBy.isEmpty())
				sortingField = SortingField.getSortingField(sortBy.toUpperCase());

			entityList.sort(sorting.getComparator(sortingField, pagination.getAsc()));
		}

		// paginating
		return entityList.stream()
				.skip(pagination.getStart())
				.limit(pagination.getLimit())
				.collect(toList());
	}

	public List<T> getPaginatedAnnotations(Pagination pagination, List<T> list) {
		// paginating
		return list.stream()
				.skip(pagination.getStart())
				.limit(pagination.getLimit())
				.collect(toList());
	}

	public Map<String, List<String>> getDistinctFieldValues(List<T> list, Map<Column, Function<T, Set<String>>> fieldValueMap, ColumnFieldMapping mapping) {
		Map<String, List<String>> map = new HashMap<>();
		
		for(Entry<Column, Function<T, Set<String>>> entry: fieldValueMap.entrySet()) {
			Set<String> distinctValues = new HashSet<>();
			for(T thing: list) {
				Set<String> set = entry.getValue().apply(thing);
				if(set != null) {
					//log.info("FilterService: " + set);
					distinctValues.addAll(set);
				}
			}
			ArrayList<String> valueList = new ArrayList<>(distinctValues);
			valueList.sort(Comparator.naturalOrder());
			map.put(mapping.getFieldFilterName(entry.getKey()), valueList);

		}
		return map;
	}

	public Map<String, List<String>> getDistinctFieldValuesWithStats(List<T> list, Map<Column, Function<T, Set<String>>> fieldValueMap, ColumnFieldMapping mapping) {
		Map<String, List<String>> map = new HashMap<>();
		fieldValueMap.forEach((column, function) -> {
			Set<String> distinctValues = new HashSet<>();
			List<String> allValues = new ArrayList<>();
			list.forEach(entity -> {
				distinctValues.addAll(function.apply(entity));
				allValues.addAll(function.apply(entity));
			});
			Map<String, List<String>> stats = allValues.stream().collect(groupingBy(value -> value));
			List<String> valueList = distinctValues.stream()
					.map(val -> val + " (" + stats.get(val).size() + ")")
					.sorted(Comparator.naturalOrder())
					.collect(toList());
			map.put(mapping.getFieldFilterName(column), valueList);
		});
		return map;
	}
}

