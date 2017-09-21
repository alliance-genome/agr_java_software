package org.alliancegenome.api.translator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.api.dao.DiseaseDAO;
import org.alliancegenome.indexer.document.DiseaseAnnotationDocument;
import org.elasticsearch.search.SearchHit;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.StringJoiner;

public class DiseaseAnnotationToTdfTranslator {

    private Logger log = Logger.getLogger(getClass());

    public String getAllRows(DiseaseDAO.SearchHitIterator hitIterator) {
        StringBuilder builder = new StringBuilder();
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
            joiner.add(diseaseAnnotationDocument.getDiseaseID());
            joiner.add(diseaseAnnotationDocument.getDiseaseName());
            joiner.add(diseaseAnnotationDocument.getSpecies().getName());
            joiner.add(diseaseAnnotationDocument.getGeneDocument().getPrimaryId());
            joiner.add(diseaseAnnotationDocument.getGeneDocument().getSymbol());
            joiner.add(diseaseAnnotationDocument.getAssociationType());

            // publication list
            StringJoiner pubJoiner = new StringJoiner(",");
            diseaseAnnotationDocument.getPublications().forEach(publication -> {
                pubJoiner.add(publication.getPubMedId());
            });
            joiner.add(pubJoiner.toString());
            joiner.add(diseaseAnnotationDocument.getGeneDocument().getDataProvider());

            builder.append(joiner.toString());
            builder.append(System.getProperty("line.separator"));
        }

        return builder.toString();

    }
}
