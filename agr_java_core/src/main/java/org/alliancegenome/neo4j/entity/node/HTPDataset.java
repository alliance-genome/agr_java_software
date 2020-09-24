package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

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
