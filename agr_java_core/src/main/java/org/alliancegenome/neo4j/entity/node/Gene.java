package org.alliancegenome.neo4j.entity.node;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;
import org.alliancegenome.neo4j.entity.relationship.Paralogous;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
@Schema(name="Gene", description="POJO that represents the Gene")
public class Gene extends GeneticEntity implements Comparable<Gene> {

	public Gene() {
		this.crossReferenceType = CrossReferenceType.GENE;
	}

	@JsonView({View.Orthology.class, View.Expression.class})
	private String taxonId;

	@JsonView({View.GeneAPI.class})
	private String geneSynopsis;

	@JsonView({View.GeneAPI.class})
	private String automatedGeneSynopsis;

	@JsonView({View.GeneAPI.class})
	private String geneSynopsisUrl;

	@JsonView({View.GeneAPI.class, View.Expression.class})
	private String dataProvider;

	@JsonView(value = {View.GeneAPI.class, View.AlleleVariantSequenceConverterForES.class})
	private String name;

	@Convert(value = DateConverter.class)
	@JsonView(value = {View.GeneAPI.class})
	private Date dateProduced;

	private String description;

	private String geneticEntityExternalUrl;
	private String modLocalId;
	private String modGlobalCrossRefId;
	private String modGlobalId;
	private Entity createdBy;

	@JsonView(value = {View.GeneAPI.class})
	private SOTerm soTerm;

	@Relationship(type = "ANNOTATED_TO")
	private Set<GOTerm> goTerms;

	@Relationship(type = "ORTHOLOGOUS")
	private List<Orthologous> orthoGenes;

	@Relationship(type = "PARALOGOUS")
	private List<Paralogous> paraGenes;

	@Relationship(type = "ASSOCIATION")
	@JsonView({View.GeneAPI.class, View.VariantAPI.class, View.AlleleAPI.class,View.AlleleVariantSequenceConverterForES.class})
	private List<GenomeLocation> genomeLocations;

	@Relationship(type = "LOCATED_ON")
	private List<Chromosome> chromsomes;

	@Relationship(type = "IS_ALLELE_OF", direction = Relationship.Direction.INCOMING)
	@JsonView(value = {View.GeneAPI.class})
	private List<Allele> alleles;

	@Relationship(type = "ASSOCIATION", direction = Relationship.Direction.UNDIRECTED)
	private List<DiseaseEntityJoin> diseaseEntityJoins;

	@Relationship(type = "ASSOCIATION", direction = Relationship.Direction.UNDIRECTED)
	private List<BioEntityGeneExpressionJoin> entityGeneExpressionJoins;

	@Relationship(type = "ASSOCIATION")
	private List<PhenotypeEntityJoin> phenotypeEntityJoins;

	@Relationship(type = "ASSOCIATION", direction = Relationship.Direction.UNDIRECTED)
	private List<OrthologyGeneJoin> orthologyGeneJoins;

	@Relationship(type = "HAS_PHENOTYPE")
	private List<Phenotype> phenotypes;

	@Relationship(type = "ASSOCIATION")
	private List<InteractionGeneJoin> interactions;

	@Relationship(type = "EXPRESSED_IN")
	private List<ExpressionBioEntity> expressionBioEntities;

	public String getNameKey() {
		return getNameKey(species.getType());
	}
	
	@JsonIgnore
	public String getNameKey(SpeciesType speciesType) {
		StringBuffer buffer = new StringBuffer();
		if (species != null) {
			buffer.append(symbol);
			buffer.append(" (");
			buffer.append(speciesType.getAbbreviation());
			buffer.append(")");
		}
		return buffer.toString();
	}

	public Set<GOTerm> getGoParentTerms() {
		Set<GOTerm> parentTerms = new HashSet<>();
		CollectionUtils.emptyIfNull(goTerms).forEach(term -> parentTerms.addAll(term.getParentTerms()));
		return parentTerms;
	}

	@Override
	public int compareTo(Gene gene) {
		if (gene == null)
			return -1;
		if (species == null && gene.getSpecies() != null)
			return -1;
		if (species != null && gene.getSpecies() == null)
			return 1;
		if (species != null && gene.getSpecies() != null && !species.equals(gene.species))
			return species.compareTo(gene.species);
		if (symbol == null && gene.getSymbol() == null)
			return 0;
		if (symbol == null)
			return 1;
		if (gene.symbol == null)
			return -1;
		return symbol.compareTo(gene.getSymbol());
	}

	@Override
	public String toString() {
		return primaryKey + ", " + symbol;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Gene gene = (Gene) o;
		return Objects.equals(primaryKey, gene.primaryKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(primaryKey);
	}


}
