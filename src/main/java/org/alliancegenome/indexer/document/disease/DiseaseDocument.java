package org.alliancegenome.indexer.document.disease;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.indexer.document.ESDocument;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class DiseaseDocument extends ESDocument {

    private String doId;
    private String primaryKey;
    private String name;
    private String species;
    private List<AnnotationDocument> annotations;
    private List<DiseaseDocument> parents;
    private List<DiseaseDocument> children;
    private List<String> synonyms;

    // gene-search related fields
    private Set<String> geneNames = new HashSet<>();
    private Set<String> geneSymbols = new HashSet<>();
    private Set<String> geneAliases = new HashSet<>();

    public void addGeneName(String name) {
        geneNames.add(name);
    }

    public void addGeneSymbol(String symbol) {
        this.geneSymbols.add(symbol);
    }

    public void addGeneAliases(Set<String> aliases) {
        this.geneAliases.addAll(aliases);
    }


    @JsonIgnore
    public String getDocumentId() {
        return doId;
    }
}
