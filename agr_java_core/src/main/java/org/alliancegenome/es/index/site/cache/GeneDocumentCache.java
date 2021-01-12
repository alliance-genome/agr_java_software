package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

    
    private Map<String,Set<String>> soTermNames = new HashMap<>();
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

        
        document.setPhenotypeStatements(phenotypeStatements.get(id));

        handleSoTermNames(document);

    }

    public void handleSoTermNames(SearchableItemDocument document) {
        String id = document.getPrimaryKey();

        Set<String> allBiotypes = soTermNameWithParents.get(id);

        document.setSoTermNameWithParents(allBiotypes);
        document.setBiotypes(allBiotypes);

        document.setBiotype0(new HashSet<String>(CollectionUtils.intersection(allBiotypes, biotypeLevel0)));
        document.setBiotype1(new HashSet<String>(CollectionUtils.intersection(allBiotypes, biotypeLevel1)));
        document.setBiotype2(new HashSet<String>(CollectionUtils.intersection(allBiotypes, biotypeLevel2)));


        //if the type is ncRNA gene and not a child, also add "unclassified ncRNA" at level 1
        if (document.getBiotypes().contains("ncRNA_gene") && CollectionUtils.isEmpty(document.getBiotype1())) {
            document.getBiotypes().add("unclassified_ncRNA");
            document.getBiotype1().add("unclassified_ncRNA");
        }

        //same for lncRNA genes, but one level deeper
        if (document.getBiotypes().contains("lncRNA_gene") && CollectionUtils.isEmpty(document.getBiotype2())) {
            document.getBiotypes().add("unclassified lncRNA gene");
            document.getBiotype2().add("unclassified lncRNA gene");
        }

        if (CollectionUtils.isEmpty(document.getBiotype0())) {
            document.getBiotypes().add("other_gene");
            document.getBiotype0().add("other_gene");
            document.getBiotype1().addAll(
                    soTermNames.get(id).stream()
                            .map(x -> {if (StringUtils.equals(x,"gene")) { return "unclassified gene";} return x;})
                            .map(x -> {if (StringUtils.equals(x,"biological_region")) { return "unclassified biological region";} return x;})
                            .collect(Collectors.toSet()));
            document.getBiotypes().addAll(document.getBiotype1());
        }
    }

}
