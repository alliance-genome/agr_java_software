package org.alliancegenome.agr_submission.entities;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.alliancegenome.agr_submission.BaseEntity;
import org.alliancegenome.agr_submission.views.View;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity @ApiModel
@Getter @Setter @ToString
public class SchemaVersion extends BaseEntity {

    @Id @GeneratedValue
    @JsonView({View.SchemaVersionView.class, View.ReleaseVersionView.class, View.DataFileView.class})
    private Long id;
    @JsonView({View.SchemaVersionView.class, View.ReleaseVersionView.class, View.DataFileView.class})
    private String schema;
    
    @ManyToMany(mappedBy="schemaVersions")
    private List<ReleaseVersion> releaseVersions;

}
