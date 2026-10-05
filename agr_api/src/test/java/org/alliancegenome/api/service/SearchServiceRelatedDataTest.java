package org.alliancegenome.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.api.es.dao.SearchDAO;
import org.alliancegenome.api.es.search.Category;
import org.alliancegenome.api.es.search.RelatedDataLink;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.junit.Test;

public class SearchServiceRelatedDataTest {

	@Test
	public void keepsLinkOrderFieldsLabelsAndZeroCountFiltering() {
		CountingDAO dao = new CountingDAO();
		dao.zeroCategory = Category.VARIANT.getName();
		SearchService service = new SearchService(dao);
		Map<String, Object> gene = result(Category.GENE, "gene-alpha");

		service.addRelatedDataLinks(gene);

		List<RelatedDataLink> links = links(gene);
		assertEquals(List.of(Category.DISEASE.getName(), Category.ALLELE.getName(), Category.GO.getName(), Category.MODEL.getName()),
			links.stream().map(RelatedDataLink::getCategory).toList());
		for (RelatedDataLink link : links) {
			assertEquals("genes", link.getTargetField());
			assertEquals("gene-alpha", link.getSourceName());
			assertEquals(Long.valueOf(7), link.getCount());
			assertEquals(null, link.getLabel());
		}
		assertEquals(5, dao.queries.size());
		assertEquals(0, dao.individualCounts);
	}

	@Test
	public void plansFiftyGeneAndModelResultsWithoutPerResultSearches() {
		for (Category category : List.of(Category.GENE, Category.MODEL)) {
			CountingDAO dao = new CountingDAO();
			SearchService service = new SearchService(dao);
			List<Map<String, Object>> results = new ArrayList<>();
			for (int index = 0; index < 50; index++) {
				results.add(result(category, "name-" + index));
			}

			service.addRelatedDataLinks(results);

			assertEquals(category == Category.GENE ? 250 : 150, dao.queries.size());
			assertEquals(1, dao.batchedCalls);
			assertEquals(0, dao.individualCounts);
			assertEquals(category == Category.GENE ? 5 : 3, links(results.getLast()).size());
		}
	}

	@Test
	public void preservesPrecomputedDataIncludingEmptyAndNullValues() {
		CountingDAO dao = new CountingDAO();
		SearchService service = new SearchService(dao);
		Map<String, Object> gene = result(Category.GENE, "gene");
		List<Map<String, Object>> existing = List.of(Map.of("count", 3, "category", "existing"));
		gene.put("relatedData", existing);
		Map<String, Object> empty = result(Category.MODEL, "model");
		empty.put("relatedData", List.of());
		Map<String, Object> nullValue = result(Category.ALLELE, "allele");
		nullValue.put("relatedData", null);

		service.addRelatedDataLinks(List.of(gene, empty, nullValue));

		assertSame(existing, gene.get("relatedData"));
		assertEquals(List.of(), empty.get("relatedData"));
		assertTrue(nullValue.containsKey("relatedData"));
		assertEquals(null, nullValue.get("relatedData"));
		assertTrue(dao.queries.isEmpty());
	}

	@Test
	public void duplicateNamesReuseCountsButKeepIndependentLinkObjects() {
		CountingDAO dao = new CountingDAO();
		SearchService service = new SearchService(dao);
		Map<String, Object> first = result(Category.GENE, "same-gene");
		Map<String, Object> second = result(Category.GENE, "same-gene");

		service.addRelatedDataLinks(List.of(first, second));

		assertEquals(5, dao.queries.size());
		assertEquals(links(first), links(second));
		assertNotSame(links(first).getFirst(), links(second).getFirst());
	}

	@Test
	public void mixedCategoriesAndAllGoBranchesRetainTheirDifferentTargets() {
		CountingDAO dao = new CountingDAO();
		SearchService service = new SearchService(dao);
		Map<String, Object> disease = result(Category.DISEASE, "disease");
		Map<String, Object> allele = result(Category.ALLELE, "allele");
		Map<String, Object> model = result(Category.MODEL, "model");
		List<Map<String, Object>> results = new ArrayList<>(List.of(disease, allele, model));
		for (String branch : List.of("biological_process", "molecular_function", "cellular_component")) {
			Map<String, Object> go = result(Category.GO, branch);
			go.put("branch", branch);
			results.add(go);
		}

		service.addRelatedDataLinks(results);

		assertEquals(List.of("diseasesWithParents", "diseasesWithParents", "diseasesWithParents"), links(disease).stream().map(RelatedDataLink::getTargetField).toList());
		assertEquals(List.of("alleles", "alleles", "alleles"), links(allele).stream().map(RelatedDataLink::getTargetField).toList());
		assertEquals(List.of("models", "models", "models"), links(model).stream().map(RelatedDataLink::getTargetField).toList());
		assertEquals("biologicalProcessWithParents", links(results.get(3)).getFirst().getTargetField());
		assertEquals("molecularFunctionWithParents", links(results.get(4)).getFirst().getTargetField());
		assertEquals(List.of("cellularComponentWithParents", "cellularComponentExpressionWithParents"), links(results.get(5)).stream().map(RelatedDataLink::getTargetField).toList());
		assertEquals(List.of("Genes Annotated with this GO Term", "Genes Expressed in this Structure"), links(results.get(5)).stream().map(RelatedDataLink::getLabel).toList());
		assertEquals(13, dao.queries.size());
	}

	@Test
	public void retainsExclusionPredicateSemanticsForLeadingMinusNames() {
		CountingDAO dao = new CountingDAO();
		SearchService service = new SearchService(dao);

		service.addRelatedDataLinks(result(Category.GENE, "-excluded"));

		for (QueryBuilder query : dao.queries) {
			BoolQueryBuilder bool = (BoolQueryBuilder) query;
			assertEquals(1, bool.mustNot().size());
			TermQueryBuilder term = (TermQueryBuilder) bool.mustNot().getFirst();
			assertEquals("genes.keyword", term.fieldName());
			assertEquals("excluded", term.value());
		}
	}

	@Test
	public void unknownCategoriesReceiveEmptyLinksWithoutCountQueries() {
		CountingDAO dao = new CountingDAO();
		Map<String, Object> result = new LinkedHashMap<>(Map.of("category", "unknown"));

		new SearchService(dao).addRelatedDataLinks(result);

		assertEquals(List.of(), result.get("relatedData"));
		assertTrue(dao.queries.isEmpty());
	}

	@Test
	public void failedCountsDoNotPartiallyEnrichTheResultPage() {
		CountingDAO dao = new CountingDAO();
		dao.fail = true;
		List<Map<String, Object>> results = List.of(result(Category.GENE, "gene"), result(Category.MODEL, "model"));

		assertThrows(IllegalStateException.class, () -> new SearchService(dao).addRelatedDataLinks(results));

		assertTrue(results.stream().noneMatch(result -> result.containsKey("relatedData")));
	}

	private static Map<String, Object> result(Category category, String name) {
		return new LinkedHashMap<>(Map.of("category", category.getName(), "nameKey", name));
	}

	@SuppressWarnings("unchecked")
	private static List<RelatedDataLink> links(Map<String, Object> result) {
		return (List<RelatedDataLink>) result.get("relatedData");
	}

	private static class CountingDAO extends SearchDAO {
		private List<QueryBuilder> queries = List.of();
		private int individualCounts;
		private int batchedCalls;
		private String zeroCategory = "";
		private boolean fail;

		@Override
		public List<Long> performCountQueries(List<QueryBuilder> queries) {
			this.queries = List.copyOf(queries);
			batchedCalls++;
			if (fail) {
				throw new IllegalStateException("Synthetic Elasticsearch failure");
			}
			return queries.stream().map(query -> query.toString().contains(zeroCategory) && !zeroCategory.isEmpty() ? 0L : 7L).toList();
		}

		@Override
		public Long performCountQuery(QueryBuilder query) {
			individualCounts++;
			throw new AssertionError("Individual count query must not run during enrichment");
		}
	}
}
