package org.alliancegenome.es.index.site.doclet;

import java.util.List;

import org.alliancegenome.es.index.ESDoclet;
import org.alliancegenome.neo4j.view.PublicView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class OrthologyDoclet extends ESDoclet {
	
	private String primaryKey;
	@JsonView(PublicView.Homology.class)
	private String isBestScore;
	@JsonView(PublicView.Homology.class)
	private String isBestRevScore;
	private String confidence;

	private String gene1Species;
	private String gene2Species;

	@JsonView(PublicView.Homology.class)
	private String gene1SpeciesName;
	@JsonView(PublicView.Homology.class)
	private String gene2SpeciesName;

	@JsonView(PublicView.Homology.class)
	private String gene1Symbol;
	@JsonView(PublicView.Homology.class)
	private String gene2Symbol;

	@JsonView(PublicView.Homology.class)
	private String gene1AgrPrimaryId;
	@JsonView(PublicView.Homology.class)
	private String gene2AgrPrimaryId;

	@JsonView(PublicView.Homology.class)
	private List<String> predictionMethodsNotCalled;
	@JsonView(PublicView.Homology.class)
	private List<String> predictionMethodsMatched;
	@JsonView(PublicView.Homology.class)
	private List<String> predictionMethodsNotMatched;

}
