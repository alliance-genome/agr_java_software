package org.alliancegenome.core.translators.tdf;

import java.util.*;

import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.config.ConfigHelper;

public class ExpressionToTdfTranslator {

    public String getAllRows(List<ExpressionDetail> annotations, boolean isMultipleGenes) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        if (isMultipleGenes) {
            headerJoiner.add("Species");
            headerJoiner.add("Gene Symbol");
            headerJoiner.add("Gene ID");
        }
        headerJoiner.add("Location");
        headerJoiner.add("Stage");
        headerJoiner.add("Assay");
        headerJoiner.add("Source");
        headerJoiner.add("Reference");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        annotations.forEach(expressionDetail -> {
            StringJoiner joiner = new StringJoiner("\t");
            if (isMultipleGenes) {
                joiner.add(expressionDetail.getGene().getSpecies().getName());
                joiner.add(expressionDetail.getGene().getSymbol());
                joiner.add(expressionDetail.getGene().getPrimaryKey());
            }
            joiner.add(expressionDetail.getTermName());
            joiner.add(expressionDetail.getStage().getName());
            joiner.add(expressionDetail.getAssay().getDisplaySynonym());
            String crossRefs = "";
            if (expressionDetail.getCrossReferences() != null) {
                StringJoiner crossRefJoiner = new StringJoiner(",");
                expressionDetail.getCrossReferences().forEach(crossReference -> crossRefJoiner.add(crossReference.getDisplayName()));
                crossRefs = crossRefJoiner.toString();
            }
            joiner.add(crossRefs);
            // add list of publications
            String publications = "";
            if (expressionDetail.getPublications() != null) {
                StringJoiner pubJoiner = new StringJoiner(",");
                expressionDetail.getPublications().forEach(publication -> pubJoiner.add(publication.getPubId()));
                publications = pubJoiner.toString();
            }
            joiner.add(publications);
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();

    }
}
