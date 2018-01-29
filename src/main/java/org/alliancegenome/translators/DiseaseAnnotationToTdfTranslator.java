package org.alliancegenome.translators;

import java.io.IOException;
import java.util.StringJoiner;

import org.alliancegenome.shared.es.document.site_index.DiseaseAnnotationDocument;
import org.alliancegenome.shared.es.util.SearchHitIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DiseaseAnnotationToTdfTranslator {

	private Log log = LogFactory.getLog(getClass());

	public String getAllRows(SearchHitIterator hitIterator) {
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
