package org.alliancegenome.neo4j.entity.node;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class Gene extends Neo4jEntity implements Comparable<Gene> {

    @JsonView({View.GeneAPI.class, View.Orthology.class, View.Interaction.class, View.Expression.class, View.Phenotype.class})
    @JsonProperty(value="id")
    private String primaryKey;
    
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
    
    @JsonView(value={View.GeneAPI.class})
    private String name;

    @Convert(value = DateConverter.class)
    @JsonView(value={View.GeneAPI.class})
    private Date dateProduced;
    
    private String description;
    
    @JsonView({View.GeneAPI.class, View.Orthology.class, View.Interaction.class, View.Expression.class})
    private String symbol;
    
    private String geneticEntityExternalUrl;
    private String modCrossRefCompleteUrl;
    private String modLocalId;
    private String modGlobalCrossRefId;
    private String modGlobalId;
    private Entity createdBy;
    
    @JsonView(value={View.GeneAPI.class})
    private SOTerm soTerm;

    @Relationship(type = "FROM_SPECIES")
    @JsonView(value={View.GeneAPI.class})
    private Species species;

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<Synonym> synonyms = new HashSet<>();
    
    // Converts the list of synonym objects to a list of strings
    @JsonView(value={View.GeneAPI.class})
    @JsonProperty(value="synonyms")
    public List<String> getSynonymList() {
        List<String> list = new ArrayList<String>();
        for(Synonym s: synonyms) {
            list.add(s.getName());
        }
        return list;
    }

    @Relationship(type = "ALSO_KNOWN_AS")
    private Set<SecondaryId> secondaryIds = new HashSet<>();

    @Relationship(type = "ANNOTATED_TO")
    private Set<GOTerm> goTerms = new HashSet<>();

    @Relationship(type = "ORTHOLOGOUS")
    private List<Orthologous> orthoGenes = new ArrayList<>();

    @Relationship(type = "LOCATED_ON")
    @JsonView({View.GeneAPI.class})
    private List<GenomeLocation> genomeLocations;

    @Relationship(type = "CROSS_REFERENCE")
    @JsonView({View.GeneAPI.class})
    private List<CrossReference> crossReferences;
    
    @JsonView({View.GeneAPI.class})
    @JsonProperty(value="crossReferences")
    public Map<String, Object> getCrossReferenceMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        
        List<CrossReference> othersList = new ArrayList<CrossReference>();
        map.put("other", othersList);
        for(CrossReference cr: crossReferences) {
            String type = "gene";
            if(cr.getCrossRefType().startsWith(type + "/")) {
                type = cr.getCrossRefType().replace(type + "/", "");
                map.put(type, cr);
            } else if(cr.getCrossRefType().equals(type)) {
                map.put("primary", cr);
            } else if(cr.getCrossRefType().equals("generic_cross_reference")) {
                othersList.add(cr);
            }
        }
        return map;
    }

    @Relationship(type = "IS_ALLELE_OF", direction = Relationship.INCOMING)
    @JsonView(value={View.GeneAllelesAPI.class})
    private List<Allele> alleles;

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

    public String getNameKey() {
        String nameKey = symbol;
        if (species != null) {
            nameKey += " (" + species.getType().getAbbreviation() + ")";
        }
        return nameKey;
    }

    public Set<GOTerm> getGoParentTerms() {
        Set<GOTerm> parentTerms = new HashSet<>();
        CollectionUtils.emptyIfNull(goTerms).forEach(term -> {
            parentTerms.addAll(term.getParentTerms());
        });
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
}
