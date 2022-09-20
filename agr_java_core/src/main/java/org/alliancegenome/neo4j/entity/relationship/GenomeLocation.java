package org.alliancegenome.neo4j.entity.relationship;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.node.Chromosome;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity(label = "GenomicLocation")
@Getter
@Setter
public class GenomeLocation extends Neo4jEntity {

	@JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
	private String chromosome;

	@JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
	private Long start;

	@JsonView({View.Default.class, View.AlleleVariantSequenceConverterForES.class})
	private Long end;

	@JsonView({View.Default.class})
	private String assembly;

	@JsonView({View.Default.class})
	private String strand;

	public String getChromosomeAndPosition() {
		String response = chromosome;
		response += ":";
		if (start != null) {
			response += start;
		}
		if (end != null) {
			response += "-";
			response += end;
		}
		return response;
	}
}
