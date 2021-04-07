package org.alliancegenome.es.index.site.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import org.alliancegenome.core.config.Constants;
import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.neo4j.entity.node.TranscriptLevelConsequence;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SearchableItemDocument extends ESDocument {

    String alterationType;
    String automatedGeneSynopsis;
    String branch;
    String category;
    String dataProvider;
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

    Set<String> age;
    Set<String> anatomicalExpression;
    Set<String> anatomicalExpressionWithParents;
    Set<String> assays;
    Set<String> associatedSpecies;
    Set<String> biologicalProcessAgrSlim;
    Set<String> biologicalProcessWithParents;
    Set<String> biotype0;
    Set<String> biotype1;
    Set<String> biotype2;
    Set<String> biotypes;
    Set<String> cellularComponentAgrSlim;
    Set<String> cellularComponentWithParents;
    Set<String> chromosomes;
    Set<String> constructs;
    Set<String> constructExpressedComponent;
    Set<String> constructKnockdownComponent;
    Set<String> constructRegulatoryRegion;
    Map<String, List<CrossReferenceDoclet>> crossReferencesMap;
    List<CrossReferenceDoclet> crossReferenceList;
    Set<String> crossReferences;
    Set<String> diseases;
    Set<String> diseasesAgrSlim;
    Set<String> diseaseGroup;
    Set<String> diseasesWithParents;
    Set<String> expressionStages;
    Set<String> alleles;
    Set<String> genes;
    Set<String> go_genes;
    Set<String> go_species;
    Set<String> models;
    Set<String> molecularConsequence;
    Set<String> molecularFunctionAgrSlim;
    Set<String> molecularFunctionWithParents;
    Set<String> parentDiseaseNames;
    Set<String> phenotypeStatements;
    Set<String> sampleIds;
    Set<String> secondaryIds;
    Set<String> sex;
    Set<String> strictOrthologySymbols;
    Set<String> soTermNameWithParents;
    Set<String> subcellularExpressionWithParents;
    Set<String> subcellularExpressionAgrSlim;
    Set<String> synonyms;
    Set<String> tags;
    Set<String> variants;
    Set<String> variantSynonyms;
    Set<String> variantType;
    Set<String> whereExpressed;
    
    List<TranscriptLevelConsequence> transcriptLevelConsequences = new ArrayList<>();
    String chromosome;
    boolean searchable = true;
    String matchedWithHtp;

//  @Override
//  @JsonIgnore
//  public String getDocumentId() {
//      //todo: Variant ids (hgvs nomenclature) can be too long for ES, while
//      // the odds of a collision are probably low, this isn't really the right solution
//      return primaryKey.substring(0, Math.min(primaryKey.length(),512));
//  }
    // TODO 04/07/2021 - Olin - in getting rid of the "documentId" we may have to override the getPrimaryKey method to do this logic
    

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
