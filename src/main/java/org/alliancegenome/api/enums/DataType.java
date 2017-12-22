package org.alliancegenome.api.enums;

public enum DataType {

    BGI("Basic Gene Information", "json", true, true),
    DOA("Disease Ontology Annotations", "json", true, true),
    GFF("Gene Features File", "gff3", true, true),
    GOA("Gene Ontology Annotations", "json", true, true),
    ORTHO("Orthology", "json", true, true),
    BAI("Basic Allele Information", "json", true, true),

    DO("Disease Ontology", "obo", false, false),
    GO("Gene Ontology", "obo", false, false),
    SO("Sequence Ontology", "obo", false, false),
    ;

    private boolean modRequired;
    private boolean validationRequired;
    private String fileExtension;
    private String description;

    private DataType(String description, String fileExtension, boolean modRequired, boolean validationRequired) {
        this.description = description;
        this.fileExtension = fileExtension;
        this.modRequired = modRequired;
        this.validationRequired = validationRequired;
    }

    public String getDescription() {
        return description;
    }
    public String getFileExtension() {
        return fileExtension;
    }
    public boolean isModRequired() {
        return modRequired;
    }
    public boolean isValidationRequired() {
        return validationRequired;
    }

    public static DataType fromString(String string) {
        for(DataType dt: DataType.values()) {
            if(dt.name().toLowerCase().equals(string.toLowerCase())) {
                return dt;
            }
        }
        return null;
    }

}