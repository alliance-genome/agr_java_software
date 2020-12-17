package org.alliancegenome.api.service;

import org.alliancegenome.cache.repository.helper.AlleleFiltering;
import org.alliancegenome.cache.repository.helper.AlleleSorting;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Exon;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.repository.VariantRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Range;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

        // Check strand info off transcript
        String strandGene = transcript.getGenomeLocation().getStrand();

        // Neither variants nor transcripts have strand info in the GenomicLocation node.
        // For that reason I resort to the strand info of the associated gene.
        if (strandGene.isEmpty())
            strandGene = transcript.getGene().getGenomeLocations().get(0).getStrand();
        // strand info can be empty of null. In both cases, the missing info disallows to
        // calculate the exon number in question.
        if (strandGene.isEmpty())
            strandGene = null;
        Optional<Boolean> strand = Optional.ofNullable(strandGene)
                .map(strandValue -> strandValue.equals("+"));

        List<Range<Long>> exonRanges = exons.stream()
                .map(exon -> Range.between(exon.getLocation().getStart(), exon.getLocation().getEnd()))
                .sorted(Comparator.comparing(Range::getMinimum))
                .collect(toList());
        // there is an issue with the module setup of the range class.
        // exonRanges.sort(Collections.reverse())
        if (strand.isPresent() && !strand.get()) {
            exonRanges = exons.stream()
                    .map(exon -> Range.between(exon.getLocation().getStart(), exon.getLocation().getEnd()))
                    .sorted(Comparator.comparing(Range::getMinimum, Collections.reverseOrder()))
                    .collect(toList());
        }

        // It's an intron if no exon overlap is found.
        String location = "Intron";
        boolean foundExon = false;
        for (int index = 0; index < exonRanges.size(); index++) {
            Range<Long> exonRange = exonRanges.get(index);
            // fully contains the variant
            if (exonRange.containsRange(variantRange)) {
                location = "Exon";
                if (!strand.isEmpty())
                    location += " " + (index + 1);
                foundExon = true;
                break;
            }
        }
        // check if there is partial overlap
        if (!foundExon) {
            for (int index = 0; index < exonRanges.size(); index++) {
                Range<Long> exonRange = exonRanges.get(index);
                // partial overlap with variant
                try {
                    exonRange.intersectionWith(variantRange);
                    location += "/Exon";
                    break;
                } catch (IllegalArgumentException e) {
                    // ignore as it means there is no intersection
                    // bad API: should have a boolean that checks if there is an intersection
                }
            }
        }
        transcript.setIntronExonLocation(location);
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
