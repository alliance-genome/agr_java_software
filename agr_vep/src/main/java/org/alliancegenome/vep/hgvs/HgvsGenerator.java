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
		return generateHgvsp(transcript, codingResult, null, null);
	}

	public String generateHgvsp(TranscriptModel transcript, CodingResult codingResult,
			String altCds, String cdsSequence) {
		if (codingResult == null) return null;
		if (codingResult.getProteinPosition() <= 0) return null;

		String proteinId = transcript.getProteinId();

		// Use VEP's notation-based formatter when available (from TranscriptVariationAllele)
		HgvsNotation n = codingResult.getHgvsNotation();
		if (n != null && n.type != null && codingAnnotator != null) {
			String consequence = codingResult.getConsequence();
			boolean isStopLost = consequence != null && consequence.contains("stop_lost");
			boolean isStartLost = consequence != null && consequence.contains("start_lost");
			return codingAnnotator.vepGetHgvsProteinFormat(n, proteinId, isStopLost, isStartLost,
				altCds, cdsSequence);
		}

		// Fallback to consequence-based formatter
		return protein.generate(proteinId, codingResult);
	}

	public void setCodingAnnotator(TranscriptVariationAllele codingAnnotator) {
		this.codingAnnotator = codingAnnotator;
	}

	private TranscriptVariationAllele codingAnnotator;
}
