package org.alliancegenome.vep.hgvs;

import org.alliancegenome.vep.annotation.TranscriptVariationAllele;
import org.alliancegenome.vep.annotation.TranscriptVariationAllele.CodingResult;
import org.alliancegenome.vep.annotation.TranscriptVariationAllele.HgvsNotation;
import org.alliancegenome.vep.model.TranscriptModel;
import org.alliancegenome.vep.reference.ContigAccessionMap;
import org.alliancegenome.vep.reference.ReferenceGenome;

public class HgvsGenerator {

	private final VariationFeature genomic;
	private final HgvsCodingNotation coding;
	private final TranscriptVariationAlleleFormat protein;

	public HgvsGenerator(ContigAccessionMap contigMap) {
		this(contigMap, null);
	}

	public HgvsGenerator(ContigAccessionMap contigMap, ReferenceGenome reference) {
		this.genomic = new VariationFeature(contigMap, reference);
		this.coding = new HgvsCodingNotation(reference);
		this.protein = new TranscriptVariationAlleleFormat();
	}

	public String generateHgvsg(String chr, int start, int end, String vepRef, String vepAlt) {
		return genomic.generate(chr, start, end, vepRef, vepAlt);
	}

	public String generateHgvsc(TranscriptModel transcript, int variantStart, int variantEnd,
			String vepAllele, String refAllele, int cdsPosition) {
		return coding.generate(transcript, transcript.getChr(), variantStart, variantEnd,
			vepAllele, refAllele, cdsPosition, transcript.isCoding());
	}

	public String generateHgvsp(TranscriptModel transcript, CodingResult codingResult) {
		if (codingResult == null) return null;
		if (codingResult.getProteinPosition() <= 0) return null;

		String proteinId = transcript.getProteinId();

		// VEP hgvs_protein() — use notation-based formatter matching Perl exactly
		HgvsNotation n = codingResult.getHgvsNotation();
		if (n != null && n.type != null && codingAnnotator != null) {
			String consequence = codingResult.getConsequence();
			boolean isStopLost = consequence != null && consequence.contains("stop_lost");
			boolean isStartLost = consequence != null && consequence.contains("start_lost");
			return codingAnnotator.vepGetHgvsProteinFormat(n, proteinId, isStopLost, isStartLost,
				codingResult.getAltCdsSequence(), codingResult.getCdsSequence());
		}

		// Fallback to consequence-based formatter
		return protein.generate(proteinId, codingResult);
	}

	public void setCodingAnnotator(TranscriptVariationAllele codingAnnotator) {
		this.codingAnnotator = codingAnnotator;
	}

	private TranscriptVariationAllele codingAnnotator;
}
