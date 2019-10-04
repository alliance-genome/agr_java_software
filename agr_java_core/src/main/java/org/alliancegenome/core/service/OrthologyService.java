package org.alliancegenome.core.service;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.cache.repository.DiseaseCacheRepository;
import org.alliancegenome.cache.repository.ExpressionCacheRepository;
import org.alliancegenome.cache.repository.GeneCacheRepository;
import org.alliancegenome.es.index.site.doclet.OrthologyDoclet;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;
import org.alliancegenome.neo4j.entity.node.OrthologyGeneJoin;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.alliancegenome.neo4j.view.View;

import java.util.*;
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
                        orth.getIsBestScore(),
                        orth.getIsBestRevScore(),
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

    public static JsonResultResponse<OrthologView> getOrthologViewList(Gene gene) {
        return getOrthologViewList(gene, new OrthologyFilter());
    }


    private static JsonResultResponse<OrthologView> getOrthologViewList(Gene gene, OrthologyFilter filter) {
        JsonResultResponse<OrthologView> response = new JsonResultResponse<>();
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
                        //gene.setSpeciesName(ortho.getGene1().getSpecies() == null ? null : ortho.getGene1().getSpecies().getName());
                        view.setGene(gene);
                        //ortho.getGene2().setSpeciesName(ortho.getGene2().getSpecies() == null ? null : ortho.getGene2().getSpecies().getName());
                        view.setHomologGene(ortho.getGene2());
                        view.setBest(ortho.getIsBestScore());
                        view.setBestReverse(ortho.getIsBestRevScore());
                        if (ortho.isStrictFilter()) {
                            view.setStringencyFilter("stringent");
                        } else if (ortho.isModerateFilter()) {
                            view.setStringencyFilter("moderate");
                        }
                        view.setPredictionMethodsMatched(getMatchedMethods(join));
                        view.setPredictionMethodsNotMatched(getNotMatchedMethods(join));
                        view.setPredictionMethodsNotCalled(getNotCalledMethods(join));
                        orthologList.add(view);
                    });
            response.setResults(orthologList);
            response.setTotal(orthologList.size());
            return response;
        }
        return response;
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
        JsonResultResponse<OrthologView> response = OrthologyService.getOrthologViewList(gene, filter);
        return response;
    }

    private static ExpressionCacheRepository expressionCacheRepository = new ExpressionCacheRepository();
    private static DiseaseCacheRepository diseaseCacheRepository = new DiseaseCacheRepository();

    public static JsonResultResponse<OrthologView> getOrthologyMultiGeneJson(List<String> geneIDs, OrthologyFilter filter) {
        GeneCacheRepository repo = new GeneCacheRepository();
        List<OrthologView> orthologViewList = repo.getAllOrthologyGenes(geneIDs);
        List<OrthologView> orthologViewFiltered = orthologViewList;
        List<OrthologView> orthologViewFilteredModerate = orthologViewList;

        System.out.println("Number of genes for orthology: " + geneIDs.size());

        orthologViewList.sort(Comparator.comparing(o -> o.getHomologGene().getSymbol().toLowerCase()));
        orthologViewFiltered = orthologViewList.stream()
                .filter(orthologView -> orthologView.getStringencyFilter().equalsIgnoreCase("Stringent"))
                .skip(filter.getStart() - 1)
                .limit(filter.getRows())
                .collect(Collectors.toList());


         if (filter.getStringency() != null && filter.getStringency().equals(OrthologyFilter.Stringency.MODERATE)){

             orthologViewFilteredModerate = orthologViewList.stream()
                    .filter(orthologView -> orthologView.getStringencyFilter().equalsIgnoreCase(filter.getStringency().name()))
                    .skip(filter.getStart() - 1)
                    .limit(filter.getRows())
                    .collect(Collectors.toList());
             orthologViewFiltered.addAll(orthologViewFilteredModerate);
        }
        if (filter.getStringency() != null && filter.getStringency().equals(OrthologyFilter.Stringency.ALL))
       {

             orthologViewFiltered = orthologViewList.stream()
                    .skip(filter.getStart() - 1)
                    .limit(filter.getRows())
                    .collect(Collectors.toList());
        }
        System.out.println("Number of genes for orthology: " + orthologViewFiltered.size());

        // <geneID, Map<variableName,variableValue>>
        Map<String, Object> map = new HashMap<>();

        orthologViewFiltered.forEach(orthologView -> {
            putGeneInfo(map, orthologView.getGene());
            putGeneInfo(map, orthologView.getHomologGene());
        });
        JsonResultResponse<OrthologView> response = new JsonResultResponse<>();
        response.setResults(orthologViewFiltered);
        response.setTotal(orthologViewList.size());
        response.setSupplementalData(map);
        return response;
    }

    private static void putGeneInfo(Map<String, Object> map, Gene gene) {
        Map<String, Object> data = new HashMap<>();
        data.put("taxonId", gene.getTaxonId());
        data.put("hasExpressionAnnotations", expressionCacheRepository.hasExpression(gene.getPrimaryKey()));
        data.put("hasDiseaseAnnotations", diseaseCacheRepository.hasDiseaseAnnotations(gene.getPrimaryKey()));
        map.put(gene.getPrimaryKey(), data);
    }

    public static JsonResultResponse<OrthologView> getOrthologyGenes(List<String> geneIDList, OrthologyFilter orthoFilter) {
        GeneCacheRepository repo = new GeneCacheRepository();
        List<OrthologView> orthologViewList = repo.getAllOrthologyGenes(geneIDList);
        List<OrthologView> filteredOrthologViewList = orthologViewList;


        if (orthoFilter.getStringency() != null && !orthoFilter.getStringency().equals(OrthologyFilter.Stringency.ALL)) {
             filteredOrthologViewList = orthologViewList.stream()
                    .filter(orthologView -> orthologView.getStringencyFilter().equalsIgnoreCase(orthoFilter.getStringency().name()))
                    .collect(Collectors.toList());
        }


        System.out.println("Number of genes for orthology: " + filteredOrthologViewList.size());

        JsonResultResponse<OrthologView> response = new JsonResultResponse<>();
        response.setResults(filteredOrthologViewList);
        response.setTotal(filteredOrthologViewList.size());
        return response;
    }

    @Setter
    @Getter
    public static class Response extends JsonResultResponse<OrthologView> {

        @JsonView(View.Orthology.class)
        private List<OrthologView> results;
        @JsonView(View.Orthology.class)
        private int total;
        @JsonView(View.Orthology.class)
        private String errorMessage;
    }
}

