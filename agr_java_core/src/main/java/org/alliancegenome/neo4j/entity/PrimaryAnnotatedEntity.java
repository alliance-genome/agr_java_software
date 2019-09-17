package org.alliancegenome.neo4j.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
public class PrimaryAnnotatedEntity implements Comparable<PrimaryAnnotatedEntity>, Serializable {

    @JsonView({View.Default.class, View.API.class})
    protected String id;
    @JsonView({View.Default.class, View.API.class})
    protected String name;
    @JsonView({View.Default.class, View.API.class})
    protected String displayName;
    @JsonView({View.Default.class, View.API.class})
    protected GeneticEntity.CrossReferenceType type;
    @JsonView({View.Default.class, View.API.class})
    protected CrossReference crossReference;
    @JsonView({View.PrimaryAnnotation.class})
    protected DOTerm disease;
    @Convert(value = DateConverter.class)
    private Date dateProduced;

    @JsonView({View.Default.class})
    protected Species species;

    @Override
    public int compareTo(PrimaryAnnotatedEntity o) {
        return 0;
    }


    @Override
    public String toString() {
        return id + ":" + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimaryAnnotatedEntity that = (PrimaryAnnotatedEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
