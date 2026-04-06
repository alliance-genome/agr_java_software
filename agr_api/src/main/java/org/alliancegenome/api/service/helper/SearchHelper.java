package org.alliancegenome.api.service.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.model.search.AggResult;
import org.alliancegenome.es.model.search.Category;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

import jakarta.ws.rs.core.UriInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchHelper {

	private static final String[] SUFFIX_LIST = {".htmlSmoosh", ".keywordAutocomplete", ".keyword", ".autocomplete", ".smoosh", ".synonyms", ".symbols", ".text", ".classicText", ".standardText", ".letterText", ".bigrams", ".standardBigrams"};

	private HashMap<String, List<String>> categoryFilters = new HashMap<>() {
		{
			put(Category.GENE.getName(), new ArrayList<>() {
				{
					add("species");
					add("biotypes");
					add("diseasesAgrSlim");
					add("biologicalProcessAgrSlim");
					add("molecularFunctionAgrSlim");
					add("cellularComponentAgrSlim");
					add("anatomicalExpressionSlim");
					add("subcellularExpressionAgrSlim");
				}
			});
			put(Category.GO.getName(), new ArrayList<>() {
				{
					add("branch");
					add("associatedSpecies");
					add("genes");
				}
			});
			put(Category.DATASET.getName(), new ArrayList<String>() {
				{
					add("species");
					add("tags");
					add("assays");
					add("anatomicalExpressionSlim");
					add("sex");
//					  add("stage"); will be implemented in the future
				}
			});
			put(Category.DISEASE.getName(), new ArrayList<>() {
				{
					add("diseaseGroup");
					add("genes");
					add("associatedSpecies");
				}
			});
			put(Category.ALLELE.getName(), new ArrayList<>() {
				{
					add("species");
					add("alterationType");
					add("variantType");
					add("molecularConsequence");
					add("diseasesAgrSlim");
					add("genes");
					add("constructExpressedComponent");
					add("constructKnockdownComponent");
					add("constructRegulatoryRegion");
				}
			});
			put(Category.MODEL.getName(), new ArrayList<>() {
				{
					add("species");
					add("diseasesAgrSlim");
					add("genes");
					add("alleles");
				}
			});
			put(Category.VARIANT.getName(), new ArrayList<>() {
				{
					add("species");
					add("alterationType");
					add("variantType");
					add("molecularConsequence");
					add("diseasesAgrSlim");
					add("genes");
				}
			});
		}
	};

	public Map<String, String> highlightCollapseMap = new HashMap<>() {
		{
			put("anatomicalExpressionSlim", "expression");
			put("anatomicalExpressionWithParents", "expression");
			put("cellularComponentExpression", "expression");
			put("cellularComponentExpressionWithParents", "expression");
			put("cellularComponentExpressionAgrSlim", "expression");
			put("expressionStages", "expression");
			put("whereExpressed", "expression");
		}
	};

	public Map<String, Float> getBoostMap() {
		return boostMap;
	}

	private Map<String, Float> boostMap = new HashMap<>() {
		{
			put("symbol", 5.0F);
			put("symbol.autocomplete", 2.0F);
			put("name.autocomplete", 0.1F);
			put("synonyms.keyword", 2.0F);
			put("synonyms", 2.0F);
			put("genes", 0.5F);
		}
	};

	public List<String> getSearchFields() {
		return searchFields;
	}

	private List<String> searchFields = new ArrayList<>() {
		{
			add("alleles");
			add("alleles.text");
			add("alleles.autocomplete");
			add("anatomicalExpressionSlim");
			add("anatomicalExpressionSlim.keyword");
			add("anatomicalExpressionWithParents");
			add("anatomicalExpressionWithParents.keyword");
			add("associatedSpecies");
			add("associatedSpecies.synonyms");
			add("automatedGeneDescription");
			add("biotypes");
			add("biologicalProcessWithParents");
			add("cellularComponentWithParents");
			add("cellularComponentExpression");
			add("cellularComponentExpression.keyword");
			add("cellularComponentExpressionWithParents");
			add("cellularComponentExpressionWithParents.keyword");
			add("cellularComponentExpressionAgrSlim");
			add("cellularComponentExpressionAgrSlim.keyword");
			add("chromosomes");
			add("consequences.hgvsc");
			add("consequences.hgvsg");
			add("consequences.hgvsp");
			add("consequences.gene");
			add("constructs");
			add("constructs.keyword");
			add("constructs.classicText");
			add("constructExpressedComponent");
			add("constructKnockdownComponent");
			add("constructRegulatoryRegion");
			add("crossReferences");
			add("crossReferences.classicText");
			add("definition");
			add("definition.standardText");
			add("diseases");
			add("diseasesAgrSlim");
			add("diseasesWithParents");
			add("disease_genes");
			add("disease_synonyms");
			add("variantType");
			add("expressionStages");
			add("expressionStages.standardText");
			add("external_ids");
			add("genes");
			add("genes.keyword");
			add("genes.autocomplete");
			add("genes.keywordAutocomplete");
			add("geneDescription");
			add("geneSynonyms");
			add("geneCrossReferences");
			add("globalId");
			add("subtype");
			add("go_genes");
			add("go_synonyms");
			add("curie");
			add("id");
			add("localId");
			add("nameKey");
			add("nameKey.autocomplete");
			add("nameKey.htmlSmoosh");
			add("nameKey.keyword");
			add("nameKey.standardBigrams");
			add("nameKey.keywordAutocomplete");
			add("name");
			add("name.autocomplete");
			add("name.htmlSmoosh");
			add("name.keyword");
			add("name.standardBigrams");
			add("nameText");
			add("nameText.keyword");
			add("nameText.standardText");
			add("models");
			add("modLocalId");
			add("molecularConsequence");
			add("molecularFunctionWithParents");
			add("phenotypeStatements");
			add("primaryKey");
			add("sampleIds");
			add("variants");
			add("variants.keyword");
			add("variants.standardText");
			add("variantSynonyms");
			add("variantSynonyms.keyword");
			add("variantSynonyms.standardText");
//			  add("stage");
//			  add("stage.keyword"); will be implemented in the future
			add("symbol");
			add("symbol.autocomplete");
			add("symbol.keyword");
			add("symbol.htmlSmoosh");
			add("symbolText");
			add("symbolText.keyword");
			add("symbolText.standardText");
			add("synonyms");
			add("synonyms.keyword");
			add("synonyms.htmlSmoosh");
			add("synonyms.standardBigrams");
			add("species");
			add("species.synonyms");
			add("secondaryIds");
			add("soTermName");
			add("soTermName.letterText");
			add("strictOrthologySymbols.autocomplete");
			add("strictOrthologySymbols.keyword");
			add("summary");
			add("variantName");
			add("whereExpressed");
			add("whereExpressed.keyword");
		}
	};

	@Getter
	private final List<String> responseFields = new ArrayList<>() {
		{
			add("alterationType");
			add("biologicalProcess");
			add("branch");
			add("category");
			add("cellularComponent");
			add("crossReferences");
			add("crossReferenceLinks");
			add("curie");
			add("dataProvider");
			add("definition");
			add("description");
			add("geneDescription");
			add("automatedGeneDescription");
			add("diseases");
			add("variantType");
			add("external_ids");
			add("gene_chromosome_ends");
			add("gene_chromosome_starts");
			add("gene_chromosomes");
			add("genes");
			add("href");
			add("modCrossRefCompleteUrl");
			add("molecularConsequence");
			add("molecularFunction");
			add("name");
			add("nameKey");
			add("nameKey");
			add("primaryKey");
			add("soTermName");
			add("species");
			add("summary");
			add("symbol");
			add("synonyms");
			add("tags");
			add("variants");
			add("variantName");
			add("relatedData");
		}
	};

	private List<String> highlightBlacklistFields = new ArrayList<>() {
		{
			add("go_genes");
			add("name.autocomplete");
		}
	};

	public List<AggregationBuilder> createAggBuilder(String category, Boolean expandBiotypes) {
		List<AggregationBuilder> ret = new ArrayList<>();

		if (category == null || !categoryFilters.containsKey(category)) {
			TermsAggregationBuilder term = AggregationBuilders.terms("categories");
			term.field("category");
			term.size(50);
			ret.add(term);
		} else {
			for (String item : categoryFilters.get(category)) {
				if (item.equals("biotypes")) {
					if (expandBiotypes) {
						ret.add(getBiotypeAggQuery());
					} else {
						TermsAggregationBuilder term = AggregationBuilders.terms("biotypes").field("biotype0.keyword");
						term.size(999);
						ret.add(term);
					}
				} else {
					TermsAggregationBuilder term = AggregationBuilders.terms(item);
					term.field(item + ".keyword");
					term.size(999);
					ret.add(term);
				}
			}
		}

		return ret;
	}

	public TermsAggregationBuilder getBiotypeAggQuery() {
		TermsAggregationBuilder biotype0 = AggregationBuilders.terms("biotypes").field("biotype0.keyword").subAggregation(AggregationBuilders.terms("biotype1").field("biotype1.keyword").subAggregation(AggregationBuilders.terms("biotype2").field("biotype2.keyword")));
		return biotype0;
	}

	public ArrayList<AggResult> formatAggResults(String category, SearchResponse res) {
		ArrayList<AggResult> ret = new ArrayList<>();

		if (category == null) {
			Terms aggs = res.getAggregations().get("categories");
			Set<String> acceptableKeys = new HashSet<>(categoryFilters.keySet());
			AggResult ares = new AggResult("category", aggs, acceptableKeys);
			orderCategoryBuckets(ares);
			ret.add(ares);
		} else {
			if (categoryFilters.containsKey(category)) {
				for (String item : categoryFilters.get(category)) {
					Terms aggs = res.getAggregations().get(item);
					AggResult ares = new AggResult(item, aggs, null);
					ret.add(ares);
				}
			}
		}

		return ret;
	}

	private void orderCategoryBuckets(AggResult aggResult) {
		aggResult.getValues().sort((a, b) -> Long.compare(b.getTotal(), a.getTotal()));
	}

	public boolean filterIsValid(String category, String fieldName) {
		String newFieldName = fieldName;
		if (this.isExcluded(fieldName)) {
			newFieldName = fieldName.substring(1);
		}
		if (searchFields.contains(newFieldName)) {
			return true;
		}

		if (!categoryFilters.containsKey(category)) {
			return false;
		}

		List<String> fields = categoryFilters.get(category);

		return fields.contains(newFieldName);
	}

	public void applyFilters(BoolQueryBuilder bool, String category, UriInfo uriInfo) {
		if (categoryFilters.containsKey(category)) {
			for (String item : categoryFilters.get(category)) {
				if (uriInfo.getQueryParameters().containsKey(item)) {
					for (String param : uriInfo.getQueryParameters().get(item)) {
						bool.filter(new TermQueryBuilder(item + ".keyword", param));
					}
				}
			}
		}
	}

	public ArrayList<Map<String, Object>> formatResults(SearchResponse res, List<String> searchedTerms) {
		log.debug("Formatting Results: ");
		ArrayList<Map<String, Object>> ret = new ArrayList<>();

		for (SearchHit hit : res.getHits()) {
			Map<String, List<String>> map = new HashMap<>();
			for (String key : hit.getHighlightFields().keySet()) {

				ArrayList<String> list = new ArrayList<>();
				for (Text t : hit.getHighlightFields().get(key).getFragments()) {
					list.add(t.string());
				}

				String name = hit.getHighlightFields().get(key).getName();

				for (int i = 0; i < SUFFIX_LIST.length; i++) {
					name = name.replace(SUFFIX_LIST[i], "");
				}

				name = highlightCollapseMap.getOrDefault(name, name);

				if (map.containsKey(name)) {
					Set<String> existingRaw = new HashSet<>();
					for (String existing : map.get(name)) {
						existingRaw.add(existing.replaceAll("</?em>", ""));
					}
					for (String item : list) {
						if (!existingRaw.contains(item.replaceAll("</?em>", ""))) {
							map.get(name).add(item);
						}
					}
				} else {
					map.put(name, list);
				}

			}
			hit.getSourceAsMap().put("highlights", map);
			Object id = hit.getSourceAsMap().get("primaryKey");
			if (id == null) {
				id = hit.getSourceAsMap().get("curie");
			}
			hit.getSourceAsMap().put("id", id);
			hit.getSourceAsMap().put("score", hit.getScore());
			if (hit.getExplanation() != null) {
				hit.getSourceAsMap().put("explanation", hit.getExplanation());
			}

			hit.getSourceAsMap().put("missingTerms", findMissingTerms(Arrays.asList(hit.getMatchedQueries()), searchedTerms));
			ret.add(hit.getSourceAsMap());
		}
		log.debug("Finished Formatting Results: ");
		return ret;
	}

	private List<String> findMissingTerms(List<String> matchedTerms, List<String> searchedTerms) {

		List<String> terms = new ArrayList<>();

		// if only one term was searched, just assume it matched
		// (not for efficiency, avoids false negatives - if the document came back, the
		// single term matched)
		if (matchedTerms == null || searchedTerms == null || searchedTerms.size() == 1) {
			return terms; // just give up and return an empty list
		}

		terms.addAll(searchedTerms);
		terms.removeAll(matchedTerms);

		return terms;
	}

	public HighlightBuilder buildHighlights() {

		HighlightBuilder hlb = new HighlightBuilder();

		for (String field : searchFields) {
			if (!highlightBlacklistFields.contains(field)) {
				hlb.field(field);
			}
		}

		return hlb;
	}

	public Boolean isExcluded(String value) {
		return value.charAt(0) == '-';
	}

}
