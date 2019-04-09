package org.alliancegenome.agr_submission.main.migrationmodels;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "_type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ESDataFile.class, name = "data_file"),
    @JsonSubTypes.Type(value = ESSnapshot.class, name = "data_snapshot"),
    @JsonSubTypes.Type(value = ESMetaData.class, name = "meta_data"),
})
public abstract class ESHit {
    private String _id;
    private String _index;
    
    public abstract void generateAPICalls();
    
}