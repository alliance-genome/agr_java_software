package org.alliancegenome.neo4j.repository;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;
import org.alliancegenome.neo4j.entity.node.OrthologyGeneJoin;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.model.Result;

import java.util.*;
import java.util.stream.Collectors;

public class OrthologousRepository extends Neo4jRepository<Orthologous> {

    public static final String COLLECT_DISTINCT_MATCHED = "collect(distinct matched)";
    public static final String COLLECT_DISTINCT_NOT_MATCHED = "collect(distinct notMatched)";
    public static final String COLLECT_DISTINCT_NOT_CALLED = "collect(distinct notCalled)";
    private final Logger log = LogManager.getLogger(getClass());

    public OrthologousRepository() {
        super(Orthologous.class);
    }


    public JsonResultResponse<OrthologView> getOrthologyByTwoSpecies(String speciesOne, String speciesTwo, OrthologyFilter filter) {

        final String taxonOne = SpeciesType.getTaxonId(speciesOne);
        final String taxonTwo = SpeciesType.getTaxonId(speciesTwo);

        String query = " MATCH p1=(g:Gene)-[ortho:ORTHOLOGOUS]->(gh:Gene), ";
        query += "p4=(g)--(s:OrthologyGeneJoin)--(gh:Gene), ";
        query += "p5=(s)-[:MATCHED]-(matched:OrthoAlgorithm), ";
        query += "p6=(s)-[:NOT_MATCHED]-(notMatched:OrthoAlgorithm), ";
        query += "p7=(s)-[:NOT_CALLED]-(notCalled:OrthoAlgorithm) ";
        query += " where g.taxonId = '" + taxonOne + "'";
        query += " and   gh.taxonId = '" + taxonTwo + "' ";
        if (filter.getStringency() != null) {
            if (filter.getStringency().equals(OrthologyFilter.Stringency.STRINGENT))
                query += "and ortho.strictFilter = true ";
            if (filter.getStringency().equals(OrthologyFilter.Stringency.MODERATE))
                query += "and ortho.moderateFilter = true ";
        }
        String recordQuery = query + "return distinct g, gh, collect(distinct ortho), " +
                COLLECT_DISTINCT_MATCHED + ", " + COLLECT_DISTINCT_NOT_MATCHED + ", " + COLLECT_DISTINCT_NOT_CALLED + " order by g.symbol, gh.symbol ";
        recordQuery += " SKIP " + (filter.getStart() - 1) + " limit " + filter.getRows();

        Result result = queryForResult(recordQuery);
        Set<OrthologView> orthologViews = new LinkedHashSet<>();
        result.forEach(objectMap -> {
            OrthologView view = new OrthologView();
            Gene gene = (Gene) objectMap.get("g");
            gene.setSpeciesName(SpeciesType.fromTaxonId(taxonOne).getName());
            view.setGene(gene);

            Gene homologGene = (Gene) objectMap.get("gh");
            homologGene.setSpeciesName(SpeciesType.fromTaxonId(taxonTwo).getName());
            view.setHomologGene(homologGene);

            view.setBest(((List<Orthologous>) objectMap.get("collect(distinct ortho)")).get(0).isBestScore());
            view.setBestReverse(((List<Orthologous>) objectMap.get("collect(distinct ortho)")).get(0).isBestRevScore());

            view.setPredictionMethodsMatched(getMethodList(objectMap, COLLECT_DISTINCT_MATCHED));
            view.setPredictionMethodsNotMatched(getMethodList(objectMap, COLLECT_DISTINCT_NOT_MATCHED));
            view.setPredictionMethodsNotCalled(getMethodList(objectMap, COLLECT_DISTINCT_NOT_CALLED));
            orthologViews.add(view);
        });

        String countQuery = query + "return distinct g, gh";
        Result count = queryForResult(countQuery);
        JsonResultResponse<OrthologView> response = new JsonResultResponse<>();
        response.setResults(new ArrayList<>(orthologViews));
        List<Integer> counterSet = new ArrayList<>();
        count.forEach(stringObjectMap -> counterSet.add(1));
        response.setTotal(counterSet.size());
        return response;
    }

    private List<String> getMethodList(Map<String, Object> objectMap, String alias) {
        List<OrthoAlgorithm> algorithms = (List<OrthoAlgorithm>) objectMap.get(alias);

        return algorithms.stream()
                .map(orthoAlgorithm -> orthoAlgorithm.getName())
                .sorted(String::compareTo)
                .collect(Collectors.toList());
    }


}
