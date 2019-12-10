package org.alliancegenome.es.index.site.document;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.doclet.GenomeLocationDoclet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneDocument extends SearchableItemDocument {

    public static final String CATEGORY = "gene";

    {
        category = CATEGORY;
    }


    private String taxonId;


    private Set<String> biologicalProcessWithParents;
    private Set<String> molecularFunctionWithParents;
    private Set<String> cellularComponentWithParents;

    private Set<String> biologicalProcessAgrSlim;
    private Set<String> molecularFunctionAgrSlim;
    private Set<String> cellularComponentAgrSlim;

    private String geneLiteratureUrl;
    @JsonProperty("crossReferences")
    private Map<String, List<CrossReferenceDoclet>> crossReferencesMap;
    private String dataProvider;
    private Date dateProduced;

    private String geneSynopsisUrl;
    private List<GenomeLocationDoclet> genomeLocations;
    private String soTermId;
    private String soTermName;
    private Set<String> soTermNameWithParents;
    private Set<String> soTermNameAgrSlim;
    private String release;
    private String geneSynopsis;
    private String automatedGeneSynopsis;
    private Set<String> strictOrthologySymbols;
    private String geneticEntityExternalUrl;
    private Set<String> alleles;
    private Set<String> whereExpressed;
    private Set<String> anatomicalExpression;         //uberon slim
    private Set<String> anatomicalExpressionWithParents;
    private Set<String> subcellularExpressionWithParents;
    private Set<String> subcellularExpressionAgrSlim;

    private String modLocalId;
    private String modGlobalCrossRefId;
    private String modGlobalId;

    @Override
    @JsonIgnore
    public String getDocumentId() {
        return primaryKey;
    }

    @Override
    public String toString() {
        return primaryKey + ": " + symbol;
    }
}
