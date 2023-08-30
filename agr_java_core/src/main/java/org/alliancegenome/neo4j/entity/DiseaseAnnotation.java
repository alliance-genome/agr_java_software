package org.alliancegenome.neo4j.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.PresentationEntity;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.ExperimentalCondition;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.entity.node.PublicationJoin;
import org.alliancegenome.neo4j.entity.node.Source;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"disease", "gene", "allele", "geneticEntityType", "associationType", "ecoCode", "source", "publications"})
@Schema(name = "DiseaseAnnotation", description = "POJO that represents a Disease Annotation")
public class DiseaseAnnotation extends ConditionAnnotation implements Comparable<DiseaseAnnotation>, Serializable, PresentationEntity {

	public static final String NOT_ASSOCIATION_TYPE = "not";

	@JsonView({View.DiseaseAnnotation.class})
	private String primaryKey;
	@JsonView({View.DiseaseAnnotation.class})
	private Source source;
	@JsonView({View.DiseaseAnnotation.class})
	private DOTerm disease;
	@JsonView({View.DiseaseAnnotation.class})
	private Gene gene;
	@JsonView({View.DiseaseAnnotation.class})
	private AffectedGenomicModel model;
	@JsonView({View.DiseaseAnnotationAll.class})
	private Gene orthologyGene;
	@JsonView({View.DiseaseAnnotation.class})
	@JsonProperty(value = "allele")
	private Allele feature;
	@JsonView({View.DiseaseAnnotation.class})
	private List<PrimaryAnnotatedEntity> primaryAnnotatedEntities;
	@JsonView({View.DiseaseAnnotation.class})
	private List<Reference> references;
	@JsonView({View.DiseaseAnnotation.class})
	
	// This attribute will go away and be replaced by publicationJoin objects that keep the pub/evCodes pairs
	private List<Publication> publications;
	// This attribute will go away and be replaced by publicationJoin objects that keep the pub/evCodes pairs
	@JsonView({View.DiseaseAnnotation.class})
	private List<ECOTerm> evidenceCodes;
	
	@JsonView({View.DiseaseAnnotation.class})
	private String associationType;
	@JsonView({View.DiseaseCacher.class})
	private int sortOrder;
	@JsonView({View.DiseaseAnnotation.class})
	private List<Gene> orthologyGenes;

	@JsonIgnore
	private List<PublicationJoin> publicationJoins;
	
	@JsonView({View.DiseaseAnnotation.class})
	private List<Map<String, CrossReference>> providers;

	transient boolean remove = false;
	
	public void addOrthologousGene(Gene gene) {
		if (orthologyGenes == null)
			orthologyGenes = new ArrayList<>();
		orthologyGenes.add(gene);
	}

	public void addOrthologousGenes(List<Gene> genes) {
		if (genes == null)
			return;
		if (orthologyGenes == null)
			orthologyGenes = new ArrayList<>();
		orthologyGenes.addAll(genes);
		orthologyGenes = orthologyGenes.stream()
				.distinct()
				.sorted(Comparator.comparing(Gene::getSymbol))
				.collect(Collectors.toList());
	}

	public void addPrimaryAnnotatedEntity(PrimaryAnnotatedEntity entity) {
		if (primaryAnnotatedEntities == null)
			primaryAnnotatedEntities = new ArrayList<>();
		if (!primaryAnnotatedEntities.contains(entity))
			primaryAnnotatedEntities.add(entity);
	}

	public void addPrimaryAnnotatedEntityDuplicate(PrimaryAnnotatedEntity entity) {
		if (primaryAnnotatedEntities == null)
			primaryAnnotatedEntities = new ArrayList<>();
		primaryAnnotatedEntities.add(entity);
	}

	public void addAllPrimaryAnnotatedEntities(List<PrimaryAnnotatedEntity> annotatedEntities) {
		if (annotatedEntities == null)
			return;
		if (primaryAnnotatedEntities == null)
			primaryAnnotatedEntities = new ArrayList<>();
		primaryAnnotatedEntities.addAll(annotatedEntities);
		primaryAnnotatedEntities = primaryAnnotatedEntities.stream()
				.distinct()
				.sorted(Comparator.comparing(PrimaryAnnotatedEntity::getName))
				.collect(Collectors.toList());
	}

	@JsonView({View.DiseaseCacher.class})
	// lists the agr_do slim parents
	private Set<String> parentIDs;

	@JsonIgnore
	public String getDocumentId() {
		return primaryKey;
	}

	@Override
	public int compareTo(DiseaseAnnotation doc) {
		return 0;
	}

	@JsonView({View.Default.class})
	public String getGeneticEntityType() {
		return feature != null ? "allele" : "gene";
	}

	public void setGeneticEntityType(String name) {
		//can be ignored as is it calculated from the existence from the feature attribute.
	}

	public DiseaseAnnotation() {
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DiseaseAnnotation that = (DiseaseAnnotation) o;
		return sortOrder == that.sortOrder &&
				Objects.equals(primaryKey, that.primaryKey) &&
				Objects.equals(source, that.source) &&
				Objects.equals(disease, that.disease) &&
				Objects.equals(gene, that.gene) &&
				Objects.equals(feature, that.feature) &&
				Objects.equals(references, that.references) &&
				Objects.equals(publications, that.publications) &&
				Objects.equals(evidenceCodes, that.evidenceCodes) &&
				Objects.equals(associationType, that.associationType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, disease, gene, feature, publications, evidenceCodes, associationType);
	}

	@Override
	public String toString() {
		String primaryKey = disease.getPrimaryKey() + " : ";
		if (gene != null)
			primaryKey += gene.getPrimaryKey();
		if (associationType != null)
			primaryKey += associationType;
		return primaryKey + " : " + getConditionSummary();
	}

	private String getConditionSummary() {
		StringBuilder builder = new StringBuilder();
		if (getConditions() != null) {
			getConditions().forEach((s, experimentalConditions) -> {
				builder.append(s + ":" + experimentalConditions.stream().map(ExperimentalCondition::getConditionStatement).reduce((c1, c2) -> c1 + "," + c2));
			});
		}
		return builder.toString();
	}


	public void addPublicationJoins(List<PublicationJoin> joins) {
		if (joins == null)
			return;
		if (publicationJoins == null)
			publicationJoins = new ArrayList<>();
		publicationJoins.addAll(joins);
		publicationJoins = publicationJoins.stream()
				.distinct()
				.collect(Collectors.toList());

		if (publications == null)
			publications = new ArrayList<>();
		publications.addAll(publicationJoins.stream()
				.map(PublicationJoin::getPublication)
				.distinct()
				.collect(Collectors.toList()));
		publications = publications.stream()
				.distinct()
				.sorted(Comparator.naturalOrder())
				.collect(Collectors.toList());
		
		evidenceCodes = publicationJoins.stream()
			.filter(publicationJoin -> CollectionUtils.isNotEmpty(publicationJoin.getEcoCode()))
			.map(PublicationJoin::getEcoCode)
			.flatMap(Collection::stream)
			.distinct()
			.collect(Collectors.toList());
	}

	public Species getSpecies() {
		if (gene != null)
			return gene.getSpecies();
		if (feature != null)
			return feature.getSpecies();
		return model.getSpecies();
	}

}
