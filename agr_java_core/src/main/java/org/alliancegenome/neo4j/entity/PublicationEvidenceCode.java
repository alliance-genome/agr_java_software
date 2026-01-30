package org.alliancegenome.neo4j.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.view.PublicView;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicationEvidenceCode implements Comparable<PublicationEvidenceCode>, Serializable {

	@JsonView({ PublicView.Default.class, PublicView.API.class }) protected String id;
	@JsonView({ PublicView.Default.class, PublicView.API.class }) protected String name;
	@JsonView({ PublicView.Default.class, PublicView.API.class }) protected String displayName;
	@JsonView({ PublicView.Default.class, PublicView.API.class }) protected GeneticEntity.CrossReferenceType type;
	@JsonView({ PublicView.Default.class, PublicView.API.class }) protected CrossReference crossReference;
	@JsonView({ PublicView.PrimaryAnnotation.class, PublicView.DiseaseAnnotation.class }) protected List<DOTerm> diseases;
	@JsonView({ PublicView.DiseaseAnnotation.class }) private List<Publication> publications;
	@JsonView({ PublicView.DiseaseAnnotation.class })
	@JsonProperty(value = "evidenceCodes") private List<ECOTerm> ecoCodes;

	@Convert(value = DateConverter.class) private Date dateProduced;

	private List<DiseaseAnnotation> annotations;

	@JsonView({ PublicView.Default.class }) protected Species species;

	@Override
	public int compareTo(PublicationEvidenceCode o) {
		return 0;
	}

	@Override
	public String toString() {
		return id + ":" + name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PublicationEvidenceCode that = (PublicationEvidenceCode) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	public void addDisease(DOTerm disease) {
		if (diseases == null) {
			diseases = new ArrayList<>();
		}
		diseases.add(disease);
		diseases = new ArrayList<>(new HashSet<>(diseases));
	}
}
