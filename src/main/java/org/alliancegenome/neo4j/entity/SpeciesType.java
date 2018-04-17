package org.alliancegenome.neo4j.entity;

import org.alliancegenome.es.index.site.doclet.SpeciesDoclet;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public enum SpeciesType {
    HUMAN("Homo sapiens", "HUMAN", "NCBITaxon:9606", "Hsa", "HUMAN", "Human", "9606"),
    RAT("Rattus norvegicus", "RGD", "NCBITaxon:10116", "Rno", "RGD", "Rat Genome Database", "10116"),
    MOUSE("Mus musculus", "MGI", "NCBITaxon:10090", "Mmu", "MGD", "Mouse Genome Database", "10090"),
    ZEBRAFISH("Danio rerio", "ZFIN", "NCBITaxon:7955", "Dre", "ZFIN", "Zebrafish Information Network", "7955"),
    FLY("Drosophila melanogaster", "FB", "NCBITaxon:7227", "Dme", "FB", "Fly Base", "7227"),
    WORM("Caenorhabditis elegans", "WB","NCBITaxon:6239", "Cel", "WB", "Worm Base", "6239"),
    YEAST("Saccharomyces cerevisiae", "SGD","NCBITaxon:4932", "Sce", "SGD", "Saccharomyces Genome Database", "4932");
    
    private String name;
    private String taxonID;
    private String displayName;
    private String abbreviation;
    private String modName;
    private String databaseName;
    private String taxonIDPart;

    public static SpeciesType getTypeByName(String name) {
        for (SpeciesType type : values())
            if (type.name.equals(name))
                return type;
        return null;
    }


    
    public static SpeciesDoclet fromModName(String modName) {
        for(SpeciesType species: SpeciesType.values()) {
            if(species.modName.equals(modName)) {
                return getDoclet(species);
            }
        }
        return null;
    }

    public static SpeciesDoclet fromTaxonIdPart(String taxonIDPart) {
        for(SpeciesType species: SpeciesType.values()) {
            if(species.taxonIDPart.equals(taxonIDPart)) {
                return getDoclet(species);
            }
        }
        return null;
    }

    public static SpeciesDoclet getByModNameOrIdPart(String string) {
        if(fromTaxonIdPart(string) != null) {
            return fromTaxonIdPart(string);
        }
        if(fromModName(string) != null) {
            return fromModName(string);
        }
        return null;
    }

    public SpeciesDoclet getDoclet() {
        return getDoclet(this);
    }
    
    public static SpeciesDoclet getDoclet(SpeciesType type) {
        SpeciesDoclet ret = new SpeciesDoclet();
        ret.setName(type.name);
        ret.setTaxonID(type.taxonID);
        ret.setDisplayName(type.displayName);
        ret.setAbbreviation(type.abbreviation);
        ret.setModName(type.modName);
        ret.setDatabaseName(type.databaseName);
        ret.setTaxonIDPart(type.taxonIDPart);
        return ret;
    }

}
