package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
@Getter
@Setter
public class Publication extends Neo4jEntity implements Comparable<Publication> {

    @JsonView({View.Interaction.class, View.Expression.class})
    private String primaryKey;
    @JsonView({View.Phenotype.class,View.Interaction.class, View.Expression.class})
    private String pubMedId;
    @JsonView({View.Phenotype.class,View.Interaction.class, View.Expression.class})
    private String pubMedUrl;
    @JsonView({View.Phenotype.class,View.Interaction.class, View.Expression.class})
    private String pubModId;
    @JsonView({View.Phenotype.class,View.Interaction.class, View.Expression.class})
    private String pubModUrl;
    @JsonView({View.Phenotype.class, View.Expression.class})
    private String pubId;

    @Relationship(type = "ANNOTATED_TO")
    private List<EvidenceCode> evidence;

    public void setPubIdFromId() {
        if (StringUtils.isNotEmpty(pubMedId))
            pubId = pubMedId;
        else
            pubId = pubModId;
    }

    @Override
    public String toString() {
        return primaryKey;
    }

    @Override
    public int compareTo(Publication o) {
        return pubId.compareTo(o.getPubId());
    }
}
