package org.alliancegenome.es.model.search;

public enum Category {

    ALLELE("allele", true),
    DISEASE("disease", true),
    DISEASE_ANNOTATION("diseaseAnnotation", false),
    GENE("gene", true),
    GO("go", true);

    private String name;
    private Boolean searchable;

    private Category(String name, Boolean searchable) {
        this.name = name;
        this.searchable = searchable;
    }

    public String getName() {
        return name;
    }

    public Boolean isSearchable() {
        return searchable;
    }
}
