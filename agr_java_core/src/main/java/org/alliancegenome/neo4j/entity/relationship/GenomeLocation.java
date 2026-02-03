package org.alliancegenome.neo4j.entity.relationship;

import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.PublicView;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity(label = "GenomicLocation")
@Getter
@Setter
public class GenomeLocation extends Neo4jEntity {

	@JsonView({PublicView.Default.class, CurationView.VariantDocument.class})
	private String chromosome;

	@JsonView({PublicView.Default.class, CurationView.VariantDocument.class})
	private Long start;

	@JsonView({PublicView.Default.class, CurationView.VariantDocument.class})
	private Long end;

	@JsonView({PublicView.Default.class})
	private String assembly;

	@JsonView({PublicView.Default.class})
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
