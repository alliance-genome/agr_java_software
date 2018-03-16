package org.alliancegenome.indexer.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class DiseaseAnnotationDocument {
    private String primaryKey;
    private String diseaseID;
    private Set<String> parentDiseaseIDs;
    private String diseaseName;
    @JsonProperty("disease_species")
    private SpeciesDoclet species;
    private String associationType;
    private GeneDocument geneDocument;
    private FeatureDocument featureDocument;
    private List<PublicationDoclet> publications;
    private List<String> parentTermIDs;
    private List<String> parentTermNames;
    private SourceDoclet source;
    private boolean searchable = true;
    private String category;


}
