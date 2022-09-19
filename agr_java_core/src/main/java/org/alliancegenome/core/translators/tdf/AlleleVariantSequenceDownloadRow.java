package org.alliancegenome.core.translators.tdf;

import java.util.List;
import java.util.StringJoiner;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AlleleVariantSequenceDownloadRow {

	private String alleleID;
	private String alleleSymbol;
	private String alleleSynonyms;
	private String variantCategory;
	private String hasDisease;
	private String hasPhenotype;
	private String hgvsgName;
	private String variantType;
	private String sequenceFeature;
	private String sequenceFeatureType;
	private String sequenceFeatureAssociatedGene;
	private String sequenceFeatureAssociatedGeneID;
	private String location;
	private List<String> molecularConsequences;
	private String vepImpact;
	private String siftPrediction;
	private String siftScore;
	private String polyphenPrediction;
	private String polyphenScore;
	
	public String getMolecularConsequences() {
		StringJoiner dataJoiner = new StringJoiner(",");
		for(String s: molecularConsequences) {
			dataJoiner.add(s);
		}
		return dataJoiner.toString();
	}
}
