package org.alliancegenome.api.entity;

import java.util.Date;

import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;

@Data
@JsonView({View.API.class})
public class ReleaseInfoDocument extends ESDocument {

	private String category = "release_info";
	private String releaseVersion;
	private Date releaseDate;
	
	@Override
	public String getType() {
		return category;
	}
	
}
