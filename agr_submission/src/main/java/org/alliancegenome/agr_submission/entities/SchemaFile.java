package org.alliancegenome.agr_submission.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.alliancegenome.agr_submission.BaseEntity;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

@Entity @ApiModel
@Getter @Setter
public class SchemaFile extends BaseEntity {

    @Id @GeneratedValue
    private Long id;
    private String filePath;
    
    @ManyToOne
    private SchemaVersion schemaVersion;
    
    @ManyToOne
    private DataType dataType;

}
