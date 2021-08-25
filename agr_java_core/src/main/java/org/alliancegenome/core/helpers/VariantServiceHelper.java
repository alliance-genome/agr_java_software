package org.alliancegenome.core.helpers;

import org.alliancegenome.neo4j.entity.node.Exon;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Range;

import java.util.*;

import static java.util.stream.Collectors.toList;

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
            // exon fully contains the variant
            if (exonRange.containsRange(variantRange)) {
                location = "Exon";
                if (strand.isPresent())
                    location += " " + (index + 1) + " / " + exonRanges.size();
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
                    foundExon = true;
                    break;
                } catch (IllegalArgumentException e) {
                    // ignore as it means there is no intersection
                    // bad API: should have a boolean that checks if there is an intersection
                }
            }
        }
        // check intron number
        if (!foundExon && exonRanges.size() > 1) {
            List<Range<Long>> intronRanges = new ArrayList<>();
            Range<Long> exonRangeLower = exonRanges.get(0);
            for (int index = 1; index < exonRanges.size(); index++) {
                Range<Long> exonRangeUpper = exonRanges.get(index);
                intronRanges.add(Range.between(exonRangeLower.getMaximum() + 1, exonRangeUpper.getMinimum() - 1));
                exonRangeLower = exonRangeUpper;
            }
            for (int index = 0; index < intronRanges.size(); index++) {
                Range<Long> intronRange = intronRanges.get(index);
                // intron fully contains the variant
                if (intronRange.containsRange(variantRange)) {
                    if (strand.isPresent())
                        location += " " + (index + 1) + " / " + intronRanges.size();
                    break;
                }
            }
        }

        transcript.setIntronExonLocation(location);
    }
}
