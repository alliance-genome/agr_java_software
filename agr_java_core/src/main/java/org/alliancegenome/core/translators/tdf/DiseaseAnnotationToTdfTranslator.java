package org.alliancegenome.core.translators.tdf;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.site.document.DiseaseAnnotationDocument;
import org.alliancegenome.es.util.SearchHitIterator;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.EvidenceCode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DiseaseAnnotationToTdfTranslator {

    private Log log = LogFactory.getLog(getClass());

    public String getAllRows(SearchHitIterator hitIterator) {
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
        headerJoiner.add("Source");
        headerJoiner.add("References");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());

        while (hitIterator.hasNext()) {
            SearchHit hit = hitIterator.next();
            String sourceAsString = hit.getSourceAsString();

            DiseaseAnnotationDocument diseaseAnnotationDocument = null;
            try {
                ObjectMapper mapper = new ObjectMapper();
                diseaseAnnotationDocument = mapper.readValue(sourceAsString, DiseaseAnnotationDocument.class);
            } catch (IOException e) {
                log.error("Could not deserialize", e);
                continue;
            }
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(diseaseAnnotationDocument.getGeneDocument().getPrimaryId());
            joiner.add(diseaseAnnotationDocument.getGeneDocument().getSymbol());
            joiner.add(diseaseAnnotationDocument.getSpecies().getName());
            if (diseaseAnnotationDocument.getAlleleDocument() != null) {
                joiner.add(diseaseAnnotationDocument.getAlleleDocument().getPrimaryKey());
                joiner.add(diseaseAnnotationDocument.getAlleleDocument().getSymbol());
                joiner.add("allele");
            } else {
                joiner.add("");
                joiner.add("");
                joiner.add("");
            }
            joiner.add(diseaseAnnotationDocument.getAssociationType());
            joiner.add(diseaseAnnotationDocument.getDiseaseID());
            joiner.add(diseaseAnnotationDocument.getDiseaseName());

            // evidence code list
            StringJoiner evidenceJoiner = new StringJoiner(",");
            Set<String> evidenceCodes = diseaseAnnotationDocument.getPublications()
                    .stream()
                    .map(publicationDoclet -> new HashSet<>(publicationDoclet.getEvidenceCodes()))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

            evidenceCodes.forEach(evidenceJoiner::add);
            joiner.add(evidenceJoiner.toString());
            joiner.add(diseaseAnnotationDocument.getSource().getName());

            // publication list
            StringJoiner pubJoiner = new StringJoiner(",");
            diseaseAnnotationDocument.getPublications().forEach(publication -> {
                pubJoiner.add(publication.getPubId());
            });
            joiner.add(pubJoiner.toString());
            builder.append(joiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        }

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
            Set<String> evidenceCodes = diseaseAnnotation.getEvidenceCodes()
                    .stream()
                    .map(EvidenceCode::getPrimaryKey)
                    .collect(Collectors.toSet());

            evidenceCodes.forEach(evidenceJoiner::add);
            joiner.add(evidenceJoiner.toString());
            //joiner.add(diseaseAnnotation.getSource().getName());

            // publication list
            StringJoiner pubJoiner = new StringJoiner(",");
            diseaseAnnotation.getPublications().forEach(publication -> {
                pubJoiner.add(publication.getPubId());
            });
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
            joiner.add(diseaseAnnotation.getOrthologyGene().getSpeciesName());

            // evidence code list
            StringJoiner evidenceJoiner = new StringJoiner(",");
            Set<String> evidenceCodes = diseaseAnnotation.getEvidenceCodes()
                    .stream()
                    .map(EvidenceCode::getPrimaryKey)
                    .collect(Collectors.toSet());

            evidenceCodes.forEach(evidenceJoiner::add);
            joiner.add(evidenceJoiner.toString());
            joiner.add("Alliance");
            //joiner.add(diseaseAnnotation.getSource().getName());

            // publication list
            StringJoiner pubJoiner = new StringJoiner(",");
            diseaseAnnotation.getPublications().forEach(publication -> {
                pubJoiner.add(publication.getPubId());
            });
            joiner.add(pubJoiner.toString());
            builder.append(joiner.toString());
            builder.append(System.getProperty("line.separator"));
        });

        return builder.toString();
    }
}
