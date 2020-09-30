package org.alliancegenome.es.index.site.document;

import java.util.*;

import org.alliancegenome.core.config.Constants;
import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@Getter
@Setter
public class SearchableItemDocument extends ESDocument {

    String automatedGeneSynopsis;
    String branch;
    String category;
    String description;
    String definition;
    String geneLiteratureUrl;
    String geneSynopsis;
    String geneSynopsisUrl;
    String globalId;
    String href; //GO terms use this rather than modCrossRefCompleteUrl
    String id; //GO terms use this rather than primaryKey
    String localId;
    String name;
    @JsonProperty("name_key")
    String nameKey;
    String nameText;
    String modCrossRefCompleteUrl;
    String modLocalId;
    Double popularity;
    String primaryKey;
    String soTermId;
    String soTermName;
    String species;
    String summary;
    String symbol;
    String symbolText;


    Set<String> age = new HashSet<>();
    Set<String> anatomicalExpression = new HashSet<>();         //uberon slim
    Set<String> anatomicalExpressionWithParents = new HashSet<>();
    Set<String> assembly = new HashSet<>();
    Set<String> associatedSpecies = new HashSet<>();
    Set<String> biologicalProcessAgrSlim = new HashSet<>();
    Set<String> biologicalProcessWithParents = new HashSet<>();
    Set<String> biotype0 = new HashSet<>();
    Set<String> biotype1 = new HashSet<>();
    Set<String> biotype2 = new HashSet<>();
    Set<String> biotypes = new HashSet<>();
    Set<String> cellularComponentAgrSlim = new HashSet<>();
    Set<String> cellularComponentWithParents = new HashSet<>();
    Set<String> chromosomes = new HashSet<>();
    Set<String> constructs = new HashSet<>();
    Set<String> constructExpressedComponent = new HashSet<>();
    Set<String> constructKnockdownComponent = new HashSet<>();
    Set<String> constructRegulatoryRegion = new HashSet<>();
    Map<String, List<CrossReferenceDoclet>> crossReferencesMap;
    List<CrossReferenceDoclet> crossReferenceList;
    Set<String> crossReferences = new HashSet<>();
    Set<String> diseases = new HashSet<>();
    Set<String> diseasesAgrSlim = new HashSet<>();
    Set<String> diseaseGroup = new HashSet<>();
    Set<String> diseasesWithParents = new HashSet<>();
    Set<String> expressionStages = new HashSet<>();
    Set<String> alleles = new HashSet<>();
    Set<String> genes = new HashSet<>();
    Set<String> go_genes;
    Set<String> go_species;
    Set<String> models = new HashSet<>();
    Set<String> molecularConsequence = new HashSet<>();
    Set<String> molecularFunctionAgrSlim = new HashSet<>();
    Set<String> molecularFunctionWithParents = new HashSet<>();
    Set<String> parentDiseaseNames = new HashSet<>();
    Set<String> phenotypeStatements = new HashSet<>();
    Set<String> sampleIds = new HashSet<>();
    Set<String> secondaryIds = new HashSet<>();
    Set<String> sex = new HashSet<>();
    Set<String> strictOrthologySymbols = new HashSet<>();
    Set<String> soTermNameWithParents = new HashSet<>();
    Set<String> subcellularExpressionWithParents = new HashSet<>();
    Set<String> subcellularExpressionAgrSlim = new HashSet<>();
    Set<String> synonyms = new HashSet<>();
    Set<String> tags = new HashSet<>();
    Set<String> variants = new HashSet<>();
    Set<String> variantSynonyms = new HashSet<>();
    Set<String> variantType = new HashSet<>();
    Set<String> whereExpressed = new HashSet<>();

    boolean searchable = true;

    @Override
    @JsonIgnore
    public String getDocumentId() {
        //todo: Variant ids (hgvs nomenclature) can be too long for ES, while
        // the odds of a collision are probably low, this isn't really the right solution
        return primaryKey.substring(0, Math.min(primaryKey.length(),512));
    }

    @Override
    @JsonIgnore
    public String getType() {
        return Constants.SEARCHABLE_ITEM;
    }

    public void setNameKeyWithSpecies(String nameKey, String species) {
        this.nameKey = nameKey;
        if (species != null) {
            this.nameKey += " (" + species + ")";
        }
    }

    public String toString() {
        return primaryKey;
    }

}
