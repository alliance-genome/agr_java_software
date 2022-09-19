package org.alliancegenome.neo4j.entity;

import java.util.List;

import org.alliancegenome.neo4j.entity.node.AllianceReleaseInfo;
import org.alliancegenome.neo4j.entity.node.ModFileMetadata;
import org.alliancegenome.neo4j.entity.node.OntologyFileMetadata;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReleaseSummary {
	
	@JsonView({View.API.class})
	private AllianceReleaseInfo releaseInfo;
	@JsonView({View.API.class})
	private List<ModFileMetadata> metaData;
	@JsonView({View.API.class})
	private List<OntologyFileMetadata> ontologyMetaData;

}
