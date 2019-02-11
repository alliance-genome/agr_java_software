package org.alliancegenome.neo4j.entity.node;

import org.alliancegenome.core.service.SourceService;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@NodeEntity
@Getter
@Setter
public class DiseaseEntityJoin extends EntityJoin {

    @Relationship(type = "ASSOCIATION")
    private DOTerm disease;

    @Relationship(type = "FROM_ORTHOLOGOUS_GENE")
    private Gene orthologyGene;

    // Singular at the moment.
    // Make sure this is singular here
    // might turn into a collection i
    private String dataProvider;

    public Source getSource() {
        SourceService service = new SourceService();
        Optional<Source> first = service.getAllSources(disease).stream()
                .filter(source -> source.getSpeciesType().getDisplayName().equalsIgnoreCase(dataProvider))
                .findFirst();
        if(first.isPresent()) return first.get();
        Source source = new Source();
        source.setName(dataProvider);
        return source;
    }
}
