package org.alliancegenome.api.enums;

public enum Mod {
    FB("Fly Base"),
    Human("Human"),
    MGD("Mouse Genome Database"),
    RGD("Rat Genome Database"),
    SGD("Saccharomyces Genome Database"),
    WB("Worm Base"),
    ZFIN("Zebrafish Information Network"),
    ;

    private String description;

    private Mod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public static Mod fromString(String string) {
        for(Mod mod: Mod.values()) {
            if(mod.name().toLowerCase().equals(string.toLowerCase())) {
                return mod;
            }
        }
        return null;
    }
}
