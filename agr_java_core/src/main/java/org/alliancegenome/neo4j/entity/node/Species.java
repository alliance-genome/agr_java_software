package org.alliancegenome.neo4j.entity.node;

import java.util.HashSet;
import java.util.Set;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
@Schema(name="Species", description="POJO that represents the Species")
public class Species extends Neo4jEntity implements Comparable<Species> {

	@JsonView({View.Default.class,	View.AlleleVariantSequenceConverterForES.class})
	@JsonProperty(value = "taxonId")
	private String primaryKey;

	@JsonView({View.Default.class,	View.AlleleVariantSequenceConverterForES.class})
	private String name;
	@JsonView({View.Default.class,View.AlleleVariantSequenceConverterForES.class})
	private String shortName;
	@JsonView({View.Default.class,View.AlleleVariantSequenceConverterForES.class})
	private String dataProviderFullName;
	@JsonView({View.Default.class,View.AlleleVariantSequenceConverterForES.class})
	private String dataProviderShortName;
	
	@JsonView({View.DiseaseCacher.class, View.Orthology.class})
	private int phylogeneticOrder;

	@JsonView({View.Default.class,View.AlleleVariantSequenceConverterForES.class})
	private String commonNames;

	@Relationship(type = "CREATED_BY")
	private Set<Gene> genes = new HashSet<>();

	public static Species getSpeciesFromTaxonId(String taxonID) {
		for (SpeciesType species : SpeciesType.values()) {
			if (species.getTaxonID().equals(taxonID)) {
				Species spec = new Species();
				spec.setPrimaryKey(taxonID); // Needed for the download file
				spec.setName(species.getName());
				return spec;
			}
		}
		return null;
	}

	@Override
	public int compareTo(Species species) {
		return getType().compareTo(species.getType());
	}

	public SpeciesType getType() {
		return SpeciesType.getTypeByName(this.name);
	}

	@Override
	public String toString() {
		return primaryKey + " : " + name;
	}
}
