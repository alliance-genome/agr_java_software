package org.alliancegenome.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                    .filter(join -> filter.getSpecies() == null ||
                            (filter.getSpecies() != null &&
                                    (filter.getSpecies().contains(join.getGene2().getSpecies().getName()) || filter.getSpecies().contains(join.getGene2().getTaxonId()))))
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
            return orthologList;
        }
        return null;
    }

    private static boolean isAllMatchMethods(OrthologyGeneJoin join, OrthologyFilter filter) {
        if(filter.getMethods()== null)
            return true;
        List<String> unmatched = new ArrayList<>();
        filter.getMethods().forEach(method -> {
            if (!getMatchedMethods(join).contains(method))
                unmatched.add(method);
        });
        return unmatched.size() == 0;
    }

    public static String getOrthologyJson(Gene gene, OrthologyFilter filter) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.registerModule(new OrthologyModule());
        return mapper.writerWithView(View.OrthologyView.class).writeValueAsString(OrthologyService.getOrthologViewList(gene, filter));
    }

}
