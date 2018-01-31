package org.alliancegenome.indexer.document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class GeneDocument extends SearchableItem {

    { category = "gene"; }

    private List<String> gene_molecular_function;
    private String taxonId;
    private String symbol;
    private String species;
    private List<String> gene_biological_process;
    private List<String> synonyms;
    private String geneLiteratureUrl;
    private List<CrossReferenceDoclet> crossReferences;
    private String dataProvider;
    private Date dateProduced;
    private List<DiseaseDocument> diseases = new ArrayList<>();
    private String geneSynopsisUrl;
    private List<GenomeLocationDoclet> genomeLocations;
    private String soTermId;
    private List<String> secondaryIds;
    private String soTermName;
    private String release;
    private String geneSynopsis;
    private List<String> gene_cellular_component;
    private List<OrthologyDoclet> orthology;
    private List<FeatureDocument> alleles;
    private String geneticEntityExternalUrl;

    private String modCrossRefCompleteUrl;
    private String modLocalId;
    private String modGlobalCrossRefId;
    private String modGlobalId;

}
