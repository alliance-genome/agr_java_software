package org.alliancegenome.es.index.site.doclet;

import org.alliancegenome.es.index.ESDoclet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SourceDoclet extends ESDoclet {

    private SpeciesDoclet species;
    private String url;
    private String diseaseUrl;
    private String name;

    @Override
    public String toString() {
        return name;
    }
}
