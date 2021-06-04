package org.alliancegenome.neo4j.entity.node;

import java.util.Date;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@NodeEntity
@Getter
@Setter
@Schema(name = "AllianceReleaseInfo", description = "POJO that represents the Allele")
public class AllianceReleaseInfo extends Neo4jEntity {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@JsonView({View.API.class})
    //@Convert(value = DateConverter.class)
    private Date releaseDate;
    @JsonView({View.API.class})
    private String releaseVersion;
    @JsonView({View.API.class})
    //@Convert(value = DateConverter.class)
    private Date snapShotDate;

}
