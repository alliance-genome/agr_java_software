package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.alliancegenome.api.service.helper.SearchHelper;
import org.alliancegenome.api.es.dao.SearchDAO;
import org.alliancegenome.api.es.search.Category;
import org.alliancegenome.api.es.search.RelatedDataLink;
import org.alliancegenome.api.es.search.SearchApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

@RequestScoped
public class SearchService {
	private static final Logger LOG = Logger.getLogger(SearchService.class);

	// SCRUM-6096: tokens matching <PREFIX>:<localId> are treated as exact-curie candidates
	// and OR'd into the must clause as plain term queries, bypassing Lucene query_string's
	// split_on_whitespace=false behaviour that otherwise breaks multi-curie searches against
	// keyword fields (e.g. RGD genes whose primary curie sits only on `curie`).
	private static final Pattern CURIE_TOKEN_PATTERN = Pattern.compile("^[A-Za-z]+:[A-Za-z0-9_.\\-]+$");

	private final SearchDAO searchDAO;

	private SearchHelper searchHelper = new SearchHelper();

	private static QueryManipulationService queryManipulationService = new QueryManipulationService();

	public SearchService() {
		this(new SearchDAO());
	}

	SearchService(SearchDAO searchDAO) {
		this.searchDAO = Objects.requireNonNull(searchDAO);
	}

	public SearchApiResponse query(String q, String category, int limit, int offset, String sortBy, Boolean debug, UriInfo uriInfo) {

		long started = System.nanoTime();
		long mainSearchMillis = -1;
		long relatedDataMillis = -1;
		int relatedCountFilters = -1;
		boolean completed = false;
		try {
			SearchApiResponse result = new SearchApiResponse();

			if (StringUtils.isNotEmpty(q) && q.startsWith("debug")) {
				debug = true;
				q = q.replaceFirst("debug", "").trim();
			}

			MultivaluedMap filterMap = getFilters(category, uriInfo);

			QueryBuilder query = buildFunctionQuery(q, category, filterMap);

			QueryRescorerBuilder rescorerBuilder = buildRescorer(q);

			List<AggregationBuilder> aggBuilders = searchHelper.createAggBuilder(category, biotypeSelected(filterMap));

			HighlightBuilder hlb = searchHelper.buildHighlights();

			LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
			if (sortBy != null && sortBy.length() > 0) {
				sorts.put(sortBy, SortOrder.ASC);
			}

			SearchResponse searchResponse;
			long mainSearchStarted = System.nanoTime();
			try {
				searchResponse = searchDAO.performQuery(query, aggBuilders, rescorerBuilder, searchHelper.getResponseFields(), limit, offset, hlb, sorts, debug);
			} finally {
				mainSearchMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - mainSearchStarted);
			}

			if (debug != null && debug) {
				LOG.info("Search Query: " + q);
			} else {
				LOG.debug("Search Query: " + q);
			}

			result.setTotal(searchResponse.getHits().getTotalHits().value);
			result.setResults(searchHelper.formatResults(searchResponse, tokenizeQuery(q)));
			long relatedDataStarted = System.nanoTime();
			try {
				relatedCountFilters = populateRelatedDataLinks(result.getResults());
			} finally {
				relatedDataMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - relatedDataStarted);
			}
			result.setAggregations(searchHelper.formatAggResults(category, searchResponse));

			completed = true;
			return result;
		} finally {
			long totalMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
			if (totalMillis >= 1000 || Boolean.TRUE.equals(debug)) {
				int countRequests = relatedCountFilters < 0 ? -1 : (relatedCountFilters + SearchDAO.COUNT_QUERY_BATCH_SIZE - 1) / SearchDAO.COUNT_QUERY_BATCH_SIZE;
				LOG.infof("Search timing: total_ms=%d main_es_ms=%d related_data_ms=%d related_count_filters=%d related_count_requests=%d completed=%s",
					totalMillis, mainSearchMillis, relatedDataMillis, relatedCountFilters, countRequests, completed);
			}
		}
	}

	public QueryRescorerBuilder buildRescorer(String q) {
		if (StringUtils.isEmpty(q)) {

			List<FunctionScoreQueryBuilder.FilterFunctionBuilder> functionList = new ArrayList<>();
			functionList.add(variantDemotion());
			functionList.add(geneCategoryBoost());
			functionList.add(humanSpeciesBoost());
			functionList.add(documentHasDiseaseBoost());
			functionList.add(proteinCodingBoost());
			functionList.add(rnaBoost());
			functionList.add(pseudogeneBoost());

			return new QueryRescorerBuilder(new FunctionScoreQueryBuilder(functionList.toArray(new FunctionScoreQueryBuilder.FilterFunctionBuilder[functionList.size()])));
		}

		return new QueryRescorerBuilder(new FunctionScoreQueryBuilder(buildBoostFunctions(q)));
	}

	public QueryBuilder buildFunctionQuery(String q, String category, MultivaluedMap<String, String> filters) {

		BoolQueryBuilder bool = buildQuery(q, category, filters);

		if (StringUtils.isEmpty(q)) {

			List<FunctionScoreQueryBuilder.FilterFunctionBuilder> functionList = new ArrayList<>();

			FieldValueFactorFunctionBuilder popularity = ScoreFunctionBuilders.fieldValueFactorFunction("popularity");
			popularity.missing(1D);
			popularity.modifier(FieldValueFactorFunction.Modifier.SQRT);
			popularity.factor(1.1F);

			functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchAllQuery(), popularity));

			return new FunctionScoreQueryBuilder(bool, functionList.toArray(new FunctionScoreQueryBuilder.FilterFunctionBuilder[functionList.size()]));
		}

		FunctionScoreQueryBuilder builder = new FunctionScoreQueryBuilder(bool, buildBoostFunctions(q));

		return builder;
	}

	public FunctionScoreQueryBuilder.FilterFunctionBuilder[] buildBoostFunctions(String q) {
		List<FunctionScoreQueryBuilder.FilterFunctionBuilder> functionList = new ArrayList<>();
		// variant demotion
		functionList.add(variantDemotion());

		// gene category boost
		functionList.add(geneCategoryBoost());

		// human data boost
		functionList.add(humanSpeciesBoost());

		// gene biotype boost
		functionList.add(proteinCodingBoost());
		functionList.add(rnaBoost());
		functionList.add(pseudogeneBoost());

		functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("nameKey.keyword", q), ScoreFunctionBuilders.weightFactorFunction(1000F)));

		functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("primaryKey", q), ScoreFunctionBuilders.weightFactorFunction(1000F)));
		functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("curie", q), ScoreFunctionBuilders.weightFactorFunction(1000F)));

		functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("nameKey.keywordAutocomplete", q), ScoreFunctionBuilders.weightFactorFunction(500F)));

		functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("nameKey.standardBigrams", q), ScoreFunctionBuilders.weightFactorFunction(500F)));

		functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("species", q), ScoreFunctionBuilders.weightFactorFunction(2F)));

		functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("species.synonyms", q), ScoreFunctionBuilders.weightFactorFunction(2F)));

		functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("automatedGeneDescription", q), ScoreFunctionBuilders.weightFactorFunction(1.5F)));
		functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("geneDescription", q), ScoreFunctionBuilders.weightFactorFunction(1.5F)));

		functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("diseases", q), ScoreFunctionBuilders.weightFactorFunction(1.2F)));

		functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("diseasesWithParents", q), ScoreFunctionBuilders.weightFactorFunction(1.01F)));

		// per term boost, add a 'should' clause for each individual term
		List<String> tokens = tokenizeQuery(q);
		for (String token : tokens) {
			MultiMatchQueryBuilder mmq = multiMatchQuery(token);
			searchHelper.getSearchFields().stream().forEach(mmq::field);
			mmq.type(MultiMatchQueryBuilder.Type.CROSS_FIELDS);
			mmq.operator(Operator.AND);
			mmq.fields(searchHelper.getBoostMap());
			mmq.queryName(token);
			functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(mmq, ScoreFunctionBuilders.weightFactorFunction(10.0F)));
		}

		return functionList.toArray(new FunctionScoreQueryBuilder.FilterFunctionBuilder[functionList.size()]);

	}

	private FunctionScoreQueryBuilder.FilterFunctionBuilder variantDemotion() {
		return new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("alterationType", "variant"), ScoreFunctionBuilders.weightFactorFunction(0.09f));
	}

	private FunctionScoreQueryBuilder.FilterFunctionBuilder geneCategoryBoost() {
		return new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("category", Category.GENE.getName()), ScoreFunctionBuilders.weightFactorFunction(1.1F));
	}

	private FunctionScoreQueryBuilder.FilterFunctionBuilder proteinCodingBoost() {
		return new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("soTermName", "protein_coding_gene"), ScoreFunctionBuilders.weightFactorFunction(1.3F));
	}

	private FunctionScoreQueryBuilder.FilterFunctionBuilder rnaBoost() {
		return new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("soTermNameWithParents", "ncRNA_gene"), ScoreFunctionBuilders.weightFactorFunction(1.2F));
	}

	private FunctionScoreQueryBuilder.FilterFunctionBuilder pseudogeneBoost() {
		return new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("soTermName", "pseudogene"), ScoreFunctionBuilders.weightFactorFunction(0.5F));
	}

	private FunctionScoreQueryBuilder.FilterFunctionBuilder humanSpeciesBoost() {
		return new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("species", "Homo sapiens"), ScoreFunctionBuilders.weightFactorFunction(1.5F));
	}

	private FunctionScoreQueryBuilder.FilterFunctionBuilder documentHasDiseaseBoost() {
		return new FunctionScoreQueryBuilder.FilterFunctionBuilder(existsQuery("diseases"), ScoreFunctionBuilders.weightFactorFunction(1.1F));
	}

	public BoolQueryBuilder buildQuery(String queryTerm, String category, MultivaluedMap<String, String> filters) {

		BoolQueryBuilder bool = boolQuery();

		// handle the query input, if necessary
		if (StringUtils.isNotEmpty(queryTerm)) {

			queryTerm = queryManipulationService.processQuery(queryTerm);

			QueryStringQueryBuilder builder = queryStringQuery(queryTerm).defaultOperator(Operator.OR).allowLeadingWildcard(true);

			// add the fields one at a time
			searchHelper.getSearchFields().stream().forEach(builder::field);

			// this applies individual boosts, if they're in the map
			builder.fields(searchHelper.getBoostMap());

			// SCRUM-6096: query_string against keyword fields handles whitespace-separated
			// tokens as a single literal, so multi-curie searches like
			// "RGD:1306828 RGD:628748" return 0 hits whenever the primary curie lives only
			// on a keyword field. Detect ID-shaped tokens and OR-in plain term queries on
			// the curie/primary-key/cross-reference keyword fields as a parallel match path.
			List<String> curieTokens = new ArrayList<>();
			for (String tok : tokenizeQuery(queryTerm)) {
				if (CURIE_TOKEN_PATTERN.matcher(tok).matches()) {
					curieTokens.add(tok);
				}
			}
			if (curieTokens.isEmpty()) {
				bool.must(builder);
			} else {
				BoolQueryBuilder curieMatches = boolQuery();
				for (String tok : curieTokens) {
					// Tag each clause with the token name so the response's matched_queries
					// reflects the exact-term path; otherwise the UI labels the token as
					// "Missing" because the cross_fields MultiMatch boost named after the
					// token never matches a curie that lives only on a keyword field.
					curieMatches.should(termQuery("curie", tok).queryName(tok));
					curieMatches.should(termQuery("primaryKey", tok).queryName(tok));
					curieMatches.should(termQuery("globalId.keyword", tok).queryName(tok));
					curieMatches.should(termQuery("crossReferences.keyword", tok).queryName(tok));
				}
				bool.must(boolQuery().should(builder).should(curieMatches).minimumShouldMatch(1));
			}

		} else {
			bool.must(matchAllQuery());
		}

		// apply filters if a category has been set
		if (StringUtils.isNotEmpty(category)) {
			bool.filter(new TermQueryBuilder("category", category));
			// expand the map of lists and add each key,value pair as filters
			filters.entrySet().stream().forEach(entry -> entry.getValue().stream().forEach(value -> {
				if (value.charAt(0) == '-') {
					value = value.substring(1);
					// apply if a filter must be excluded
					bool.mustNot(new TermQueryBuilder(entry.getKey() + ".keyword", value));
				} else {
					bool.filter(new TermQueryBuilder(entry.getKey() + ".keyword", value));
				}
			}));

		} else {
			bool.filter(termQuery("searchable", true));
		}

		return bool;
	}

	public MultivaluedMap<String, String> getFilters(String category, UriInfo uriInfo) {
		MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
		uriInfo.getQueryParameters().entrySet().stream().filter(entry -> searchHelper.filterIsValid(category, entry.getKey())).forEach(entry -> map.addAll(entry.getKey(), entry.getValue()));
		return map;
	}

	public List<String> tokenizeQuery(String query) {
		List<String> tokens = new ArrayList<>();

		if (StringUtils.isEmpty(query)) {
			return tokens;
		}

		// undo colon escaping
		query = query.replaceAll("\\\\:", ":");

		// normalize the whitespace
		query = query.replaceAll("\\s+", " ");

		// extract quoted phrases
		Pattern p = Pattern.compile("\"([^\"]*)\"");
		Matcher m = p.matcher(query);
		while (m.find()) {
			String phrase = m.group(1);
			tokens.add(phrase);
			query = query.replaceAll("\"" + phrase + "\"", "");
		}

		// normalize the whitespace again
		query = query.replaceAll("\\s+", " ");

		// add the tokens
		tokens.addAll(Arrays.asList(query.split("\\s")));

		// strip boolean tokens, empty strings and spaces strings
		List<String> tokensToRemove = new ArrayList<>();
		tokensToRemove.add("AND");
		tokensToRemove.add("OR");
		tokensToRemove.add("NOT");
		tokensToRemove.add("");

		tokens.removeAll(tokensToRemove);

		return tokens;

	}

	public void addRelatedDataLinks(List<Map<String, Object>> results) {
		populateRelatedDataLinks(results);
	}

	private int populateRelatedDataLinks(List<Map<String, Object>> results) {
		List<PendingRelatedData> pending = new ArrayList<>();
		Map<RelatedDataKey, QueryBuilder> queries = new LinkedHashMap<>();
		for (Map<String, Object> result : results) {
			// Preserve precomputed data, including empty lists, exactly as before.
			if (result.containsKey("relatedData")) {
				continue;
			}
			List<RelatedDataLink> links = buildRelatedDataLinks(result);
			pending.add(new PendingRelatedData(result, links));
			for (RelatedDataLink link : links) {
				RelatedDataKey key = relatedDataKey(link);
				queries.computeIfAbsent(key, ignored -> buildRelatedDataQuery(link.getCategory(), link.getTargetField(), link.getSourceName()));
			}
		}
		List<Long> counts;
		try {
			counts = searchDAO.performCountQueries(new ArrayList<>(queries.values()));
			if (counts.size() != queries.size()) {
				throw new IllegalStateException("Related-data counts do not match requested filters");
			}
		} catch (RuntimeException e) {
			// Count badges are optional: preserve the successful main search, while
			// never presenting partial counts as exact or overwriting stored links.
			LOG.warn("Related-data count enrichment failed; returning results without calculated links", e);
			for (PendingRelatedData item : pending) {
				item.result().put("relatedData", new ArrayList<>());
			}
			return -1;
		}
		Map<RelatedDataKey, Long> countsByLink = new LinkedHashMap<>();
		int index = 0;
		for (RelatedDataKey key : queries.keySet()) {
			countsByLink.put(key, counts.get(index++));
		}
		for (PendingRelatedData item : pending) {
			for (RelatedDataLink link : item.links()) {
				link.setCount(countsByLink.get(relatedDataKey(link)));
			}
			item.result().put("relatedData", item.links().stream().filter(link -> link.getCount() > 0).collect(Collectors.toList()));
		}
		return queries.size();
	}

	public void addRelatedDataLinks(Map<String, Object> result) {
		addRelatedDataLinks(List.of(result));
	}

	private List<RelatedDataLink> buildRelatedDataLinks(Map<String, Object> result) {
		String nameKey = (String) result.get("nameKey");
		// String name = (String) result.get("name");
		String category = (String) result.get("category");

		List<RelatedDataLink> links = new ArrayList<>();

		if (StringUtils.equals(category, Category.GENE.getName())) {
			links.add(createRelatedDataLink(Category.DISEASE.getName(), "genes", nameKey));
			links.add(createRelatedDataLink(Category.ALLELE.getName(), "genes", nameKey));
			links.add(createRelatedDataLink(Category.VARIANT.getName(), "genes", nameKey));
			links.add(createRelatedDataLink(Category.GO.getName(), "genes", nameKey));
			links.add(createRelatedDataLink(Category.MODEL.getName(), "genes", nameKey));
		} else if (StringUtils.equals(category, Category.DISEASE.getName())) {
			links.add(createRelatedDataLink(Category.GENE.getName(), "diseasesWithParents", nameKey));
			links.add(createRelatedDataLink(Category.ALLELE.getName(), "diseasesWithParents", nameKey));
			links.add(createRelatedDataLink(Category.MODEL.getName(), "diseasesWithParents", nameKey));
		} else if (StringUtils.equals(category, Category.ALLELE.getName())) {
			links.add(createRelatedDataLink(Category.DISEASE.getName(), "alleles", nameKey));
			links.add(createRelatedDataLink(Category.GENE.getName(), "alleles", nameKey));
			links.add(createRelatedDataLink(Category.MODEL.getName(), "alleles", nameKey));
		} else if (StringUtils.equals(category, Category.MODEL.getName())) {
			links.add(createRelatedDataLink(Category.GENE.getName(), "models", nameKey));
			links.add(createRelatedDataLink(Category.ALLELE.getName(), "models", nameKey));
			links.add(createRelatedDataLink(Category.DISEASE.getName(), "models", nameKey));
		} else if (StringUtils.equals(category, Category.GO.getName())) {
			String goType = (String) result.get("branch");
			if (StringUtils.equals(goType, "biological_process")) {
				links.add(createRelatedDataLink(Category.GENE.getName(), "biologicalProcessWithParents", nameKey, "Genes Annotated with this GO Term"));
			} else if (StringUtils.equals(goType, "molecular_function")) {
				links.add(createRelatedDataLink(Category.GENE.getName(), "molecularFunctionWithParents", nameKey, "Genes Annotated with this GO Term"));
			} else if (StringUtils.equals(goType, "cellular_component")) {
				links.add(createRelatedDataLink(Category.GENE.getName(), "cellularComponentWithParents", nameKey, "Genes Annotated with this GO Term"));
				links.add(createRelatedDataLink(Category.GENE.getName(), "cellularComponentExpressionWithParents", nameKey, "Genes Expressed in this Structure"));
			}
		}

		return links;
	}

	public RelatedDataLink getRelatedDataLink(String targetCategory, String targetField, String sourceName) {
		return getRelatedDataLink(targetCategory, targetField, sourceName, null);
	}

	public RelatedDataLink getRelatedDataLink(String targetCategory, String targetField, String sourceName, String label) {
		RelatedDataLink link = createRelatedDataLink(targetCategory, targetField, sourceName, label);
		link.setCount(searchDAO.performCountQueries(List.of(buildRelatedDataQuery(targetCategory, targetField, sourceName))).getFirst());
		return link;
	}

	private QueryBuilder buildRelatedDataQuery(String targetCategory, String targetField, String sourceName) {
		MultivaluedMap<String, String> filters = new MultivaluedHashMap<>();
		filters.add(targetField, sourceName);
		return buildQuery(null, targetCategory, filters);
	}

	private RelatedDataLink createRelatedDataLink(String targetCategory, String targetField, String sourceName) {
		return createRelatedDataLink(targetCategory, targetField, sourceName, null);
	}

	private RelatedDataLink createRelatedDataLink(String targetCategory, String targetField, String sourceName, String label) {
		RelatedDataLink relatedDataLink = new RelatedDataLink();
		relatedDataLink.setCategory(targetCategory);
		relatedDataLink.setTargetField(targetField);
		relatedDataLink.setSourceName(sourceName);
		relatedDataLink.setLabel(label);
		return relatedDataLink;
	}

	private RelatedDataKey relatedDataKey(RelatedDataLink link) {
		return new RelatedDataKey(link.getCategory(), link.getTargetField(), link.getSourceName());
	}

	private record RelatedDataKey(String targetCategory, String targetField, String sourceName) { }

	private record PendingRelatedData(Map<String, Object> result, List<RelatedDataLink> links) { }

	private Boolean biotypeSelected(MultivaluedMap<String, String> filterMap) {
		if (filterMap.containsKey("biotypes")) {
			List<String> biotypes = filterMap.get("biotypes");
			for (String value : biotypes) {
				if (!searchHelper.isExcluded(value)) {
					return true;
				}
			}
		}
		return false;
	}
}
