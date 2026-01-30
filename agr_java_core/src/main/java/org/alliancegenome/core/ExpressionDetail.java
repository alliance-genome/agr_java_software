package org.alliancegenome.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.MMOTerm;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.entity.node.Stage;
import org.alliancegenome.neo4j.view.PublicView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExpressionDetail implements Comparable, Serializable {

	@JsonView(PublicView.Expression.class)
	private Gene gene;
	@JsonView(PublicView.Expression.class)
	private String termName;
	@JsonView(PublicView.Expression.class)
	private Stage stage;
	@JsonView(PublicView.Expression.class)
	private MMOTerm assay;
	@JsonView(PublicView.Expression.class)
	private TreeSet<Publication> publications;
	@JsonView(PublicView.Expression.class)
	private String dataProvider;
	@JsonView(PublicView.Expression.class)
	private List<CrossReference> crossReferences;

	@JsonView(PublicView.Expression.class)
	private List<String> termIDs = new ArrayList<>(6);

	@JsonView(PublicView.Expression.class)
	private List<String> uberonTermIDs = new ArrayList<>(6);

	@JsonView(PublicView.Expression.class)
	private List<String> goTermIDs = new ArrayList<>(6);

	@JsonView(PublicView.Expression.class)
	private String stageTermID;

	public void addTermIDs(Collection<String> ids) {
		termIDs.addAll(ids);
	}

	public void addTermID(String primaryKey) {
		termIDs.add(primaryKey);
	}

	@Override
	public int compareTo(Object o) {
		return 0;
	}
}
