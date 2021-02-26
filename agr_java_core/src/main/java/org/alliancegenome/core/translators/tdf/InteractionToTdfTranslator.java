package org.alliancegenome.core.translators.tdf;

import java.util.*;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.api.entity.JoinTypeValue;

public class InteractionToTdfTranslator {

    public String getAllRows(List<InteractionGeneJoin> annotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        if (annotations.get(0).getJoinType().equalsIgnoreCase(JoinTypeValue.genetic_interaction.getName())) {
        //headerJoiner.add("Focus gene molecule type ID");
        //headerJoiner.add("Focus gene molecule type");
        headerJoiner.add("Interaction type");
        headerJoiner.add("Focus gene experimental role ID");
        headerJoiner.add("Focus gene experimental role");
        
        headerJoiner.add("Interactor gene ID");
        headerJoiner.add("Interactor gene");
        headerJoiner.add("Interactor species ID");
        headerJoiner.add("Interactor species");
        
        //headerJoiner.add("Interactor molecule type ID");
        //headerJoiner.add("Interactor molecule type");
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
        //new column from genetic interaction
        headerJoiner.add("genetic perturbation A");
        headerJoiner.add("genetic perturbation B");
        headerJoiner.add("Phenotype or trait");
        }
        else {
            headerJoiner.add("Focus gene molecule type ID");
            headerJoiner.add("Focus gene molecule type");
            headerJoiner.add("Interaction type");
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
            //new column from genetic interaction
            //headerJoiner.add("genetic perturbation A");
            //headerJoiner.add("genetic perturbation B");
            //headerJoiner.add("Phenotype or trait");
        }

        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        annotations.forEach(interactionGeneJoin -> {
            StringJoiner joiner = new StringJoiner("\t");
            if (interactionGeneJoin.getJoinType().equalsIgnoreCase(JoinTypeValue.molecular_interaction.getName()))
            joiner.add(interactionGeneJoin.getInteractorAType().getPrimaryKey());
            if (interactionGeneJoin.getJoinType().equalsIgnoreCase(JoinTypeValue.molecular_interaction.getName()))
            joiner.add(interactionGeneJoin.getInteractorAType().getLabel());
            joiner.add(interactionGeneJoin.getJoinType());
            joiner.add(interactionGeneJoin.getInteractorARole().getPrimaryKey());
            joiner.add(interactionGeneJoin.getInteractorARole().getLabel());
            
            joiner.add(interactionGeneJoin.getGeneB().getPrimaryKey());
            joiner.add(interactionGeneJoin.getGeneB().getSymbol());
            joiner.add(interactionGeneJoin.getGeneB().getSpecies().getPrimaryKey());
            joiner.add(interactionGeneJoin.getGeneB().getSpecies().getName());
            if (interactionGeneJoin.getJoinType().equalsIgnoreCase(JoinTypeValue.molecular_interaction.getName()))
            joiner.add(interactionGeneJoin.getInteractorBType().getPrimaryKey());
            if (interactionGeneJoin.getJoinType().equalsIgnoreCase(JoinTypeValue.molecular_interaction.getName()))
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
            
            String sourceDatabaseID="";
            String sourceDatabase="";
            if(interactionGeneJoin.getSourceDatabase()!=null ) {//maybe null here
              joiner.add(interactionGeneJoin.getSourceDatabase().getPrimaryKey());
              joiner.add(interactionGeneJoin.getSourceDatabase().getLabel());
            }
            else {
                joiner.add(sourceDatabaseID);
                joiner.add(sourceDatabase);
            }
            
            
            if (interactionGeneJoin.getAggregationDatabase() !=null) {//maybe null here
              joiner.add(interactionGeneJoin.getAggregationDatabase().getPrimaryKey());
              joiner.add(interactionGeneJoin.getAggregationDatabase().getLabel());
            }
            
            if (interactionGeneJoin.getPublication()!=null)
             joiner.add(interactionGeneJoin.getPublication().getPubId());
            else 
                joiner.add(""); 
            //genetic perturbation alleleA
            if (interactionGeneJoin.getJoinType().equalsIgnoreCase(JoinTypeValue.genetic_interaction.getName()))
            if (interactionGeneJoin.getAlleleA()!=null) {
                joiner.add(interactionGeneJoin.getAlleleA().getPrimaryKey());
            }
            else {
                joiner.add("");
            }
            //genetic perturbation, alleleB
            if (interactionGeneJoin.getJoinType().equalsIgnoreCase(JoinTypeValue.genetic_interaction.getName()))
            if (interactionGeneJoin.getAlleleB()!=null) {
                joiner.add(interactionGeneJoin.getAlleleB().getPrimaryKey());
            }
            else {
                joiner.add("");
            }
            //phenotypes
            String phenotypeIds = "";
            if (interactionGeneJoin.getJoinType().equalsIgnoreCase(JoinTypeValue.genetic_interaction.getName())) {
             if (interactionGeneJoin.getPhenotypes() != null) {
                StringJoiner phenotypeJoiner = new StringJoiner(",");
                interactionGeneJoin.getPhenotypes().forEach(phenotype -> phenotypeJoiner.add(phenotype.getPrimaryKey()));
                phenotypeIds = phenotypeJoiner.toString();
             }
             joiner.add(phenotypeIds);
            }
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });

        return builder.toString();

    }
}
