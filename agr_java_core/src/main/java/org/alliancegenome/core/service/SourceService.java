package org.alliancegenome.core.service;

import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Source;

public class SourceService {


    public Source getSource(SpeciesType type, String link) {
        Source source = new Source();
        source.setUrl(link);
        source.setName(type.getDisplayName());
        return source;
    }
}
