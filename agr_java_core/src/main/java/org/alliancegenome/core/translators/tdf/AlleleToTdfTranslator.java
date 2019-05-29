package org.alliancegenome.core.translators.tdf;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.SimpleTerm;

import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

public class AlleleToTdfTranslator {

    public String getAllRows(List<Allele> annotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Symbol");
        headerJoiner.add("ID");
        headerJoiner.add("Synonyms");
        headerJoiner.add("Source");
        headerJoiner.add("Associated Human Disease");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        annotations.forEach(allele -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(allele.getSymbolText());
            joiner.add(allele.getPrimaryKey());
            // add list of synonyms
            String synonyms = "";
            if (allele.getSynonyms() != null) {
                StringJoiner synonymJoiner = new StringJoiner(",");
                allele.getSynonyms().forEach(synonym -> synonymJoiner.add(synonym.getName()));
                synonyms = synonymJoiner.toString();
            }
            joiner.add(synonyms);
            joiner.add("");
            // add list of diseases
            String disease = "";
            if (allele.getDiseases() != null) {
                StringJoiner diseaseJoiner = new StringJoiner(",");
                allele.getDiseases().stream().sorted(Comparator.comparing(SimpleTerm::getName)).forEach(diseaseTerm -> diseaseJoiner.add(diseaseTerm.getName()));
                disease = diseaseJoiner.toString();
            }
            joiner.add(disease);
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();

    }
}
