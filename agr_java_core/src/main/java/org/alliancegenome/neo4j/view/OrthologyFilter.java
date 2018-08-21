package org.alliancegenome.neo4j.view;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Setter
@Getter
public class OrthologyFilter {
    private Stringency stringency;
    private List<String> species;
    private List<String> methods;
    private int rows;
    private int start;

    public OrthologyFilter() {
        this.stringency = Stringency.ALL;
    }

    public OrthologyFilter(String stringency, String species, String methods) {
        this.stringency = Stringency.getOrthologyFilter(stringency);
        if (species != null && !species.isEmpty())
            this.species = Arrays.asList(species.split(","));
        if (methods != null && !methods.isEmpty())
            this.methods = Arrays.asList(methods.split(","));
    }

    public int getLast() {
        return start + rows;
    }

    public enum Stringency {
        ALL(""),
        STRINGENT("stringent"),
        MODERATE("moderate");

        private String name;

        Stringency(String name) {
            this.name = name;
        }

        public static Stringency getOrthologyFilter(String name) {
            if (name == null || name.isEmpty() || name.equalsIgnoreCase("all"))
                return ALL;
            if (name.trim().equalsIgnoreCase(STRINGENT.name))
                return STRINGENT;
            if (name.trim().equalsIgnoreCase(MODERATE.name))
                return MODERATE;
            return null;
        }
    }
}