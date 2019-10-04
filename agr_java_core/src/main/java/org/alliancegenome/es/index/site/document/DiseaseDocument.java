package org.alliancegenome.es.index.site.document;


import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiseaseDocument extends SearchableItemDocument {

    public static final String CATEGORY = "disease";
    {
        category = CATEGORY;
    }

    private String doId;
    private String primaryKey;


    private String definition;
    private Date dateProduced;
    private List<String> definitionLinks;
    private Set<String> associatedSpecies;
    private Set<String> parentDiseaseNames;
    private Set<String> diseaseGroup;
    @JsonProperty("crossReferences")
    private Map<String, List<CrossReferenceDoclet>> crossReferencesMap;
    private List<SourceDoclet> sourceList;
    @JsonProperty("disease_group")
    private Set<String> highLevelSlimTermNames = new HashSet<>();

    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

    public DiseaseDocument() {
    }

    // for cloning purposes
    public DiseaseDocument(DiseaseDocument doc) {
        category = doc.category;
        doId = doc.doId;
        primaryKey = doc.primaryKey;
        name = doc.name;
        nameKey = doc.nameKey;
        description = doc.description;
        primaryKey = doc.primaryKey;
        searchable = doc.searchable;
        definition = doc.definition;
        dateProduced = doc.dateProduced;
        // note these attributes are not cloned deeply (they typically do not change)
        definitionLinks = doc.definitionLinks;
        synonyms = doc.synonyms;
        crossReferencesMap = doc.crossReferencesMap;
        sourceList = doc.sourceList;
        highLevelSlimTermNames = doc.highLevelSlimTermNames;
        phenotypeStatements = doc.phenotypeStatements;
    }
}
