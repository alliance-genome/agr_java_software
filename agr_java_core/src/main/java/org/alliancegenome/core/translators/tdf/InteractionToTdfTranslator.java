package org.alliancegenome.core.translators.tdf;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

import java.util.List;
import java.util.StringJoiner;

public class InteractionToTdfTranslator {

    public String getAllRows(List<InteractionGeneJoin> annotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");

        headerJoiner.add("Focus gene molecule type ID");
        headerJoiner.add("Focus gene molecule type");
        headerJoiner.add("Focus gene experimental role ID");
        headerJoiner.add("Focus gene experimental role");
        
        headerJoiner.add("Interactor gene ID");
        headerJoiner.add("Interactor gene");
        headerJoiner.add("Interactor species ID");
        headerJoiner.add("Interactor species");
        
        headerJoiner.add("Interactor molecule type ID");
        headerJoiner.add("Interactor molecule type");
        headerJoiner.add("Interactor experimental role ID");
        headerJoiner.add("Interactor experimental role");
        
        headerJoiner.add("Interaction type ID");
        headerJoiner.add("Interaction type");
        
        headerJoiner.add("Detection method IDs");
        headerJoiner.add("Detection methods");
        
        headerJoiner.add("Source ID");
        headerJoiner.add("Source DB ID");
        headerJoiner.add("Source DB");
        headerJoiner.add("Aggregation DB ID");
        headerJoiner.add("Aggregation DB");
        headerJoiner.add("Reference");

        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        annotations.forEach(interactionGeneJoin -> {
            StringJoiner joiner = new StringJoiner("\t");

            joiner.add(interactionGeneJoin.getInteractorAType().getPrimaryKey());
            joiner.add(interactionGeneJoin.getInteractorAType().getLabel());
            joiner.add(interactionGeneJoin.getInteractorARole().getPrimaryKey());
            joiner.add(interactionGeneJoin.getInteractorARole().getLabel());
            
            joiner.add(interactionGeneJoin.getGeneB().getPrimaryKey());
            joiner.add(interactionGeneJoin.getGeneB().getSymbol());
            joiner.add(interactionGeneJoin.getGeneB().getSpecies().getType().getTaxonID());
            joiner.add(interactionGeneJoin.getGeneB().getSpecies().getName());
            
            joiner.add(interactionGeneJoin.getInteractorBType().getPrimaryKey());
            joiner.add(interactionGeneJoin.getInteractorBType().getLabel());
            joiner.add(interactionGeneJoin.getInteractorBRole().getPrimaryKey());
            joiner.add(interactionGeneJoin.getInteractorBRole().getLabel());
            
            joiner.add(interactionGeneJoin.getInteractionType().getPrimaryKey());
            joiner.add(interactionGeneJoin.getInteractionType().getLabel());
            
            // add list of detectionMethods Ids
            String detectionMethodIds = "";
            if (interactionGeneJoin.getDetectionsMethods() != null) {
                StringJoiner methodJoiner = new StringJoiner(",");
                interactionGeneJoin.getDetectionsMethods().forEach(method -> methodJoiner.add(method.getPrimaryKey()));
                detectionMethodIds = methodJoiner.toString();
            }
            joiner.add(detectionMethodIds);
            
            // add list of detectionMethods Labels
            String detectionMethods = "";
            if (interactionGeneJoin.getDetectionsMethods() != null) {
                StringJoiner methodJoiner = new StringJoiner(",");
                interactionGeneJoin.getDetectionsMethods().forEach(method -> methodJoiner.add(method.getLabel()));
                detectionMethods = methodJoiner.toString();
            }
            joiner.add(detectionMethods);

            // add list of detectionMethods Labels
            String sourceIds = "";
            if (interactionGeneJoin.getCrossReferences() != null) {
                StringJoiner methodJoiner = new StringJoiner(",");
                interactionGeneJoin.getCrossReferences().forEach(method -> methodJoiner.add(method.getPrimaryKey()));
                sourceIds = methodJoiner.toString();
            }
            joiner.add(sourceIds);
            
            joiner.add(interactionGeneJoin.getSourceDatabase().getPrimaryKey());
            joiner.add(interactionGeneJoin.getSourceDatabase().getLabel());
            
            joiner.add(interactionGeneJoin.getAggregationDatabase().getPrimaryKey());
            joiner.add(interactionGeneJoin.getAggregationDatabase().getLabel());
            
            joiner.add(interactionGeneJoin.getPublication().getPubId());

            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();

    }
}
