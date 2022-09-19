package org.alliancegenome.neo4j.entity.node;

import java.util.Objects;

import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
@JsonPropertyOrder({"id", "name", "definition"})
public class ZECOTerm extends SimpleTerm implements Comparable<ZECOTerm> {

	@JsonView({View.DiseaseAPI.class})
	private String definition;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ZECOTerm ecoTerm = (ZECOTerm) o;
		return Objects.equals(primaryKey, ecoTerm.primaryKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(definition);
	}

	@Override
	public int compareTo(ZECOTerm o) {
		return name.compareTo(o.name);
	}
}
