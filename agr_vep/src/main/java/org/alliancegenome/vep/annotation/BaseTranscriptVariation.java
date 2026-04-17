package org.alliancegenome.vep.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alliancegenome.vep.bio.Sequence;
import org.alliancegenome.vep.debug.Trace;
import org.alliancegenome.vep.model.CdsSegment;
import org.alliancegenome.vep.model.ExonModel;
import org.alliancegenome.vep.model.Mapper;
import org.alliancegenome.vep.model.TranscriptMapper;
import org.alliancegenome.vep.model.TranscriptModel;
import org.alliancegenome.vep.reference.ReferenceGenome;

/**
 * Port of Bio::EnsEMBL::Variation::BaseTranscriptVariation. Uses
 * TranscriptMapper → Mapper for all coordinate conversions.
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

	public BaseTranscriptVariation(TranscriptModel transcript, int genomicStart, int genomicEnd) {
		this.transcript = transcript;
		this.genomicStart = genomicStart;
		this.genomicEnd = genomicEnd;
		compute();
	}

	/** Backward-compatible 4-arg constructor — tva parameter is unused. */
	public BaseTranscriptVariation(TranscriptModel transcript, int genomicStart, int genomicEnd, TranscriptVariationAllele tva) {
		this(transcript, genomicStart, genomicEnd);
	}

	private void compute() {
		TranscriptMapper mapper = new TranscriptMapper(transcript);
		int strand = transcript.isPositiveStrand() ? 1 : -1;
		String trId = transcript.getTranscriptId();
		String vfPos = genomicStart + ":" + genomicEnd;

		// VEP cdna_start/end (BaseTranscriptVariation.pm line 143-148)
		List<Mapper.Result> cdnaCoords = mapper.genomic2cdna(genomicStart, genomicEnd, strand);
		if (cdnaCoords.isEmpty()) {
			return;
		}
		Mapper.Result cdnaFirst = cdnaCoords.get(0);
		Mapper.Result cdnaLast = cdnaCoords.get(cdnaCoords.size() - 1);
		this.cdnaStart = cdnaFirst.isGap() ? -1 : cdnaFirst.coordinate.start;
		this.cdnaEnd = cdnaLast.isGap() ? -1 : cdnaLast.coordinate.end;
		Trace.log("BTV.cdna_start", "tr=%s vf=%s cdna_start=%s cdna_end=%s", trId, vfPos, Trace.undef(cdnaStart), Trace.undef(cdnaEnd));

		// VEP cds_start/end (BaseTranscriptVariation.pm line 258-264)
		List<Mapper.Result> cdsCoords = mapper.genomic2cds(genomicStart, genomicEnd, strand);
		if (cdsCoords.isEmpty()) {
			return;
		}
		Mapper.Result cdsFirst = cdsCoords.get(0);
		Mapper.Result cdsLast = cdsCoords.get(cdsCoords.size() - 1);
		int exonPhase = transcript.getStartExonPhase();
		int phaseOffset = exonPhase > 0 ? exonPhase : 0;
		this.cdsStart = cdsFirst.isGap() ? -1 : cdsFirst.coordinate.start + phaseOffset;
		this.cdsEnd = cdsLast.isGap() ? -1 : cdsLast.coordinate.end + phaseOffset;
		Trace.log("BTV.cds_start", "tr=%s vf=%s exon_phase=%d cds_start=%s cds_end=%s", trId, vfPos, exonPhase, Trace.undef(cdsStart), Trace.undef(cdsEnd));

		if (this.cdsStart < 0 && this.cdsEnd < 0) {
			return;
		}

		// VEP translation_start/end (BaseTranscriptVariation.pm line 371-376)
		List<Mapper.Result> pepCoords = mapper.genomic2pep(genomicStart, genomicEnd, strand);
		if (pepCoords.isEmpty()) {
			return;
		}
		Mapper.Result pepFirst = pepCoords.get(0);
		Mapper.Result pepLast = pepCoords.get(pepCoords.size() - 1);
		this.translationStart = pepFirst.isGap() ? -1 : pepFirst.coordinate.start;
		this.translationEnd = pepLast.isGap() ? -1 : pepLast.coordinate.end;
		Trace.log("BTV.translation_start", "tr=%s vf=%s translation_start=%s translation_end=%s", trId, vfPos, Trace.undef(translationStart), Trace.undef(translationEnd));

		// VEP codon_position (TranscriptVariation.pm line 287-307)
		int cdnaCodingStart = mapper.getCdnaCodingStart();
		if (cdnaCodingStart <= 0) {
			cdnaCodingStart = 1;
		}
		if (this.cdnaStart > 0 && cdnaCodingStart > 0) {
			this.codonPosition = ((this.cdnaStart - cdnaCodingStart + phaseOffset) % 3) + 1;
		}
	}

	public int cdnaStart() {
		return cdnaStart;
	}

	public int cdnaEnd() {
		return cdnaEnd;
	}

	public int cdsStart() {
		return cdsStart;
	}

	public int cdsEnd() {
		return cdsEnd;
	}

	public int translationStart() {
		return translationStart;
	}

	public int translationEnd() {
		return translationEnd;
	}

	public int codonPosition() {
		return codonPosition;
	}

	public TranscriptModel transcript() {
		return transcript;
	}

	/**
	 * VEP BaseTranscriptVariation::exon_number (line 679-713). Returns "N/total"
	 * string or null if not in an exon.
	 */
	public String exonNumber() {
		List<ExonModel> exons = transcript.getExons();
		int total = exons.size();

		// VEP overlap formula: (bvf_end >= feat_start) AND (bvf_start <= feat_end).
		// For insertions (genomicStart > genomicEnd), this correctly excludes
		// boundary insertions where the inserted bases fall outside the exon.
		List<Integer> numbers = new ArrayList<>();
		for (int i = 0; i < exons.size(); i++) {
			ExonModel exon = exons.get(i);
			if (genomicEnd >= exon.getStart() && genomicStart <= exon.getEnd()) {
				numbers.add(exon.getOrdinal());
			}
		}
		if (numbers.isEmpty()) {
			return null;
		}
		Collections.sort(numbers);
		String num = numbers.size() > 1 ? numbers.get(0) + "-" + numbers.get(numbers.size() - 1) : String.valueOf(numbers.get(0));
		return num + "/" + total;
	}

	/**
	 * VEP BaseTranscriptVariation::intron_number (line 727-758). Returns "N/total"
	 * string or null if not in an intron.
	 */
	public String intronNumber() {
		List<int[]> introns = transcript.getIntronIntervals();
		if (introns == null || introns.isEmpty()) {
			return null;
		}
		int total = introns.size();

		// VEP overlap formula: (bvf_end >= feat_start) AND (bvf_start <= feat_end).
		// For insertions (genomicStart > genomicEnd), this correctly excludes
		// boundary insertions.
		// Intron numbering follows transcription order: introns are stored in
		// genomic order; for minus strand, reverse the index.
		boolean positiveStrand = transcript.isPositiveStrand();
		List<Integer> numbers = new ArrayList<>();
		for (int i = 0; i < introns.size(); i++) {
			int[] intron = introns.get(i);
			if (genomicEnd >= intron[0] && genomicStart <= intron[1]) {
				int ordinal = positiveStrand ? i + 1 : total - i;
				numbers.add(ordinal);
			}
		}
		if (numbers.isEmpty()) {
			return null;
		}
		Collections.sort(numbers);
		String num = numbers.size() > 1 ? numbers.get(0) + "-" + numbers.get(numbers.size() - 1) : String.valueOf(numbers.get(0));
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

		if (gEnd < tStart) {
			return tStart - gEnd;
		}
		if (gStart > tEnd) {
			return gStart - tEnd;
		}
		return 0;
	}

	/**
	 * VEP VariationFeatureOverlapAllele::feature_seq (line 246-264). Returns the
	 * allele sequence in the transcript strand orientation. If VF strand !=
	 * transcript strand, reverse complement.
	 */
	public static String featureSeq(String allele, boolean variantPositiveStrand, boolean transcriptPositiveStrand) {
		if (allele == null || "-".equals(allele) || allele.isEmpty()) {
			return allele;
		}
		if (variantPositiveStrand != transcriptPositiveStrand) {
			return Sequence.reverseComplement(allele);
		}
		return allele;
	}

	/**
	 * VEP Transcript::translateable_seq (Transcript.pm line 905-934).
	 * Perl builds this from the spliced mRNA (exon-only sequence), not directly
	 * from CDS genomic coordinates:
	 *   $mrna = $self->spliced_seq();
	 *   $mrna = substr($mrna, cdna_coding_start - 1, cdna_coding_end - cdna_coding_start + 1);
	 *   $mrna = "N" x start_phase . $mrna   if start_phase > 0;
	 *
	 * This matters for the ~19 FB transcripts where a CDS segment extends past its
	 * exon (e.g. FBtr0335486 CDS 7614843-7615447 vs exon 7614843-7615444). The old
	 * Java code fetched CDS coords from the reference genome, including intron bases.
	 * Perl's spliced_seq is purely exonic, so the overshoot wraps into the next exon.
	 * Using spliced_seq produces the correct peptide (and MD5 for PolyPhen/SIFT).
	 */
	public static String translateableSeq(TranscriptModel transcript, ReferenceGenome reference) {
		if (transcript.getCdsSegments().isEmpty()) return null;

		// Step 1: build spliced mRNA (Perl Transcript::spliced_seq)
		String splicedSeq = buildSplicedSeq(transcript, reference);
		if (splicedSeq == null || splicedSeq.isEmpty()) return null;

		// Step 2: extract CDS from spliced mRNA
		int codingStart = transcript.getCdnaCodingStart(); // 1-based
		int codingEnd = computeCdnaCodingEnd(transcript);  // 1-based
		if (codingStart <= 0 || codingEnd <= 0) return null;
		codingEnd = Math.min(codingEnd, splicedSeq.length());
		if (codingStart > splicedSeq.length()) return null;

		String cds = splicedSeq.substring(codingStart - 1, codingEnd);

		// Step 3: phase padding (Transcript.pm line 929-931)
		int phase = transcript.getTranslationStartExonPhase();
		String result;
		if (phase > 0) {
			StringBuilder out = new StringBuilder();
			for (int i = 0; i < phase; i++) out.append('N');
			out.append(cds);
			result = out.toString();
		} else {
			result = cds;
		}
		Trace.log("BTV._translateable_seq", "tr=%s phase=%d len=%d first30=%s last30=%s",
			transcript.getTranscriptId(), phase, result.length(),
			result.length() >= 30 ? result.substring(0, 30) : result,
			result.length() >= 30 ? result.substring(result.length() - 30) : result);
		return result;
	}

	/**
	 * Perl Transcript::spliced_seq — concatenation of all exon sequences in
	 * transcript order (5' to 3').
	 */
	public static String buildSplicedSeq(TranscriptModel transcript, ReferenceGenome reference) {
		String chr = transcript.getChr();
		List<ExonModel> exons = transcript.getExons();
		StringBuilder mrna = new StringBuilder();
		if (transcript.isPositiveStrand()) {
			for (ExonModel exon : exons) {
				String seq = reference.getSequence(chr, exon.getStart(), exon.getEnd());
				mrna.append(seq.toUpperCase());
			}
		} else {
			for (int i = exons.size() - 1; i >= 0; i--) {
				ExonModel exon = exons.get(i);
				String seq = reference.getSequence(chr, exon.getStart(), exon.getEnd());
				mrna.append(Sequence.reverseComplement(seq.toUpperCase()));
			}
		}
		return mrna.toString();
	}

	/**
	 * Perl Transcript::cdna_coding_end (Transcript.pm line 1027-1074).
	 * Walks exons in transcript order; at the translation end_Exon, adds
	 * translation->end (CDS end offset relative to exon start, may exceed exon
	 * length). Identifies end_Exon via CDS segment start overlap (matching Perl
	 * BaseGXF.pm line 568 exon assignment).
	 */
	public static int computeCdnaCodingEnd(TranscriptModel transcript) {
		if (!transcript.isCoding()) return 0;
		List<CdsSegment> cdsSegs = transcript.getCdsSegments();
		if (cdsSegs.isEmpty()) return 0;

		CdsSegment lastCds = transcript.isPositiveStrand()
			? cdsSegs.get(cdsSegs.size() - 1) : cdsSegs.get(0);

		int cdnaPos = 0;
		if (transcript.isPositiveStrand()) {
			for (ExonModel exon : transcript.getExons()) {
				if (lastCds.getStart() >= exon.getStart() && lastCds.getStart() <= exon.getEnd()) {
					int translationEnd = (lastCds.getEnd() - exon.getStart()) + 1;
					return cdnaPos + translationEnd;
				}
				cdnaPos += exon.getEnd() - exon.getStart() + 1;
			}
		} else {
			List<ExonModel> exons = transcript.getExons();
			for (int i = exons.size() - 1; i >= 0; i--) {
				ExonModel exon = exons.get(i);
				if (lastCds.getEnd() >= exon.getStart() && lastCds.getEnd() <= exon.getEnd()) {
					int translationEnd = (exon.getEnd() - lastCds.getStart()) + 1;
					return cdnaPos + translationEnd;
				}
				cdnaPos += exon.getEnd() - exon.getStart() + 1;
			}
		}
		return 0;
	}

	/**
	 * VEP BaseTranscriptVariation::_three_prime_utr (line 1097-1099). Builds the 3'
	 * UTR sequence.
	 */
	public static String threePrimeUtr(TranscriptModel transcript, ReferenceGenome reference) {
		String chr = transcript.getChr();
		List<ExonModel> exons = transcript.getExons();
		List<CdsSegment> cdsSegments = transcript.getCdsSegments();
		if (exons.isEmpty() || cdsSegments.isEmpty()) {
			return "";
		}

		int cdsEnd;
		if (transcript.isPositiveStrand()) {
			cdsEnd = cdsSegments.get(cdsSegments.size() - 1).getEnd();
		} else {
			cdsEnd = cdsSegments.get(0).getStart();
		}

		StringBuilder utr = new StringBuilder();
		if (transcript.isPositiveStrand()) {
			for (ExonModel exon : exons) {
				if (exon.getStart() > cdsEnd) {
					String seq = reference.getSequence(chr, exon.getStart(), exon.getEnd());
					utr.append(seq.toUpperCase());
				} else if (exon.getEnd() > cdsEnd) {
					String seq = reference.getSequence(chr, cdsEnd + 1, exon.getEnd());
					utr.append(seq.toUpperCase());
				}
			}
		} else {
			for (int i = exons.size() - 1; i >= 0; i--) {
				ExonModel exon = exons.get(i);
				if (exon.getEnd() < cdsEnd) {
					String seq = reference.getSequence(chr, exon.getStart(), exon.getEnd());
					utr.append(Sequence.reverseComplement(seq.toUpperCase()));
				} else if (exon.getStart() < cdsEnd) {
					String seq = reference.getSequence(chr, exon.getStart(), cdsEnd - 1);
					utr.append(Sequence.reverseComplement(seq.toUpperCase()));
				}
			}
		}

		return utr.toString();
	}

	/**
	 * VEP BaseTranscriptVariation::_five_prime_utr (line 1093-1095). Builds the 5'
	 * UTR sequence.
	 */
	public static String fivePrimeUtr(TranscriptModel transcript, ReferenceGenome reference) {
		String chr = transcript.getChr();
		List<ExonModel> exons = transcript.getExons();
		List<CdsSegment> cdsSegments = transcript.getCdsSegments();
		if (exons.isEmpty() || cdsSegments.isEmpty()) {
			return "";
		}

		int cdsStart;
		if (transcript.isPositiveStrand()) {
			cdsStart = cdsSegments.get(0).getStart();
		} else {
			cdsStart = cdsSegments.get(cdsSegments.size() - 1).getEnd();
		}

		StringBuilder utr = new StringBuilder();
		if (transcript.isPositiveStrand()) {
			for (ExonModel exon : exons) {
				if (exon.getEnd() < cdsStart) {
					String seq = reference.getSequence(chr, exon.getStart(), exon.getEnd());
					utr.append(seq.toUpperCase());
				} else if (exon.getStart() < cdsStart) {
					String seq = reference.getSequence(chr, exon.getStart(), cdsStart - 1);
					utr.append(seq.toUpperCase());
				}
			}
		} else {
			for (int i = exons.size() - 1; i >= 0; i--) {
				ExonModel exon = exons.get(i);
				if (exon.getStart() > cdsStart) {
					String seq = reference.getSequence(chr, exon.getStart(), exon.getEnd());
					utr.append(Sequence.reverseComplement(seq.toUpperCase()));
				} else if (exon.getEnd() > cdsStart) {
					String seq = reference.getSequence(chr, cdsStart + 1, exon.getEnd());
					utr.append(Sequence.reverseComplement(seq.toUpperCase()));
				}
			}
		}

		return utr.toString();
	}

	/**
	 * VEP genomic2cds for a single position. Equivalent of the old
	 * genomicToCdsPosition helper. Returns CDS position (1-based) or -1 if not in
	 * CDS.
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
	 * VEP genomic2cdna for a single position. Equivalent of the old
	 * computeCdnaPosition helper. Returns cDNA position (1-based) or -1 if not in
	 * cDNA.
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
