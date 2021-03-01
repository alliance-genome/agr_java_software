package org.alliancegenome.api.entity;

public enum  JoinTypeValue {
    genetic_interaction("genetic_interaction"), 
    molecular_interaction("molecular_interaction"),
    ;
    private String name="molecular_interaction";
    
    JoinTypeValue(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
