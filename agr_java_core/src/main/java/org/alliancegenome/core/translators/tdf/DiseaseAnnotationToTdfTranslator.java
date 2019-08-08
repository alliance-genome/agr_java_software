package org.alliancegenome.core.translators.tdf;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DiseaseAnnotationToTdfTranslator {

    private Log log = LogFactory.getLog(getClass());

    public String getAllRows(List<DiseaseAnnotation> diseaseAnnotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Gene ID");
        headerJoiner.add("Gene Symbol");
        headerJoiner.add("Species");
        headerJoiner.add("Genetic Entity ID");
        headerJoiner.add("Genetic Entity Symbol");
        headerJoiner.add("Genetic Entity Type");
        headerJoiner.add("Association Type");
        headerJoiner.add("Disease ID");
        headerJoiner.add("Disease Name");
        headerJoiner.add("Evidence Code");
        headerJoiner.add("Based On");
        headerJoiner.add("Source");
        headerJoiner.add("References");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        diseaseAnnotations.forEach(diseaseAnnotation -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(diseaseAnnotation.getGene().getPrimaryKey());
            joiner.add(diseaseAnnotation.getGene().getSymbol());
            joiner.add(diseaseAnnotation.getGene().getSpecies().getSpecies());
            if (diseaseAnnotation.getFeature() != null) {
                joiner.add(diseaseAnnotation.getFeature().getPrimaryKey());
                joiner.add(diseaseAnnotation.getFeature().getSymbol());
                joiner.add("allele");
            } else {
                joiner.add("");
                joiner.add("");
                joiner.add("");
            }
            joiner.add(diseaseAnnotation.getAssociationType());
            joiner.add(diseaseAnnotation.getDisease().getPrimaryKey());
            joiner.add(diseaseAnnotation.getDisease().getName());

            // evidence code list
            StringJoiner evidenceJoiner = new StringJoiner(",");
            Set<String> evidenceCodes = diseaseAnnotation.getEcoCodes()
                    .stream()
                    .map(ECOTerm::getPrimaryKey)
                    .collect(Collectors.toSet());

            evidenceCodes.forEach(evidenceJoiner::add);
            joiner.add(evidenceJoiner.toString());

            List<Gene> orthologyGenes = diseaseAnnotation.getOrthologyGenes();
            if (orthologyGenes != null) {
                StringJoiner basedOnJoiner = new StringJoiner(",");
                orthologyGenes.forEach(gene -> basedOnJoiner.add(gene.getPrimaryKey() + ":" + gene.getSymbol()));
                joiner.add(basedOnJoiner.toString());
            } else
                joiner.add("");

            // source list
            joiner.add(diseaseAnnotation.getSource().getName());

            // publications list
            StringJoiner pubJoiner = new StringJoiner(",");
            diseaseAnnotation.getPublications().forEach(publication -> pubJoiner.add(publication.getPubId()));
            joiner.add(pubJoiner.toString());
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());

        });

        return builder.toString();

    }

    public String getAllRowsForRibbon(List<DiseaseAnnotation> diseaseAnnotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Species");
        headerJoiner.add("Gene ID");
        headerJoiner.add("Gene Symbol");
        headerJoiner.add("Disease ID");
        headerJoiner.add("Disease Name");
        headerJoiner.add("Genetic entity type");
        headerJoiner.add("Genetic entity Symbol");
        headerJoiner.add("Genetic entity ID");
        headerJoiner.add("Association Type");
        headerJoiner.add("Evidence Codes");
        headerJoiner.add("Source");
        headerJoiner.add("Based-On Genes");
        headerJoiner.add("References");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        diseaseAnnotations.forEach(diseaseAnnotation -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(diseaseAnnotation.getGene().getSpecies().getSpecies());
            joiner.add(diseaseAnnotation.getGene().getPrimaryKey());
            joiner.add(diseaseAnnotation.getGene().getSymbol());
            joiner.add(diseaseAnnotation.getDisease().getPrimaryKey());
            joiner.add(diseaseAnnotation.getDisease().getName());
            joiner.add(diseaseAnnotation.getGeneticEntityType());
            if(diseaseAnnotation.getFeature() != null){
                joiner.add(diseaseAnnotation.getFeature().getSymbolText());
                joiner.add(diseaseAnnotation.getFeature().getPrimaryKey());

            } else {
                joiner.add("");
                joiner.add("");
            }


            joiner.add(diseaseAnnotation.getAssociationType());

            // evidence code list
            StringJoiner evidenceJoiner = new StringJoiner(",");
            Set<String> evidenceCodes = diseaseAnnotation.getEcoCodes()
                    .stream()
                    .map(ECOTerm::getPrimaryKey)
                    .collect(Collectors.toSet());

            evidenceCodes.forEach(evidenceJoiner::add);
            joiner.add(evidenceJoiner.toString());
            // source list
            joiner.add(diseaseAnnotation.getSource().getName());

            // basedOn info

            List<Gene> orthologyGenes = diseaseAnnotation.getOrthologyGenes();
            if (orthologyGenes != null) {
                StringJoiner basedOnJoiner = new StringJoiner(",");
                orthologyGenes.forEach(gene -> basedOnJoiner.add(gene.getPrimaryKey() + ":" + gene.getSymbol()));
                joiner.add(basedOnJoiner.toString());
            } else
                joiner.add("");

            // publications list
            StringJoiner pubJoiner = new StringJoiner(",");
            diseaseAnnotation.getPublications().forEach(publication -> pubJoiner.add(publication.getPubId()));
            joiner.add(pubJoiner.toString());
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());

        });

        return builder.toString();

    }

    public String getEmpiricalDiseaseByGene(List<DiseaseAnnotation> diseaseAnnotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Disease");
        headerJoiner.add("Genetic Entity ID");
        headerJoiner.add("Genetic Entity Symbol");
        headerJoiner.add("Genetic Entity Type");
        headerJoiner.add("Association Type");
        headerJoiner.add("Evidence Code");
        headerJoiner.add("Source");
        headerJoiner.add("References");
        builder.append(headerJoiner.toString());
        builder.append(System.getProperty("line.separator"));

        diseaseAnnotations.forEach(diseaseAnnotation -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(diseaseAnnotation.getDisease().getName());
            if (diseaseAnnotation.getFeature() != null) {
                joiner.add(diseaseAnnotation.getFeature().getPrimaryKey());
                joiner.add(diseaseAnnotation.getFeature().getSymbol());
                joiner.add("allele");
            } else {
                joiner.add("");
                joiner.add("");
                joiner.add("gene");
            }
            joiner.add(diseaseAnnotation.getAssociationType());

            // evidence code list
            StringJoiner evidenceJoiner = new StringJoiner(",");
            Set<String> evidenceCodes = diseaseAnnotation.getEcoCodes()
                    .stream()
                    .map(ECOTerm::getPrimaryKey)
                    .collect(Collectors.toSet());

            evidenceCodes.forEach(evidenceJoiner::add);
            joiner.add(evidenceJoiner.toString());
            //joiner.add(diseaseAnnotation.getSource().getName());

            // publications list
            StringJoiner pubJoiner = new StringJoiner(",");
            diseaseAnnotation.getPublications().forEach(publication -> pubJoiner.add(publication.getPubId()));
            joiner.add(pubJoiner.toString());
            builder.append(joiner.toString());
            builder.append(System.getProperty("line.separator"));
        });

        return builder.toString();
    }

    public String getDiseaseViaOrthologyByGene(List<DiseaseAnnotation> diseaseAnnotations) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Disease");
        headerJoiner.add("Association");
        headerJoiner.add("Ortholog Gene ID");
        headerJoiner.add("Ortholog Gene Symbol");
        headerJoiner.add("Ortholog Species");
        headerJoiner.add("Evidence Code");
        headerJoiner.add("Source");
        headerJoiner.add("References");
        builder.append(headerJoiner.toString());
        builder.append(System.getProperty("line.separator"));

        diseaseAnnotations.forEach(diseaseAnnotation -> {
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(diseaseAnnotation.getDisease().getName());
            joiner.add(diseaseAnnotation.getAssociationType());
            joiner.add(diseaseAnnotation.getOrthologyGene().getPrimaryKey());
            joiner.add(diseaseAnnotation.getOrthologyGene().getSymbol());
            joiner.add(diseaseAnnotation.getOrthologyGene().getSpecies().getName());

            // evidence code list
            StringJoiner evidenceJoiner = new StringJoiner(",");
            Set<String> evidenceCodes = diseaseAnnotation.getEcoCodes()
                    .stream()
                    .map(ECOTerm::getPrimaryKey)
                    .collect(Collectors.toSet());

            evidenceCodes.forEach(evidenceJoiner::add);
            joiner.add(evidenceJoiner.toString());
            joiner.add("Alliance");
            //joiner.add(diseaseAnnotation.getSource().getName());

            // publications list
            StringJoiner pubJoiner = new StringJoiner(",");
            diseaseAnnotation.getPublications().forEach(publication -> pubJoiner.add(publication.getPubId()));
            joiner.add(pubJoiner.toString());
            builder.append(joiner.toString());
            builder.append(System.getProperty("line.separator"));
        });

        return builder.toString();
    }
}
