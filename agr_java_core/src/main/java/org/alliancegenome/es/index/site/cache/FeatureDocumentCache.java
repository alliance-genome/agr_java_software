package org.alliancegenome.es.index.site.cache;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.index.site.document.FeatureDocument;
import org.alliancegenome.neo4j.entity.node.Feature;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class FeatureDocumentCache extends IndexerCache {

    private Map<String, Feature> featureMap = new HashMap<>();

    public void addCachedFields(Iterable<FeatureDocument> featureDocuments) {

        for (FeatureDocument featureDocument : featureDocuments) {
            String id = featureDocument.getPrimaryKey();

            featureDocument.setDiseases(diseases.get(id));
            featureDocument.setGenes(genes.get(id));
            featureDocument.setPhenotypeStatements(phenotypeStatements.get(id));

        }

    }

}
