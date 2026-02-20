package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.view.PublicView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity(label = "Transcript")
@Getter
@Setter
@Schema(name = "Transcript", description = "POJO that represents the Transcript")
public class Transcript extends Neo4jEntity implements Comparable<Transcript> {

	@JsonView({ PublicView.Default.class, PublicView.API.class, CurationView.VariantSummaryDocument.class })
	@JsonProperty(value = "id") protected String primaryKey;

	@JsonView({ PublicView.Default.class, PublicView.API.class, CurationView.VariantSummaryDocument.class }) protected String name;

	@Override
	public int compareTo(Transcript o) {
		return name.compareTo(o.getName());
	}

	@JsonView({ PublicView.Default.class, PublicView.API.class })
	@Relationship(type = "ASSOCIATION") private List<TranscriptLevelConsequence> consequences;

	@JsonView({ PublicView.Default.class, PublicView.API.class })
	@Relationship(type = "ASSOCIATION") private GenomeLocation genomeLocation;

	@JsonView({ PublicView.Default.class, PublicView.API.class, CurationView.VariantSummaryDocument.class })
	@Relationship(type = "TRANSCRIPT_TYPE", direction = Relationship.Direction.INCOMING) private SOTerm type;

	@JsonView({ PublicView.Default.class, PublicView.API.class })
	@Relationship(type = "TRANSCRIPT") private Gene gene;

	@Relationship(type = "EXON", direction = Relationship.Direction.INCOMING) private List<Exon> exons;

	@JsonView({ PublicView.Default.class, PublicView.API.class }) private String intronExonLocation;

	@Override
	public String toString() {
		return name;
	}
}
