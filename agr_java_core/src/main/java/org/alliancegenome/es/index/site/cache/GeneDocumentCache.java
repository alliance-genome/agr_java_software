package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneDocumentCache extends IndexerCache {

    private final Logger log = LogManager.getLogger(getClass());

    private Map<String, Gene> geneMap = new HashMap<>();

    private Map<String,Set<String>> strictOrthologySymbols = new HashMap<>();

    private Map<String,Set<String>> biologicalProcessWithParents = new HashMap<>();
    private Map<String,Set<String>> biologicalProcessAgrSlim = new HashMap<>();
    private Map<String,Set<String>> cellularComponentWithParents = new HashMap<>();
    private Map<String,Set<String>> cellularComponentAgrSlim = new HashMap<>();
    private Map<String,Set<String>> molecularFunctionWithParents = new HashMap<>();
    private Map<String,Set<String>> molecularFunctionAgrSlim = new HashMap<>();

    private Map<String,Set<String>> subcellularExpressionWithParents = new HashMap<>();
    private Map<String,Set<String>> subcellularExpressionAgrSlim = new HashMap<>();

    private Map<String,Set<String>> whereExpressed = new HashMap<>();
    private Map<String,Set<String>> anatomicalExpression = new HashMap<>();         //uberon slim
    private Map<String,Set<String>> anatomicalExpressionWithParents = new HashMap<>();

    private Map<String,Set<String>> soTermNameWithParents = new HashMap<>();
    private Map<String,Set<String>> soTermNameAgrSlim = new HashMap<>();



    public void addCachedFields(Iterable<SearchableItemDocument> documents) {
        for (SearchableItemDocument document : documents) {
            addCachedFields(document);
        }
    }

    public void addCachedFields(SearchableItemDocument document) {
        String id = document.getPrimaryKey();

        super.addCachedFields(document);

        document.setStrictOrthologySymbols(strictOrthologySymbols.get(id));
        document.setDiseasesAgrSlim(diseasesAgrSlim.get(id));
        document.setDiseasesWithParents(diseasesWithParents.get(id));

        document.setBiologicalProcessWithParents(biologicalProcessWithParents.get(id));
        document.setBiologicalProcessAgrSlim(biologicalProcessAgrSlim.get(id));
        document.setCellularComponentWithParents(cellularComponentWithParents.get(id));
        document.setCellularComponentAgrSlim(cellularComponentAgrSlim.get(id));
        document.setMolecularFunctionWithParents(molecularFunctionWithParents.get(id));
        document.setMolecularFunctionAgrSlim(molecularFunctionAgrSlim.get(id));

        document.setSubcellularExpressionWithParents(subcellularExpressionWithParents.get(id));
        document.setSubcellularExpressionAgrSlim(subcellularExpressionAgrSlim.get(id));

        document.setWhereExpressed(whereExpressed.get(id));
        document.setAnatomicalExpression(anatomicalExpression.get(id));
        document.setAnatomicalExpressionWithParents(anatomicalExpressionWithParents.get(id));

        document.setPhenotypeStatements(phenotypeStatements.get(id));

        document.setSoTermNameWithParents(soTermNameWithParents.get(id));
        document.setSoTermNameAgrSlim(soTermNameAgrSlim.get(id));
    }
}
