package org.alliancegenome.cache.repository;

import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.service.FilterService;
import org.alliancegenome.cache.repository.helper.*;
import org.alliancegenome.es.index.site.doclet.OrthologyDoclet;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;
import org.alliancegenome.neo4j.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.*;

import lombok.*;

@RequestScoped
public class OrthologyCacheRepository {

    @Inject
    private GeneCacheRepository repo;
    
    @Inject
    private ExpressionCacheRepository expressionCacheRepository;
    
    @Inject
    private DiseaseCacheRepository diseaseCacheRepository;

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
        JsonResultResponse<OrthologView> response = OrthologyCacheRepository.getOrthologViewList(gene, filter);
        return response;
    }

    public JsonResultResponse<OrthologView> getOrthologyMultiGeneJson(List<String> geneIDs, Pagination pagination) {
        long start = System.currentTimeMillis();
        List<OrthologView> orthologViewList = repo.getAllOrthologyGenes(geneIDs);
        //filtering
        FilterService<OrthologView> filterService = new FilterService<>(new OrthologyFiltering());
        List<OrthologView> orthologViewFiltered = filterService.filterAnnotations(orthologViewList, pagination.getFieldFilterValueMap());

        List<OrthologView> paginatedViewFiltered = orthologViewFiltered.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit()).sorted(Comparator.comparing(o -> o.getHomologGene().getSpecies().getPhylogeneticOrder()))
                .collect(Collectors.toList());



        // <geneID, Map<variableName,variableValue>>
        Map<String, Object> map = new HashMap<>();

        paginatedViewFiltered.forEach(orthologView -> {
            putGeneInfo(map, orthologView.getGene());
            putGeneInfo(map, orthologView.getHomologGene());
        });
        JsonResultResponse<OrthologView> response = new JsonResultResponse<>();
        response.setResults(paginatedViewFiltered);
        response.setTotal(orthologViewFiltered.size());
        response.setSupplementalData(map);
        response.calculateRequestDuration(start);
        return response;
    }

    private void putGeneInfo(Map<String, Object> map, Gene gene) {
        Map<String, Object> data = new HashMap<>();
        data.put("taxonId", gene.getTaxonId());
        data.put("hasExpressionAnnotations", expressionCacheRepository.hasExpression(gene.getPrimaryKey()));
        data.put("hasDiseaseAnnotations", diseaseCacheRepository.hasDiseaseAnnotations(gene.getPrimaryKey()));
        map.put(gene.getPrimaryKey(), data);
    }

    public JsonResultResponse<OrthologView> getOrthologyGenes(List<String> geneIDList, OrthologyFilter orthoFilter) {
        List<OrthologView> orthologViewList = repo.getAllOrthologyGenes(geneIDList);
        List<OrthologView> filteredOrthologViewList = orthologViewList;


        if (orthoFilter.getStringency() != null && !orthoFilter.getStringency().equals(OrthologyFilter.Stringency.ALL)) {
            filteredOrthologViewList = orthologViewList.stream()
                    .filter(orthologView -> orthologView.getStringencyFilter().equalsIgnoreCase(orthoFilter.getStringency().name()))
                    .collect(Collectors.toList());
        }

        JsonResultResponse<OrthologView> response = new JsonResultResponse<>();
        response.setResults(filteredOrthologViewList);
        response.setTotal(filteredOrthologViewList.size());
        return response;
    }

    public JsonResultResponse<OrthologView> getOrthologyByTwoSpecies(String taxonIDOne, String taxonIDTwo, Pagination pagination) {

        final String taxonOne = SpeciesType.getTaxonId(taxonIDOne);
        final String taxonTwo = SpeciesType.getTaxonId(taxonIDTwo);

        List<OrthologView> orthologViewList = repo.getOrthologyBySpeciesSpecies(taxonOne, taxonTwo);
        JsonResultResponse<OrthologView> response = new JsonResultResponse<>();

        //filtering
        FilterService<OrthologView> filterService = new FilterService<>(new OrthologyFiltering());
        List<OrthologView> filteredOrthologyList = filterService.filterAnnotations(orthologViewList, pagination.getFieldFilterValueMap());
        response.setTotal(filteredOrthologyList.size());

        // sorting and pagination
        response.setResults(filterService.getSortedAndPaginatedAnnotations(pagination, filteredOrthologyList, new OrthologySorting()));
        return response;
    }

    public JsonResultResponse<OrthologView> getOrthologyBySpecies(String taxonIDOne, Pagination pagination) {

        final String taxonOne = SpeciesType.getTaxonId(taxonIDOne);

        List<OrthologView> orthologViewList = repo.getOrthologyBySpecies(List.of(taxonOne));
        JsonResultResponse<OrthologView> response = new JsonResultResponse<>();

        //filtering
        FilterService<OrthologView> filterService = new FilterService<>(new OrthologyFiltering());
        List<OrthologView> filteredOrthologyList = filterService.filterAnnotations(orthologViewList, pagination.getFieldFilterValueMap());
        response.setTotal(filteredOrthologyList.size());

        // sorting and pagination
        response.setResults(filterService.getSortedAndPaginatedAnnotations(pagination, filteredOrthologyList, new OrthologySorting()));
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

