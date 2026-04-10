package org.alliancegenome.vep.annotation;

import java.util.HashMap;
import java.util.Map;

public class ConsequenceSeverity {

	private static final String[] SEVERITY_ORDER = {
		"transcript_ablation",
		"splice_acceptor_variant",
		"splice_donor_variant",
		"stop_gained",
		"frameshift_variant",
		"stop_lost",
		"start_lost",
		"transcript_amplification",
		"inframe_insertion",
		"inframe_deletion",
		"missense_variant",
		"protein_altering_variant",
		"splice_donor_5th_base_variant",
		"splice_region_variant",
		"splice_donor_region_variant",
		"splice_polypyrimidine_tract_variant",
		"incomplete_terminal_codon_variant",
		"start_retained_variant",
		"stop_retained_variant",
		"synonymous_variant",
		"coding_sequence_variant",
		"mature_miRNA_variant",
		"5_prime_UTR_variant",
		"3_prime_UTR_variant",
		"non_coding_transcript_exon_variant",
		"intron_variant",
		"NMD_transcript_variant",
		"non_coding_transcript_variant",
		"upstream_gene_variant",
		"downstream_gene_variant",
		"TFBS_ablation",
		"TFBS_amplification",
		"TF_binding_site_variant",
		"regulatory_region_ablation",
		"regulatory_region_amplification",
		"feature_elongation",
		"regulatory_region_variant",
		"feature_truncation",
		"intergenic_variant"
	};

	private static final Map<String, Integer> SEVERITY_RANK = new HashMap<>();
	private static final Map<String, String> IMPACT_MAP = new HashMap<>();

	static {
		for (int i = 0; i < SEVERITY_ORDER.length; i++) {
			SEVERITY_RANK.put(SEVERITY_ORDER[i], i);
		}

		// HIGH
		for (String s : new String[]{
			"transcript_ablation", "splice_acceptor_variant", "splice_donor_variant",
			"stop_gained", "frameshift_variant", "stop_lost", "start_lost",
			"transcript_amplification"
		}) {
			IMPACT_MAP.put(s, "HIGH");
		}

		// MODERATE
		for (String s : new String[]{
			"inframe_insertion", "inframe_deletion", "missense_variant",
			"protein_altering_variant"
		}) {
			IMPACT_MAP.put(s, "MODERATE");
		}

		// LOW
		for (String s : new String[]{
			"splice_donor_5th_base_variant", "splice_region_variant",
			"splice_donor_region_variant", "splice_polypyrimidine_tract_variant",
			"incomplete_terminal_codon_variant", "start_retained_variant",
			"stop_retained_variant", "synonymous_variant"
		}) {
			IMPACT_MAP.put(s, "LOW");
		}

		// MODIFIER — everything else defaults to MODIFIER
	}

	public static int getRank(String consequence) {
		return SEVERITY_RANK.getOrDefault(consequence, Integer.MAX_VALUE);
	}

	public static String getImpact(String consequences) {
		String bestImpact = "MODIFIER";
		int bestRank = Integer.MAX_VALUE;
		for (String term : consequences.split("&")) {
			int rank = getRank(term);
			if (rank < bestRank) {
				bestRank = rank;
				String impact = IMPACT_MAP.get(term);
				if (impact != null) {
					bestImpact = impact;
				}
			}
		}
		return bestImpact;
	}

	public static String getMostSevere(String consequences1, String consequences2) {
		int rank1 = getMostSevereRank(consequences1);
		int rank2 = getMostSevereRank(consequences2);
		return rank1 <= rank2 ? consequences1 : consequences2;
	}

	public static String getLeastSevere(String consequences1, String consequences2) {
		int rank1 = getMostSevereRank(consequences1);
		int rank2 = getMostSevereRank(consequences2);
		return rank1 >= rank2 ? consequences1 : consequences2;
	}

	public static int getMostSevereRank(String consequences) {
		int best = Integer.MAX_VALUE;
		for (String term : consequences.split("&")) {
			int rank = getRank(term);
			if (rank < best) {
				best = rank;
			}
		}
		return best;
	}

	public static String sortTerms(String compoundConsequence) {
		String[] terms = compoundConsequence.split("&");
		java.util.Arrays.sort(terms, (a, b) -> Integer.compare(getRank(a), getRank(b)));
		return String.join("&", terms);
	}

	private ConsequenceSeverity() {
	}
}
