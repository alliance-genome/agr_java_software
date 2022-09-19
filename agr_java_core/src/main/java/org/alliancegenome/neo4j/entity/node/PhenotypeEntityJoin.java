package org.alliancegenome.neo4j.entity.node;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
@Schema(name="PhenotypeEntityJoin", description="POJO that represents the Phenotype Entity join")
public class PhenotypeEntityJoin extends EntityJoin {

	private String primaryKey;
	private String dataProvider;

	@Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
	private Gene gene;

	@Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
	private Allele allele;

	@Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
	private AffectedGenomicModel model;

	@Relationship(type = "ASSOCIATION")
	private Phenotype phenotype;

	public List<Publication> getPublications() {
		if (publicationJoins == null)
			return null;
		return publicationJoins.stream()
				.map(PublicationJoin::getPublication)
				.collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return primaryKey;
	}

	public Source getSource() {
		Source source = new Source();
		source.setName(dataProvider);
		return source;
	}

}
