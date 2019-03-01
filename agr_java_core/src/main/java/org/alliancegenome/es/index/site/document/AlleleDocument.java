package org.alliancegenome.es.index.site.document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlleleDocument extends SearchableItemDocument {

    public static final String CATEGORY = "allele";

    {
        category = CATEGORY;
    }

    private String primaryKey;
    private String symbol;
    private String species;
    private String searchSymbol;
    private Date dateProduced;
    private Date dataProvider;
    private String release;
    private String localId;
    private String globalId;
    private String modCrossRefFullUrl;

    private List<String> secondaryIds;
    private List<String> synonyms;
    private List<CrossReferenceDoclet> crossReferenceList;

    @Override
    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
        // strip off html tags...
        if (symbol != null)
            searchSymbol = symbol.replaceAll("<[^>]*>", " ");
    }

    @Override
    public String toString() {
        return primaryKey + ":" + symbol;
    }
}
