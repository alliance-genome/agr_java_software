package org.alliancegenome.es.index.site.document;

import java.util.*;

import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.doclet.GenomeLocationDoclet;
import org.alliancegenome.es.index.site.doclet.OrthologyDoclet;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class GeneDocument extends SearchableItemDocument {

    public static final String CATEGORY = "gene";
    {
        category = CATEGORY;
    }


    private String taxonId;
    private String symbol;
    private String species;

    private Set<String> biologicalProcessWithParents;
    private Set<String> molecularFunctionWithParents;
    private Set<String> cellularComponentWithParents;

    private Set<String> biologicalProcessAgrSlim;
    private Set<String> molecularFunctionAgrSlim;
    private Set<String> cellularComponentAgrSlim;

    private List<String> synonyms;
    private String geneLiteratureUrl;
    @JsonProperty("crossReferences")
    private Map<String, List<CrossReferenceDoclet>> crossReferencesMap;
    private String dataProvider;
    private Date dateProduced;
    private List<DiseaseDocument> diseasesViaExperiment = new ArrayList<>();
    private List<DiseaseDocument> diseasesViaOrthology = new ArrayList<>();
    private List<PhenotypeDocument> phenotypes = new ArrayList<>();
    private String geneSynopsisUrl;
    private String primaryId;
    private List<GenomeLocationDoclet> genomeLocations;
    private String soTermId;
    private List<String> secondaryIds;
    private String soTermName;
    private String release;
    private String geneSynopsis;
    private String automatedGeneSynopsis;
    private List<OrthologyDoclet> orthology;
    private List<String> strictOrthologySymbols;
    private String geneticEntityExternalUrl;
    private Set<String> whereExpressed;
    private Set<String> anatomicalExpression;         //uberon slim
    private Set<String> anatomicalExpressionWithParents;
    private Set<String> cellularComponentExpressionWithParents;
    private Set<String> cellularComponentExpressionAgrSlim;

    private String modCrossRefCompleteUrl;
    private String modLocalId;
    private String modGlobalCrossRefId;
    private String modGlobalId;
    
    private List<FeatureDocument> alleles;
    
    @Override
    @JsonIgnore
    public String getDocumentId() {
        return primaryId;
    }
}
