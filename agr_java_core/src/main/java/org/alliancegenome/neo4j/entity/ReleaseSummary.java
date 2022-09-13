package org.alliancegenome.neo4j.entity;

import java.util.List;

import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@Getter @Setter
public class ReleaseSummary {
	
	@JsonView({View.API.class})
	private AllianceReleaseInfo releaseInfo;
	@JsonView({View.API.class})
	private List<ModFileMetadata> metaData;
	@JsonView({View.API.class})
	private List<OntologyFileMetadata> ontologyMetaData;

}
