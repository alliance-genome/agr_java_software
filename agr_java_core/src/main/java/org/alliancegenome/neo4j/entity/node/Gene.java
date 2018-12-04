package org.alliancegenome.neo4j.entity.node;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class Gene extends Neo4jEntity implements Comparable<Gene> {

    @JsonView({View.OrthologyView.class, View.InteractionView.class, View.ExpressionView.class})
    @JsonProperty("geneID")
    private String primaryKey;
    @JsonView({View.OrthologyView.class, View.ExpressionView.class})
    private String taxonId;
    @JsonView({View.OrthologyView.class, View.ExpressionView.class})
    private String speciesName;
    private String geneLiterature;
    private String geneLiteratureUrl;
    private String geneSynopsis;
    private String automatedGeneSynopsis;
    private String geneSynopsisUrl;
    @JsonView({View.ExpressionView.class})
    private String dataProvider;
    private String name;

    @Convert(value = DateConverter.class)
    private Date dateProduced;
    private String description;
    @JsonView({View.OrthologyView.class, View.InteractionView.class, View.ExpressionView.class})
    private String symbol;
    private String geneticEntityExternalUrl;

    private String modCrossRefCompleteUrl;
    private String modLocalId;
    private String modGlobalCrossRefId;
    private String modGlobalId;

    private Entity createdBy;
    private SOTerm sOTerm;

    @JsonView({View.InteractionView.class})
    @Relationship(type = "FROM_SPECIES")
    private Species species;

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<Synonym> synonyms = new HashSet<>();

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<SecondaryId> secondaryIds = new HashSet<>();

    @Relationship(type = "ANNOTATED_TO")
    private Set<GOTerm> gOTerms = new HashSet<>();

    @Relationship(type = "ORTHOLOGOUS")
    private List<Orthologous> orthoGenes = new ArrayList<>();

    @Relationship(type = "LOCATED_ON")
    private List<GenomeLocation> genomeLocations;

    @Relationship(type = "CROSS_REFERENCE")
    private List<CrossReference> crossReferences;

    @Relationship(type = "IS_ALLELE_OF", direction = Relationship.INCOMING)
    private List<Feature> features;

    @Relationship(type = "ASSOCIATION", direction = Relationship.UNDIRECTED)
    private List<DiseaseEntityJoin> diseaseEntityJoins = new ArrayList<>();

    @Relationship(type = "ASSOCIATION", direction = Relationship.UNDIRECTED)
    private List<BioEntityGeneExpressionJoin> entityGeneExpressionJoins = new ArrayList<>();

    @Relationship(type = "ASSOCIATION")
    private List<PhenotypeEntityJoin> phenotypeEntityJoins = new ArrayList<>();

    @Relationship(type = "ASSOCIATION", direction = Relationship.UNDIRECTED)
    private List<OrthologyGeneJoin> orthologyGeneJoins = new ArrayList<>();

    @Relationship(type = "HAS_PHENOTYPE")
    private List<Phenotype> phenotypes = new ArrayList<>();
    
    @Relationship(type = "ASSOCIATION")
    private List<InteractionGeneJoin> interactions = new ArrayList<>();

    @Relationship(type = "EXPRESSED_IN")
    private List<ExpressionBioEntity> expressionBioEntities = new ArrayList<>();

    //GeneDocument push-throughs, these fields can be removed from the
    //Gene object when we refactor the indexing to have direct access to
    //repository methods

    private Set<String> biologicalProcessWithParents = new HashSet<>();
    private Set<String> biologicalProcessAgrSlim = new HashSet<>();
    private Set<String> cellularComponentWithParents = new HashSet<>();
    private Set<String> cellularComponentAgrSlim = new HashSet<>();
    private Set<String> molecularFunctionWithParents = new HashSet<>();
    private Set<String> molecularFunctionAgrSlim = new HashSet<>();

    private Set<String> cellularComponentExpressionWithParents = new HashSet<>();
    private Set<String> cellularComponentExpressionAgrSlim = new HashSet<>();

    private Set<String> whereExpressed = new HashSet<>();
    private Set<String> anatomicalExpression = new HashSet<>();         //uberon slim
    private Set<String> anatomicalExpressionWithParents = new HashSet<>();

    private Set<String> phenotypeStatements = new HashSet<>();

    public String getNameKey() {
        String nameKey = symbol;
        if (species != null) {
            nameKey += " (" + species.getType().getAbbreviation() + ")";
        }
        return nameKey;
    }

    public Set<GOTerm> getGoParentTerms() {
        Set<GOTerm> parentTerms = new HashSet<>();
        CollectionUtils.emptyIfNull(gOTerms).forEach(term -> {
            parentTerms.addAll(term.getParentTerms());
        });
        return parentTerms;
    }

    public void setSpecies(Species species) {
        this.species = species;
        this.speciesName = species.getName();
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
}
