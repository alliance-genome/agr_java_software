package org.alliancegenome.api.service;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Exon;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.repository.VariantRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Range;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

@RequestScoped
public class VariantService {

    private VariantRepository variantRepo = new VariantRepository();

    public JsonResultResponse<Transcript> getTranscriptsByVariant(String variantID, Pagination pagination) {
        Variant variant = variantRepo.getVariant(variantID);

        JsonResultResponse<Transcript> response = new JsonResultResponse<>();
        if (variant == null)
            return response;

        List<Transcript> transcriptList = variant.getTranscriptList();
        response.setTotal(transcriptList.size());

        // populate location
        transcriptList.forEach(transcript -> populateIntronExonLocation(variant, transcript));

        // sorting
        Comparator<Transcript> comparatorGene = Comparator.comparing(transcript -> transcript.getGene().getSymbol());
        Comparator<Transcript> comparatorGeneSequence = comparatorGene.thenComparing(Transcript::getName);
        transcriptList.sort(comparatorGeneSequence);

        // pagination
        response.setResults(transcriptList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(toList()));
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

    public void populateIntronExonLocation(Variant variant, Transcript transcript) {
        List<Exon> exons = transcript.getExons();
        if (CollectionUtils.isEmpty(exons))
            return;

        GenomeLocation variantLoc = variant.getLocation();
        Range<Long> variantRange = Range.between(variantLoc.getStart(), variantLoc.getEnd());
        List<Range<Long>> exonRanges = exons.stream()
                .map(exon -> Range.between(exon.getLocation().getStart(), exon.getLocation().getEnd()))
                .sorted(Comparator.comparing(Range::getMinimum))
                .collect(toList());

        String location = "Intron";
        for (int index = 0; index < exonRanges.size(); index++) {
            Range<Long> exonRange = exonRanges.get(index);
            if (exonRange.containsRange(variantRange))
                location = "Exon " + index;
        }
        transcript.setIntronExonLocation(location);
    }

}
