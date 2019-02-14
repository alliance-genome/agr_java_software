package org.alliancegenome.agr_submission.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.alliancegenome.agr_submission.BaseEntity;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity @ApiModel
@Getter @Setter @ToString
public class DataFile extends BaseEntity {

    @Id @GeneratedValue
    private Long id;
    private String s3Path;
    private String urlPath;
    private Date uploadDate = new Date();
    
    @ManyToOne
    private SchemaVersion schemaVersion;
    
    @ManyToOne
    private DataType dataType;
    
    @ManyToOne
    private DataSubType dataSubType;


}
