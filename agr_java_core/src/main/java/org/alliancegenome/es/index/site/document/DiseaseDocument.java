package org.alliancegenome.es.index.site.document;


import java.util.*;
import java.util.stream.Collectors;

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

    private Set<String> parentDiseaseNames;
    private String definition;
    private Date dateProduced;
    private List<String> definitionLinks;
    private List<AnnotationDocument> annotations;
    private List<DiseaseDocument> parents;
    private List<DiseaseDocument> children;
    private List<String> synonyms;
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
        primaryId = doc.primaryId;
        searchable = doc.searchable;
        definition = doc.definition;
        dateProduced = doc.getDateProduced();
        annotations = doc.getAnnotations().stream().map(AnnotationDocument::new).collect(Collectors.toList());
        // note these attributes are not cloned deeply (they typically do not change)
        definitionLinks = doc.definitionLinks;
        parents = doc.parents;
        children = doc.children;
        synonyms = doc.synonyms;
        crossReferencesMap = doc.crossReferencesMap;
        sourceList = doc.sourceList;
        highLevelSlimTermNames = doc.highLevelSlimTermNames;
        phenotypeStatements = doc.phenotypeStatements;
    }
}
