package org.alliancegenome.core.translators.tdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.es.index.site.document.DiseaseAnnotationDocument;
import org.alliancegenome.es.index.site.document.PhenotypeAnnotationDocument;
import org.alliancegenome.es.util.SearchHitIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class PhenotypeAnnotationToTdfTranslator {

    private Log log = LogFactory.getLog(getClass());

    public String getAllRows(SearchHitIterator hitIterator) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Phenotype");
        headerJoiner.add("Genetic Entity ID");
        headerJoiner.add("Genetic Entity Symbol");
        headerJoiner.add("Genetic Entity Type");
        headerJoiner.add("References");
        builder.append(headerJoiner.toString());
        builder.append(System.getProperty("line.separator"));

        while (hitIterator.hasNext()) {
            SearchHit hit = hitIterator.next();
            String sourceAsString = hit.getSourceAsString();

            PhenotypeAnnotationDocument phenotypeAnnotationDocument = null;
            try {
                ObjectMapper mapper = new ObjectMapper();
                phenotypeAnnotationDocument = mapper.readValue(sourceAsString, PhenotypeAnnotationDocument.class);
            } catch (IOException e) {
                log.error("Could not deserialize", e);
                continue;
            }
            StringJoiner joiner = new StringJoiner("\t");
            joiner.add(phenotypeAnnotationDocument.getPhenotype());
            if (phenotypeAnnotationDocument.getFeatureDocument() != null) {
                joiner.add(phenotypeAnnotationDocument.getFeatureDocument().getPrimaryKey());
                joiner.add(phenotypeAnnotationDocument.getFeatureDocument().getSymbol());
                joiner.add("allele");
            } else {
                joiner.add("");
                joiner.add("");
                joiner.add("");
            }
            // publication list
            StringJoiner pubJoiner = new StringJoiner(",");
            phenotypeAnnotationDocument.getPublications().forEach(publication -> {
                pubJoiner.add(publication.getPubId());
            });
            joiner.add(pubJoiner.toString());
            builder.append(joiner.toString());
            builder.append(System.getProperty("line.separator"));
        }

        return builder.toString();

    }
}
