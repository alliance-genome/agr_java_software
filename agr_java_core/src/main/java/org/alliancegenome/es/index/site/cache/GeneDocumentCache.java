package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.document.GeneDocument;
import org.alliancegenome.neo4j.entity.node.Gene;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private Map<String,Set<String>> cellularComponentExpressionWithParents = new HashMap<>();
    private Map<String,Set<String>> cellularComponentExpressionAgrSlim = new HashMap<>();

    private Map<String,Set<String>> whereExpressed = new HashMap<>();
    private Map<String,Set<String>> anatomicalExpression = new HashMap<>();         //uberon slim
    private Map<String,Set<String>> anatomicalExpressionWithParents = new HashMap<>();



    public void addCachedFields(Iterable<GeneDocument> geneDocuments) {
        for (GeneDocument geneDocument : geneDocuments) {
            String id = geneDocument.getPrimaryId();

            geneDocument.setAlleles(alleles.get(id));
            geneDocument.setStrictOrthologySymbols(strictOrthologySymbols.get(id));
            geneDocument.setDiseases(diseases.get(id));

            geneDocument.setBiologicalProcessWithParents(biologicalProcessWithParents.get(id));
            geneDocument.setBiologicalProcessAgrSlim(biologicalProcessAgrSlim.get(id));
            geneDocument.setCellularComponentWithParents(cellularComponentWithParents.get(id));
            geneDocument.setCellularComponentAgrSlim(cellularComponentAgrSlim.get(id));
            geneDocument.setMolecularFunctionWithParents(molecularFunctionWithParents.get(id));
            geneDocument.setMolecularFunctionAgrSlim(molecularFunctionAgrSlim.get(id));

            geneDocument.setCellularComponentExpressionWithParents(cellularComponentExpressionWithParents.get(id));
            geneDocument.setCellularComponentExpressionAgrSlim(cellularComponentExpressionAgrSlim.get(id));

            geneDocument.setWhereExpressed(whereExpressed.get(id));
            geneDocument.setAnatomicalExpression(anatomicalExpression.get(id));
            geneDocument.setAnatomicalExpressionWithParents(anatomicalExpressionWithParents.get(id));

            geneDocument.setPhenotypeStatements(phenotypeStatements.get(id));

        }
    }

}
