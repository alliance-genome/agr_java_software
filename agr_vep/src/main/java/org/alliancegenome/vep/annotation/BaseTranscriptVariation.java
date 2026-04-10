package org.alliancegenome.vep.annotation;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.vep.bio.Sequence;
import org.alliancegenome.vep.model.ExonModel;
import org.alliancegenome.vep.model.Mapper;
import org.alliancegenome.vep.model.TranscriptMapper;
import org.alliancegenome.vep.model.TranscriptModel;

/**
 * Port of Bio::EnsEMBL::Variation::BaseTranscriptVariation.
 * Uses TranscriptMapper → Mapper for all coordinate conversions.
 */
public class BaseTranscriptVariation {

	private final TranscriptModel transcript;
	private final int genomicStart;
	private final int genomicEnd;

	private int cdnaStart = -1;
	private int cdnaEnd = -1;
	private int cdsStart = -1;
	private int cdsEnd = -1;
	private int translationStart = -1;
	private int translationEnd = -1;
	private int codonPosition;

	public BaseTranscriptVariation(TranscriptModel transcript, int genomicStart, int genomicEnd,
			TranscriptVariationAllele tva) {
		this.transcript = transcript;
		this.genomicStart = genomicStart;
		this.genomicEnd = genomicEnd;
		compute();
	}

	private void compute() {
		TranscriptMapper mapper = new TranscriptMapper(transcript);
		int strand = transcript.isPositiveStrand() ? 1 : -1;

		// VEP cdna_start/end (BaseTranscriptVariation.pm line 143-148)
		List<Mapper.Result> cdnaCoords = mapper.genomic2cdna(genomicStart, genomicEnd, strand);
		Mapper.Result cdnaFirst = cdnaCoords.get(0);
		Mapper.Result cdnaLast = cdnaCoords.get(cdnaCoords.size() - 1);
		this.cdnaStart = cdnaFirst.isGap() ? -1 : cdnaFirst.coordinate.start;
		this.cdnaEnd = cdnaLast.isGap() ? -1 : cdnaLast.coordinate.end;

		// VEP cds_start/end (BaseTranscriptVariation.pm line 258-264)
		List<Mapper.Result> cdsCoords = mapper.genomic2cds(genomicStart, genomicEnd, strand);
		Mapper.Result cdsFirst = cdsCoords.get(0);
		Mapper.Result cdsLast = cdsCoords.get(cdsCoords.size() - 1);
		int exonPhase = transcript.getStartExonPhase();
		int phaseOffset = exonPhase > 0 ? exonPhase : 0;
		this.cdsStart = cdsFirst.isGap() ? -1 : cdsFirst.coordinate.start + phaseOffset;
		this.cdsEnd = cdsLast.isGap() ? -1 : cdsLast.coordinate.end + phaseOffset;

		if (this.cdsStart < 0 && this.cdsEnd < 0) return;

		// VEP translation_start/end (BaseTranscriptVariation.pm line 371-376)
		List<Mapper.Result> pepCoords = mapper.genomic2pep(genomicStart, genomicEnd, strand);
		Mapper.Result pepFirst = pepCoords.get(0);
		Mapper.Result pepLast = pepCoords.get(pepCoords.size() - 1);
		this.translationStart = pepFirst.isGap() ? -1 : pepFirst.coordinate.start;
		this.translationEnd = pepLast.isGap() ? -1 : pepLast.coordinate.end;

		// VEP codon_position (TranscriptVariation.pm line 287-307)
		int cdnaCodingStart = mapper.getCdnaCodingStart();
		if (cdnaCodingStart <= 0) cdnaCodingStart = 1;
		if (this.cdnaStart > 0 && cdnaCodingStart > 0) {
			this.codonPosition = ((this.cdnaStart - cdnaCodingStart + phaseOffset) % 3) + 1;
		}
	}

	public int cdnaStart() { return cdnaStart; }
	public int cdnaEnd() { return cdnaEnd; }
	public int cdsStart() { return cdsStart; }
	public int cdsEnd() { return cdsEnd; }
	public int translationStart() { return translationStart; }
	public int translationEnd() { return translationEnd; }
	public int codonPosition() { return codonPosition; }
	public TranscriptModel transcript() { return transcript; }

	/**
	 * VEP BaseTranscriptVariation::exon_number (line 679-713).
	 * Returns "N/total" string or null if not in an exon.
	 */
	public String exonNumber() {
		List<ExonModel> exons = transcript.getExons();
		int total = exons.size();
		int gStart = Math.min(genomicStart, genomicEnd);
		int gEnd = Math.max(genomicStart, genomicEnd);

		List<Integer> numbers = new ArrayList<>();
		for (int i = 0; i < exons.size(); i++) {
			ExonModel exon = exons.get(i);
			if (gEnd >= exon.getStart() && gStart <= exon.getEnd()) {
				numbers.add(exon.getOrdinal());
			}
		}
		if (numbers.isEmpty()) return null;
		java.util.Collections.sort(numbers);
		String num = numbers.size() > 1
			? numbers.get(0) + "-" + numbers.get(numbers.size() - 1)
			: String.valueOf(numbers.get(0));
		return num + "/" + total;
	}

	/**
	 * VEP BaseTranscriptVariation::intron_number (line 727-758).
	 * Returns "N/total" string or null if not in an intron.
	 */
	public String intronNumber() {
		List<int[]> introns = transcript.getIntronIntervals();
		if (introns == null || introns.isEmpty()) return null;
		int total = introns.size();
		int gStart = Math.min(genomicStart, genomicEnd);
		int gEnd = Math.max(genomicStart, genomicEnd);

		List<Integer> numbers = new ArrayList<>();
		for (int i = 0; i < introns.size(); i++) {
			int[] intron = introns.get(i);
			if (gEnd >= intron[0] && gStart <= intron[1]) {
				numbers.add(i + 1);
			}
		}
		if (numbers.isEmpty()) return null;
		java.util.Collections.sort(numbers);
		String num = numbers.size() > 1
			? numbers.get(0) + "-" + numbers.get(numbers.size() - 1)
			: String.valueOf(numbers.get(0));
		return num + "/" + total;
	}

	/**
	 * VEP BaseTranscriptVariation::distance_to_transcript (line 582-612).
	 */
	public int distanceToTranscript() {
		int tStart = transcript.getStart();
		int tEnd = transcript.getEnd();
		int gStart = Math.min(genomicStart, genomicEnd);
		int gEnd = Math.max(genomicStart, genomicEnd);

		if (gEnd < tStart) return tStart - gEnd;
		if (gStart > tEnd) return gStart - tEnd;
		return 0;
	}

	/**
	 * VEP VariationFeatureOverlapAllele::feature_seq (line 246-264).
	 * Returns the allele sequence in the transcript strand orientation.
	 * If VF strand != transcript strand, reverse complement.
	 */
	public static String featureSeq(String allele, boolean variantPositiveStrand,
			boolean transcriptPositiveStrand) {
		if (allele == null || "-".equals(allele) || allele.isEmpty()) return allele;
		if (variantPositiveStrand != transcriptPositiveStrand) {
			return Sequence.reverseComplement(allele);
		}
		return allele;
	}

	/**
	 * VEP genomic2cds for a single position.
	 * Equivalent of the old genomicToCdsPosition helper.
	 * Returns CDS position (1-based) or -1 if not in CDS.
	 */
	public static int genomicToCds(TranscriptModel transcript, int genomicPos) {
		TranscriptMapper mapper = new TranscriptMapper(transcript);
		int strand = transcript.isPositiveStrand() ? 1 : -1;
		List<Mapper.Result> results = mapper.genomic2cds(genomicPos, genomicPos, strand);
		int exonPhase = transcript.getStartExonPhase();
		int phaseOffset = exonPhase > 0 ? exonPhase : 0;
		for (Mapper.Result r : results) {
			if (r.isCoordinate()) {
				return r.coordinate.start + phaseOffset;
			}
		}
		return -1;
	}

	/**
	 * VEP genomic2cdna for a single position.
	 * Equivalent of the old computeCdnaPosition helper.
	 * Returns cDNA position (1-based) or -1 if not in cDNA.
	 */
	public static int genomicToCdna(TranscriptModel transcript, int genomicPos) {
		TranscriptMapper mapper = new TranscriptMapper(transcript);
		int strand = transcript.isPositiveStrand() ? 1 : -1;
		List<Mapper.Result> results = mapper.genomic2cdna(genomicPos, genomicPos, strand);
		for (Mapper.Result r : results) {
			if (r.isCoordinate()) {
				return r.coordinate.start;
			}
		}
		return -1;
	}
}
