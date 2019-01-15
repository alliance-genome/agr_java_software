package org.alliancegenome.core.translators.tdf;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;

import java.util.List;
import java.util.StringJoiner;

public class PhenotypeAnnotationToTdfTranslator {

    public String getAllRows(List<PhenotypeAnnotation> annotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Phenotype");
        headerJoiner.add("Genetic Entity ID");
        headerJoiner.add("Genetic Entity Symbol");
        headerJoiner.add("Genetic Entity Type");
        headerJoiner.add("References");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        annotations.forEach(annotation -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(annotation.getPhenotype());
            if (annotation.getAllele() != null) {
                joiner.add(annotation.getAllele().getPrimaryKey());
                joiner.add(annotation.getAllele().getSymbol());
                joiner.add("allele");
            } else {
                joiner.add("");
                joiner.add("");
                joiner.add("");
            }
            // publication list
            StringJoiner pubJoiner = new StringJoiner(",");
            annotation.getPublications().forEach(publication -> pubJoiner.add(publication.getPubId()));
            joiner.add(pubJoiner.toString());
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();

    }
}
