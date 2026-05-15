package org.alliancegenome.neo4j.entity.node;

import java.util.Date;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.view.PublicView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
@Schema(name = "AllianceReleaseInfo", description = "POJO that represents the Allele")
public class AllianceReleaseInfo extends Neo4jEntity {

	/**
	 * 
	 */
	@JsonView({PublicView.API.class})
	@Convert(value = DateConverter.class)
	private Date releaseDate;
	@JsonView({PublicView.API.class})
	private String releaseVersion;
	@JsonView({PublicView.API.class})
	@Convert(value = DateConverter.class)
	private Date snapShotDate;

}
