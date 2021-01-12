package org.alliancegenome.cache.repository.helper;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;

import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Source;

public class SourceServiceHelper {


    public Source getSource(SpeciesType type, String link) {
        Source source = new Source();
        source.setUrl(link);
        source.setName(type.getDisplayName());
        return source;
    }

    // ToDo: remove dependency on DOTerm by moving links off DOTerm node
    public List<Source> getAllSources(DOTerm doTerm) {

        List<Source> sourceDoclets = Arrays.stream(SpeciesType.values())
                .map(speciesType -> {
                    Source source = new Source();
                    source.setName(speciesType.getDisplayName());
                    source.setSpeciesType(speciesType);
                    if (speciesType.equals(SpeciesType.HUMAN)) {
                        source.setName(SpeciesType.RAT.getDisplayName());
                        source.setUrl(doTerm.getHumanOnlyRgdLink());
                    }
                    if (speciesType == SpeciesType.FLY && doTerm.getFlybaseLink() != null) {
                        source.setUrl(doTerm.getFlybaseLink());
                    }
                    if (speciesType == SpeciesType.RAT && doTerm.getRgdLink() != null) {
                        source.setUrl(doTerm.getRgdLink());
                        source.setUrl(doTerm.getRatOnlyRgdLink());
                    }
                    if (speciesType == SpeciesType.MOUSE && doTerm.getMgiLink() != null) {
                        source.setUrl(doTerm.getMgiLink());
                    }
                    if (speciesType == SpeciesType.ZEBRAFISH && doTerm.getZfinLink() != null) {
                        source.setUrl(doTerm.getZfinLink());
                    }
                    if (speciesType == SpeciesType.HUMAN && doTerm.getHumanLink() != null) {
                        source.setUrl(doTerm.getHumanLink());
                    }
                    if (speciesType == SpeciesType.YEAST && doTerm.getSgdLink() != null) {
                        source.setUrl(doTerm.getSgdLink());
                    }
                    if (speciesType == SpeciesType.WORM && doTerm.getWormbaseLink() != null) {
                        source.setUrl(doTerm.getWormbaseLink());
                    }
                    return source;
                })
                .collect(toList());

        return sourceDoclets;
    }


}
