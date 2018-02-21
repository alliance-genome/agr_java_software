package org.alliancegenome.indexer.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class FeatureDocument extends ESDocument {

    private String category = "allele";
    private String primaryKey;
    private String symbol;
    private Date dateProduced;
    private Date dataProvider;
    private String release;
    private String localId;
    private String globalId;

    private List<String> secondaryIds;
    private List<String> synonyms;
    private GeneDocument geneDocument;
    private List<DiseaseDocument> diseaseDocuments;


    @Override
    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }
}
