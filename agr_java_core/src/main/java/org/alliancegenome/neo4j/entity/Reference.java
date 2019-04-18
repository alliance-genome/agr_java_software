package org.alliancegenome.neo4j.entity;

import java.io.Serializable;
import java.util.List;

import org.alliancegenome.neo4j.entity.node.EvidenceCode;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Reference implements Comparable<Reference>, Serializable {

    @JsonView({View.DiseaseAnnotation.class})
    private Publication publication;
    @JsonView({View.DiseaseAnnotation.class})
    private List<EvidenceCode> evidenceCodes;

    @Override
    public int compareTo(Reference o) {
        return publication.compareTo(o.getPublication());
    }
}
