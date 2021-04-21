package org.alliancegenome.api.service;

import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.*;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.cache.repository.helper.*;
import org.alliancegenome.core.helpers.VariantServiceHelper;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.repository.VariantRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Range;

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
        transcriptList.forEach(transcript -> VariantServiceHelper.populateIntronExonLocation(variant, transcript));

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

    public JsonResultResponse<Allele> getAllelesByVariant(String variantID, Pagination pagination) {
        Variant variant = variantRepo.getVariant(variantID);

        JsonResultResponse<Allele> response = new JsonResultResponse<>();
        if (variant == null)
            return response;

        List<Allele> alleles = variantRepo.getAllelesOfVariant(variantID);
        response.setTotal(alleles.size());

        // sorting
        FilterService<Allele> service = new FilterService<>(new AlleleFiltering());
        response.setResults(service.getSortedAndPaginatedAnnotations(pagination, alleles, new AlleleSorting()));
        return response;
    }
}
