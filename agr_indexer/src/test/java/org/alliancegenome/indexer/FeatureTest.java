package org.alliancegenome.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.core.translators.document.FeatureTranslator;
import org.alliancegenome.es.index.site.document.FeatureDocument;
import org.alliancegenome.neo4j.entity.node.Feature;
import org.alliancegenome.neo4j.repository.FeatureRepository;

public class FeatureTest {

    public static void main(String[] args) throws Exception {
        FeatureRepository repo = new FeatureRepository();
        FeatureTranslator trans = new FeatureTranslator();

        Feature feature = repo.getFeature("MGI:5496433");

        FeatureDocument featureDocument = trans.translate(feature);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(featureDocument);
        System.out.println(json);
    }
}
