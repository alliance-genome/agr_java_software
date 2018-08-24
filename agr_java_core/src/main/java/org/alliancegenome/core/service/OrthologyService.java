package org.alliancegenome.core.service;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.site.doclet.OrthologyDoclet;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;
import org.alliancegenome.neo4j.entity.node.OrthologyGeneJoin;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class OrthologyService {

    public static List<OrthologyDoclet> getOrthologyDoclets(Gene gene) {
        if (gene.getOrthologyGeneJoins().size() > 0) {
            List<OrthologyDoclet> orthologyDoclets = new ArrayList<>();

            HashMap<String, Orthologous> lookup = new HashMap<>();
            for (Orthologous o : gene.getOrthoGenes()) {
                lookup.put(o.getPrimaryKey(), o);
            }

            gene.getOrthologyGeneJoins().stream()
                    .filter(join -> lookup.containsKey(join.getPrimaryKey())).forEach(join -> {

                ArrayList<String> matched = getMatchedMethods(join);
                ArrayList<String> notMatched = getNotMatchedMethods(join);
                ArrayList<String> notCalled = getNotCalledMethods(join);

                Orthologous orth = lookup.get(join.getPrimaryKey());
                OrthologyDoclet doc = new OrthologyDoclet(
                        orth.getPrimaryKey(),
                        orth.isBestScore(),
                        orth.isBestRevScore(),
                        orth.getConfidence(),
                        orth.getGene1().getSpecies() == null ? null : orth.getGene1().getSpecies().getPrimaryKey(),
                        orth.getGene2().getSpecies() == null ? null : orth.getGene2().getSpecies().getPrimaryKey(),
                        orth.getGene1().getSpecies() == null ? null : orth.getGene1().getSpecies().getName(),
                        orth.getGene2().getSpecies() == null ? null : orth.getGene2().getSpecies().getName(),
                        orth.getGene1().getSymbol(),
                        orth.getGene2().getSymbol(),
                        orth.getGene1().getPrimaryKey(),
                        orth.getGene2().getPrimaryKey(),
                        notCalled, matched, notMatched
                );
                orthologyDoclets.add(doc);
            });
            return orthologyDoclets;
        }
        return null;
    }

    private static ArrayList<String> getNotCalledMethods(OrthologyGeneJoin join) {
        ArrayList<String> notCalled = new ArrayList<>();
        if (join.getNotCalled() != null) {
            notCalled.addAll(join.getNotCalled().stream()
                    .sorted()
                    .map(OrthoAlgorithm::getName)
                    .collect(Collectors.toList()));
        }
        return notCalled;
    }

    private static ArrayList<String> getNotMatchedMethods(OrthologyGeneJoin join) {
        ArrayList<String> notMatched = new ArrayList<>();
        if (join.getNotMatched() != null) {
            notMatched.addAll(join.getNotMatched().stream()
                    .sorted()
                    .map(OrthoAlgorithm::getName)
                    .collect(Collectors.toList()));
        }
        return notMatched;
    }

    private static ArrayList<String> getMatchedMethods(OrthologyGeneJoin join) {
        ArrayList<String> matched = new ArrayList<>();
        if (join.getMatched() != null) {
            matched.addAll(join.getMatched().stream()
                    .sorted()
                    .map(OrthoAlgorithm::getName)
                    .collect(Collectors.toList()));
        }
        return matched;
    }

    public static List<OrthologView> getOrthologViewList(Gene gene) {
        return getOrthologViewList(gene, new OrthologyFilter());
    }


    public static List<OrthologView> getOrthologViewList(Gene gene, OrthologyFilter filter) {
        if (gene.getOrthologyGeneJoins().size() > 0) {
            List<OrthologView> orthologList = new ArrayList<>();

            HashMap<String, Orthologous> lookup = new HashMap<>();
            gene.getOrthoGenes()
                    .stream()
                    .filter(orthologous -> orthologous.hasFilter(filter))
                    .filter(join -> filter.getTaxonIDs() == null ||
                            (filter.getTaxonIDs() != null &&
                                    (filter.getTaxonIDs().contains(join.getGene2().getSpecies().getName()) || filter.getTaxonIDs().contains(join.getGene2().getTaxonId()))))
                    .forEach(orthologous ->
                            lookup.put(orthologous.getPrimaryKey(), orthologous)
                    );

            gene.getOrthologyGeneJoins().stream()
                    .filter(join -> lookup.containsKey(join.getPrimaryKey()))
                    .filter(join -> isAllMatchMethods(join, filter))
                    .forEach(join -> {
                        Orthologous ortho = lookup.get(join.getPrimaryKey());
                        OrthologView view = new OrthologView();
                        gene.setSpeciesName(ortho.getGene1().getSpecies() == null ? null : ortho.getGene1().getSpecies().getName());
                        view.setGene(gene);
                        ortho.getGene2().setSpeciesName(ortho.getGene2().getSpecies() == null ? null : ortho.getGene2().getSpecies().getName());
                        view.setHomologGene(ortho.getGene2());
                        view.setBest(ortho.isBestScore());
                        view.setBestReverse(ortho.isBestRevScore());
                        view.setPredictionMethodsMatched(getMatchedMethods(join));
                        view.setPredictionMethodsNotMatched(getNotMatchedMethods(join));
                        view.setPredictionMethodsNotCalled(getNotCalledMethods(join));
                        view.calculateCounts();
                        orthologList.add(view);
                    });
            List<OrthologView> finalOrthologList = new ArrayList<>();
            int index = 1;
            for (OrthologView view : orthologList) {
                if (index >= (filter.getStart() - 1) && index < filter.getLast()) {
                    finalOrthologList.add(view);
                }
                index++;
            }
            return finalOrthologList;
        }
        return new ArrayList<>();
    }

    private static boolean isAllMatchMethods(OrthologyGeneJoin join, OrthologyFilter filter) {
        if (filter.getMethods() == null)
            return true;
        List<String> unmatched = new ArrayList<>();
        filter.getMethods().forEach(method -> {
            if (!getMatchedMethods(join).contains(method))
                unmatched.add(method);
        });
        return unmatched.size() == 0;
    }

    public static JsonResultResponse<OrthologView> getOrthologyJson(Gene gene, OrthologyFilter filter) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.registerModule(new OrthologyModule());
        List<OrthologView> orthologViewList = OrthologyService.getOrthologViewList(gene, filter);
        JsonResultResponse<OrthologView> response = new JsonResultResponse<>();
        response.setResults(orthologViewList);
        // remove pagination filter data to obtain complete set.
        filter.resetPaginationData();
        response.setTotal(OrthologyService.getOrthologViewList(gene, filter).size());
        return response;
    }

    public static JsonResultResponse<OrthologView> getOrthologyMultiGeneJson(Collection<Gene> geneList, OrthologyFilter filter) {
        List<OrthologView> orthologViewList =
                geneList.stream()
                        .flatMap(gene -> OrthologyService.getOrthologViewList(gene, filter).stream())
                        .collect(Collectors.toList());
        JsonResultResponse<OrthologView> response = new JsonResultResponse<>();
        response.setResults(orthologViewList);
        response.setTotal(orthologViewList.size());
        return response;
    }

    @Setter
    @Getter
    public static class Response extends JsonResultResponse<OrthologView> {

        @JsonView(View.OrthologyView.class)
        private List<OrthologView> results;
        @JsonView(View.OrthologyView.class)
        private int total;
        @JsonView(View.OrthologyView.class)
        private String errorMessage;
    }
}

