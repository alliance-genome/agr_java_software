package org.alliancegenome.es.index.data.enums;

import org.alliancegenome.es.index.data.doclet.DataTypeDoclet;

import lombok.Getter;

@Getter
public enum DataType {
    // Default data type if index does not contain the data type then this will be used to inject a document
    BGI("Basic Gene Information", "json", true, true, true),
    DAF("Disease Ontology Annotations File", "json", true, true, true),
    ORTHO("Orthology", "json", true, true, true),
    ALLELE("Allele Information", "json", true, true, true),
    GENOTYPE("Genotype Information File", "json", true, true, true),
    PHENOTYPE("Phenotype Information File", "json", true, true, true),
    EXPRESSION("Expression Information File", "json", true, true, true),
    VARIATION("Variation Information File", "json", true, true, true),
    SQTR("Sequence Targeting Reagent File", "json", true, true, true),
    
    // No schema required for these but will still stick them in the correct schema directory
    GAF("Gene Ontology Annotations File", "gaf", true, false, false),
    GFF("Gene Features File", "gff", true, false, false),

    // No verification yet for these either
    DO("Disease Ontology", "obo", false, false, false),
    GO("Gene Ontology", "obo", false, false, false),
    SO("Sequence Ontology", "obo", false, false, false),
    ;

    private boolean taxonIdRequired;
    private boolean validationRequired;
    private String fileExtension;
    private String description;
    private boolean modVersionStored;

    private DataType(String description, String fileExtension, boolean taxonIdRequired, boolean validationRequired, boolean modVersionStored) {
        this.description = description;
        this.fileExtension = fileExtension;
        this.taxonIdRequired = taxonIdRequired;
        this.validationRequired = validationRequired;
        this.modVersionStored = modVersionStored;
    }

    public static DataTypeDoclet fromString(String string) {
        for(DataType dt: DataType.values()) {
            if(dt.name().toLowerCase().equals(string.toLowerCase())) {
                return getDoclet(dt);
            }
        }
        return null;
    }

    public static DataTypeDoclet getDoclet(DataType type) {
        DataTypeDoclet ret = new DataTypeDoclet();
        ret.setTaxonIdRequired(type.isTaxonIdRequired());
        ret.setValidationRequired(type.isValidationRequired());
        ret.setFileExtension(type.getFileExtension());
        ret.setDescription(type.getDescription());
        ret.setModVersionStored(type.isModVersionStored());
        ret.setName(type.name());
        return ret;
    }

}
