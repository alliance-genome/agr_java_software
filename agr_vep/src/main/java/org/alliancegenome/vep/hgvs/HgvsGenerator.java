package org.alliancegenome.vep.hgvs;

import org.alliancegenome.vep.annotation.CodingAnnotator.CodingResult;
import org.alliancegenome.vep.model.TranscriptModel;
import org.alliancegenome.vep.reference.ContigAccessionMap;
import org.alliancegenome.vep.reference.ReferenceGenome;

public class HgvsGenerator {

	private final HgvsGenomicNotation genomic;
	private final HgvsCodingNotation coding;
	private final HgvsProteinNotation protein;

	public HgvsGenerator(ContigAccessionMap contigMap) {
		this(contigMap, null);
	}

	public HgvsGenerator(ContigAccessionMap contigMap, ReferenceGenome reference) {
		this.genomic = new HgvsGenomicNotation(contigMap, reference);
		this.coding = new HgvsCodingNotation(reference);
		this.protein = new HgvsProteinNotation();
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
		return protein.generate(proteinId, codingResult);
	}
}
