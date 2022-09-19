package org.alliancegenome.neo4j.entity.node;

import java.util.List;

import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.Setter;

@NodeEntity(label = "GeneLevelConsequence")
@Getter
@Setter
@Schema(name = "GeneLevelConsequence", description = "POJO that represents Gene Level Consequences")
public class GeneLevelConsequence extends Neo4jEntity implements Comparable<GeneLevelConsequence> {

	@JsonView({View.Default.class, View.API.class,View.AlleleVariantSequenceConverterForES.class})
	@JsonProperty(value = "id")
	protected String primaryKey;

	@JsonView({View.Default.class, View.API.class,View.AlleleVariantSequenceConverterForES.class})
	@JsonProperty(value = "consequence")
	private String geneLevelConsequence;

	@Override
	public int compareTo(GeneLevelConsequence o) {
		return 0;
	}

	public List<String> getIndividualConsequences() {
		if (geneLevelConsequence == null)
			return null;
		return Lists.newArrayList(geneLevelConsequence.split("&"));
	}

}
