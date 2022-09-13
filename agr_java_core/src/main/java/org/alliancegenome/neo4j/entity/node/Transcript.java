package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@NodeEntity(label = "Transcript")
@Getter
@Setter
@Schema(name = "Transcript", description = "POJO that represents the Transcript")
public class Transcript extends Neo4jEntity implements Comparable<Transcript> {

	@JsonView({View.Default.class, View.API.class,View.AlleleVariantSequenceConverterForES.class})
	@JsonProperty(value = "id")
	protected String primaryKey;

	@JsonView({View.Default.class, View.API.class,View.AlleleVariantSequenceConverterForES.class})
	protected String name;

	@Override
	public int compareTo(Transcript o) {
		return name.compareTo(o.getName());
	}

	@JsonView({View.Default.class, View.API.class})
	@Relationship(type = "ASSOCIATION")
	private List<TranscriptLevelConsequence> consequences;

	@JsonView({View.Default.class, View.API.class})
	@Relationship(type = "ASSOCIATION")
	private GenomeLocation genomeLocation;

	@JsonView({View.Default.class, View.API.class,View.AlleleVariantSequenceConverterForES.class})
	@Relationship(type = "TRANSCRIPT_TYPE", direction = Relationship.INCOMING)
	private SOTerm type;

	@JsonView({View.Default.class, View.API.class})
	@Relationship(type = "TRANSCRIPT")
	private Gene gene;

	@Relationship(type = "EXON", direction = Relationship.INCOMING)
	private List<Exon> exons;

	@JsonView({View.Default.class, View.API.class})
	private String intronExonLocation;

	@Override
	public String toString() {
		return name;
	}
}
