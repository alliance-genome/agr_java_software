package org.alliancegenome.neo4j.entity.node;

import java.util.List;
import java.util.stream.Collectors;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class PhenotypeEntityJoin extends EntityJoin {

    private String primaryKey;
    private String joinType;
    private String dataProvider;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Gene gene;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Allele allele;

    @Relationship(type = "ASSOCIATION")
    private Phenotype phenotype;

    @Relationship(type = "EVIDENCE")
    private List<PublicationJoin> phenotypePublicationJoins;

    public List<Publication> getPublications() {
        if (phenotypePublicationJoins == null)
            return null;
        return phenotypePublicationJoins.stream()
                .map(PublicationJoin::getPublication)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return primaryKey;
    }

    public Source getSource() {
        Source source = new Source();
        source.setName(dataProvider);
        return source;
    }

}
