package org.alliancegenome.neo4j.entity.node;

import java.util.*;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.*;

import lombok.*;

@NodeEntity
@Getter
@Setter
@Schema(name="HTPDataset", description="POJO that represents a High Throughput Dataset")
public class HTPDataset extends Neo4jEntity {

    private String primaryKey;
    private String summary;
    private String title;
    private String crossRefCompleteUrl;

    @Relationship(type = "CROSS_REFERENCE")
    protected List<CrossReference> crossReferences = new ArrayList<>();

}
