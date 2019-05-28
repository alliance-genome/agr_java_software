package org.alliancegenome.core.translators.tdf;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

import java.util.List;
import java.util.StringJoiner;

public class InteractionToTdfTranslator {

    public String getAllRows(List<InteractionGeneJoin> annotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Molecule Type");
        headerJoiner.add("Interactor Gene Symbol");
        headerJoiner.add("Interactor Gene ID");
        headerJoiner.add("Species");
        headerJoiner.add("Interactor Molecule Type");
        headerJoiner.add("Detection Methods");
        headerJoiner.add("Source");
        headerJoiner.add("Reference");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        annotations.forEach(interactionGeneJoin -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(interactionGeneJoin.getInteractorAType().getLabel());
            joiner.add(interactionGeneJoin.getGeneB().getSymbol());
            joiner.add(interactionGeneJoin.getGeneB().getPrimaryKey());
            joiner.add(interactionGeneJoin.getGeneB().getSpecies().getName());
            joiner.add(interactionGeneJoin.getInteractorBType().getLabel());
            // add list of detectionMethods
            String detectionMethods = "";
            if (interactionGeneJoin.getDetectionsMethods() != null) {
                StringJoiner methodJoiner = new StringJoiner(",");
                interactionGeneJoin.getDetectionsMethods().forEach(method -> methodJoiner.add(method.getLabel()));
                detectionMethods = methodJoiner.toString();
            }
            joiner.add(detectionMethods);
            joiner.add(interactionGeneJoin.getSourceDatabase().getLabel());
            joiner.add(interactionGeneJoin.getPublication().getPubId());
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();

    }
}
