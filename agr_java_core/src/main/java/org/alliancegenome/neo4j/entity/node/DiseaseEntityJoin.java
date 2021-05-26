package org.alliancegenome.neo4j.entity.node;

import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.cache.repository.helper.SourceServiceHelper;
import org.apache.commons.collections.CollectionUtils;
import org.neo4j.ogm.annotation.*;

import lombok.*;
import lombok.extern.log4j.Log4j2;

@NodeEntity
@Getter
@Setter
@Log4j2
public class DiseaseEntityJoin extends EntityJoin {

    @Relationship(type = "ASSOCIATION")
    private DOTerm disease;

    @Relationship(type = "FROM_ORTHOLOGOUS_GENE")
    private List<Gene> orthologyGenes;

    @Relationship(type = "EVIDENCE")
    private List<PublicationJoin> publicationJoins;

    @Relationship(type = "ANNOTATION_SOURCE_CROSS_REFERENCE")
    private List<CrossReference> providerList;

    @Relationship(type = "INDUCES")
    private List<ExperimentalCondition> inducerConditionList;

    @Relationship(type = "HAS_CONDITION")
    private List<ExperimentalCondition> hasConditionList;

    @Relationship(type = "AMELIORATES")
    private List<ExperimentalCondition> ameliorateConditionList;

    @Relationship(type = "EXACERBATES")
    private List<ExperimentalCondition> exacerbateConditionList;

/*
    @Relationship(type = "ASSOCIATION")
    private List<ExperimentalCondition> experimentalConditionList;

    @Relationship(type = "ASSOCIATION")
    private List<ExperimentalCondition> experimentalConditionList;

    @Relationship(type = "ASSOCIATION")
    private List<ExperimentalCondition> experimentalConditionList;
*/

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

    // ToDo: This is hard-coded to handle the OMIM via RGD case.
    // It's needs to be modelled differently. The CrossReference nodes need to be on
    // the PublicationJoin  node
    // This is just a quick fix and is not scalable
    public List<Map<String, CrossReference>> getDataProviderList() {
        if (providerList == null && dataProvider == null)
            return null;
        if (providerList == null && !dataProvider.equals("Alliance")) {
            return null;
        }

        List<Map<String, CrossReference>> dataProviderList = new ArrayList<>(2);
        if (dataProvider != null && dataProvider.equals("Alliance")) {
            Map<String, CrossReference> providerMap = new HashMap<>();
            CrossReference ref = new CrossReference();
            ref.setDisplayName("Alliance");
            providerMap.put("sourceProvider", ref);
            dataProviderList.add(providerMap);
            return dataProviderList;
        }

        if (providerList == null) {
            return null;
        }
        try {
            providerList.stream()
                    .filter(crossReference -> crossReference.getLoadedDB() != null)
                    .filter(CrossReference::getCuratedDB)
                    .forEach(crossReference -> {
                        Map<String, CrossReference> providerMap = new HashMap<>();
                        providerMap.put("sourceProvider", crossReference);
                        if (!crossReference.getDisplayName().equals("RGD")) {
                            List<CrossReference> loadRefs = providerList.stream()
                                    .filter(crossRef -> crossRef.getLoadedDB() != null)
                                    .filter(CrossReference::getLoadedDB)
                                    .collect(Collectors.toList());

                            if (loadRefs.size() > 1)
                                throw new RuntimeException(("There are more than 3 CrossReferences "));
                            if (CollectionUtils.isNotEmpty(loadRefs))
                                providerMap.put("loadProvider", loadRefs.get(0));
                        }
                        dataProviderList.add(providerMap);
                    });
        } catch (Exception e) {
            log.error("error while looping PK " + primaryKey, e);
        }
        return dataProviderList;
    }

}
