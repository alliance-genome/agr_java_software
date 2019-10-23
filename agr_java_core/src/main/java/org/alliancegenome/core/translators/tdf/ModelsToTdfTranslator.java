package org.alliancegenome.core.translators.tdf;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.Phenotype;
import org.alliancegenome.neo4j.entity.node.SimpleTerm;

import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class ModelsToTdfTranslator {

    public String getAllPrimaryModelRows(List<PrimaryAnnotatedEntity> annotations) {
        System.out.println(annotations.size());
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Model Name");
        headerJoiner.add("ID");
        headerJoiner.add("Associated Human Disease");
        headerJoiner.add("Associated Phenotype");
        headerJoiner.add("Source");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        annotations.forEach(primaryAnnotatedEntity -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(primaryAnnotatedEntity.getDisplayName());
            joiner.add(primaryAnnotatedEntity.getId());
            // add list of synonyms
            // add list of diseases
            String disease = "";
            if (primaryAnnotatedEntity.getDiseases() != null) {
                StringJoiner diseaseJoiner = new StringJoiner(",");
                primaryAnnotatedEntity.getDiseases().stream().sorted(Comparator.comparing(SimpleTerm::getName)).forEach(diseaseTerm -> diseaseJoiner.add(diseaseTerm.getName()));
                disease = diseaseJoiner.toString();
            }


            joiner.add(disease);
             String phenotypes="";
            if (primaryAnnotatedEntity.getPhenotypes() != null) {


                 phenotypes = primaryAnnotatedEntity.getPhenotypes().stream()
                        .collect(Collectors.joining(","));
            }
            joiner.add(phenotypes);
            joiner.add(primaryAnnotatedEntity.getSource().getName());
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();

    }
}
