package org.alliancegenome.core.helpers;

import static java.util.stream.Collectors.toList;

import java.util.*;

import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Range;

public class VariantServiceHelper {


    public static void populateIntronExonLocation(Variant variant, Transcript transcript) {
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
}
