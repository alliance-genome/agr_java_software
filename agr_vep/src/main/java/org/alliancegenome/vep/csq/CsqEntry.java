package org.alliancegenome.vep.csq;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CsqEntry {

	private String allele;               // 0
	private String consequence;          // 1
	private String impact;               // 2
	private String symbol;               // 3
	private String gene;                 // 4
	private String featureType;          // 5
	private String feature;              // 6
	private String biotype;              // 7
	private String exon;                 // 8
	private String intron;               // 9
	private String hgvsc;                // 10
	private String hgvsp;                // 11
	private String cdnaPosition;         // 12
	private String cdsPosition;          // 13
	private String proteinPosition;      // 14
	private String aminoAcids;           // 15
	private String codons;               // 16
	private String existingVariation;    // 17
	private String distance;             // 18
	private String strand;               // 19
	private String flags;                // 20
	private String geneLevelConsequence; // 21
	private String symbolSource;         // 22
	private String hgncId;               // 23
	private String givenRef;             // 24
	private String usedRef;              // 25
	private String bamEdit;              // 26
	private String source;               // 27
	private String hgvsOffset;           // 28
	private String hgvsg;                // 29
	private String polyPhenPrediction;   // 30
	private String polyPhenScore;        // 31
	private String siftPrediction;       // 32
	private String siftScore;            // 33
	private String transcriptName;       // 34
	private String genomicEndPosition;   // 35
	private String genomicStartPosition; // 36
	private String gffSource;            // 37

	public String toVcfString() {
		// VEP OutputFactory/VCF.pm line 397: $data = '' if $data eq '-'
		// for all fields EXCEPT Allele
		return String.join("|",
			safe(allele),
			vcf(consequence),
			vcf(impact),
			vcf(symbol),
			vcf(gene),
			vcf(featureType),
			vcf(feature),
			vcf(biotype),
			vcf(exon),
			vcf(intron),
			vcf(hgvsc),
			// VEP OutputFactory.pm line 1662-1663: URL-encode "=" in HGVSp for VCF
			vcf(hgvsp != null ? hgvsp.replace("=", "%3D") : null),
			vcf(cdnaPosition),
			vcf(cdsPosition),
			vcf(proteinPosition),
			vcf(aminoAcids),
			vcf(codons),
			vcf(existingVariation),
			vcf(distance),
			vcf(strand),
			vcf(flags),
			vcf(geneLevelConsequence),
			vcf(symbolSource),
			vcf(hgncId),
			vcf(givenRef),
			vcf(usedRef),
			vcf(bamEdit),
			vcf(source),
			vcf(hgvsOffset),
			vcf(hgvsg),
			vcf(polyPhenPrediction),
			vcf(polyPhenScore),
			vcf(siftPrediction),
			vcf(siftScore),
			vcf(transcriptName),
			vcf(genomicEndPosition),
			vcf(genomicStartPosition),
			vcf(gffSource)
		);
	}

	private static String safe(String s) {
		return s != null ? s : "";
	}

	/** VEP OutputFactory/VCF.pm line 397-404: convert "-" to empty, escape VCF-unsafe chars */
	private static String vcf(String s) {
		if (s == null) return "";
		if ("-".equals(s)) return "";
		// VEP character escaping (OutputFactory/VCF.pm line 398-404)
		s = s.replace(" ", "_");
		s = s.replace(",", "&");
		s = s.replace(";", "%3B");
		s = s.replace("|", "&");
		return s;
	}
}
