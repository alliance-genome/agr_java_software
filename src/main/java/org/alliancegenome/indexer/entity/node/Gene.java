package org.alliancegenome.indexer.entity.node;

import lombok.Getter;
import lombok.Setter;

import org.alliancegenome.indexer.entity.Neo4jEntity;
import org.alliancegenome.indexer.entity.relationship.GenomeLocation;
import org.alliancegenome.indexer.entity.relationship.Orthologous;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.RelationshipEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NodeEntity
@Getter
@Setter
public class Gene extends Neo4jEntity implements Comparable<Gene> {

	private String primaryKey;
	private String taxonId;
	private String geneLiterature;
	private String geneLiteratureUrl;
	private String geneSynopsis;
	private String geneSynopsisUrl;
	private String dataProvider;
	private String name;

	//private Date dateProduced;
	private String description;
	private String symbol;
	private String geneticEntityExternalUrl;

	// Used to be in modCrossReference
	private String modCrossRefCompleteUrl;
	private String localId;
	private String modGlobalCrossRefId;
	private String modGlobalId;
	
	private Entity createdBy;
	private SOTerm sOTerm;

	@Relationship(type = "FROM_SPECIES")
	private Species species;

	@Relationship(type = "ALSO_KNOWN_AS")
	private Set<Synonym> synonyms = new HashSet<>();

	@Relationship(type = "ALSO_KNOWN_AS")
	private Set<SecondaryId> secondaryIds = new HashSet<>();

	@Relationship(type = "ANNOTATED_TO")
	private Set<GOTerm> gOTerms = new HashSet<>();

	@Relationship(type = "IS_IMPLICATED_IN")
	private Set<DOTerm> dOTerms = new HashSet<>();
	
	@Relationship(type = "ORTHOLOGOUS", direction=Relationship.UNDIRECTED)
	private List<Orthologous> orthoGenes;

	@Relationship(type = "LOCATED_ON")
	private List<GenomeLocation> genomeLocations;

	@Relationship(type = "CROSS_REFERENCE")
	private List<CrossReference> crossReferences;
	
	@Relationship(type = "ASSOCIATION", direction=Relationship.UNDIRECTED)
	private List<OrthologyGeneJoin> orthologyGeneJoins;
	

	@Override
	public int compareTo(Gene gene) {
		if (gene == null)
			return -1;
		if (!species.equals(gene.species))
			return species.compareTo(gene.species);
		if (symbol == null && gene.getSymbol() == null)
			return 0;
		if (symbol == null)
			return 1;
		if (gene.symbol == null)
			return -1;
		return symbol.compareTo(gene.getSymbol());
	}

}
