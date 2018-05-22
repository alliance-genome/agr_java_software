package org.alliancegenome.es.index.site.document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeatureDocument extends SearchableItemDocument {

    { category = "allele"; }
    
    private String primaryKey;
    private String symbol;
    private Date dateProduced;
    private Date dataProvider;
    private String release;
    private String localId;
    private String globalId;
    private String modCrossRefFullUrl;

    private List<String> secondaryIds;
    private List<String> synonyms;
    private GeneDocument geneDocument;
    private List<DiseaseDocument> diseaseDocuments = new ArrayList<>();


    @Override
    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

    @Override
    public String getType() {
        return category;
    }
}
