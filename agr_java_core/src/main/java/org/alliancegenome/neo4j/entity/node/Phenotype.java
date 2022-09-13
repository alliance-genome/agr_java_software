package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@NodeEntity
@Getter
@Setter
@Schema(name="Phenotype", description="POJO that represents the Phenotype")
public class Phenotype extends Neo4jEntity {
	@JsonView({View.PhenotypeAPI.class, View.Interaction.class})
	private String primaryKey;
	@JsonView({View.AlleleAPI.class, View.Interaction.class})
	private String phenotypeStatement;

	@Relationship(type = "IS_IMPLICATED_IN", direction = Relationship.INCOMING)
	private List<Gene> genes;

	@Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
	private List<PhenotypeEntityJoin> phenotypeEntityJoins;

	@Override
	public String toString() {
		return phenotypeStatement;
	}

	public boolean equalsPhenotype(Phenotype phenotype) {
		return phenotypeStatement.equals(phenotype.getPhenotypeStatement());
	}
}
