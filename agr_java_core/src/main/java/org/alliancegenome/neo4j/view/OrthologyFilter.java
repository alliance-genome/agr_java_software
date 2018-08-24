package org.alliancegenome.neo4j.view;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.SpeciesType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
public class OrthologyFilter {
    private Stringency stringency;
    private List<String> taxonIDs;
    private List<String> methods;
    private int rows = 1000000;
    private int start;

    public OrthologyFilter() {
        this.stringency = Stringency.ALL;
    }

    public OrthologyFilter(String stringency, List<String> taxonIDs, List<String> methods) {
        this.stringency = Stringency.getOrthologyFilter(stringency);
        if (taxonIDs != null && !taxonIDs.isEmpty()) {
            this.taxonIDs = taxonIDs.stream()
                    .map(SpeciesType::getTaxonId)
                    .collect(Collectors.toList());
        }
        if (methods != null && !methods.isEmpty())
            this.methods = methods;
    }

    public int getLast() {
        return start + rows;
    }

    public void resetPaginationData(){
        start = 1;
        rows = 20;
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

        public Boolean isStrict() {
            return this.equals(STRINGENT);
        }

        public Boolean isModerate() {
            return this.equals(MODERATE);
        }
    }
}