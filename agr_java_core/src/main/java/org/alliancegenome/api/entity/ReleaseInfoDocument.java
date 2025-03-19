package org.alliancegenome.api.entity;

import java.util.Date;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;

@Data
@JsonView({View.API.class})
public class ReleaseInfoDocument extends ESDocument {
	{
		category = "release_info";
	}
	private String releaseVersion;
	private Date releaseDate;
}
