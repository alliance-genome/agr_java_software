package org.alliancegenome.indexer.document;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class GeneDocument {
    private String category;
    private String href;
    private String name;
    private String name_key;
    private String description;

    private List<String> gene_molecular_function;
    private String taxonId;
    private String symbol;
    private String species;
    private List<String> gene_biological_process;
    private List<String> synonyms;
    private String geneLiteratureUrl;
    private String dataProvider;
    private Date dateProduced;
    private List<DiseaseDocument> diseases = new ArrayList<>();
    private String geneSynopsisUrl;
    private String primaryId;
    private String soTermId;
    private List<String> secondaryIds;
    private String soTermName;
    private String release;
    private String geneSynopsis;
    private List<String> gene_cellular_component;
    private String geneticEntityExternalUrl;

    private String modCrossRefCompleteUrl;
    private String modLocalId;
    private String modGlobalCrossRefId;
    private String modGlobalId;

}
