package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class PublicationJoin extends Association {

    @JsonView({View.API.class})
    protected String primaryKey;
    protected String joinType;

    @JsonView({View.API.class})
    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private Publication publication;

    // annotation inferred on
    @Relationship(type = "PRIMARY_GENETIC_ENTITY")
    private List<AffectedGenomicModel> models;

    @Override
    public String toString() {
        return publication.getPubId();
        // return model != null ? publication.getPubId() + ":" + model.getName() : publication.getPubId();
    }
}
