package org.alliancegenome.core.translators.tdf;

import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.node.*;

public class GeneToTdfTranslator {

    public String getAllRowsSpecies(List<Gene> genes) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("TaxonID");
        headerJoiner.add("Gene ID");
        headerJoiner.add("Gene Symbol");
        headerJoiner.add("NCBI ID");
        headerJoiner.add("Ensembl ID");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        genes.forEach(gene -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(gene.getTaxonId());
            joiner.add(gene.getPrimaryKey());
            joiner.add(gene.getSymbol());
            String ncbiIDs = getCrossReferences(gene, "NCBI");
            joiner.add(ncbiIDs == null ? "" : ncbiIDs);
            String ensemblIDs = getCrossReferences(gene, "ENSEMBL");
            joiner.add(ensemblIDs == null ? "" : ensemblIDs);
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();

    }

    public String getAllRowsEnsembl(List<Gene> genes) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("TaxonID");
        headerJoiner.add("Ensembl ID");
        headerJoiner.add("Gene ID");
        headerJoiner.add("Gene Symbol");
        headerJoiner.add("NCBI ID");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        Map<List<CrossReference>, List<Gene>> groupByEnsemblId = genes.stream()
                .collect(groupingBy(GeneticEntity::getCrossReferences));

        Map<String, Set<Gene>> geneMap = new HashMap<>();

        groupByEnsemblId.forEach((key, value) -> {
            Set<String> ensIDs = key.stream()
                    .filter(crossReference -> crossReference.getName().startsWith("ENSEMBL"))
                    .map(CrossReference::getLocalId)
                    .collect(Collectors.toSet());
            ensIDs.forEach(id -> {
                if (geneMap.get(id) != null) {
                    geneMap.get(id).addAll(value);
                } else {
                    geneMap.put(id, new HashSet<>(value));
                }
            });
        });

        geneMap.forEach((id, geneSet) -> {
            StringJoiner joiner = new StringJoiner("\t");
            // use first one
            joiner.add(geneSet.iterator().next().getTaxonId());
            joiner.add(id);
            String ids = geneSet.stream().map(GeneticEntity::getPrimaryKey).collect(Collectors.joining(","));
            joiner.add(ids);
            String symbols = geneSet.stream().map(GeneticEntity::getSymbol).collect(Collectors.joining(","));
            joiner.add(symbols);

            String ncbis = geneSet.stream()
                    .map(GeneticEntity::getCrossReferences)
                    .flatMap(Collection::stream)
                    .filter(s -> s.getName().startsWith("NCBI"))
                    .map(CrossReference::getLocalId)
                    .distinct()
                    .collect(joining(","));
            joiner.add(ncbis);
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();
    }

    public String getCrossReferences(Gene gene, String ids) {
        return gene.getCrossReferences().stream()
                .filter(reference -> reference.getName().startsWith(ids))
                .map(CrossReference::getLocalId)
                .distinct()
                .collect(Collectors.joining(","));
    }

    public String getAllRowsNcbi(List<Gene> genes) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("TaxonID");
        headerJoiner.add("Ncbi ID");
        headerJoiner.add("Gene ID");
        headerJoiner.add("Gene Symbol");
        headerJoiner.add("Ensembl ID");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        Map<List<CrossReference>, List<Gene>> groupByEnsemblId = genes.stream()
                .collect(groupingBy(GeneticEntity::getCrossReferences));

        Map<String, Set<Gene>> geneMap = new HashMap<>();

        groupByEnsemblId.forEach((key, value) -> {
            Set<String> ensIDs = key.stream()
                    .filter(crossReference -> crossReference.getName().startsWith("NCBI"))
                    .map(CrossReference::getLocalId)
                    .collect(Collectors.toSet());
            ensIDs.forEach(id -> {
                if (geneMap.get(id) != null) {
                    geneMap.get(id).addAll(value);
                } else {
                    geneMap.put(id, new HashSet<>(value));
                }
            });
        });

        geneMap.forEach((id, geneSet) -> {
            StringJoiner joiner = new StringJoiner("\t");
            // use first one
            joiner.add(geneSet.iterator().next().getTaxonId());
            joiner.add(id);
            String ids = geneSet.stream().map(GeneticEntity::getPrimaryKey).collect(Collectors.joining(","));
            joiner.add(ids);
            String symbols = geneSet.stream().map(GeneticEntity::getSymbol).collect(Collectors.joining(","));
            joiner.add(symbols);

            String ncbis = geneSet.stream()
                    .map(GeneticEntity::getCrossReferences)
                    .flatMap(Collection::stream)
                    .filter(s -> s.getName().startsWith("ENSEMBL"))
                    .map(CrossReference::getLocalId)
                    .distinct()
                    .collect(joining(","));
            joiner.add(ncbis);
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();
    }
}
