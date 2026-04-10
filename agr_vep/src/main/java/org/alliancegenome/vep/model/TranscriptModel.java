package org.alliancegenome.vep.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import htsjdk.samtools.util.Locatable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranscriptModel implements Locatable {

	private String id;
	private String transcriptId;
	private String name;
	private String geneId;
	private String geneSymbol;
	private String geneCurie;
	private String biotype;
	private String proteinId;
	private String chr;
	private int start;
	private int end;
	private boolean positiveStrand;
	private String source;

	private String peptideMd5;
	private boolean cdsStartNF;
	private int codonTable = 1; // 1=standard, 2=vertebrate mitochondrial
	private int loadOrder; // GFF3 file order for stable transcript sorting

	// VEP Transcript fields for codon_position (TranscriptVariation.pm line 292-302)
	private int cdnaCodingStart; // cDNA position where coding begins (1-based, after 5'UTR)
	private int startExonPhase;  // phase of the first coding exon (0, 1, or 2)

	private List<ExonModel> exons = new ArrayList<>();
	private List<CdsSegment> cdsSegments = new ArrayList<>();

	private List<int[]> intronIntervals;

	public void sortAndIndex() {
		Collections.sort(exons);
		Collections.sort(cdsSegments);
		for (int i = 0; i < exons.size(); i++) {
			exons.get(i).setOrdinal(i + 1);
		}
		intronIntervals = null;
	}

	public boolean isCoding() {
		return !cdsSegments.isEmpty();
	}

	public List<int[]> getIntronIntervals() {
		if (intronIntervals == null) {
			intronIntervals = new ArrayList<>();
			for (int i = 0; i < exons.size() - 1; i++) {
				int intronStart = exons.get(i).getEnd() + 1;
				int intronEnd = exons.get(i + 1).getStart() - 1;
				if (intronEnd >= intronStart) {
					intronIntervals.add(new int[]{intronStart, intronEnd});
				}
			}
		}
		return intronIntervals;
	}

	public String getExonNumber(int pos) {
		for (ExonModel exon : exons) {
			if (exon.contains(pos)) {
				if (positiveStrand) {
					return exon.getOrdinal() + "/" + exons.size();
				} else {
					return (exons.size() - exon.getOrdinal() + 1) + "/" + exons.size();
				}
			}
		}
		return null;
	}

	public String getIntronNumber(int pos) {
		List<int[]> introns = getIntronIntervals();
		for (int i = 0; i < introns.size(); i++) {
			if (pos >= introns.get(i)[0] && pos <= introns.get(i)[1]) {
				if (positiveStrand) {
					return (i + 1) + "/" + introns.size();
				} else {
					return (introns.size() - i) + "/" + introns.size();
				}
			}
		}
		return null;
	}

	public boolean isInExon(int pos) {
		for (ExonModel exon : exons) {
			if (exon.contains(pos)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInIntron(int pos) {
		for (int[] intron : getIntronIntervals()) {
			if (pos >= intron[0] && pos <= intron[1]) {
				return true;
			}
		}
		return false;
	}

	public boolean isInCds(int pos) {
		for (CdsSegment cds : cdsSegments) {
			if (cds.contains(pos)) {
				return true;
			}
		}
		return false;
	}

	public int getCdsStart() {
		if (cdsSegments.isEmpty()) return -1;
		return cdsSegments.get(0).getStart();
	}

	public int getCdsEnd() {
		if (cdsSegments.isEmpty()) return -1;
		return cdsSegments.get(cdsSegments.size() - 1).getEnd();
	}

	public boolean isInTranscriptExonicRegion(int pos) {
		if (isInExon(pos)) return true;
		// Position may be in the mRNA extent but outside explicit exon features (common in SGD)
		// If it's before the first exon or after the last exon (within mRNA bounds), treat as exonic
		if (exons.isEmpty()) return false;
		int firstExonStart = exons.get(0).getStart();
		int lastExonEnd = exons.get(exons.size() - 1).getEnd();
		return (pos >= start && pos < firstExonStart) || (pos > lastExonEnd && pos <= end);
	}

	public boolean isIn5PrimeUtr(int pos) {
		if (!isCoding()) return false;
		if (!isInExon(pos) && !isInTranscriptExonicRegion(pos)) return false;
		if (positiveStrand) {
			return pos < getCdsStart();
		} else {
			return pos > getCdsEnd();
		}
	}

	public boolean isIn3PrimeUtr(int pos) {
		if (!isCoding()) return false;
		if (!isInExon(pos) && !isInTranscriptExonicRegion(pos)) return false;
		if (positiveStrand) {
			return pos > getCdsEnd();
		} else {
			return pos < getCdsStart();
		}
	}

	@Override
	public String getContig() {
		return chr;
	}

	@Override
	public String toString() {
		return transcriptId + " (" + geneSymbol + ") " + chr + ":" + start + "-" + end;
	}
}
