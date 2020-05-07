package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.apache.commons.collections.CollectionUtils;
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

    Set<String> biotypeLevel0 = new HashSet<>() {{
        add("protein_coding_gene");
        add("pseudogene");
        add("ncRNA_gene");
        add("other_gene");
    }};

    Set<String> biotypeLevel1 = new HashSet<>() {{
        add("unclassified_ncRNA_gene");
        add("lncRNA_gene");
        add("piRNA_gene");
        add("miRNA_gene");
        add("snoRNA_gene");
        add("tRNA_gene");
        add("snRNA_gene");
        add("rRNA_gene");
        add("enzymatic_RNA_gene");
        add("SRP_RNA_gene");
        add("scRNA_gene");
        add("RNase_P_RNA_gene");
        add("telomerase_RNA_gene");
        add("RNase_MRP_RNA_gene");
        add("unclassified_gene");
        add("heritable_phenotypic_marker");
        add("gene_segment");
        add("pseudogenic_gene_segment");
        add("transposable_element_gene");
        add("blocked_reading_frame");
    }};

    Set<String> biotypeLevel2 = new HashSet<>() {{
        add("unclassified_lncRNA_gene");
        add("lincRNA_gene");
        add("antisense_lncRNA_gene");
        add("sense intronic_ncRNA_gene");
        add("bidirectional_promoter_lncRNA");
        add("sense_overlap_ncRNA_gene");
    }};

    Set<String> otherGenes = new HashSet<>() {{
        add("unclassified_gene");
        add("heritable_phenotypic_marker");
        add("gene_segment");
        add("pseudogenic_gene_segment");
        add("transposable_element_gene");
        add("blocked_reading_frame");
    }};

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

        setSoTermNames(document);

    }

    public void setSoTermNames(SearchableItemDocument document) {
        String id = document.getPrimaryKey();

        Set<String> allBiotypes = soTermNameWithParents.get(id);

        document.setSoTermNameWithParents(allBiotypes);
        document.setBiotypes(soTermNameAgrSlim.get(id));

        document.setBiotype0(new HashSet<String>(CollectionUtils.intersection(allBiotypes, biotypeLevel0)));
        document.setBiotype1(new HashSet<String>(CollectionUtils.intersection(allBiotypes, biotypeLevel1)));
        document.setBiotype2(new HashSet<String>(CollectionUtils.intersection(allBiotypes, biotypeLevel2)));

        //add "other gene"
        if (CollectionUtils.containsAny(document.getBiotype1(), otherGenes)) {
            document.getBiotypes().add("other_gene");
            document.getBiotype0().add("other_gene");
        }

    }

}
