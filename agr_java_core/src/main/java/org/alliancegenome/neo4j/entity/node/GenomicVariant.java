package org.alliancegenome.neo4j.entity.node;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Schema(name = "Variant", description = "POJO that represents the Variant")
public class GenomicVariant extends Variant {

    private String hgvsG;
    private String hgvsC;
    private String hgvsP;


    @JsonView({View.Default.class, View.API.class})
    public List<String> getHgvsG() {
        if (CollectionUtils.isNotEmpty(transcriptList)) {
            return transcriptList.stream()
                    .map(Transcript::getConsequences)
                    .flatMap(Collection::stream)
                    .map(TranscriptLevelConsequence::getHgvsVEPGeneNomenclature)
                    .collect(Collectors.toList());
        }
        return null;
    }

    @JsonView({View.Default.class, View.API.class})
    public List<String> getHgvsC() {
        if (CollectionUtils.isNotEmpty(transcriptList)) {
            return transcriptList.stream()
                    .map(Transcript::getConsequences)
                    .flatMap(Collection::stream)
                    .map(TranscriptLevelConsequence::getHgvsCodingNomenclature)
                    .collect(Collectors.toList());
        }
        return null;
    }

    @JsonView({View.Default.class, View.API.class})
    public List<String> getHgvsP() {
        if (CollectionUtils.isNotEmpty(transcriptList)) {
            return transcriptList.stream()
                    .map(Transcript::getConsequences)
                    .flatMap(Collection::stream)
                    .map(TranscriptLevelConsequence::getHgvsProteinNomenclature)
                    .collect(Collectors.toList());
        }
        return null;
    }
}
