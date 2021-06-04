package org.alliancegenome.es.index.site.document;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

import org.alliancegenome.core.config.Constants;
import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.neo4j.entity.node.TranscriptLevelConsequence;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.*;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SearchableItemDocument extends ESDocument {

    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String alterationType;
    String automatedGeneSynopsis;
    String branch;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String category;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String chromosome;
    String dataProvider;
    String description;
    String definition;
    String geneLiteratureUrl;
    String geneSynopsis;
    String geneSynopsisUrl;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String globalId;
    String href; //GO terms use this rather than modCrossRefCompleteUrl
    String localId;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String name;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    @JsonProperty("name_key")
    String nameKey;
    String nameText;
    String modCrossRefCompleteUrl;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String modLocalId;
    Double popularity;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String primaryKey;
    //@JsonView({View.AlleleVariantSequenceConverterForES.class})
    boolean searchable = true;
    String soTermId;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String soTermName;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String species;
    String summary;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String symbol;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String symbolText;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    String variantName;

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
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> chromosomes;
    Set<String> constructs;
    Set<String> constructExpressedComponent;
    Set<String> constructKnockdownComponent;
    Set<String> constructRegulatoryRegion;
    Map<String, List<CrossReferenceDoclet>> crossReferencesMap;
    List<CrossReferenceDoclet> crossReferenceList;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> crossReferences;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> diseases;
    Set<String> diseasesAgrSlim;
    Set<String> diseaseGroup;
    Set<String> diseasesWithParents;
    Set<String> expressionStages;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> alleles;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> genes;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> geneIds;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> geneSynonyms;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> geneCrossReferences;
    Set<String> go_genes;
    Set<String> go_species;

    Set<String> models;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> molecularConsequence;
    Set<String> molecularFunctionAgrSlim;
    Set<String> molecularFunctionWithParents;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> parentDiseaseNames;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> phenotypeStatements;
    Set<String> sampleIds;
    Set<String> secondaryIds;
    Set<String> sex;
    Set<String> strictOrthologySymbols;
    Set<String> soTermNameWithParents;
//    Set<String> stage;
    Set<String> subcellularExpressionWithParents;
    Set<String> subcellularExpressionAgrSlim;
    Set<String> synonyms;
    Set<String> tags;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> variants;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> variantSynonyms;
    @JsonView({View.AlleleVariantSequenceConverterForES.class})
    Set<String> variantType;
    Set<String> whereExpressed;
    
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
