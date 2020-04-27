package org.alliancegenome.neo4j.entity.node;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alliancegenome.cache.repository.helper.SourceServiceHelper;
import org.apache.commons.collections.CollectionUtils;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import lombok.Getter;
import lombok.Setter;

@NodeEntity
@Getter
@Setter
public class DiseaseEntityJoin extends EntityJoin {

    @Relationship(type = "ASSOCIATION")
    private DOTerm disease;

    @Relationship(type = "FROM_ORTHOLOGOUS_GENE")
    private List<Gene> orthologyGenes;

    @Relationship(type = "EVIDENCE")
    private List<PublicationJoin> publicationJoins;

    @Relationship(type = "ANNOTATION_SOURCE_CROSS_REFERENCE")
    private List<CrossReference> providerList;

    // Singular at the moment.
    // Make sure this is singular here
    // might turn into a collection i
    private String dataProvider;
    private int sortOrder;

    public Source getSource() {
        SourceServiceHelper service = new SourceServiceHelper();
        Optional<Source> first = service.getAllSources(disease).stream()
                .filter(source -> source.getSpeciesType().getDisplayName().equalsIgnoreCase(dataProvider))
                .findFirst();
        if (first.isPresent()) return first.get();
        Source source = new Source();
        source.setName(dataProvider);
        return source;
    }

    public CrossReference getSourceProvider() {
        if (checkValidity()) return null;

        List<CrossReference> refs = providerList.stream()
                .filter(crossReference -> crossReference.getLoadedDB() != null)
                .filter(CrossReference::getCuratedDB)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(refs))
            return refs.get(0);
        else
            return null;

    }

    public CrossReference getLoadProvider() {
        if (checkValidity()) return null;

        List<CrossReference> refs = providerList.stream()
                .filter(crossReference -> crossReference.getLoadedDB() != null)
                .filter(CrossReference::getLoadedDB)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(refs))
            return refs.get(0);
        else
            return null;

    }

    private boolean checkValidity() {
        if (providerList == null)
            return true;

/*
        if (providerList.size() > 2)
            throw new RuntimeException("More than 2 CrossReference nodes per DiseaseEntityJoin found [" + primaryKey + "]");
*/
        return false;
    }
}
