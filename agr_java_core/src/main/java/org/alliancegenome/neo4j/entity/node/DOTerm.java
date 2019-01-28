package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.core.service.SourceService;
import org.alliancegenome.es.util.DateConverter;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.view.View;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.util.*;
import java.util.stream.Collectors;

@NodeEntity
@Getter
@Setter
public class DOTerm extends SimpleTerm {

    public static final String HIGH_LEVEL_TERM_LIST_SLIM = "DO_AGR_slim";

    private String doUrl;
    private String doDisplayId;
    @JsonView({View.DiseaseAPI.class})
    private String doId;
    private String doPrefix;
    @JsonView({View.DiseaseAPI.class})
    private String definition;
    @JsonView({View.DiseaseAPI.class})
    @JsonProperty(value = "definitionLinks")
    private List<String> defLinks;
    private List<String> subset;

    private String nameKey;
    private String is_obsolete;

    @Convert(value = DateConverter.class)
    private Date dateProduced;

    private String zfinLink;
    private String humanLink;
    private String rgdLink;
    private String sgdLink;
    private String ratOnlyRgdLink;
    private String humanOnlyRgdLink;
    private String wormbaseLink;
    private String flybaseLink;
    private String mgiLink;

    @JsonView(value = {View.DiseaseAPI.class})
    @JsonProperty(value = "sources")
    public List<Source> getSourceList() {
        SourceService service = new SourceService();
        List<Source> sources = new ArrayList<>();
        sources.add(service.getSource(SpeciesType.RAT, rgdLink));
        sources.add(service.getSource(SpeciesType.MOUSE, mgiLink));
        sources.add(service.getSource(SpeciesType.ZEBRAFISH, zfinLink));
        sources.add(service.getSource(SpeciesType.FLY, flybaseLink));
        sources.add(service.getSource(SpeciesType.WORM, wormbaseLink));
        sources.add(service.getSource(SpeciesType.YEAST, sgdLink));
        return sources;
    }

    private List<DOTerm> highLevelTermList = new ArrayList<>(2);

    @Relationship(type = "IS_IMPLICATED_IN", direction = Relationship.INCOMING)
    private List<Gene> genes;

    @Relationship(type = "ASSOCIATION", direction = Relationship.INCOMING)
    private List<DiseaseEntityJoin> diseaseEntityJoins;

    @Relationship(type = "IS_A")
    private List<DOTerm> parents;

    @Relationship(type = "IS_A", direction = Relationship.INCOMING)
    protected List<DOTerm> children;

    @Relationship(type = "ALSO_KNOWN_AS")
    private List<Synonym> synonyms;

    @JsonView(value = {View.DiseaseAPI.class})
    @JsonProperty(value = "synonyms")
    public List<String> getSynonymList() {
        if (synonyms == null)
            return null;
        List<String> list = new ArrayList<>();
        for (Synonym s : synonyms) {
            list.add(s.getPrimaryKey());
        }
        return list;
    }

    @Relationship(type = "CROSS_REFERENCE")
    private List<CrossReference> crossReferences;

    @JsonView({View.DiseaseAPI.class})
    @JsonProperty(value = "crossReferences")
    /// ToDO: could combine this with the method in GeneticEntity.
    /// maybe, common super class or a service / utility class
    public Map<String, Object> getCrossReferenceMap() {
        if (crossReferences == null)
            return null;
        Map<String, Object> map = new HashMap<>();

        List<CrossReference> othersList = new ArrayList<>();
        for (CrossReference cr : crossReferences) {
            othersList.add(cr);
            map.put("other", othersList);
        }
        return map;
    }

    public void setDoId(String doId) {
        this.doId = doId;
    }

    @Override
    public String toString() {
        return primaryKey + ":" + name;
    }

    @JsonView({View.DiseaseAPI.class})
    @JsonProperty(value = "children")
    public List<SimpleTerm> getChildrenList() {
        return getSimpleTerms(children);
    }

    @JsonView({View.DiseaseAPI.class})
    @JsonProperty(value = "parents")
    public List<SimpleTerm> getParentList() {
        return getSimpleTerms(parents);
    }

    private List<SimpleTerm> getSimpleTerms(List<DOTerm> parents) {
        if (parents == null)
            return null;
        return parents.stream()
                .map(doTerm -> {
                    SimpleTerm term = new SimpleTerm();
                    term.setPrimaryKey(doTerm.primaryKey);
                    term.setName(doTerm.getName());
                    return term;
                })
                .collect(Collectors.toList());
    }

    public String getID(){
        return doId;
    }

}
