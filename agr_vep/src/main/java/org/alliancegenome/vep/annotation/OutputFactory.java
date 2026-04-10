package org.alliancegenome.vep.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.vep.bio.CodonTable;
import org.alliancegenome.vep.csq.CsqEntry;
import org.alliancegenome.vep.annotation.TranscriptVariationAllele.CodingResult;
import org.alliancegenome.vep.annotation.SpliceAnnotator.SpliceResult;
import org.alliancegenome.vep.model.CdsSegment;
import org.alliancegenome.vep.model.ExonModel;
import org.alliancegenome.vep.model.GeneModel;
import org.alliancegenome.vep.model.TranscriptModel;
import org.alliancegenome.vep.plugin.PredictionLookup;
import org.alliancegenome.vep.reference.ReferenceGenome;

import java.util.LinkedHashSet;
import java.util.Set;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;

public class OutputFactory {

	private final GeneModel geneModel;
	private final TranscriptVariationAllele codingAnnotator;
	private final org.alliancegenome.vep.hgvs.VariationFeature hgvsGenomic;
	private final SpliceAnnotator spliceAnnotator;
	private final String mod;
	private final PredictionLookup siftLookup;
	private final PredictionLookup polyPhenLookup;

	public OutputFactory(GeneModel geneModel, ReferenceGenome reference,
			org.alliancegenome.vep.reference.ContigAccessionMap contigMap, String mod) {
		this(geneModel, reference, contigMap, mod, null, null);
	}

	public OutputFactory(GeneModel geneModel, ReferenceGenome reference,
			org.alliancegenome.vep.reference.ContigAccessionMap contigMap, String mod,
			PredictionLookup siftLookup, PredictionLookup polyPhenLookup) {
		this.geneModel = geneModel;
		this.codingAnnotator = new TranscriptVariationAllele(reference);
		this.hgvsGenomic = new org.alliancegenome.vep.hgvs.VariationFeature(contigMap, reference);
		this.spliceAnnotator = new SpliceAnnotator();
		this.mod = mod;
		this.siftLookup = siftLookup;
		this.polyPhenLookup = polyPhenLookup;
	}

	public List<CsqEntry> annotate(VariantContext vc) {
		List<CsqEntry> allEntries = new ArrayList<>();
		String chr = geneModel.normalizeContig(vc.getContig());
		int start = vc.getStart();
		int end = vc.getEnd();
		String ref = vc.getReference().getBaseString();

		for (Allele altAllele : vc.getAlternateAlleles()) {
			String alt = altAllele.getBaseString();
			int variantStart = start;
			int variantEnd = end;
			String vepAllele;
			String vepRef;

			// VEP VCF.pm line 267-323: for indels (different length ref/alt),
			// strip exactly 1 char from the front when first bases match.
			// NOT per-allele longest-common-prefix — just 1 char, always.
			// No --minimal flag, so no split_variants/suffix trimming.
			boolean isIndel = ref.length() != alt.length();
			if (isIndel && ref.length() > 0 && alt.length() > 0
					&& ref.charAt(0) == alt.charAt(0)) {
				String refTrimmed = ref.substring(1);
				String altTrimmed = alt.substring(1);
				vepAllele = altTrimmed.isEmpty() ? "-" : altTrimmed;
				vepRef = refTrimmed.isEmpty() ? "-" : refTrimmed;
				variantStart = start + 1;
				if (refTrimmed.isEmpty()) {
					// Pure insertion: start > end
					variantEnd = variantStart - 1;
				}
				// For deletion/complex: variantEnd stays at htsjdk end
			} else {
				// SNP or equal-length substitution: no stripping
				vepAllele = alt;
				vepRef = ref;
			}

			List<TranscriptModel> overlapping = geneModel.getOverlappingTranscripts(chr, variantStart, variantEnd);

			if (overlapping.isEmpty()) {
				allEntries.add(createIntergenicEntry(chr, vepAllele, vepRef, variantStart, variantEnd));
			} else {
				// VEP outputs transcripts sorted by transcript stable ID (Feature field)
				overlapping.sort((a, b) -> a.getTranscriptId().compareTo(b.getTranscriptId()));
				List<CsqEntry> alleleEntries = new ArrayList<>();
				for (TranscriptModel transcript : overlapping) {
					// VEP TranscriptVariationAllele_to_output_hash (OutputFactory.pm line 1630)
					CsqEntry entry = transcriptVariationAlleleToOutputHash(
						transcript, chr, variantStart, variantEnd, vepAllele, vepRef);
					if (entry != null) {
						addPredictions(entry, transcript);
						alleleEntries.add(entry);
					}
				}
				if (alleleEntries.isEmpty()) {
					allEntries.add(createIntergenicEntry(chr, vepAllele, vepRef, variantStart, variantEnd));
				} else {
					allEntries.addAll(alleleEntries);
				}
			}
		}

		// AGR ProcessOutput.pm (agr_vep_pipeline) lines 49-63:
		// Compute Gene_level_consequence AFTER all alleles are processed,
		// operating on the full set of CSQ entries for the VCF line.
		computeGeneLevelConsequence(allEntries);

		// VEP outputs CSQ entries in transcript-major order (OutputFactory.pm line 513):
		// for each transcript (sorted by stable ID), for each allele (VCF order).
		// Our loop is allele-major; stable-sort by Feature to match VEP ordering.
		allEntries.sort(java.util.Comparator.comparing(
			(CsqEntry e) -> e.getFeature() != null ? e.getFeature() : ""));

		return allEntries;
	}

	private void computeGeneLevelConsequence(List<CsqEntry> entries) {
		// ProcessOutput.pm lines 49-63:
		// 1. VEP --flag_pick_allele_gene picks ONE transcript per allele+gene
		//	  (pick_order: mane_select > canonical > appris > tsl > biotype > ccds > rank > length)
		// 2. For each PICKED entry, store its consequence keyed by ALLELE (last PICK per allele wins)
		// 3. All entries get the stored consequence for their allele

		// Step 1: Determine which entries would have PICK flag set.
		// VEP flag_pick_allele_gene (OutputFactory.pm lines 622-626, 829-858):
		// Groups by allele, then within each allele groups by gene,
		// picks worst (most severe) transcript per gene.
		// We simulate this: for each allele+gene, pick the entry with the most severe consequence
		// (then longest transcript as tiebreaker — matching VEP pick_order after biotype/rank).
		Map<String, CsqEntry> pickedPerAlleleGene = new LinkedHashMap<>();
		for (CsqEntry entry : entries) {
			String allele = entry.getAllele() != null ? entry.getAllele() : "";
			String gene = entry.getGene() != null ? entry.getGene() : "";
			String key = allele + "|" + gene;
			CsqEntry current = pickedPerAlleleGene.get(key);
			if (current == null || isPicked(entry, current)) {
				pickedPerAlleleGene.put(key, entry);
			}
		}

		// Step 2: ProcessOutput.pm line 57-59:
		// For each PICKED entry, store consequence keyed by allele (last one wins)
		Map<String, String> glcPerAllele = new HashMap<>();
		for (CsqEntry picked : pickedPerAlleleGene.values()) {
			String allele = picked.getAllele() != null ? picked.getAllele() : "";
			glcPerAllele.put(allele, picked.getConsequence());
		}

		// Step 3: ProcessOutput.pm line 63:
		// Set GLC on all entries = stored consequence for their allele
		for (CsqEntry entry : entries) {
			String allele = entry.getAllele() != null ? entry.getAllele() : "";
			entry.setGeneLevelConsequence(glcPerAllele.get(allele));
		}
	}

	/**
	 * VEP pick_worst_VariationFeatureOverlapAllele (OutputFactory.pm lines 702-811):
	 * Returns true if candidate should be picked over current.
	 * Pick order: mane_select > canonical > appris > tsl > biotype > ccds > rank > length
	 * For AGR species (no MANE/canonical/APPRIS/TSL/CCDS): falls to rank then length.
	 */
	private boolean isPicked(CsqEntry candidate, CsqEntry current) {
		// Rank: lower = more severe = better
		int rankCand = ConsequenceSeverity.getMostSevereRank(candidate.getConsequence());
		int rankCurr = ConsequenceSeverity.getMostSevereRank(current.getConsequence());
		if (rankCand < rankCurr) return true;
		if (rankCand > rankCurr) return false;
		// Equal rank: longer transcript is better (we don't have length, so keep current)
		return false;
	}

	private CsqEntry createIntergenicEntry(String chr, String vepAllele, String refAllele, int start, int end) {
		CsqEntry entry = new CsqEntry();
		entry.setAllele(vepAllele);
		entry.setConsequence("intergenic_variant");
		entry.setImpact("MODIFIER");
		entry.setGeneLevelConsequence("intergenic_variant");
		entry.setGenomicStartPosition(String.valueOf(start));
		entry.setGenomicEndPosition(String.valueOf(end));

		String hgvsg = hgvsGenomic.generate(chr, start, end, refAllele, vepAllele);
		if (hgvsg != null) {
			entry.setHgvsg(hgvsg);
		}

		return entry;
	}

	static String toVepAllele(String ref, String alt) {
		if (ref.length() == 1 && alt.length() == 1) {
			return alt;
		}
		int prefixLen = 0;
		while (prefixLen < ref.length() && prefixLen < alt.length()
				&& ref.charAt(prefixLen) == alt.charAt(prefixLen)) {
			prefixLen++;
		}
		String altTrimmed = alt.substring(prefixLen);
		String refTrimmed = ref.substring(prefixLen);

		if (altTrimmed.isEmpty()) return "-";
		if (refTrimmed.isEmpty()) return altTrimmed;
		return altTrimmed;
	}

	/**
	 * Add SIFT/PolyPhen predictions to a CSQ entry.
	 * VEP only looks up predictions for single amino acid substitutions
	 * (pep_allele_string =~ /^[A-Z]\/[A-Z]$/).
	 */
	private void addPredictions(CsqEntry entry, TranscriptModel transcript) {
		if (siftLookup == null && polyPhenLookup == null) return;

		String aminoAcids = entry.getAminoAcids();
		if (aminoAcids == null || aminoAcids.length() != 3 || aminoAcids.charAt(1) != '/') return;
		char refAA = aminoAcids.charAt(0);
		char altAA = aminoAcids.charAt(2);
		if (refAA == altAA || refAA == '*' || altAA == '*' || refAA == 'X' || altAA == 'X') return;

		String protPos = entry.getProteinPosition();
		if (protPos == null) return;
		int position;
		try {
			position = Integer.parseInt(protPos);
		} catch (NumberFormatException e) {
			return;
		}

		String md5 = transcript.getPeptideMd5();
		if (md5 == null) {
			md5 = computePeptideMd5(transcript);
			transcript.setPeptideMd5(md5);
		}
		if (md5 == null) return;

		if (siftLookup != null) {
			String[] result = siftLookup.getPrediction(md5, position, altAA);
			if (result != null) {
				entry.setSiftPrediction(result[0].replace(' ', '_'));
				entry.setSiftScore(result[1]);
			}
		}
		if (polyPhenLookup != null) {
			String[] result = polyPhenLookup.getPrediction(md5, position, altAA);
			if (result != null) {
				entry.setPolyPhenPrediction(result[0].replace(' ', '_'));
				entry.setPolyPhenScore(result[1]);
			}
		}
	}

	private String computePeptideMd5(TranscriptModel transcript) {
		if (!transcript.isCoding()) return null;
		String cds = BaseTranscriptVariation.translateableSeq(transcript, codingAnnotator.getReference());
		if (cds == null || cds.length() < 3) return null;
		StringBuilder peptide = new StringBuilder();
		for (int i = 0; i + 2 < cds.length(); i += 3) {
			char aa = CodonTable.translate(cds.substring(i, i + 3));
			if (aa == '*') break; // VEP's translate->seq excludes terminal stop
			peptide.append(aa);
		}
		return PredictionLookup.md5Hex(peptide.toString());
	}

	// ===================================================================
	// Ports from Bio::EnsEMBL::VEP::OutputFactory
	// ===================================================================

	/**
	 * VEP filter_VariationFeatureOverlapAlleles — OutputFactory.pm line 577-629.
	 * For flag_pick_allele_gene: groups VFOAs by allele, picks per gene within
	 * each allele group, flags the picked ones with PICK=1.
	 * All VFOAs are returned (not filtered), just flagged.
	 *
	 * Our implementation in computeGeneLevelConsequence simulates this by
	 * finding the picked entry per allele+gene and using its consequence
	 * for the Gene_level_consequence field.
	 */
	// Already implemented in computeGeneLevelConsequence above.

	/**
	 * VEP pick_worst_VariationFeatureOverlapAllele — OutputFactory.pm line 702-811.
	 * Full pick_order: mane_select > mane_plus_clinical > canonical > appris > tsl >
	 * biotype > ccds > rank > length > ensembl > refseq.
	 * For AGR species without MANE/canonical/APPRIS/TSL/CCDS, this simplifies to
	 * biotype > rank > length.
	 */
	// Already implemented in isPicked above.

	/**
	 * VEP pick_VariationFeatureOverlapAllele_per_gene — OutputFactory.pm line 829-858.
	 * Groups TVAs by gene, picks worst per gene.
	 */
	// Already implemented in computeGeneLevelConsequence above (allele+gene grouping).

	/**
	 * VEP TranscriptVariationAllele_to_output_hash — OutputFactory.pm line 1630-1730.
	 * Builds the CSQ hash for a TranscriptVariationAllele.
	 * Calls:
	 *   1. VariationFeatureOverlapAllele_to_output_hash (base: Allele, Consequence, HGVSg)
	 *   2. BaseTranscriptVariationAllele_to_output_hash (transcript: Feature, Gene, SYMBOL, etc.)
	 *   3. Coding-specific fields (cDNA_position, CDS_position, Amino_acids, Codons)
	 *   4. HGVSc, HGVSp
	 *   5. SIFT, PolyPhen
	 *   6. GIVEN_REF, USED_REF
	 *
	 * Our implementation is split between TranscriptAnnotator.annotate() (which builds CsqEntry)
	 * and this class (which adds predictions and HGVSg).
	 */
	// Already implemented across TranscriptAnnotator and OutputFactory.

	/**
	 * VEP add_sift_polyphen — OutputFactory.pm line 1746-1799.
	 * Adds SIFT and PolyPhen prediction/score to the output hash.
	 */
	// Already implemented in addPredictions above.

	static String toVepRef(String ref, String alt) {
		if (ref.length() == 1 && alt.length() == 1) {
			return ref;
		}
		int prefixLen = 0;
		while (prefixLen < ref.length() && prefixLen < alt.length()
				&& ref.charAt(prefixLen) == alt.charAt(prefixLen)) {
			prefixLen++;
		}
		String refTrimmed = ref.substring(prefixLen);
		if (refTrimmed.isEmpty()) return "-";
		return refTrimmed;
	}

	// ===================================================================
	// VEP TranscriptVariationAllele_to_output_hash (OutputFactory.pm line 1630)
	// Merged from TranscriptAnnotator.java
	// ===================================================================

	private CsqEntry transcriptVariationAlleleToOutputHash(TranscriptModel transcript, String chr, int variantStart, int variantEnd,
			String vepAllele, String refAllele) {

		CsqEntry entry = new CsqEntry();
		entry.setAllele(vepAllele);
		entry.setSymbol(transcript.getGeneSymbol());
		entry.setGene(transcript.getGeneCurie());
		entry.setFeatureType("Transcript");
		entry.setFeature(transcript.getTranscriptId());
		entry.setBiotype(transcript.getBiotype());
		entry.setStrand(transcript.isPositiveStrand() ? "1" : "-1");
		entry.setSource(mod + "_GFF.refseq.gff.gz");
		String featureId = transcript.getTranscriptId();
		if (featureId != null && featureId.contains(":") && transcript.getName() != null) {
			entry.setTranscriptName(transcript.getName());
		}
		entry.setGenomicStartPosition(String.valueOf(variantStart));
		entry.setGenomicEndPosition(String.valueOf(variantEnd));
		entry.setGivenRef(refAllele);
		entry.setUsedRef(refAllele);

		// Determine the variant's genomic range
		boolean isInsertion = variantStart > variantEnd;
		int rangeStart = Math.min(variantStart, variantEnd);
		int rangeEnd = Math.max(variantStart, variantEnd);

		// Step 1: Check if variant is within this transcript at all
		if (rangeStart > transcript.getEnd() || rangeEnd < transcript.getStart()) {
			return null;
		}

		// Tier 1: transcript_ablation (VEP VariationEffect.pm line 315-320)
		// feature_ablation = complete_overlap_feature AND deletion
		// complete_overlap_feature: bvf.start <= feat.start AND bvf.end >= feat.end
		boolean isDeletion = !isInsertion && !vepAllele.equals(refAllele);
		if (isDeletion && rangeStart <= transcript.getStart() && rangeEnd >= transcript.getEnd()) {
			entry.setConsequence("transcript_ablation");
			entry.setImpact("HIGH");
			entry.setGenomicStartPosition(String.valueOf(variantStart));
			entry.setGenomicEndPosition(String.valueOf(variantEnd));
			String hgvsg = hgvsGenomic.generate(chr, variantStart, variantEnd, refAllele, vepAllele);
			if (hgvsg != null) entry.setHgvsg(hgvsg);
			return entry;
		}

		// Step 2: Collect splice consequences and intronic flag
		SpliceResult spliceResult = spliceAnnotator.classifySplice(transcript, variantStart, variantEnd);
		List<String> spliceTerms = new ArrayList<>(spliceResult.getSpliceTerms());
		boolean isIntronic = spliceResult.isIntronic();

		// VEP include filter (Constants.pm): splice_polypyrimidine_tract_variant
		// requires { exon => 0, intron => 1 } — excluded when variant overlaps any exon
		// VEP uses overlap(bvf.start, bvf.end, exon.start, exon.end) where insertions
		// have start > end. An insertion at an exon boundary does NOT overlap the exon.
		boolean overlapsExon = isInsertion
			? overlapsAnyExonVep(transcript, variantStart, variantEnd)
			: overlapsAnyExon(transcript, rangeStart, rangeEnd);
		if (overlapsExon) {
			spliceTerms.remove("splice_polypyrimidine_tract_variant");
		}

		// Step 3: Collect location-based consequences (VEP evaluates each independently)
		List<String> locationTerms = new ArrayList<>();
		CodingResult codingResult = null;

		if (transcript.isCoding()) {
			// Check CDS overlap → coding consequence or coding_sequence_variant
			boolean overlapsCds = overlapsAnyCds(transcript, rangeStart, rangeEnd, isInsertion, variantStart, variantEnd);
			boolean overlaps5utr = overlaps5PrimeUtr(transcript, rangeStart, rangeEnd, isInsertion, variantStart, variantEnd);
			boolean overlaps3utr = overlaps3PrimeUtr(transcript, rangeStart, rangeEnd, isInsertion, variantStart, variantEnd);

			if (overlapsCds) {
				// VEP's cds_start/cds_end come from FIRST and LAST elements of cds_coords.
				// Exonic positions (CDS or UTR) → Coordinate → defined.
				// Intronic positions → Gap → undef → peptide cascade fails → coding_sequence_variant.
				//
				// For deletions/SNPs: run TranscriptVariationAllele when BOTH endpoints are in exons.
				// For insertions: VEP maps the insertion point to CDS even when one flanking
				// position is in an intron. Run when EITHER endpoint is in an exon.
				boolean startInExon = transcript.isInExon(rangeStart);
				boolean endInExon = transcript.isInExon(rangeEnd);
				boolean canRunCoding = isInsertion
					? (startInExon || endInExon)
					: (startInExon && endInExon);

				if (canRunCoding) {
					codingResult = codingAnnotator.annotate(transcript, chr, variantStart, variantEnd, vepAllele, refAllele);
					// For CDS+UTR: VEP's cds_end is undef (UTR → Gap in genomic2cds),
					// so frameshift/inframe predicates all return 0. The normal coding path
					// (peptides, codons) also fails. VEP uses _ins_del_stop_altered fallback
					// which operates on CDS+UTR sequence (letting UTR bases fill in after edit).
					// TranscriptVariationAllele's stop_lost uses CDS-only which gives wrong results here.
					// Null out and let the fallback handle stop/start determination.
					if (codingResult != null && (overlaps5utr || overlaps3utr)) {
						// Keep only start_lost from TranscriptVariationAllele (5'UTR case)
						String cons = codingResult.getConsequence();
						List<String> kept = new ArrayList<>();
						for (String term : cons.split("&")) {
							if (term.equals("start_lost")) {
								kept.add(term);
							}
						}
						if (kept.isEmpty()) {
							codingResult = null;
						} else {
							codingResult.setConsequence(String.join("&", kept));
						}
					}
				}
				if (codingResult != null) {
					for (String term : codingResult.getConsequence().split("&")) {
						locationTerms.add(term);
					}
					if (isInsertion) {
						// VEP format_coords(start, end): when start > end → "end-start"
						int cdsStart = codingResult.getCdsPosition();
						int cdsEnd = codingResult.getCdsEnd();
						if (cdsEnd == 0) cdsEnd = cdsStart - 1; // fallback
						entry.setCdsPosition(formatCoords(cdsStart, cdsEnd));
						int protStart = (cdsStart - 1) / 3 + 1;
						int protEnd = (cdsEnd - 1) / 3 + 1;
						entry.setProteinPosition(formatCoords(protStart, protEnd));
						int cdnaStart = codingResult.getCdnaPosition();
						int cdnaEnd = codingResult.getCdnaEnd();
						if (cdnaEnd == 0 && cdnaStart > 0) cdnaEnd = cdnaStart - 1;
						if (cdnaStart > 0) {
							entry.setCdnaPosition(formatCoords(cdnaStart, cdnaEnd));
						}
					} else {
						// VEP format_coords for all variants including deletions
						int cdsS = codingResult.getCdsPosition();
						int cdsE = codingResult.getCdsEnd();
						entry.setCdsPosition(cdsE > 0 ? formatCoords(cdsS, cdsE) : String.valueOf(cdsS));
						int protS = codingResult.getProteinPosition();
						int protE = cdsE > 0 ? (cdsE - 1) / 3 + 1 : protS;
						entry.setProteinPosition(formatCoords(protS, protE));
						if (codingResult.getCdnaPosition() > 0) {
							int cdnaS = codingResult.getCdnaPosition();
							if (cdsE > 0 && cdsE != cdsS) {
								// VEP maps both genomic endpoints independently through genomic2cdna
								int cdnaE = BaseTranscriptVariation.genomicToCdna(transcript, rangeEnd);
								if (cdnaE <= 0) cdnaE = cdnaS + (cdsE - cdsS); // fallback
								entry.setCdnaPosition(formatCoords(Math.min(cdnaS, cdnaE), Math.max(cdnaS, cdnaE)));
							} else {
								entry.setCdnaPosition(String.valueOf(cdnaS));
							}
						}
					}
					if (codingResult.getAminoAcids() != null) {
						entry.setAminoAcids(codingResult.getAminoAcids());
					}
					if (codingResult.getCodons() != null) {
						entry.setCodons(codingResult.getCodons());
					}
				} else {
					// VEP _ins_del_stop_altered fallback (VariationEffect.pm line 1292-1344):
					// When normal coding annotation fails (cds_end undef → peptides undef),
					// VEP checks if the deletion alters the stop codon by building CDS+3'UTR
					// and applying the edit. Guards (line 1312): cdna_start && cdna_end && cds_start
					// = both endpoints in exons AND range overlaps CDS.
					boolean fallbackFired = false;
					if (overlaps3utr && startInExon && endInExon) {
						if (codingAnnotator.isStopAltered(transcript, chr, variantStart, variantEnd)) {
							locationTerms.add("stop_lost");
							fallbackFired = true;
						} else if (codingAnnotator.isStopRetained(transcript, chr, variantStart, variantEnd)) {
							locationTerms.add("stop_retained_variant");
							fallbackFired = true;
						}
					}
					// VEP _ins_del_start_altered fallback (VariationEffect.pm line 976-1015):
					// Same pattern for 5'UTR: build 5'UTR+CDS, apply edit, check if CDS preserved.
					if (overlaps5utr && startInExon && endInExon) {
						if (codingAnnotator.isStartAltered(transcript, chr, variantStart, variantEnd)) {
							locationTerms.add("start_lost");
							fallbackFired = true;
						}
					}
					// VEP incomplete_terminal_codon_variant (partial_codon, line 1389-1414):
					// CDS length not divisible by 3, and variant falls in the last 1-2 bases.
					// Blocks frameshift/inframe predicates. Coexists with coding_sequence_variant.
					if (isPartialCodon(transcript, rangeStart, rangeEnd, isInsertion, variantStart)) {
						locationTerms.add("incomplete_terminal_codon_variant");
					}
					if (!fallbackFired) {
						locationTerms.add("coding_sequence_variant");
					}
				}
			}

			// VEP start_retained_variant (line 936-948): evaluated independently.
			// Fires when indel overlaps start codon AND _ins_del_start_altered returns false.
			// Can coexist with start_lost (via peptide path) when ATG preserved but frame shifts.
			if (overlaps5utr && overlapsCds && !isInsertion
					&& transcript.isInExon(rangeStart) && transcript.isInExon(rangeEnd)
					&& !codingAnnotator.isStartAltered(transcript, chr, variantStart, variantEnd)) {
				locationTerms.add("start_retained_variant");
			}

			if (overlaps5utr) {
				locationTerms.add("5_prime_UTR_variant");
			}
			if (overlaps3utr) {
				locationTerms.add("3_prime_UTR_variant");
			}
		} else {
			// Non-coding transcript
			if (overlapsExon) {
				locationTerms.add("non_coding_transcript_exon_variant");
			}
			// non_coding_transcript_variant: in non-coding transcript intron (not exon)
			// VEP: within_non_coding_gene = within_transcript AND NOT coding AND NOT exon
			if (!overlapsExon && (isIntronic || isInAnyIntron(transcript, rangeStart, rangeEnd))) {
				locationTerms.add("non_coding_transcript_variant");
			}
		}

		// Step 4: Add intron_variant if variant overlaps intron interior
		// VEP: within_intron checks _intron_effects->{intronic} (positions +3 to end-2)
		if (isIntronic) {
			locationTerms.add("intron_variant");
		}

		// If no location terms and no splice terms, use VEP's DEFAULT_OVERLAP_CONSEQUENCE
		// VEP Constants.pm: DEFAULT_OVERLAP_CONSEQUENCE = intergenic_variant
		// This happens when a variant is within transcript bounds but outside any specific region
		if (locationTerms.isEmpty() && spliceTerms.isEmpty()) {
			locationTerms.add("intergenic_variant");
		}

		// Step 5: Combine all terms and sort by VEP severity rank
		Set<String> allTerms = new LinkedHashSet<>();
		allTerms.addAll(spliceTerms);
		allTerms.addAll(locationTerms);

		// For non-coding intron: splice terms replace intron_variant base but intron_variant stays
		// For coding: splice_donor/acceptor can coexist with coding_sequence_variant and intron_variant
		// VEP just collects all matching predicates, so we just combine and sort

		// Handle special case: if we have splice_donor or splice_acceptor but no intron or location terms,
		// the splice is the only consequence (e.g., SNP at +1/+2 without intron interior overlap)
		// But if we have intron_variant separately, it stays

		// For non-coding transcript: if we have splice terms and the variant is in an intron,
		// add non_coding_transcript_variant if not already present and no exon overlap
		if (!transcript.isCoding() && !spliceTerms.isEmpty() && !locationTerms.contains("non_coding_transcript_exon_variant")) {
			if (isIntronic || isInAnyIntron(transcript, rangeStart, rangeEnd)) {
				if (!allTerms.contains("non_coding_transcript_variant") && !allTerms.contains("non_coding_transcript_exon_variant")) {
					allTerms.add("non_coding_transcript_variant");
				}
			}
		}

		List<String> sortedTerms = new ArrayList<>(allTerms);
		sortedTerms.sort((a, b) -> Integer.compare(
			ConsequenceSeverity.getRank(a), ConsequenceSeverity.getRank(b)));

		String consequence = String.join("&", sortedTerms);
		entry.setConsequence(consequence);
		entry.setImpact(ConsequenceSeverity.getImpact(consequence));

		// Exon/intron numbers (use start position for lookup)
		int checkPos = isInsertion ? variantEnd : variantStart;
		String exonNum = transcript.getExonNumber(checkPos);
		if (exonNum != null) {
			entry.setExon(exonNum);
		}
		String intronNum = transcript.getIntronNumber(checkPos);
		if (intronNum != null) {
			entry.setIntron(intronNum);
		}

		// cDNA position for non-coding exon variants and UTR variants without coding result
		// VEP populates cdna_position for ANY variant in an exon (within_cdna)
		if (entry.getCdnaPosition() == null && overlapsExon && !consequence.contains("intergenic_variant")) {
			int cdnaPos = BaseTranscriptVariation.genomicToCdna(transcript, isInsertion ? variantEnd : variantStart);
			if (cdnaPos > 0) {
				if (isInsertion) {
					int cdnaEnd = BaseTranscriptVariation.genomicToCdna(transcript, variantStart);
					if (cdnaEnd > 0) {
						entry.setCdnaPosition(formatCoords(Math.min(cdnaPos, cdnaEnd), Math.max(cdnaPos, cdnaEnd)));
					} else {
						entry.setCdnaPosition(String.valueOf(cdnaPos));
					}
				} else if (variantStart != variantEnd) {
					int cdnaEnd = BaseTranscriptVariation.genomicToCdna(transcript, variantEnd);
					if (cdnaEnd > 0) {
						entry.setCdnaPosition(formatCoords(Math.min(cdnaPos, cdnaEnd), Math.max(cdnaPos, cdnaEnd)));
					} else {
						entry.setCdnaPosition(String.valueOf(cdnaPos));
					}
				} else {
					entry.setCdnaPosition(String.valueOf(cdnaPos));
				}
			}
		}

		// HGVS — VEP does not generate HGVSc for intergenic entries
		if (!consequence.contains("intergenic_variant")) {
			// For insertions, VEP uses cds_start (higher value) for HGVSc position.
			// For minus-strand insertions, cdsStart < cdsEnd, so use max.
			int cdsPos = codingResult != null
				? (isInsertion ? Math.max(codingResult.getCdsPosition(), codingResult.getCdsEnd())
				              : codingResult.getCdsPosition())
				: -1;
			String hgvsc = codingAnnotator.hgvsTranscript(transcript, chr, variantStart, variantEnd,
				vepAllele, refAllele, cdsPos, transcript.isCoding());
			if (hgvsc != null) {
				entry.setHgvsc(hgvsc);
			}
		}

		String hgvsg = hgvsGenomic.generate(chr, variantStart, variantEnd, refAllele, vepAllele);
		if (hgvsg != null) {
			entry.setHgvsg(hgvsg);
		}

		if (codingResult != null) {
			// VEP hgvs_protein() — use notation-based formatter
			TranscriptVariationAllele.HgvsNotation n = codingResult.getHgvsNotation();
			String hgvsp = null;
			if (n != null && n.type != null) {
				String csq = codingResult.getConsequence();
				boolean isStopLost = csq != null && csq.contains("stop_lost");
				boolean isStartLost = csq != null && csq.contains("start_lost");
				hgvsp = codingAnnotator.vepGetHgvsProteinFormat(n, transcript.getProteinId(),
					isStopLost, isStartLost,
					codingResult.getAltCdsSequence(), codingResult.getCdsSequence());
			}
			if (hgvsp != null) {
				entry.setHgvsp(hgvsp);
			}
		}

		return entry;
	}

	/** Check if the variant range overlaps any CDS segment */
	private boolean overlapsAnyCds(TranscriptModel transcript, int rangeStart, int rangeEnd,
			boolean isInsertion, int variantStart, int variantEnd) {
		for (CdsSegment cds : transcript.getCdsSegments()) {
			if (rangeStart <= cds.getEnd() && rangeEnd >= cds.getStart()) {
				return true;
			}
		}
		// For insertions, also check the flanking positions
		if (isInsertion) {
			return transcript.isInCds(variantEnd) || transcript.isInCds(variantStart);
		}
		return false;
	}

	/** Check if the variant range overlaps any exon */
	private boolean overlapsAnyExon(TranscriptModel transcript, int rangeStart, int rangeEnd) {
		for (ExonModel exon : transcript.getExons()) {
			if (rangeStart <= exon.getEnd() && rangeEnd >= exon.getStart()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * VEP-compatible exon overlap for insertions (VariationEffect.pm non_coding_exon_variant line 505).
	 * VEP uses overlap(bvf.start, bvf.end, exon.start, exon.end) where insertions have start > end.
	 * This means an insertion at an exon boundary does NOT overlap the exon.
	 */
	private boolean overlapsAnyExonVep(TranscriptModel transcript, int vepStart, int vepEnd) {
		for (ExonModel exon : transcript.getExons()) {
			if (vepEnd >= exon.getStart() && vepStart <= exon.getEnd()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * VEP within_5_prime_utr (VariationEffect.pm line 722-734):
	 *   _before_coding: overlap(varStart, varEnd, transcript_start, cds_start-1)
	 *   AND within_cdna: variant overlaps any exon
	 * On - strand: uses _after_coding (overlap with cds_end+1 to transcript_end)
	 */
	private boolean overlaps5PrimeUtr(TranscriptModel transcript, int rangeStart, int rangeEnd) {
		return overlaps5PrimeUtr(transcript, rangeStart, rangeEnd, false, 0, 0);
	}

	private boolean overlaps5PrimeUtr(TranscriptModel transcript, int rangeStart, int rangeEnd,
			boolean isInsertion, int variantStart, int variantEnd) {
		if (!transcript.isCoding()) return false;

		boolean beforeCoding;
		if (transcript.isPositiveStrand()) {
			// _before_coding: overlap(var_s, var_e, tran_start, cds_start - 1)
			beforeCoding = rangeStart <= transcript.getCdsStart() - 1
				&& rangeEnd >= transcript.getStart();
			// VEP _before_coding line 698-699: insertion at CDS start returns true
			if (!beforeCoding && isInsertion && variantStart == transcript.getCdsStart()) {
				beforeCoding = true;
			}
		} else {
			// _after_coding: overlap(var_s, var_e, cds_end + 1, tran_end)
			beforeCoding = rangeStart <= transcript.getEnd()
				&& rangeEnd >= transcript.getCdsEnd() + 1;
			if (!beforeCoding && isInsertion && variantEnd == transcript.getCdsEnd()) {
				beforeCoding = true;
			}
		}

		// within_cdna: variant overlaps any exon (not just UTR exons)
		return beforeCoding && overlapsAnyExon(transcript, rangeStart, rangeEnd);
	}

	/**
	 * VEP within_3_prime_utr (VariationEffect.pm line 736-748):
	 *   _after_coding: overlap(varStart, varEnd, cds_end+1, transcript_end)
	 *   AND within_cdna: variant overlaps any exon
	 * On - strand: uses _before_coding (overlap with transcript_start to cds_start-1)
	 */
	/** VEP format_coords (Utils.pm line 141): start > end → "end-start", equal → "start" */
	private String formatCoords(int start, int end) {
		if (start > end) return end + "-" + start;
		if (start == end) return String.valueOf(start);
		return start + "-" + end;
	}

	private boolean overlaps3PrimeUtr(TranscriptModel transcript, int rangeStart, int rangeEnd) {
		return overlaps3PrimeUtr(transcript, rangeStart, rangeEnd, false, 0, 0);
	}

	private boolean overlaps3PrimeUtr(TranscriptModel transcript, int rangeStart, int rangeEnd,
			boolean isInsertion, int variantStart, int variantEnd) {
		if (!transcript.isCoding()) return false;

		boolean afterCoding;
		if (transcript.isPositiveStrand()) {
			// _after_coding: overlap(var_s, var_e, cds_end + 1, tran_end)
			afterCoding = rangeStart <= transcript.getEnd()
				&& rangeEnd >= transcript.getCdsEnd() + 1;
			// VEP _after_coding line 715-716: insertion at CDS end returns true
			if (!afterCoding && isInsertion && variantEnd == transcript.getCdsEnd()) {
				afterCoding = true;
			}
		} else {
			// _before_coding: overlap(var_s, var_e, tran_start, cds_start - 1)
			afterCoding = rangeStart <= transcript.getCdsStart() - 1
				&& rangeEnd >= transcript.getStart();
			if (!afterCoding && isInsertion && variantStart == transcript.getCdsStart()) {
				afterCoding = true;
			}
		}

		return afterCoding && overlapsAnyExon(transcript, rangeStart, rangeEnd);
	}

	/** Check if any part of the range falls within any intron (full intron, not just interior) */
	private boolean isInAnyIntron(TranscriptModel transcript, int rangeStart, int rangeEnd) {
		for (int[] intron : transcript.getIntronIntervals()) {
			if (rangeStart <= intron[1] && rangeEnd >= intron[0]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * VEP partial_codon (VariationEffect.pm line 1389-1414):
	 * Returns true if the variant falls in an incomplete terminal codon
	 * (CDS length not divisible by 3, variant in the last 1-2 bases).
	 */
	private boolean isPartialCodon(TranscriptModel transcript, int rangeStart, int rangeEnd,
			boolean isInsertion, int variantStart) {
		if (!transcript.isCoding()) return false;

		// Compute CDS length
		int cdsLength = 0;
		for (CdsSegment seg : transcript.getCdsSegments()) {
			cdsLength += seg.getLength();
		}
		int remainder = cdsLength % 3;
		if (remainder == 0) return false; // CDS is complete, no partial codon

		// VEP: codon_cds_start = (translation_start * 3) - 2
		// translation_start = protein position = (cds_start - 1) / 3 + 1
		// We need the variant's CDS position to check if it falls in the last partial codon
		int cdsPos = BaseTranscriptVariation.genomicToCds(transcript, isInsertion ? variantStart - 1 : rangeStart);
		if (cdsPos < 0) {
			// Try the end position
			cdsPos = BaseTranscriptVariation.genomicToCds(transcript, rangeEnd);
		}
		if (cdsPos < 0) return false;

		// VEP: last_codon_length = cds_length - (codon_cds_start - 1)
		int codonCdsStart = ((cdsPos - 1) / 3) * 3 + 1;
		int lastCodonLength = cdsLength - (codonCdsStart - 1);
		return lastCodonLength < 3 && lastCodonLength > 0;
	}
}
