package org.alliancegenome.neo4j.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.alliancegenome.es.index.site.doclet.SpeciesDoclet;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum SpeciesType {
    HUMAN("Homo sapiens", "HUMAN", "NCBITaxon:9606", "Hsa", "HUMAN", "Human", "9606", 0),
    RAT("Rattus norvegicus", "RGD", "NCBITaxon:10116", "Rno", "RGD", "Rat Genome Database", "10116", 1),
    MOUSE("Mus musculus", "MGI", "NCBITaxon:10090", "Mmu", "MGD", "Mouse Genome Database", "10090", 2),
    ZEBRAFISH("Danio rerio", "ZFIN", "NCBITaxon:7955", "Dre", "ZFIN", "Zebrafish Information Network", "7955", 3),
    FLY("Drosophila melanogaster", "FB", "NCBITaxon:7227", "Dme", "FB", "Fly Base", "7227", 4),
    WORM("Caenorhabditis elegans", "WB", "NCBITaxon:6239", "Cel", "WB", "Worm Base", "6239", 5),
    YEAST("Saccharomyces cerevisiae", "SGD", "NCBITaxon:559292", "Sce", "SGD", "Saccharomyces Genome Database", "559292", 6);

    public static final String NCBITAXON = "NCBITaxon:";
    private String name;
    private String displayName;
    private String taxonID;
    private String abbreviation;
    private String modName;
    private String databaseName;
    private String taxonIDPart;
    private int orderID;

    public static SpeciesType getTypeByName(String name) {
        for (SpeciesType type : values())
            if (type.name.equals(name))
                return type;
        return null;
    }

    public static SpeciesType getTypeByPartialName(String name) {
        List<SpeciesType> species = Arrays.stream(values())
                .filter(type -> type.name.toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
        return species != null && species.size() == 1 ? species.get(0) : null;
    }


    public static SpeciesDoclet fromModName(String modName) {
        for (SpeciesType species : SpeciesType.values()) {
            if (species.modName.equals(modName)) {
                return getDoclet(species);
            }
        }
        return null;
    }

    public static SpeciesDoclet fromTaxonIdPart(String taxonIDPart) {
        for (SpeciesType species : SpeciesType.values()) {
            if (species.taxonIDPart.equals(taxonIDPart)) {
                return getDoclet(species);
            }
        }
        return null;
    }

    public static SpeciesDoclet fromTaxonId(String taxonID) {
        for (SpeciesType species : SpeciesType.values()) {
            if (species.taxonID.equals(taxonID)) {
                return getDoclet(species);
            }
        }
        return null;
    }

    public static SpeciesDoclet getByModNameOrIdPart(String string) {
        if (fromTaxonIdPart(string) != null) {
            return fromTaxonIdPart(string);
        }
        if (fromModName(string) != null) {
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
        ret.setOrderID(type.orderID);
        return ret;
    }

    public static String getTaxonId(String species) {
        if (species == null)
            return null;
        // return name if it already is the full taxon ID
        if (species.startsWith(NCBITAXON))
            return species;
        // if only a number is provided then prefix it with taxon...
        if (StringUtils.isNumeric(species))
            return NCBITAXON + species;
        SpeciesType typeByName = getTypeByName(species);
        if (typeByName != null)
            return typeByName.getTaxonID();
        typeByName = getTypeByPartialName(species);
        if (typeByName != null)
            return typeByName.getTaxonID();
        return species;
    }
}
