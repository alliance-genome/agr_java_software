package org.alliancegenome.neo4j.entity.node;

import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.util.DateConverter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.util.Date;

@NodeEntity(label = "AffectedGenomicModel")
@Getter
@Setter
public class AffectedGenomicModel extends GeneticEntity {

    private String name;
    private String localId;
    private String globalId;
    private String modCrossRefCompleteUrl;

}
