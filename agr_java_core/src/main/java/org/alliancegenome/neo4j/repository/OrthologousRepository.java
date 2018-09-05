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
import java.util.stream.StreamSupport;

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

        StringJoiner sj = new StringJoiner(",");
        if (filter.hasMethods()) {
            filter.getMethods().forEach(method -> sj.add("'" + method + "'"));
        }
        String query = " MATCH p1=(g:Gene)-[ortho:ORTHOLOGOUS]->(gh:Gene), ";
        query += "p4=(g)--(s:OrthologyGeneJoin)--(gh:Gene), ";
        if (filter.hasMethods()) {
            query += "p5=(s)-[:MATCHED]-(matched:OrthoAlgorithm {name:" + sj.toString() + "}), ";
        } else {
            query += "p5=(s)-[:MATCHED]-(matched:OrthoAlgorithm), ";
        }
        query += "p6=(s)-[:NOT_MATCHED]-(notMatched:OrthoAlgorithm), ";
        query += "p7=(s)-[:NOT_CALLED]-(notCalled:OrthoAlgorithm) ";
        query += " where g.taxonId = '" + taxonOne + "'";
        if (taxonTwo != null)
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
            if (taxonTwo != null)
                homologGene.setSpeciesName(SpeciesType.fromTaxonId(taxonTwo).getName());
            else
                homologGene.setSpeciesName(SpeciesType.fromTaxonId(homologGene.getTaxonId()).getName());
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


    public List<OrthoAlgorithm> getAllMethods() {
        String query = " MATCH (algorithm:OrthoAlgorithm) return distinct algorithm order by algorithm.name ";
        Iterable<OrthoAlgorithm> algorithms = neo4jSession.query(OrthoAlgorithm.class, query, new HashMap<>());
        return StreamSupport.stream(algorithms.spliterator(), false)
                .collect(Collectors.toList());
    }
}
