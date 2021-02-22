package org.alliancegenome.neo4j.repository;

import java.util.*;
import java.util.stream.*;

import org.alliancegenome.cache.repository.helper.FilterFunction;
import org.alliancegenome.cache.repository.helper.InteractionAnnotationFiltering;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.BaseFilter;

public class InteractionRepository extends Neo4jRepository<InteractionGeneJoin> {

    public InteractionRepository() {
        super(InteractionGeneJoin.class);
    }

    private String interactionsQuery = "MATCH p1=(g1:Gene)--(igj:InteractionGeneJoin)--(g2:Gene), p2=(igj:InteractionGeneJoin)--(s) where g1.primaryKey = {primaryKey}"
            + " OPTIONAL MATCH p3=(g1:Gene)-->(s1:Species) "
            + " OPTIONAL MATCH p4=(g2:Gene)-->(s2:Species) ";

    public List<InteractionGeneJoin> getInteractions(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();
        List<InteractionGeneJoin> results = new ArrayList<>();

        map.put("primaryKey", primaryKey);
        String query = interactionsQuery + " RETURN p1, p2, p3, p4";
        //String query = "MATCH p1=(g:Gene)-[iw:INTERACTS_WITH]->(g2:Gene), p2=(g:Gene)-->(igj:InteractionGeneJoin)--(s) where g.primaryKey = {primaryKey} and iw.uuid = igj.primaryKey RETURN p1, p2";

        Iterable<InteractionGeneJoin> joins = query(query, map);
        for (InteractionGeneJoin join : joins) {
            results.add(join);
        }
        return results;
    }
    //get Interactions by gene as primaryKey, and filter out result by Pagination, add this function for test filter here.
    public List<InteractionGeneJoin> getInteractions(String primaryKey, Pagination pagination) {
        HashMap<String, String> map = new HashMap<>();
        List<InteractionGeneJoin> results = new ArrayList<>();

        map.put("primaryKey", primaryKey);
        String query = interactionsQuery + " RETURN p1, p2, p3, p4";
        //String query = "MATCH p1=(g:Gene)-[iw:INTERACTS_WITH]->(g2:Gene), p2=(g:Gene)-->(igj:InteractionGeneJoin)--(s) where g.primaryKey = {primaryKey} and iw.uuid = igj.primaryKey RETURN p1, p2";

        Iterable<InteractionGeneJoin> joins = query(query, map);
        for (InteractionGeneJoin join : joins) {
            results.add(join);
        }
        //filtering
        List<InteractionGeneJoin> filteredInteractionAnnotationList = filterInteractionAnnotations(results, pagination.getFieldFilterValueMap(), true);

        return filteredInteractionAnnotationList;
    }
    public long getInteractionCount(String geneID) {
        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("primaryKey", geneID);

        String cypher = interactionsQuery + " RETURN count(distinct igj) as total";
        return (Long) queryForResult(cypher, bindingValueMap).iterator().next().get("total");
    }

    public long getInteractorCount(String geneID) {
        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("primaryKey", geneID);

        String cypher = interactionsQuery + " RETURN count(distinct g2) as total";
        return (Long) queryForResult(cypher, bindingValueMap).iterator().next().get("total");
    }

    public List<InteractionGeneJoin> getAllInteractions() {
        //String allInteractionsQuery = "MATCH p=(igj:InteractionGeneJoin)--(t) ";
        String allInteractionsQuery = "MATCH p1=(g1:Gene)--(igj:InteractionGeneJoin)--(g2:Gene), p2=(igj:InteractionGeneJoin)--(s) "
                + " OPTIONAL MATCH p3=(g1:Gene)-->(s1:Species) "
                + " OPTIONAL MATCH p4=(g2:Gene)-->(s2:Species) ";
        //allInteractionsQuery += " where g1.primaryKey = 'MGI:109583' ";
        //allInteractionsQuery += " where g1.primaryKey = 'MGI:103170' ";
        //allInteractionsQuery += " where g1.primaryKey = 'FB:FBgn0029891' ";
        //String query = allInteractionsQuery + " RETURN p ";
        //String query = allInteractionsQuery + " RETURN p1, p2, p3, p4";
        //String query = "MATCH p1=(g1:Gene)--(igj:InteractionGeneJoin)--(g2:Gene), p2=(igj:InteractionGeneJoin)--(s) where g1.primaryKey = 'WB:WBGene00000390'"
        String query = "MATCH p1=(g1:Gene)--(igj:InteractionGeneJoin)--(g2:Gene), p2=(igj:InteractionGeneJoin)--(s) "
                + " OPTIONAL MATCH p3=(g1:Gene)-->(s1:Species) "
                + " OPTIONAL MATCH p4=(g2:Gene)-->(s2:Species) ";
        query +=  " RETURN p1, p2, p3, p4";
        Iterable<InteractionGeneJoin> joins = query(query, new HashMap<>());
        return StreamSupport.stream(joins.spliterator(), false)
                .peek(this::populateSpeciesInfo)
                .collect(Collectors.toList());
    }

    private void populateSpeciesInfo(InteractionGeneJoin join) {
        Gene geneA = join.getGeneA();
        geneA.setSpecies(Species.getSpeciesFromTaxonId(geneA.getTaxonId()));
        Gene geneB = join.getGeneB();
        geneB.setSpecies(Species.getSpeciesFromTaxonId(geneB.getTaxonId()));
    }
    private List<InteractionGeneJoin> filterInteractionAnnotations(List<InteractionGeneJoin> interactionAnnotationList, BaseFilter fieldFilterValueMap, boolean useGeneAasSource) {
        if (interactionAnnotationList == null)
            return null;
        if (fieldFilterValueMap == null)
            return interactionAnnotationList;
        return interactionAnnotationList.stream()
                .filter(annotation -> containsFilterValue(annotation, fieldFilterValueMap, useGeneAasSource))
                .collect(Collectors.toList());
    }

    private boolean containsFilterValue(InteractionGeneJoin annotation, BaseFilter fieldFilterValueMap, boolean useGeneAasSource) {
        // remove entries with null values.
        fieldFilterValueMap.values().removeIf(Objects::isNull);
        Set<Boolean> filterResults = fieldFilterValueMap.entrySet().stream()
                .map((entry) -> {
                    FilterFunction<InteractionGeneJoin, String> filterFunction = InteractionAnnotationFiltering.filterFieldMap.get(entry.getKey());
                    if (filterFunction == null)
                        return null;
                    return filterFunction.containsFilterValue(annotation, entry.getValue());
                })
                .collect(Collectors.toSet());

        return !filterResults.contains(false);
    }

}
