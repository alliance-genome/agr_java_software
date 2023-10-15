package org.alliancegenome.neo4j.repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;
import org.alliancegenome.neo4j.view.HomologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.neo4j.ogm.model.Result;

public class OrthologousRepository extends Neo4jRepository<Orthologous> {

	public static final String COLLECT_DISTINCT_MATCHED = "collect(distinct matched)";
	public static final String COLLECT_DISTINCT_NOT_MATCHED = "collect(distinct notMatched)";
	public static final String COLLECT_DISTINCT_NOT_CALLED = "collect(distinct notCalled)";

	public OrthologousRepository() {
		super(Orthologous.class);
	}


	public JsonResultResponse<HomologView> getOrthologyByTwoSpecies(String speciesOne, String speciesTwo, OrthologyFilter filter) {

		final String taxonOne = SpeciesType.getTaxonId(speciesOne);
		final String taxonTwo = SpeciesType.getTaxonId(speciesTwo);

		StringJoiner sj = new StringJoiner(",");
		if (filter.hasMethods()) {
			filter.getMethods().forEach(method -> sj.add("'" + method + "'"));
		}
		String query = " MATCH p1=(g:Gene)-[ortho:ORTHOLOGOUS]->(gh:Gene), ";
		query += "p4=(g:Gene)-->(s:OrthologyGeneJoin)-->(gh:Gene) ";
		if (filter.hasMethods()) {
			query += ", p5=(s:OrthologyGeneJoin)-[:MATCHED]->(matched:OrthoAlgorithm {name:" + sj.toString() + "}) ";
		}
		query += " where g.taxonId = '" + taxonOne + "'";
		if (taxonTwo != null)
			query += " and	 gh.taxonId = '" + taxonTwo + "' ";
		if (filter.getStringency() != null) {
			if (filter.getStringency().equals(OrthologyFilter.Stringency.STRINGENT))
				query += " and ortho.strictFilter = true ";
			if (filter.getStringency().equals(OrthologyFilter.Stringency.MODERATE))
				query += " and ortho.moderateFilter = true ";
		}
		query += "OPTIONAL MATCH p6=(s:OrthologyGeneJoin)-[:NOT_MATCHED]->(notMatched:OrthoAlgorithm) ";
		query += "OPTIONAL MATCH p7=(s:OrthologyGeneJoin)-[:NOT_CALLED]->(notCalled:OrthoAlgorithm) ";
		String recordQuery = query + "return distinct g, gh, collect(distinct ortho), ";
		recordQuery += COLLECT_DISTINCT_NOT_MATCHED + ", " + COLLECT_DISTINCT_NOT_CALLED + " order by g.symbol, gh.symbol ";
		recordQuery += " SKIP " + (filter.getStart() - 1) + " limit " + filter.getRows();
		Result result = queryForResult(recordQuery);
		Set<HomologView> homologViews = new LinkedHashSet<>();
		result.forEach(objectMap -> {
			HomologView view = new HomologView();
			Gene gene = (Gene) objectMap.get("g");
			//gene.setSpeciesName(SpeciesType.fromTaxonId(taxonOne).getName());
			view.setGene(gene);

			Gene homologGene = (Gene) objectMap.get("gh");
			view.setHomologGene(homologGene);

			view.setBest(((List<Orthologous>) objectMap.get("collect(distinct ortho)")).get(0).getIsBestScore());
			view.setBestReverse(((List<Orthologous>) objectMap.get("collect(distinct ortho)")).get(0).getIsBestRevScore());

			setPredictionInfo(objectMap, view);
			homologViews.add(view);
		});

		String countQuery = query + "return distinct g, gh";
		Result count = queryForResult(countQuery);
		JsonResultResponse<HomologView> response = new JsonResultResponse<>();
		response.setResults(new ArrayList<>(homologViews));
		List<Integer> counterSet = new ArrayList<>();
		count.forEach(stringObjectMap -> counterSet.add(1));
		response.setTotal(counterSet.size());
		return response;
	}

	// do the counts manually as the query does not return those values
	private void setPredictionInfo(Map<String, Object> objectMap, HomologView view) {
		List<String> methodListNotMatched = getMethodList(objectMap, COLLECT_DISTINCT_NOT_MATCHED);
		view.setPredictionMethodsNotMatched(methodListNotMatched);
		List<String> methodListNotCalled = getMethodList(objectMap, COLLECT_DISTINCT_NOT_CALLED);
		view.setPredictionMethodsNotCalled(methodListNotCalled);
		List<String> methodListMatched = getAllMethods().stream()
				.filter(orthoAlgorithm -> !methodListNotCalled.contains(orthoAlgorithm.getName()) && !methodListNotMatched.contains(orthoAlgorithm.getName()))
				.map(OrthoAlgorithm::getName)
				.collect(Collectors.toList());
		view.setPredictionMethodsMatched(methodListMatched);
		view.setMethodCount(methodListMatched.size());
		view.setTotalMethodCount(methodListMatched.size() + methodListNotMatched.size());
	}

	private List<String> getMethodList(Map<String, Object> objectMap, String alias) {
		if (!(objectMap.get(alias) instanceof List))
			return new ArrayList<>();
		List<OrthoAlgorithm> algorithms = (List<OrthoAlgorithm>) objectMap.get(alias);

		return algorithms.stream()
				.map(orthoAlgorithm -> orthoAlgorithm.getName())
				.sorted(String::compareTo)
				.collect(Collectors.toList());
	}

	// cache variable
	private List<OrthoAlgorithm> algorithmList;

	public List<OrthoAlgorithm> getAllMethods() {
		if (algorithmList != null)
			return algorithmList;
		String query = " MATCH (algorithm:OrthoAlgorithm) return distinct algorithm order by algorithm.name ";
		Iterable<OrthoAlgorithm> algorithms = query(OrthoAlgorithm.class, query);
		algorithmList = StreamSupport.stream(algorithms.spliterator(), false)
				.collect(Collectors.toList());
		return algorithmList;
	}
}
