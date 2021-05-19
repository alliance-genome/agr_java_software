package org.alliancegenome.api.service;

import org.alliancegenome.cache.repository.helper.AlleleFiltering;
import org.alliancegenome.cache.repository.helper.AlleleSorting;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.index.VariantESDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.SOTerm;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.repository.VariantRepository;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@RequestScoped
public class VariantService {

    private VariantRepository variantRepo = new VariantRepository();
    private VariantESDAO variantDAO = new VariantESDAO();

    public JsonResultResponse<Transcript> getTranscriptsByVariant(String variantID, Pagination pagination) {
        Variant variant = getVariantById(variantID);

        JsonResultResponse<Transcript> response = new JsonResultResponse<>();
        if (variant == null)
            return response;

        List<Transcript> transcriptList = variant.getTranscriptLevelConsequence().stream()
                .map(consequence -> {
                    Transcript transcript = new Transcript();
                    transcript.setPrimaryKey(consequence.getTranscriptID());
                    // don't get an independent name from the VCF
                    if (StringUtils.isNotEmpty(consequence.getTranscriptName())) {
                        transcript.setName(consequence.getTranscriptName());
                    } else {
                        transcript.setName(consequence.getTranscriptID());
                    }
                    transcript.setConsequences(List.of(consequence));
                    SOTerm tType = new SOTerm();
                    tType.setName(consequence.getSequenceFeatureType());
                    transcript.setType(tType);
                    transcript.setGene(consequence.getAssociatedGene());
                    transcript.setIntronExonLocation(consequence.getTranscriptLocation());
                    return transcript;
                })
                .collect(Collectors.toList());
        response.setTotal(transcriptList.size());

        // populate location
///        transcriptList.forEach(transcript -> VariantServiceHelper.populateIntronExonLocation(variant, transcript));

        // sorting
///        Comparator<Transcript> comparatorGene = Comparator.comparing(transcript -> transcript.getGene().getSymbol());
///        Comparator<Transcript> comparatorGeneSequence = comparatorGene.thenComparing(Transcript::getName);
///        transcriptList.sort(comparatorGeneSequence);

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

    public Variant getVariantById(String id) {
        Variant variant = variantDAO.getVariant(id);
        // need to add gene.genomeLocation
        GenomeLocation location = variantRepo.getGenomeLocation(variant.getGene().getPrimaryKey());
        if (location != null)
            variant.getGene().setGenomeLocations(List.of(location));
        return variant;
    }
}
