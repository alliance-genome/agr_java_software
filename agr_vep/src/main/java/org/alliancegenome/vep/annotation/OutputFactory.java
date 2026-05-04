package org.alliancegenome.vep.annotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.vep.annotation.VariationEffect.Context;
import org.alliancegenome.vep.annotation.VariationEffect.SpliceResult;
import org.alliancegenome.vep.bio.CodonTable;
import org.alliancegenome.vep.csq.CsqEntry;
import org.alliancegenome.vep.debug.Trace;
import org.alliancegenome.vep.hgvs.VariationFeature;
import org.alliancegenome.vep.model.CdsSegment;
import org.alliancegenome.vep.model.ExonModel;
import org.alliancegenome.vep.model.GeneModel;
import org.alliancegenome.vep.model.TranscriptModel;
import org.alliancegenome.vep.plugin.PredictionLookup;
import org.alliancegenome.vep.reference.ContigAccessionMap;
import org.alliancegenome.vep.reference.ReferenceGenome;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;

public class OutputFactory {

	private final GeneModel geneModel;
	private final TranscriptVariationAllele codingAnnotator;
	private final VariationFeature hgvsGenomic;

	private final String mod;
	private final PredictionLookup siftLookup;
	private final PredictionLookup polyPhenLookup;

	public OutputFactory(GeneModel geneModel, ReferenceGenome reference, ContigAccessionMap contigMap, String mod) {
		this(geneModel, reference, contigMap, mod, null, null);
	}

	public OutputFactory(GeneModel geneModel, ReferenceGenome reference, ContigAccessionMap contigMap, String mod, PredictionLookup siftLookup, PredictionLookup polyPhenLookup) {
		this.geneModel = geneModel;
		this.codingAnnotator = new TranscriptVariationAllele(reference);
		this.hgvsGenomic = new VariationFeature(contigMap, reference);

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
			if (isIndel && ref.length() > 0 && alt.length() > 0 && ref.charAt(0) == alt.charAt(0)) {
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
					CsqEntry entry = transcriptVariationAlleleToOutputHash(transcript, chr, variantStart, variantEnd, vepAllele, vepRef);
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

		// VEP outputs CSQ entries in transcript-major order (OutputFactory.pm line
		// 513):
		// for each transcript (sorted by stable ID), for each allele (VCF order).
		// Our loop is allele-major; stable-sort by Feature to match VEP ordering.
		allEntries.sort(Comparator.comparing((CsqEntry e) -> e.getFeature() != null ? e.getFeature() : ""));

		return allEntries;
	}

	private void computeGeneLevelConsequence(List<CsqEntry> entries) {
		// ProcessOutput.pm lines 49-63:
		// 1. VEP --flag_pick_allele_gene picks ONE transcript per allele+gene
		// (pick_order: mane_select > canonical > appris > tsl > biotype > ccds > rank >
		// length)
		// 2. For each PICKED entry, store its consequence keyed by ALLELE (last PICK
		// per allele wins)
		// 3. All entries get the stored consequence for their allele

		// Step 1: Determine which entries would have PICK flag set.
		// VEP flag_pick_allele_gene (OutputFactory.pm lines 622-626, 829-858):
		// Groups by allele, then within each allele groups by gene,
		// picks worst (most severe) transcript per gene.
		// We simulate this: for each allele+gene, pick the entry with the most severe
		// consequence
		// (then longest transcript as tiebreaker — matching VEP pick_order after
		// biotype/rank).
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
		// For each PICKED entry, store consequence keyed by allele (last one wins).
		// Perl's iteration order is the ORIGINAL CSQ order, so iterate `entries`
		// (not pickedPerAlleleGene.values() — that iterates by gene-insertion order
		// which can overwrite with a LESS severe pick from a later-inserted gene).
		Set<CsqEntry> pickedSet = new java.util.HashSet<>(pickedPerAlleleGene.values());
		Map<String, String> glcPerAllele = new HashMap<>();
		for (CsqEntry entry : entries) {
			if (!pickedSet.contains(entry)) continue;
			String allele = entry.getAllele() != null ? entry.getAllele() : "";
			Trace.log("GLC.iterate", "allele=%s gene=%s feat=%s csq=%s prev_glc=%s",
				allele, entry.getGene(), entry.getFeature(), entry.getConsequence(), glcPerAllele.get(allele));
			glcPerAllele.put(allele, entry.getConsequence());
		}

		// Step 3: ProcessOutput.pm line 63:
		// Set GLC on all entries = stored consequence for their allele
		for (CsqEntry entry : entries) {
			String allele = entry.getAllele() != null ? entry.getAllele() : "";
			entry.setGeneLevelConsequence(glcPerAllele.get(allele));
		}
	}

	/**
	 * VEP pick_worst_VariationFeatureOverlapAllele (OutputFactory.pm lines
	 * 702-811): Returns true if candidate should be picked over current. Pick
	 * order: mane_select > canonical > appris > tsl > biotype > ccds > rank >
	 * length For AGR species (no MANE/canonical/APPRIS/TSL/CCDS): falls to rank
	 * then length.
	 */
	private boolean isPicked(CsqEntry candidate, CsqEntry current) {
		// VEP pick_order (OutputFactory.pm line 680-793): mane_select, mane_plus_clinical,
		// canonical, tsl, appris, biotype, ccds, rank, length, ensembl, refseq.
		// We approximate with biotype > rank > length — the categories we have.
		// Biotype: protein_coding=0, other=1 (lower is better).
		int bCand = "protein_coding".equals(candidate.getBiotype()) ? 0 : 1;
		int bCurr = "protein_coding".equals(current.getBiotype()) ? 0 : 1;
		if (bCand != bCurr) return bCand < bCurr;
		// Rank: lower = more severe = better
		int rankCand = ConsequenceSeverity.getMostSevereRank(candidate.getConsequence());
		int rankCurr = ConsequenceSeverity.getMostSevereRank(current.getConsequence());
		if (rankCand != rankCurr) return rankCand < rankCurr;
		// Length: longer is better (VEP line 740-744 inverts to make lowest = longest)
		if (candidate.getTranscriptLength() != current.getTranscriptLength()) {
			return candidate.getTranscriptLength() > current.getTranscriptLength();
		}
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
		while (prefixLen < ref.length() && prefixLen < alt.length() && ref.charAt(prefixLen) == alt.charAt(prefixLen)) {
			prefixLen++;
		}
		String altTrimmed = alt.substring(prefixLen);
		String refTrimmed = ref.substring(prefixLen);

		if (altTrimmed.isEmpty()) {
			return "-";
		}
		if (refTrimmed.isEmpty()) {
			return altTrimmed;
		}
		return altTrimmed;
	}

	/**
	 * Add SIFT/PolyPhen predictions to a CSQ entry. VEP only looks up predictions
	 * for single amino acid substitutions (pep_allele_string =~ /^[A-Z]\/[A-Z]$/).
	 */
	private void addPredictions(CsqEntry entry, TranscriptModel transcript) {
		if (siftLookup == null && polyPhenLookup == null) {
			return;
		}

		String aminoAcids = entry.getAminoAcids();
		if (aminoAcids == null || aminoAcids.length() != 3 || aminoAcids.charAt(1) != '/') {
			return;
		}
		char refAA = aminoAcids.charAt(0);
		char altAA = aminoAcids.charAt(2);
		if (refAA == altAA || refAA == '*' || altAA == '*' || refAA == 'X' || altAA == 'X') {
			return;
		}

		String protPos = entry.getProteinPosition();
		if (protPos == null) {
			return;
		}
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
		if (md5 == null) {
			return;
		}

		if (siftLookup != null) {
			String[] result = siftLookup.getPrediction(md5, position, altAA);
			if (result != null) {
				Trace.log("addPredictions.sift", "tr=%s md5=%s pos=%d alt=%c pred=%s score=%s",
					transcript.getTranscriptId(), md5, position, altAA, result[0], result[1]);
				entry.setSiftPrediction(result[0].replace(' ', '_'));
				entry.setSiftScore(result[1]);
			}
		}
		if (polyPhenLookup != null) {
			String[] result = polyPhenLookup.getPrediction(md5, position, altAA);
			if (result != null) {
				Trace.log("addPredictions.polyphen", "tr=%s md5=%s pos=%d alt=%c pred=%s score=%s",
					transcript.getTranscriptId(), md5, position, altAA, result[0], result[1]);
				entry.setPolyPhenPrediction(result[0].replace(' ', '_'));
				entry.setPolyPhenScore(result[1]);
			}
		}
	}

	private String computePeptideMd5(TranscriptModel transcript) {
		if (!transcript.isCoding()) {
			return null;
		}
		String cds = BaseTranscriptVariation.translateableSeq(transcript, codingAnnotator.getReference());
		if (cds == null || cds.length() < 3) {
			return null;
		}
		// Perl's Transcript::translate removes the terminal stop codon from
		// the mRNA ONLY when length % 3 == 0, then translates ALL remaining
		// codons (internal stops become '*' in the peptide).
		int wholeLen = (cds.length() / 3) * 3;
		int translateLen = wholeLen;
		if (cds.length() % 3 == 0) {
			String lastCodon = cds.substring(wholeLen - 3, wholeLen);
			if (CodonTable.translate(lastCodon) == '*') {
				translateLen = wholeLen - 3;
			}
		}
		StringBuilder peptide = new StringBuilder();
		for (int i = 0; i < translateLen; i += 3) {
			char aa = CodonTable.translate(cds.substring(i, i + 3));
			peptide.append(aa);
		}
		return PredictionLookup.md5Hex(peptide.toString());
	}

	// ===================================================================
	// Ports from Bio::EnsEMBL::VEP::OutputFactory
	// ===================================================================

	/**
	 * VEP filter_VariationFeatureOverlapAlleles — OutputFactory.pm line 577-629.
	 * For flag_pick_allele_gene: groups VFOAs by allele, picks per gene within each
	 * allele group, flags the picked ones with PICK=1. All VFOAs are returned (not
	 * filtered), just flagged.
	 *
	 * Our implementation in computeGeneLevelConsequence simulates this by finding
	 * the picked entry per allele+gene and using its consequence for the
	 * Gene_level_consequence field.
	 */
	// Already implemented in computeGeneLevelConsequence above.

	/**
	 * VEP pick_worst_VariationFeatureOverlapAllele — OutputFactory.pm line 702-811.
	 * Full pick_order: mane_select > mane_plus_clinical > canonical > appris > tsl
	 * > biotype > ccds > rank > length > ensembl > refseq. For AGR species without
	 * MANE/canonical/APPRIS/TSL/CCDS, this simplifies to biotype > rank > length.
	 */
	// Already implemented in isPicked above.

	/**
	 * VEP pick_VariationFeatureOverlapAllele_per_gene — OutputFactory.pm line
	 * 829-858. Groups TVAs by gene, picks worst per gene.
	 */
	// Already implemented in computeGeneLevelConsequence above (allele+gene
	// grouping).

	/**
	 * VEP TranscriptVariationAllele_to_output_hash — OutputFactory.pm line
	 * 1630-1730. Builds the CSQ hash for a TranscriptVariationAllele. Calls: 1.
	 * VariationFeatureOverlapAllele_to_output_hash (base: Allele, Consequence,
	 * HGVSg) 2. BaseTranscriptVariationAllele_to_output_hash (transcript: Feature,
	 * Gene, SYMBOL, etc.) 3. Coding-specific fields (cDNA_position, CDS_position,
	 * Amino_acids, Codons) 4. HGVSc, HGVSp 5. SIFT, PolyPhen 6. GIVEN_REF, USED_REF
	 *
	 * Our implementation is split between TranscriptAnnotator.annotate() (which
	 * builds CsqEntry) and this class (which adds predictions and HGVSg).
	 */
	// Already implemented across TranscriptAnnotator and OutputFactory.

	/**
	 * VEP add_sift_polyphen — OutputFactory.pm line 1746-1799. Adds SIFT and
	 * PolyPhen prediction/score to the output hash.
	 */
	// Already implemented in addPredictions above.

	static String toVepRef(String ref, String alt) {
		if (ref.length() == 1 && alt.length() == 1) {
			return ref;
		}
		int prefixLen = 0;
		while (prefixLen < ref.length() && prefixLen < alt.length() && ref.charAt(prefixLen) == alt.charAt(prefixLen)) {
			prefixLen++;
		}
		String refTrimmed = ref.substring(prefixLen);
		if (refTrimmed.isEmpty()) {
			return "-";
		}
		return refTrimmed;
	}

	// ===================================================================
	// VEP TranscriptVariationAllele_to_output_hash (OutputFactory.pm line 1630)
	// Merged from TranscriptAnnotator.java
	// ===================================================================

	private CsqEntry transcriptVariationAlleleToOutputHash(TranscriptModel transcript, String chr, int variantStart, int variantEnd, String vepAllele, String refAllele) {

		CsqEntry entry = new CsqEntry();
		entry.setAllele(vepAllele);
		entry.setSymbol(transcript.getGeneSymbol());
		entry.setGene(transcript.getGeneCurie());
		entry.setFeatureType("Transcript");
		entry.setFeature(transcript.getTranscriptId());
		entry.setBiotype(transcript.getBiotype());
		entry.setStrand(transcript.isPositiveStrand() ? "1" : "-1");
		entry.setSource(mod + "_GFF.refseq.gff.gz");
		// Transcript length for pick_order tiebreaker — VEP OutputFactory.pm line
		// 740-744 uses translateable_seq length when a translation exists (coding),
		// otherwise transcript length (sum of exon lengths).
		int trLen = 0;
		if (transcript.isCoding()) {
			for (org.alliancegenome.vep.model.CdsSegment cs : transcript.getCdsSegments()) {
				trLen += cs.getEnd() - cs.getStart() + 1;
			}
		} else {
			for (org.alliancegenome.vep.model.ExonModel ex : transcript.getExons()) {
				trLen += ex.getEnd() - ex.getStart() + 1;
			}
		}
		entry.setTranscriptLength(trLen);
		String featureId = transcript.getTranscriptId();
		// Perl's ProtFuncTranscriptNameHTP plugin queries the AGR transcript_map DB;
		// for transcripts not in the DB, it falls back to the GFF Name. Our TMAP TSV
		// is a partial export (missing rows for some transcripts like WB:R06C1.1.2),
		// but the underlying DB returns a name for every transcript equal to its GFF
		// Name attribute. Emit transcript_name whenever tm.name is set (TMAP override
		// or GFF Name) — matches Perl's DB behavior.
		// Exception: transcripts whose stable_id is "Pseudogene:..." are not in the DB
		// (they lack a MOD-prefixed transcript_id), so Perl returns empty.
		if (featureId != null && featureId.contains(":")
				&& !featureId.startsWith("Pseudogene:")
				&& transcript.getName() != null) {
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
			if (hgvsg != null) {
				entry.setHgvsg(hgvsg);
			}
			// VEP populates EXON/INTRON even for transcript_ablation
			BaseTranscriptVariation ablBvt = new BaseTranscriptVariation(transcript, variantStart, variantEnd);
			String exonNum = ablBvt.exonNumber();
			String intronNum = ablBvt.intronNumber();
			if (exonNum != null) entry.setExon(exonNum);
			if (intronNum != null) entry.setIntron(intronNum);
			return entry;
		}

		// Step 2: Collect splice consequences and intronic flag
		SpliceResult spliceResult = VariationEffect.classifySplice(transcript, variantStart, variantEnd);
		List<String> spliceTerms = new ArrayList<>(spliceResult.getSpliceTerms());
		boolean isIntronic = spliceResult.isIntronic();

		// Perl _bvfo_preds: pre-consequence predicates for include filter gating.
		PreConsequencePredicates preds = PreConsequencePredicates.compute(
			transcript, variantStart, variantEnd, refAllele, vepAllele,
			false, false);
		// Unstretched exon overlap for non-coding classification (Perl
		// non_coding_exon_variant double-checks with actual exon coords)
		boolean overlapsExon = isInsertion
			? overlapsAnyExonVep(transcript, variantStart, variantEnd)
			: overlapsAnyExon(transcript, rangeStart, rangeEnd);
		// Perl _skip_oc: polypyrimidine include filter uses STRETCHED exon
		// pre-pred (exon=0 required). The stretch applies 12bp for transcripts
		// with frameshift introns (abs(intron_end-intron_start) <= 12).
		if (preds.shouldSkip("splice_polypyrimidine_tract_variant")) {
			spliceTerms.remove("splice_polypyrimidine_tract_variant");
		} else if (overlapsExon) {
			// Fallback: even without stretch, remove polypyrimidine when
			// variant actually overlaps an exon (original behavior)
			spliceTerms.remove("splice_polypyrimidine_tract_variant");
		}

		// Step 3: Collect location-based consequences (VEP evaluates each
		// independently)
		List<String> locationTerms = new ArrayList<>();
		TranscriptVariationAllele tva = null;
		// VEP always creates BVT for any variant overlapping a transcript (coding or not).
		// Used for exon_number/intron_number ranges and (when coding) coordinate mapping.
		BaseTranscriptVariation bvt = new BaseTranscriptVariation(transcript, variantStart, variantEnd);

		if (transcript.isCoding()) {
			// Check CDS overlap → coding consequence or coding_sequence_variant
			boolean overlapsCds = overlapsAnyCds(transcript, rangeStart, rangeEnd, isInsertion, variantStart, variantEnd);
			// Perl within_cds: checks if ANY cds_coord maps to a Coordinate (not Gap).
			// For insertions at exon-intron boundaries, the geometric overlap fails but
			// BVT still maps one endpoint to CDS. Use BVT as fallback.
			if (!overlapsCds && bvt != null && (bvt.cdsStart() > 0 || bvt.cdsEnd() > 0)) {
				overlapsCds = true;
			}
			boolean overlaps5utr = overlaps5PrimeUtr(transcript, rangeStart, rangeEnd, isInsertion, variantStart, variantEnd);
			boolean overlaps3utr = overlaps3PrimeUtr(transcript, rangeStart, rangeEnd, isInsertion, variantStart, variantEnd);
			// Perl's within_cdna uses cDNA coordinate mapping (not geometric exon overlap).
			// For insertions at exon/intron boundaries, the mapper returns valid cDNA coords
			// even though geometric overlap misses. Use BVT cDNA coords as within_cdna fallback,
			// with Perl's exact guards: coord.end > 0 AND coord.start <= feat.length
			if (bvt != null && isInsertion && !overlaps5utr && !overlaps3utr) {
				int cdnaS = bvt.cdnaStart();
				int cdnaE = bvt.cdnaEnd();
				// Perl within_cdna: coord.end > 0 AND coord.start <= feat.length
				int trCdnaLen = 0;
				for (ExonModel ex : transcript.getExons()) trCdnaLen += ex.getEnd() - ex.getStart() + 1;
				if (cdnaE > 0 && cdnaS <= trCdnaLen) {
					// Valid cDNA mapping within transcript. Check _before/_after coding.
					int trStart = transcript.getStart();
					int trEnd = transcript.getEnd();
					int cdsStart = transcript.getCdsStart();
					int cdsEnd = transcript.getCdsEnd();
					if (transcript.isPositiveStrand()) {
						if (variantEnd >= trStart && variantStart <= cdsStart - 1) overlaps5utr = true;
						if (variantEnd >= cdsEnd + 1 && variantStart <= trEnd) overlaps3utr = true;
					} else {
						if (variantEnd >= cdsEnd + 1 && variantStart <= trEnd) overlaps5utr = true;
						if (variantEnd >= trStart && variantStart <= cdsStart - 1) overlaps3utr = true;
					}
				}
			}

			if (overlapsCds) {
				// VEP's cds_start/cds_end come from FIRST and LAST elements of cds_coords.
				// Exonic positions (CDS or UTR) → Coordinate → defined.
				// Intronic positions → Gap → undef → peptide cascade fails →
				// coding_sequence_variant.
				//
				// For deletions/SNPs: run TranscriptVariationAllele when BOTH endpoints are in
				// exons.
				// For insertions: VEP maps the insertion point to CDS even when one flanking
				// position is in an intron. Run when EITHER endpoint is in an exon.
				boolean startInExon = transcript.isInExon(rangeStart);
				boolean endInExon = transcript.isInExon(rangeEnd);
				boolean canRunCoding = isInsertion ? (startInExon || endInExon) : (startInExon && endInExon);

				if (canRunCoding) {
					// Create TVA with BVT (Perl: TranscriptVariationAllele created per allele)
					tva = new TranscriptVariationAllele(bvt, transcript, codingAnnotator.getReference(), chr, variantStart, variantEnd, vepAllele, refAllele);
					// For CDS+UTR: VEP's cds_end is undef (UTR → Gap in genomic2cds),
					// so frameshift/inframe predicates all return 0. The normal coding path
					// (peptides, codons) also fails. VEP uses _ins_del_stop_altered fallback
					// which operates on CDS+UTR sequence (letting UTR bases fill in after edit).
					// TranscriptVariationAllele's stop_lost uses CDS-only which gives wrong results
					// here.
					// Null out and let the fallback handle stop/start determination.
					if (tva.getConsequence() != null && (overlaps5utr || overlaps3utr)) {
						// Keep only start_lost from TranscriptVariationAllele (5'UTR case)
						String cons = tva.getConsequence();
						boolean hasStartLost = false;
						for (String term : cons.split("&")) {
							if (term.equals("start_lost")) {
								hasStartLost = true;
								break;
							}
						}
						if (!hasStartLost) {
							tva = null;
						}
					}
				}
				// VEP OutputFactory.pm line 1685-1689: format_coords(cds_start, cds_end)
				// Always populate from BVT (matches Perl: $tv->cds_start owns positions),
				// regardless of whether TVA inline consequence is kept.
				if (bvt != null) {
					int cdsS = bvt.cdsStart();
					int cdsE = bvt.cdsEnd();
					if (cdsS > 0 || cdsE > 0) {
						entry.setCdsPosition(formatCoords(cdsS, cdsE));
					}
					int protS = bvt.translationStart();
					int protE = bvt.translationEnd();
					if (protS > 0 || protE > 0) {
						entry.setProteinPosition(formatCoords(protS, protE));
					}
					int cdnaS = bvt.cdnaStart();
					int cdnaE = bvt.cdnaEnd();
					if (cdnaS > 0 || cdnaE > 0) {
						entry.setCdnaPosition(formatCoords(cdnaS, cdnaE));
					}
				}
				if (tva != null && tva.getConsequence() != null && !tva.getConsequence().isEmpty()) {
					for (String term : tva.getConsequence().split("&")) {
						if (term.isEmpty()) continue;
						// For CDS+UTR overlap, only keep start_lost
						if ((overlaps5utr || overlaps3utr) && !term.equals("start_lost")) {
							continue;
						}
						locationTerms.add(term);
					}
					// VEP evaluates stop_lost independently of other predicates.
					// Perl's stop_lost (VariationEffect.pm line 1168-1221): checks
					// _get_peptide_alleles — if ref peptide has '*' but alt doesn't,
					// it's stop_lost. For large frameshifts the ref AA often contains
					// the original stop codon.
					if (!locationTerms.contains("stop_lost")) {
						String aa = tva.getAminoAcids();
						if (aa != null && aa.contains("/")) {
							String refAa = aa.substring(0, aa.indexOf('/'));
							String altAa = aa.substring(aa.indexOf('/') + 1);
							if (refAa.contains("*") && !altAa.contains("*")) {
								locationTerms.add("stop_lost");
							}
						}
					}
					if (tva.getAminoAcids() != null) {
						entry.setAminoAcids(tva.getAminoAcids());
					}
					if (tva.getCodons() != null) {
						entry.setCodons(tva.getCodons());
					}
				} else {
					// N-allele case: TVA computed codons but no consequence (peptide()
					// returned undef because seq_is_unambiguous_dna failed). Still
					// output the codons — Perl does (codon() has no ambiguity gate).
					if (tva != null && tva.getCodons() != null) {
						entry.setCodons(tva.getCodons());
					}
					// VEP _ins_del_stop_altered fallback (VariationEffect.pm line 1292-1344):
					// When normal coding annotation fails (cds_end undef → peptides undef),
					// VEP checks if the deletion alters the stop codon by building CDS+3'UTR
					// and applying the edit. Guards (line 1312): cdna_start && cdna_end &&
					// cds_start
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
			// Fires when indel overlaps start codon AND _ins_del_start_altered returns
			// false.
			// Can coexist with start_lost (via peptide path) when ATG preserved but frame
			// shifts.
			if (overlaps5utr && overlapsCds && !isInsertion && transcript.isInExon(rangeStart) && transcript.isInExon(rangeEnd) && !codingAnnotator.isStartAltered(transcript, chr, variantStart, variantEnd)) {
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

		// If no location terms and no splice terms, use VEP's
		// DEFAULT_OVERLAP_CONSEQUENCE
		// VEP Constants.pm: DEFAULT_OVERLAP_CONSEQUENCE = intergenic_variant
		// This happens when a variant is within transcript bounds but outside any
		// specific region
		if (locationTerms.isEmpty() && spliceTerms.isEmpty()) {
			locationTerms.add("intergenic_variant");
		}

		// Step 5: Combine all terms and sort by VEP severity rank
		Set<String> allTerms = new LinkedHashSet<>();
		allTerms.addAll(spliceTerms);
		allTerms.addAll(locationTerms);

		// For non-coding intron: splice terms replace intron_variant base but
		// intron_variant stays
		// For coding: splice_donor/acceptor can coexist with coding_sequence_variant
		// and intron_variant
		// VEP just collects all matching predicates, so we just combine and sort

		// Handle special case: if we have splice_donor or splice_acceptor but no intron
		// or location terms,
		// the splice is the only consequence (e.g., SNP at +1/+2 without intron
		// interior overlap)
		// But if we have intron_variant separately, it stays

		// For non-coding transcript: if we have splice terms and the variant is in an
		// intron,
		// add non_coding_transcript_variant if not already present and no exon overlap
		if (!transcript.isCoding() && !spliceTerms.isEmpty() && !locationTerms.contains("non_coding_transcript_exon_variant")) {
			if (isIntronic || isInAnyIntron(transcript, rangeStart, rangeEnd)) {
				if (!allTerms.contains("non_coding_transcript_variant") && !allTerms.contains("non_coding_transcript_exon_variant")) {
					allTerms.add("non_coding_transcript_variant");
				}
			}
		}

		List<String> sortedTerms = new ArrayList<>(allTerms);
		sortedTerms.sort((a, b) -> Integer.compare(ConsequenceSeverity.getRank(a), ConsequenceSeverity.getRank(b)));

		String consequence = String.join("&", sortedTerms);
		entry.setConsequence(consequence);
		entry.setImpact(ConsequenceSeverity.getImpact(consequence));

		// Comprehensive trace for Perl comparison — log ALL intermediate values
		if (Trace.enabled()) {
			logComparisonTrace(transcript, variantStart, variantEnd, bvt, tva,
				consequence, refAllele, vepAllele);
		}

		// Exon/intron numbers — VEP iterates ALL overlapping exons/introns and
		// produces a range like "7-8/8". Use BVT.exonNumber/intronNumber which does
		// this iteration. Fall back to single-position lookup when BVT not available.
		String exonNum, intronNum;
		if (bvt != null) {
			exonNum = bvt.exonNumber();
			intronNum = bvt.intronNumber();
		} else {
			int checkPos = isInsertion ? variantEnd : variantStart;
			exonNum = transcript.getExonNumber(checkPos);
			intronNum = transcript.getIntronNumber(checkPos);
		}
		if (exonNum != null) {
			entry.setExon(exonNum);
		}
		if (intronNum != null) {
			entry.setIntron(intronNum);
		}

		// cDNA position for non-coding exon variants and UTR variants without coding
		// result. VEP populates cdna_position for ANY variant in an exon (within_cdna).
		// Use BVT's cdna_start/end (via the Mapper's genomic2cdna) so variants that
		// partially extend past a cDNA boundary get "?-N" / "N-?" notation matching Perl.
		// Perl's $pre->{exon} uses _overlapped_exons with SORTED (min,max) coords.
		// For insertions (VEP start > end), Java's geometric overlap misses boundary
		// positions. Use BVT cDNA coords as fallback for insertions only.
		if (entry.getCdnaPosition() == null && !consequence.contains("intergenic_variant")) {
			int cdnaS = bvt.cdnaStart();
			int cdnaE = bvt.cdnaEnd();
			if (overlapsExon) {
				if (cdnaS > 0 || cdnaE > 0) {
					entry.setCdnaPosition(formatCoords(cdnaS, cdnaE));
				}
			} else if (isInsertion) {
				// Boundary insertion: geometric overlap fails but mapper succeeds.
				// Apply Perl's within_cdna guard: coord.end > 0 AND coord.start <= feat.length
				int trCdnaLen = 0;
				for (ExonModel ex : transcript.getExons()) trCdnaLen += ex.getEnd() - ex.getStart() + 1;
				if (cdnaE > 0 && cdnaS <= trCdnaLen) {
					entry.setCdnaPosition(formatCoords(cdnaS, cdnaE));
				}
			}
		}

		// HGVS — VEP does not generate HGVSc for intergenic entries.
		// Suppress when variant extends past the transcript's exon span.
		// Perl's _get_cDNA_position handles intronic positions WITHIN the transcript
		// (computes intron offsets), so allow partial boundary when both endpoints
		// are within the transcript's genomic span.
		boolean partialBoundary = bvt != null
			&& ((bvt.cdnaStart() <= 0) != (bvt.cdnaEnd() <= 0));
		if (partialBoundary && transcript.getExons() != null && !transcript.getExons().isEmpty()) {
			int txStart = transcript.getExons().get(0).getStart();
			int txEnd = transcript.getExons().get(transcript.getExons().size() - 1).getEnd();
			int vMin = Math.min(variantStart, variantEnd);
			int vMax = Math.max(variantStart, variantEnd);
			if (vMin >= txStart && vMax <= txEnd) {
				partialBoundary = false;
			}
		}
		if (!consequence.contains("intergenic_variant") && !partialBoundary) {
			// For insertions, VEP uses cds_start (higher value) for HGVSc position.
			// For minus-strand insertions, cdsStart < cdsEnd, so use max.
			// Prefer BVT coordinates when available (matches Perl: BVT owns positions).
			int cdsPos;
			if (bvt != null && (bvt.cdsStart() > 0 || bvt.cdsEnd() > 0)) {
				cdsPos = isInsertion ? Math.max(bvt.cdsStart(), bvt.cdsEnd()) : bvt.cdsStart();
			} else if (tva != null) {
				cdsPos = isInsertion ? Math.max(tva.getCdsPosition(), tva.getCdsEnd()) : tva.getCdsPosition();
			} else {
				cdsPos = -1;
			}
			// Use tva for HGVSc if available, otherwise fall back to shared codingAnnotator
			TranscriptVariationAllele hgvsAnnotator = tva != null ? tva : codingAnnotator;
			String hgvsc = hgvsAnnotator.hgvsTranscript(transcript, chr, variantStart, variantEnd, vepAllele, refAllele, cdsPos, transcript.isCoding());
			if (hgvsc != null) {
				entry.setHgvsc(hgvsc);
			}
		}

		String hgvsg = hgvsGenomic.generate(chr, variantStart, variantEnd, refAllele, vepAllele);
		if (hgvsg != null) {
			entry.setHgvsg(hgvsg);
		}

		if (tva != null) {
			// VEP hgvs_protein() — use notation-based formatter
			TranscriptVariationAllele.HgvsNotation n = tva.getHgvsNotation();
			String hgvsp = null;
			if (n != null && n.type != null) {
				// Use entry consequence (combined terms) for stop/start flags
				String csq = entry.getConsequence();
				boolean isStopLost = csq != null && csq.contains("stop_lost");
				boolean isStartLost = csq != null && csq.contains("start_lost");
				hgvsp = tva.vepGetHgvsProteinFormat(n, transcript.getProteinId(), isStopLost, isStartLost, tva.getAltCdsSequence(), tva.getCdsSequence());
			}
			Trace.log("TVA.hgvs_protein", "tr=%s allele=%s result=%s reason=%s", transcript.getTranscriptId(), vepAllele, Trace.undef(hgvsp), n == null ? "null_notation" : (n.type == null ? "null_type" : "ok"));
			if (hgvsp != null) {
				entry.setHgvsp(hgvsp);
			}
		}

		return entry;
	}

	/**
	 * Check if the variant overlaps any CDS segment using VEP's overlap formula:
	 *	 (bvf_end >= feat_start) AND (bvf_start <= feat_end).
	 * For insertions (variantStart > variantEnd), this correctly excludes
	 * boundary insertions where the inserted bases fall outside the feature.
	 */
	private boolean overlapsAnyCds(TranscriptModel transcript, int rangeStart, int rangeEnd, boolean isInsertion, int variantStart, int variantEnd) {
		for (CdsSegment cds : transcript.getCdsSegments()) {
			// VEP overlap: (bvf_end >= feat_start) AND (bvf_start <= feat_end)
			if (variantEnd >= cds.getStart() && variantStart <= cds.getEnd()) {
				return true;
			}
		}
		return false;
	}

	/** Check exon overlap with frameshift-intron stretch (Perl _overlapped_exons line 863). */
	private boolean overlapsAnyExonStretched(TranscriptModel transcript, int rangeStart, int rangeEnd, int stretch) {
		for (ExonModel exon : transcript.getExons()) {
			if (rangeStart <= exon.getEnd() + stretch && rangeEnd >= exon.getStart() - stretch) {
				return true;
			}
		}
		return false;
	}

	/** Check if the variant range overlaps any exon (for deletions/SNPs). */
	private boolean overlapsAnyExon(TranscriptModel transcript, int rangeStart, int rangeEnd) {
		for (ExonModel exon : transcript.getExons()) {
			if (rangeStart <= exon.getEnd() && rangeEnd >= exon.getStart()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * VEP-compatible exon overlap for insertions (VariationEffect.pm
	 * non_coding_exon_variant line 505). VEP uses overlap(bvf.start, bvf.end,
	 * exon.start, exon.end) where insertions have start > end. This means an
	 * insertion at an exon boundary does NOT overlap the exon.
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
	 * VEP within_5_prime_utr (VariationEffect.pm line 722-734): _before_coding:
	 * overlap(varStart, varEnd, transcript_start, cds_start-1) AND within_cdna:
	 * variant overlaps any exon On - strand: uses _after_coding (overlap with
	 * cds_end+1 to transcript_end)
	 */
	private boolean overlaps5PrimeUtr(TranscriptModel transcript, int rangeStart, int rangeEnd) {
		return overlaps5PrimeUtr(transcript, rangeStart, rangeEnd, false, 0, 0);
	}

	private boolean overlaps5PrimeUtr(TranscriptModel transcript, int rangeStart, int rangeEnd, boolean isInsertion, int variantStart, int variantEnd) {
		if (!transcript.isCoding()) {
			return false;
		}

		boolean beforeCoding;
		if (transcript.isPositiveStrand()) {
			// _before_coding: overlap(var_s, var_e, tran_start, cds_start - 1)
			beforeCoding = rangeStart <= transcript.getCdsStart() - 1 && rangeEnd >= transcript.getStart();
			// VEP _before_coding line 698-699: insertion at CDS start returns true
			if (!beforeCoding && isInsertion && variantStart == transcript.getCdsStart()) {
				beforeCoding = true;
			}
		} else {
			// _after_coding: overlap(var_s, var_e, cds_end + 1, tran_end)
			beforeCoding = rangeStart <= transcript.getEnd() && rangeEnd >= transcript.getCdsEnd() + 1;
			if (!beforeCoding && isInsertion && variantEnd == transcript.getCdsEnd()) {
				beforeCoding = true;
			}
		}

		// within_cdna: variant overlaps any exon. For insertions use VEP overlap
		// formula so boundary insertions don't falsely match.
		boolean overlapsExonCheck;
		if (isInsertion) {
			overlapsExonCheck = overlapsAnyExonVep(transcript, variantStart, variantEnd);
		} else {
			overlapsExonCheck = overlapsAnyExon(transcript, rangeStart, rangeEnd);
		}
		return beforeCoding && overlapsExonCheck;
	}

	/**
	 * VEP within_3_prime_utr (VariationEffect.pm line 736-748): _after_coding:
	 * overlap(varStart, varEnd, cds_end+1, transcript_end) AND within_cdna: variant
	 * overlaps any exon On - strand: uses _before_coding (overlap with
	 * transcript_start to cds_start-1)
	 */
	/**
	 * VEP format_coords (Utils.pm line 141): start > end → "end-start", equal →
	 * "start"
	 */
	/**
	 * VEP Utils.pm format_coords (line 141-166).
	 * undef → "?". Both undef → "-". Otherwise normal range/point format.
	 * In our Java port, -1 is the "undef" sentinel for unset coordinates.
	 */
	private String formatCoords(int start, int end) {
		boolean hasStart = start > 0;
		boolean hasEnd = end > 0;
		if (hasStart && hasEnd) {
			if (start > end) return end + "-" + start;
			if (start == end) return String.valueOf(start);
			return start + "-" + end;
		}
		if (hasStart) return start + "-?";
		if (hasEnd) return "?-" + end;
		return "-";
	}

	private boolean overlaps3PrimeUtr(TranscriptModel transcript, int rangeStart, int rangeEnd) {
		return overlaps3PrimeUtr(transcript, rangeStart, rangeEnd, false, 0, 0);
	}

	private boolean overlaps3PrimeUtr(TranscriptModel transcript, int rangeStart, int rangeEnd, boolean isInsertion, int variantStart, int variantEnd) {
		if (!transcript.isCoding()) {
			return false;
		}

		boolean afterCoding;
		if (transcript.isPositiveStrand()) {
			// _after_coding: overlap(var_s, var_e, cds_end + 1, tran_end)
			afterCoding = rangeStart <= transcript.getEnd() && rangeEnd >= transcript.getCdsEnd() + 1;
			// VEP _after_coding line 715-716: insertion at CDS end returns true
			if (!afterCoding && isInsertion && variantEnd == transcript.getCdsEnd()) {
				afterCoding = true;
			}
		} else {
			// _before_coding: overlap(var_s, var_e, tran_start, cds_start - 1)
			afterCoding = rangeStart <= transcript.getCdsStart() - 1 && rangeEnd >= transcript.getStart();
			if (!afterCoding && isInsertion && variantStart == transcript.getCdsStart()) {
				afterCoding = true;
			}
		}

		// Also must be within_cdna (overlap an exon). For insertions use VEP's
		// overlap formula (bvf_end >= feat_start AND bvf_start <= feat_end) so
		// boundary insertions don't falsely match.
		boolean overlapsExonCheck;
		if (isInsertion) {
			overlapsExonCheck = overlapsAnyExonVep(transcript, variantStart, variantEnd);
		} else {
			overlapsExonCheck = overlapsAnyExon(transcript, rangeStart, rangeEnd);
		}
		return afterCoding && overlapsExonCheck;
	}

	/**
	 * Check if any part of the range falls within any intron (full intron, not just
	 * interior)
	 */
	private boolean isInAnyIntron(TranscriptModel transcript, int rangeStart, int rangeEnd) {
		for (int[] intron : transcript.getIntronIntervals()) {
			if (rangeStart <= intron[1] && rangeEnd >= intron[0]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * VEP partial_codon (VariationEffect.pm line 1389-1414): Returns true if the
	 * variant falls in an incomplete terminal codon (CDS length not divisible by 3,
	 * variant in the last 1-2 bases).
	 */
	private boolean isPartialCodon(TranscriptModel transcript, int rangeStart, int rangeEnd, boolean isInsertion, int variantStart) {
		if (!transcript.isCoding()) {
			return false;
		}

		// Compute CDS length
		int cdsLength = 0;
		for (CdsSegment seg : transcript.getCdsSegments()) {
			cdsLength += seg.getLength();
		}
		int remainder = cdsLength % 3;
		if (remainder == 0) {
			return false; // CDS is complete, no partial codon
		}

		// VEP: codon_cds_start = (translation_start * 3) - 2
		// translation_start = protein position = (cds_start - 1) / 3 + 1
		// We need the variant's CDS position to check if it falls in the last partial
		// codon
		int cdsPos = BaseTranscriptVariation.genomicToCds(transcript, isInsertion ? variantStart - 1 : rangeStart);
		if (cdsPos < 0) {
			// Try the end position
			cdsPos = BaseTranscriptVariation.genomicToCds(transcript, rangeEnd);
		}
		if (cdsPos < 0) {
			return false;
		}

		// VEP: last_codon_length = cds_length - (codon_cds_start - 1)
		int codonCdsStart = ((cdsPos - 1) / 3) * 3 + 1;
		int lastCodonLength = cdsLength - (codonCdsStart - 1);
		return lastCodonLength < 3 && lastCodonLength > 0;
	}

	/**
	 * Build a VariationEffect.Context from BVT (coordinates) + TVA (peptide/codon)
	 * + transcript geometry.
	 */
	private Context buildContext(TranscriptModel transcript, int variantStart, int variantEnd, String vepAllele, String refAllele, BaseTranscriptVariation bvt, TranscriptVariationAllele tva, SpliceResult spliceResult, boolean overlapsExon, boolean overlapsCds) {
		Context ctx = new Context();
		// Raw alleles
		ctx.refAllele = "-".equals(refAllele) ? "" : refAllele;
		ctx.altAllele = "-".equals(vepAllele) ? "" : vepAllele;
		// Transcript geometry
		ctx.positiveStrand = transcript.isPositiveStrand();
		ctx.trStart = transcript.getStart();
		ctx.trEnd = transcript.getEnd();
		ctx.vfStart = variantStart;
		ctx.vfEnd = variantEnd;
		List<int[]> intronList = transcript.getIntronIntervals();
		ctx.introns = intronList.toArray(new int[0][]);
		// Coding region
		if (transcript.isCoding()) {
			ctx.codingRegionStart = transcript.getCdsStart();
			ctx.codingRegionEnd = transcript.getCdsEnd();
			ctx.cdnaCodingStart = transcript.getCdnaCodingStart();
			int cdsLength = 0;
			for (CdsSegment seg : transcript.getCdsSegments()) {
				cdsLength += seg.getLength();
			}
			ctx.cdnaCodingEnd = ctx.cdnaCodingStart > 0 ? ctx.cdnaCodingStart + cdsLength - 1 : 0;
			ctx.cdsStartNF = transcript.isCdsStartNF();
			ctx.cdsEndNF = transcript.isCdsEndNF();
			ctx.codonTable = transcript.getCodonTable();
		}
		// Splice
		ctx.spliceTerms = spliceResult != null ? spliceResult.getSpliceTerms() : new ArrayList<>();
		ctx.intronic = spliceResult != null && spliceResult.isIntronic();
		// Flags
		ctx.withinCdna = overlapsExon;
		ctx.withinCds = overlapsCds;
		ctx.increaseLength = ctx.altAllele.length() > ctx.refAllele.length();
		ctx.decreaseLength = ctx.altAllele.length() < ctx.refAllele.length();
		ctx.alleleLen = ctx.altAllele.length();
		ctx.featureSeq = BaseTranscriptVariation.featureSeq(vepAllele, true, transcript.isPositiveStrand());
		// Coordinates from BVT
		if (bvt != null) {
			ctx.cdsStart = bvt.cdsStart();
			ctx.cdsEnd = bvt.cdsEnd();
			ctx.cdnaStart = bvt.cdnaStart();
			ctx.cdnaEnd = bvt.cdnaEnd();
			ctx.translationStart = bvt.translationStart();
		}
		// Peptide/codon from TVA
		if (tva != null) {
			ctx.refCodon = tva.getRawRefCodon();
			ctx.altCodon = tva.getRawAltCodon();
			// Parse peptides from amino acids string
			String aa = tva.getAminoAcids();
			if (aa != null && aa.contains("/")) {
				String[] parts = aa.split("/", -1);
				ctx.refPep = "-".equals(parts[0]) ? "" : parts[0];
				ctx.altPep = parts.length > 1 ? ("-".equals(parts[1]) ? "" : parts[1]) : "";
			} else if (tva.getRefAA() != 0) {
				ctx.refPep = String.valueOf(tva.getRefAA());
				ctx.altPep = String.valueOf(tva.getAltAA());
			}
			ctx.translateableSeq = tva.getCdsSequence();
		}
		// UTR sequences
		if (transcript.isCoding()) {
			if (ctx.translateableSeq == null) {
				ctx.translateableSeq = BaseTranscriptVariation.translateableSeq(transcript, codingAnnotator.getReference());
			}
			ctx.fivePrimeUtr = BaseTranscriptVariation.fivePrimeUtr(transcript, codingAnnotator.getReference());
			ctx.threePrimeUtr = BaseTranscriptVariation.threePrimeUtr(transcript, codingAnnotator.getReference());
		}
		return ctx;
	}

	/**
	 * Evaluate coding consequence predicates from VariationEffect.Context. Returns
	 * SO terms for all matching predicates.
	 */
	private List<String> evaluateCodingConsequences(Context ctx) {
		List<String> terms = new ArrayList<>();
		if (!ctx.withinCds) {
			return terms;
		}
		// Evaluate ALL predicates independently (Perl OverlapConsequence evaluation)
		if (VariationEffect.frameshift(ctx)) {
			terms.add("frameshift_variant");
		}
		if (VariationEffect.stopGained(ctx)) {
			terms.add("stop_gained");
		}
		if (VariationEffect.stopLost(ctx)) {
			terms.add("stop_lost");
		}
		if (VariationEffect.startLost(ctx)) {
			terms.add("start_lost");
		}
		if (VariationEffect.inframeInsertion(ctx)) {
			terms.add("inframe_insertion");
		}
		if (VariationEffect.inframeDeletion(ctx)) {
			terms.add("inframe_deletion");
		}
		if (VariationEffect.missenseVariant(ctx)) {
			terms.add("missense_variant");
		}
		if (VariationEffect.proteinAlteringVariant(ctx)) {
			terms.add("protein_altering_variant");
		}
		if (VariationEffect.partialCodon(ctx)) {
			terms.add("incomplete_terminal_codon_variant");
		}
		if (VariationEffect.startRetainedVariant(ctx)) {
			terms.add("start_retained_variant");
		}
		if (VariationEffect.stopRetained(ctx)) {
			terms.add("stop_retained_variant");
		}
		if (VariationEffect.synonymousVariant(ctx)) {
			terms.add("synonymous_variant");
		}
		if (VariationEffect.codingUnknown(ctx)) {
			terms.add("coding_sequence_variant");
		}
		if (terms.isEmpty()) {
			terms.add("coding_sequence_variant");
		}
		return terms;
	}

	/**
	 * Log ALL intermediate values for Perl comparison.
	 * Format matches the Perl trace so output can be diffed directly.
	 */
	private void logComparisonTrace(TranscriptModel transcript,
			int variantStart, int variantEnd,
			BaseTranscriptVariation bvt, TranscriptVariationAllele tva,
			String consequence, String refAllele, String vepAllele) {
		try {
			ReferenceGenome ref = codingAnnotator.getReference();
			String trId = transcript.getTranscriptId();

			// Exons
			StringBuilder exonsSb = new StringBuilder();
			for (ExonModel e : transcript.getExons()) {
				if (exonsSb.length() > 0) exonsSb.append(",");
				exonsSb.append(e.getStart()).append("-").append(e.getEnd());
			}

			// CDS segments
			StringBuilder cdsSb = new StringBuilder();
			for (CdsSegment s : transcript.getCdsSegments()) {
				if (cdsSb.length() > 0) cdsSb.append(",");
				cdsSb.append(s.getStart()).append("-").append(s.getEnd());
			}

			// Spliced mRNA
			String splicedSeq = BaseTranscriptVariation.buildSplicedSeq(transcript, ref);
			String splicedFirst10 = splicedSeq != null && splicedSeq.length() >= 10
				? splicedSeq.substring(0, 10).toUpperCase() : (splicedSeq != null ? splicedSeq.toUpperCase() : "");
			String splicedLast10 = splicedSeq != null && splicedSeq.length() >= 10
				? splicedSeq.substring(splicedSeq.length() - 10).toUpperCase() : "";

			// CDS (translateable_seq)
			String cds = BaseTranscriptVariation.translateableSeq(transcript, ref);
			String cdsFirst10 = cds != null && cds.length() >= 10
				? cds.substring(0, 10).toUpperCase() : (cds != null ? cds.toUpperCase() : "");
			String cdsLast10 = cds != null && cds.length() >= 10
				? cds.substring(cds.length() - 10).toUpperCase() : "";

			// 5' UTR
			String utr5 = BaseTranscriptVariation.fivePrimeUtr(transcript, ref);
			String utr5Last5 = utr5 != null && utr5.length() >= 5
				? utr5.substring(utr5.length() - 5) : (utr5 != null ? utr5 : "");

			// 3' UTR
			String utr3 = BaseTranscriptVariation.threePrimeUtr(transcript, ref);

			// BVT values
			int cdsStart = bvt != null ? bvt.cdsStart() : -1;
			int cdsEnd = bvt != null ? bvt.cdsEnd() : -1;
			int cdnaStart = bvt != null ? bvt.cdnaStart() : -1;
			int cdnaEnd = bvt != null ? bvt.cdnaEnd() : -1;
			int transStart = bvt != null ? bvt.translationStart() : -1;
			int transEnd = bvt != null ? bvt.translationEnd() : -1;

			// Allele-specific fields from TVA
			String aminoAcids = tva != null ? tva.getAminoAcids() : "";
			String codonsStr = tva != null ? tva.getCodons() : "";
			String refAlleleStr = refAllele != null ? refAllele : "";
			String altAlleleStr = vepAllele != null ? vepAllele : "";

			Trace.log("CMP", "tr=%s var=%s:%d-%d ref=%s alt=%s strand=%s spliced_len=%d spliced_f10=%s cdna_coding_start=%d cds_len=%d cds_f10=%s utr5_len=%d utr5_l5=%s utr3_len=%d startExonPhase=%d transStartExonPhase=%d hasFS=%b cds_start=%d cds_end=%d cdna_start=%d cdna_end=%d trans_start=%d trans_end=%d amino_acids=%s codons=%s consequence=%s",
				trId,
				transcript.getChr(), variantStart, variantEnd,
				refAlleleStr, altAlleleStr,
				transcript.isPositiveStrand() ? "+" : "-",
				splicedSeq != null ? splicedSeq.length() : 0, splicedFirst10,
				transcript.getCdnaCodingStart(),
				cds != null ? cds.length() : 0, cdsFirst10,
				utr5 != null ? utr5.length() : 0, utr5Last5,
				utr3 != null ? utr3.length() : 0,
				transcript.getStartExonPhase(), transcript.getTranslationStartExonPhase(),
				transcript.hasFrameshiftIntron(),
				cdsStart, cdsEnd, cdnaStart, cdnaEnd, transStart, transEnd,
				aminoAcids != null ? aminoAcids : "",
				codonsStr != null ? codonsStr : "",
				consequence);
		} catch (Exception e) {
			// Don't let trace errors break the pipeline
		}
	}
}
