package org.alliancegenome.api.service;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.repository.VariantRepository;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@RequestScoped
public class VariantService {

    private VariantRepository variantRepo = new VariantRepository();

    public JsonResultResponse<Transcript> getTranscriptsByVariant(String variantID, Pagination pagination) {
        Variant variant = variantRepo.getVariant(variantID);

        JsonResultResponse<Transcript> response = new JsonResultResponse<>();
        if (variant != null) {
            List<Transcript> transcriptList = variant.getTranscriptList();
            response.setTotal(transcriptList.size());

            Comparator<Transcript> comparatorGene = Comparator.comparing(transcript -> transcript.getGene().getSymbol());
            Comparator<Transcript> comparatorGeneSequence = comparatorGene.thenComparing(Transcript::getName);

            transcriptList.sort(comparatorGeneSequence);
            response.setResults(transcriptList);
        }
        return response;
    }

    public JsonResultResponse<Variant> getVariants(String id, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();

        List<Variant> variants = variantRepo.getVariantsOfAllele(id);

        JsonResultResponse<Variant> result = new JsonResultResponse<>();

        FilterService<Variant> filterService = new FilterService<>(null);
        result.setTotal(variants.size());
        result.setResults(filterService.getPaginatedAnnotations(pagination, variants));
        result.calculateRequestDuration(startDate);
        return result;
    }
}
