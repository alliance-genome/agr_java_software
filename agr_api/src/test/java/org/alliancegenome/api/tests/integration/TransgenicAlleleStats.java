package org.alliancegenome.api.tests.integration;

import static java.util.Comparator.*;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections.CollectionUtils;

import lombok.*;


public class TransgenicAlleleStats {


    private static AlleleService alleleService = new AlleleService();

    public static void main(String[] args) {
        GeneRepository repository = new GeneRepository();
        Arrays.stream(SpeciesType.values())
                .filter(speciesType -> speciesType.equals(SpeciesType.FLY))
                .forEach(speciesType -> {
                    List<Gene> genes = repository.getAllGenes(List.of(speciesType.getTaxonID()));
                    System.out.println("Organism: " + speciesType.getAbbreviation());
                    System.out.println("Total number of genes: " + genes.size());

                    Map<String, Gene> geneMap = genes.stream().collect(toMap(Gene::getPrimaryKey, gene -> gene));
                    Map<String, JsonResultResponse<Allele>> alleleMap = new HashMap<>();
                    for (int index = 0; index < genes.size(); index++) {
                        Gene gene = genes.get(index);
                        String geneID = gene.getPrimaryKey();
                        JsonResultResponse<Allele> response = alleleService.getTransgenicAlleles(geneID, new Pagination());
                        alleleMap.put(geneID, response);
                        if (index % 1000 == 0 && index % 5000 != 0 && index % 10000 != 0)
                            System.out.print(".");
                        if (index % 5000 == 0 && index % 10000 != 0)
                            System.out.print(":");
                        if (index % 10000 == 0)
                            System.out.print(index / 1000);
                    }
                    System.out.println("");

                    getOrganismTransgeneAlleles(geneMap, alleleMap);
                    System.out.println("");
                    getTransgeneAllelesSpecies(geneMap, alleleMap, speciesType);
                    System.out.println("");
                    getTransgeneAllelesHasAnnotation(geneMap, alleleMap, Allele::hasDisease);
                    System.out.println("");
                    getTransgeneAllelesHasAnnotation(geneMap, alleleMap, Allele::hasPhenotype);
                    System.out.println("");
                    getTransgeneAllelesConstructs(alleleMap);
                    System.out.println("");
                    getTransgeneAllelesExpressedGenes(alleleMap);
                    System.out.println("");
                    getTransgeneAllelesRegulatedGenes(alleleMap);
                    System.out.println("");
                    getTransgeneAllelesTargetGenes(alleleMap);
                });
        System.exit(0);
    }

    public static void getOrganismTransgeneAlleles(Map<String, Gene> geneMap, Map<String, JsonResultResponse<Allele>> alleleMap) {

        Map<String, JsonResultResponse<Allele>> sorted = alleleMap.entrySet().stream()
                .filter(entry -> entry.getValue().getTotal() > 0)
                .sorted(comparingInt(entry -> -entry.getValue().getTotal()))
                .collect(toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));

        StringBuilder totalOutput = new StringBuilder();
        sorted.forEach((geneID, alleleList) -> {
            String output = geneID;
            output += "\t";
            output += geneMap.get(geneID).getSymbol();
            output += "\t";
            output += alleleList.getTotal();
            output += System.getProperty("line.separator");
            System.out.print(output);
            totalOutput.append(output);
        });
        writeToFile(totalOutput, "allele.txt");
        System.out.println("End Gene list");
    }

    private static void getTransgeneAllelesSpecies(Map<String, Gene> geneMap, Map<String, JsonResultResponse<Allele>> alleleMap, SpeciesType type) {

        Map<String, Set<Species>> species = new HashMap<>();
        alleleMap.forEach((geneID, response) -> {
            Set<Species> collect = response.getResults().stream()
                    .map(GeneticEntity::getSpecies)
                    .collect(Collectors.toSet());
            species.put(geneID, collect);
        });

        Map<String, Set<Species>> sortedSpecies = species.entrySet().stream()
                .filter(entry -> ((entry.getValue().size() > 1) ||
                        (entry.getValue().size() == 1 && !entry.getValue().iterator().next().getType().equals(type))))
                .sorted(comparingInt(entry -> -entry.getValue().size()))
                .collect(toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));

        StringBuilder totalOutput = new StringBuilder();
        sortedSpecies.forEach((geneID, alleleList) -> {
            String output = geneID;
            output += "\t";
            output += geneMap.get(geneID).getSymbol();
            output += "\t";
            output += alleleList.stream().map(Species::getName).collect(Collectors.joining(", "));
            output += System.getProperty("line.separator");
            System.out.print(output);
            totalOutput.append(output);
        });
        writeToFile(totalOutput, "allele-species.txt");
        System.out.println("End Species list");
    }

    private static void getTransgeneAllelesHasAnnotation(Map<String, Gene> geneMap,
                                                         Map<String, JsonResultResponse<Allele>> alleleMap,
                                                         Function<Allele, Boolean> booleanFunction) {

        Map<String, List<Boolean>> disease = new HashMap<>();
        alleleMap.forEach((geneID, response) -> {
            List<Boolean> collect = response.getResults().stream()
                    // fish out the 'true' records
                    .filter(booleanFunction::apply)
                    .map(booleanFunction)
                    .collect(Collectors.toList());
            disease.put(geneID, collect);
        });

        Map<String, List<Boolean>> sortedDisease = disease.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 0)
                .sorted(comparingInt(entry -> -entry.getValue().size()))
                .collect(toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));

        StringBuilder totalOutput = new StringBuilder();
        sortedDisease.forEach((geneID, diseaseList) -> {
            String output = geneID;
            output += "\t";
            output += geneMap.get(geneID).getSymbol();
            output += "\t";
            output += diseaseList.size();
            output += System.getProperty("line.separator");
            System.out.print(output);
            totalOutput.append(output);
        });
        writeToFile(totalOutput, "allele-disease.txt");
        System.out.println("End Disease list " + booleanFunction.toString());
    }

    public static void writeToFile(StringBuilder totalOutput, String fileName) {
        try {
            Files.write(
                    Paths.get(fileName),
                    totalOutput.toString().getBytes(),
                    StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getTransgeneAllelesConstructs(Map<String, JsonResultResponse<Allele>> alleleMap) {

        List<Allele> alleles = alleleMap.values().stream()
                .map(JsonResultResponse::getResults)
                .flatMap(Collection::stream)
                .distinct()
                .filter(allele -> CollectionUtils.isNotEmpty(allele.getConstructs()))
                .sorted(comparing(allele -> -allele.getConstructs().size()))
                .collect(Collectors.toList());

        StringBuilder totalOutput = new StringBuilder();
        alleles.forEach(allele -> {
            String output = allele.getPrimaryKey();
            output += "\t";
            output += allele.getSymbolText();
            output += "\t";
            output += allele.getConstructs().size();
            output += System.getProperty("line.separator");
            System.out.print(output);
            totalOutput.append(output);
        });
        writeToFile(totalOutput, "allele-constructs.txt");
        System.out.println("End Allele / Construct list");
    }

    public static void getTransgeneAllelesExpressedGenes(Map<String, JsonResultResponse<Allele>> alleleMap) {

        List<Allele> alleles = alleleMap.values().stream()
                .map(JsonResultResponse::getResults)
                .flatMap(Collection::stream)
                .distinct()
                .filter(allele -> CollectionUtils.isNotEmpty(allele.getConstructs()))
                .filter(allele -> CollectionUtils.isNotEmpty(allele.getConstructs().stream()
                        .filter(construct -> CollectionUtils.isNotEmpty(construct.getExpressedGenes()))
                        .map(Construct::getExpressedGenes)
                        .collect(Collectors.toList())))
                .sorted(comparing(allele ->
                        -allele.getConstructs().stream().mapToInt(construct -> construct.getExpressedGenes().size()).sum()
                ))
                .collect(Collectors.toList());

        StringBuilder totalOutput = new StringBuilder();
        alleles.forEach(allele -> {
            String output = allele.getPrimaryKey();
            output += "\t";
            output += allele.getSymbolText();
            output += "\t";
            output += allele.getConstructs().stream().mapToInt(construct -> construct.getExpressedGenes().size()).sum();
            output += System.getProperty("line.separator");
            System.out.print(output);
            totalOutput.append(output);
        });
        writeToFile(totalOutput, "allele-constructs-expressed-genes.txt");
        System.out.println("End Allele / Construct / Expressed Gene list");
        System.out.println();
    }

    public static void getTransgeneAllelesRegulatedGenes(Map<String, JsonResultResponse<Allele>> alleleMap) {

        List<Allele> alleles = alleleMap.values().stream()
                .map(JsonResultResponse::getResults)
                .flatMap(Collection::stream)
                .distinct()
                .filter(allele -> CollectionUtils.isNotEmpty(allele.getConstructs()))
                .filter(allele -> CollectionUtils.isNotEmpty(allele.getConstructs().stream()
                        .filter(construct -> CollectionUtils.isNotEmpty(construct.getRegulatedByGenes()))
                        .map(Construct::getRegulatedByGenes)
                        .collect(Collectors.toList())))
                .sorted(comparing(allele ->
                        -allele.getConstructs().stream().mapToInt(construct -> construct.getRegulatedByGenes().size()).sum()
                ))
                .collect(Collectors.toList());

        StringBuilder totalOutput = new StringBuilder();
        alleles.forEach(allele -> {
            String output = allele.getPrimaryKey();
            output += "\t";
            output += allele.getSymbolText();
            output += "\t";
            output += allele.getConstructs().stream().mapToInt(construct -> construct.getRegulatedByGenes().size()).sum();
            output += System.getProperty("line.separator");
            System.out.print(output);
            totalOutput.append(output);
        });
        writeToFile(totalOutput, "allele-constructs-regulated-genes.txt");
        System.out.println("End Allele / Construct / Regulated Gene list");
        System.out.println();
    }

    public static void getTransgeneAllelesTargetGenes(Map<String, JsonResultResponse<Allele>> alleleMap) {

        List<Allele> alleles = alleleMap.values().stream()
                .map(JsonResultResponse::getResults)
                .flatMap(Collection::stream)
                .distinct()
                .filter(allele -> CollectionUtils.isNotEmpty(allele.getConstructs()))
                .filter(allele -> CollectionUtils.isNotEmpty(allele.getConstructs().stream()
                        .filter(construct -> CollectionUtils.isNotEmpty(construct.getTargetGenes()))
                        .map(Construct::getTargetGenes)
                        .collect(Collectors.toList())))
                .sorted(comparing(allele ->
                        -allele.getConstructs().stream().mapToInt(construct -> construct.getTargetGenes().size()).sum()
                ))
                .collect(Collectors.toList());

        StringBuilder totalOutput = new StringBuilder();
        alleles.forEach(allele -> {
            String output = allele.getPrimaryKey();
            output += "\t";
            output += allele.getSymbolText();
            output += "\t";
            output += allele.getConstructs().stream().mapToInt(construct -> construct.getTargetGenes().size()).sum();
            output += System.getProperty("line.separator");
            System.out.print(output);
            totalOutput.append(output);
        });
        writeToFile(totalOutput, "allele-constructs-target-genes.txt");
        System.out.println("End Allele / Construct / Target Gene list");
        System.out.println();
    }

    @Setter
    @Getter
    class TransgeneAlleleStat {
        private int expressed;
    }
}
