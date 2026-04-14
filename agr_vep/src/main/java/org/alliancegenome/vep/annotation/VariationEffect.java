package org.alliancegenome.vep.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.vep.bio.CodonTable;
import org.alliancegenome.vep.bio.Sequence;
import org.alliancegenome.vep.debug.Trace;
import org.alliancegenome.vep.model.TranscriptModel;

/**
 * Port of Bio::EnsEMBL::Variation::Utils::VariationEffect (1510 lines, 76 methods).
 * Consequence classification predicates. Each method takes variant/transcript state
 * and returns true/false.
 *
 * In VEP, these are standalone functions that take a TranscriptVariationAllele.
 * Here they are static methods taking the pre-computed values from
 * BaseTranscriptVariation and the variant alleles.
 */
public class VariationEffect {

	public static final int MAX_DISTANCE_FROM_TRANSCRIPT = 5000;
	public static int UPSTREAM_DISTANCE = MAX_DISTANCE_FROM_TRANSCRIPT;
	public static int DOWNSTREAM_DISTANCE = MAX_DISTANCE_FROM_TRANSCRIPT;

	// VEP line 80-84
	public static boolean overlap(int f1Start, int f1End, int f2Start, int f2End) {
		return (f1End >= f2Start) && (f1Start <= f2End);
	}

	// VEP line 119-137
	public static boolean withinFeature(int vfStart, int vfEnd, int featStart, int featEnd) {
		return overlap(vfStart, vfEnd, featStart, featEnd);
	}

	// VEP line 332-343
	public static boolean deletion(String allele) {
		return "-".equals(allele) || allele == null || allele.isEmpty();
	}

	// VEP line 336-342
	public static boolean insertion(String refAllele) {
		return "-".equals(refAllele) || refAllele == null || refAllele.isEmpty();
	}

	// VEP line 1346-1387: frameshift
	// abs(allele_len - var_len) % 3 != 0
	public static boolean frameshift(int cdsStart, int cdsEnd, int alleleLen) {
		if (cdsStart < 0 || cdsEnd < 0) return false;
		int varLen = cdsEnd - cdsStart + 1;
		return Math.abs(alleleLen - varLen) % 3 != 0;
	}

	// VEP line 1389-1414: partial_codon
	// Variant is in the incomplete terminal codon
	public static boolean partialCodon(int translationStart, int cdsLength) {
		if (translationStart <= 0) return false;
		int codonCdsStart = (translationStart * 3) - 2;
		int lastCodonLength = cdsLength - (codonCdsStart - 1);
		return lastCodonLength < 3 && lastCodonLength > 0;
	}

	// VEP line 935-951: within_cds
	public static boolean withinCds(int cdsStart, int cdsEnd) {
		return cdsStart > 0 || cdsEnd > 0;
	}

	// VEP line 869-889: _overlaps_start_codon
	// Variant overlaps the start codon (CDS positions 1-3)
	public static boolean overlapsStartCodon(int cdsStart, int cdsEnd, boolean cdsStartNF) {
		if (cdsStartNF) return false;
		int varStart = Math.min(cdsStart, cdsEnd);
		return varStart > 0 && varStart <= 3;
	}

	// VEP line 1271-1291: _overlaps_stop_codon
	// Variant overlaps the stop codon (last 3 CDS positions)
	public static boolean overlapsStopCodon(int cdsStart, int cdsEnd, int cdsLength) {
		if (cdsLength < 3) return false;
		int stopStart = cdsLength - 2; // 1-based start of stop codon
		int varStart = Math.min(cdsStart, cdsEnd);
		int varEnd = Math.max(cdsStart, cdsEnd);
		return overlap(varStart, varEnd, stopStart, cdsLength);
	}

	// VEP line 993-1006: start_lost
	public static boolean startLost(String refPep, String altPep) {
		if (refPep == null || altPep == null) return false;
		// Start is lost if ref starts with M but alt doesn't
		return refPep.startsWith("M") && !altPep.startsWith("M");
	}

	// VEP line 1146-1166: stop_gained
	public static boolean stopGained(String refPep, String altPep) {
		if (refPep == null || altPep == null) return false;
		return altPep.contains("*") && !refPep.contains("*");
	}

	// VEP line 1168-1221: stop_lost
	public static boolean stopLost(String refPep, String altPep) {
		if (refPep == null || altPep == null) return false;
		return !altPep.contains("*") && refPep.contains("*");
	}

	// VEP line 1223-1265: stop_retained
	public static boolean stopRetained(String refPep, String altPep) {
		if (refPep == null || altPep == null) return false;
		return altPep.startsWith("*") && refPep.startsWith("*");
	}

	// VEP line 974-991: synonymous_variant
	public static boolean synonymousVariant(String refPep, String altPep) {
		if (refPep == null || altPep == null) return false;
		return refPep.equals(altPep);
	}

	// VEP line 1008-1017: missense_variant
	public static boolean missenseVariant(String refPep, String altPep) {
		if (refPep == null || altPep == null) return false;
		return refPep.length() == 1 && altPep.length() == 1
			&& !refPep.equals(altPep) && !refPep.equals("*") && !altPep.equals("*");
	}

	// VEP line 1019-1096: inframe_insertion
	public static boolean inframeInsertion(String refCodon, String altCodon, String refPep, String altPep) {
		if (refCodon == null || altCodon == null) return false;
		if (altCodon.length() <= refCodon.length()) return false;
		if (refPep == null || altPep == null) return false;

		// Trim alt after stop
		String altPepTrimmed = altPep;
		int stopIdx = altPepTrimmed.indexOf('*');
		if (stopIdx >= 0 && stopIdx < altPepTrimmed.length() - 1) {
			altPepTrimmed = altPepTrimmed.substring(0, stopIdx + 1);
		}

		return altPepTrimmed.startsWith(refPep) || altPepTrimmed.endsWith(refPep);
	}

	// VEP line 1098-1144: inframe_deletion
	public static boolean inframeDeletion(String refCodon, String altCodon) {
		if (refCodon == null || altCodon == null) return false;
		if (altCodon.length() >= refCodon.length()) return false;

		// Check simple string match
		if (refCodon.startsWith(altCodon) || refCodon.endsWith(altCodon)) return true;

		// Check internal match via trim
		Object[] trimmed = Sequence.trimSequences(
			refCodon, altCodon, 0, 0, false, false);
		String trimmedRef = (String) trimmed[0];
		String trimmedAlt = (String) trimmed[1];

		return trimmedAlt.isEmpty() && trimmedRef.length() % 3 == 0;
	}

	// VEP line 488-496: within_intron
	public static boolean withinIntron(int vfStart, int vfEnd, int[][] introns) {
		if (introns == null) return false;
		for (int[] intron : introns) {
			if (overlap(vfStart, vfEnd, intron[0], intron[1])) return true;
		}
		return false;
	}

	// VEP line 86-108: _intron_overlap (splice region check)
	public static boolean intronOverlap(int vfStart, int vfEnd, int intronStart, int intronEnd, boolean isInsertion) {
		return overlap(vfStart, vfEnd, intronStart + 2, intronStart + 7) ||
			overlap(vfStart, vfEnd, intronEnd - 7, intronEnd - 2) ||
			overlap(vfStart, vfEnd, intronStart - 3, intronStart - 1) ||
			overlap(vfStart, vfEnd, intronEnd + 1, intronEnd + 3) ||
			(isInsertion && (
				vfStart == intronStart ||
				vfEnd == intronEnd ||
				vfStart == intronStart + 2 ||
				vfEnd == intronEnd - 2
			));
	}

	// VEP line 696-710: donor_splice_site
	public static boolean donorSpliceSite(int vfStart, int vfEnd, int intronStart, boolean positiveStrand) {
		if (positiveStrand) {
			return overlap(vfStart, vfEnd, intronStart, intronStart + 1);
		} else {
			// For minus strand, donor is at the end of the intron
			return false; // Handled by acceptor for minus strand
		}
	}

	// VEP line 718-732: acceptor_splice_site
	public static boolean acceptorSpliceSite(int vfStart, int vfEnd, int intronEnd, boolean positiveStrand) {
		if (positiveStrand) {
			return overlap(vfStart, vfEnd, intronEnd - 1, intronEnd);
		} else {
			return false;
		}
	}

	// VEP line 458-462: within_transcript
	public static boolean withinTranscript(int vfStart, int vfEnd, int trStart, int trEnd) {
		return overlap(vfStart, vfEnd, trStart, trEnd);
	}

	// VEP line 546-565: within_5_prime_utr
	public static boolean within5PrimeUtr(int cdnaStart, int cdnaCodingStart) {
		return cdnaStart > 0 && cdnaCodingStart > 0 && cdnaStart < cdnaCodingStart;
	}

	// VEP line 567-580: within_3_prime_utr
	public static boolean within3PrimeUtr(int cdnaEnd, int cdnaCodingEnd) {
		return cdnaEnd > 0 && cdnaCodingEnd > 0 && cdnaEnd > cdnaCodingEnd;
	}

	// VEP line 513-543: protein_altering_variant
	public static boolean proteinAlteringVariant(String refPep, String altPep) {
		if (refPep == null || altPep == null) return false;
		return !refPep.equals(altPep) && refPep.length() != altPep.length();
	}

	// ===================================================================
	// Context class and Context-based predicates matching Perl VariationEffect.pm
	// ===================================================================

	/** Holds pre-computed state for Context-based predicate evaluation. */
	public static class Context {
		public String refPep, altPep;
		public String refCodon, altCodon;
		public String refAllele = "", altAllele = "";
		public int cdsStart, cdsEnd;
		public int cdnaStart, cdnaEnd;
		public int translationStart;
		public boolean positiveStrand;
		public int cdnaCodingStart, cdnaCodingEnd;
		public boolean cdsStartNF, cdsEndNF;
		public int codonTable = 1;
		public String translateableSeq, fivePrimeUtr, threePrimeUtr;
		public String featureSeq;
		public int alleleLen;
		public boolean increaseLength, decreaseLength;
		public int vfStart, vfEnd, trStart, trEnd;
		public int codingRegionStart, codingRegionEnd;
		public boolean withinCdna, withinCds;
		public String fullPeptide;
		public int[][] introns;
		public List<String> spliceTerms;
		public boolean intronic;
		private final Map<String, Boolean> cache = new HashMap<>();

		public Boolean getCached(String key) { return cache.get(key); }
		public void putCache(String key, boolean value) { cache.put(key, value); }
		public boolean hasCached(String key) { return cache.containsKey(key); }
	}

	// --- Context-based predicates ---

	public static boolean withinFeature(Context ctx) {
		return overlap(ctx.vfStart, ctx.vfEnd, ctx.trStart, ctx.trEnd);
	}

	public static boolean partialOverlapFeature(Context ctx) {
		return (ctx.vfStart < ctx.trStart || ctx.vfEnd > ctx.trEnd)
			&& overlap(ctx.vfStart, ctx.vfEnd, ctx.trStart, ctx.trEnd);
	}

	public static boolean completeWithinFeature(Context ctx) {
		return ctx.vfStart >= ctx.trStart && ctx.vfEnd <= ctx.trEnd;
	}

	public static boolean completeOverlapFeature(Context ctx) {
		return ctx.vfStart <= ctx.trStart && ctx.vfEnd >= ctx.trEnd;
	}

	public static boolean deletion(Context ctx) {
		return ctx.altAllele.isEmpty();
	}

	public static boolean insertion(Context ctx) {
		return ctx.refAllele.isEmpty();
	}

	private static boolean beforeStart(Context ctx) {
		if (ctx.positiveStrand) {
			return ctx.vfEnd < ctx.trStart;
		} else {
			return ctx.vfStart > ctx.trEnd;
		}
	}

	private static boolean afterEnd(Context ctx) {
		if (ctx.positiveStrand) {
			return ctx.vfStart > ctx.trEnd;
		} else {
			return ctx.vfEnd < ctx.trStart;
		}
	}

	public static boolean upstream(Context ctx) {
		if (!beforeStart(ctx)) return false;
		int dist = ctx.positiveStrand
			? ctx.trStart - ctx.vfEnd
			: ctx.vfStart - ctx.trEnd;
		return dist <= UPSTREAM_DISTANCE;
	}

	public static boolean downstream(Context ctx) {
		if (!afterEnd(ctx)) return false;
		int dist = ctx.positiveStrand
			? ctx.vfStart - ctx.trEnd
			: ctx.trStart - ctx.vfEnd;
		return dist <= DOWNSTREAM_DISTANCE;
	}

	public static boolean affectsTranscript(Context ctx) {
		return withinFeature(ctx) || upstream(ctx) || downstream(ctx);
	}

	public static boolean withinTranscript(Context ctx) {
		return overlap(ctx.vfStart, ctx.vfEnd, ctx.trStart, ctx.trEnd);
	}

	public static boolean withinIntron(Context ctx) {
		if (ctx.introns == null) return false;
		for (int[] intron : ctx.introns) {
			if (overlap(ctx.vfStart, ctx.vfEnd, intron[0], intron[1])) return true;
		}
		return false;
	}

	public static boolean withinCds(Context ctx) {
		return ctx.withinCds;
	}

	public static boolean withinCdna(Context ctx) {
		return ctx.withinCdna;
	}

	private static boolean beforeCoding(Context ctx) {
		if (ctx.codingRegionStart <= 0) return false;
		if (ctx.positiveStrand) {
			return overlap(ctx.vfStart, ctx.vfEnd, ctx.trStart, ctx.codingRegionStart - 1);
		} else {
			return overlap(ctx.vfStart, ctx.vfEnd, ctx.codingRegionEnd + 1, ctx.trEnd);
		}
	}

	private static boolean afterCoding(Context ctx) {
		if (ctx.codingRegionEnd <= 0) return false;
		if (ctx.positiveStrand) {
			return overlap(ctx.vfStart, ctx.vfEnd, ctx.codingRegionEnd + 1, ctx.trEnd);
		} else {
			return overlap(ctx.vfStart, ctx.vfEnd, ctx.trStart, ctx.codingRegionStart - 1);
		}
	}

	public static boolean within5PrimeUtr(Context ctx) {
		return beforeCoding(ctx) && withinCdna(ctx);
	}

	public static boolean within3PrimeUtr(Context ctx) {
		return afterCoding(ctx) && withinCdna(ctx);
	}

	public static boolean overlapsStartCodon(Context ctx) {
		if (ctx.cdsStartNF) return false;
		if (ctx.cdnaCodingStart <= 0 || ctx.cdnaStart <= 0) return false;
		return overlap(ctx.cdnaStart, ctx.cdnaEnd > 0 ? ctx.cdnaEnd : ctx.cdnaStart,
			ctx.cdnaCodingStart, ctx.cdnaCodingStart + 2);
	}

	public static boolean overlapsStopCodon(Context ctx) {
		if (ctx.cdnaCodingEnd <= 0 || ctx.cdnaStart <= 0) return false;
		return overlap(ctx.cdnaStart, ctx.cdnaEnd > 0 ? ctx.cdnaEnd : ctx.cdnaStart,
			ctx.cdnaCodingEnd - 2, ctx.cdnaCodingEnd);
	}

	public static boolean insDelStartAltered(Context ctx) {
		if (!overlapsStartCodon(ctx)) return false;
		if (ctx.translateableSeq == null || ctx.fivePrimeUtr == null) return false;
		String utrAndCds = ctx.fivePrimeUtr + ctx.translateableSeq;
		int editPos = ctx.cdnaStart - 1;
		if (editPos < 0) editPos = 0;
		String modified;
		if (ctx.decreaseLength) {
			int delLen = ctx.refAllele.length();
			if (editPos + delLen > utrAndCds.length()) delLen = utrAndCds.length() - editPos;
			modified = utrAndCds.substring(0, editPos)
				+ (ctx.altAllele.isEmpty() ? "" : ctx.featureSeq != null ? ctx.featureSeq : ctx.altAllele)
				+ utrAndCds.substring(editPos + delLen);
		} else {
			String insSeq = ctx.featureSeq != null ? ctx.featureSeq : ctx.altAllele;
			modified = utrAndCds.substring(0, editPos) + insSeq + utrAndCds.substring(editPos);
		}
		if (modified.length() < ctx.translateableSeq.length()) return true;
		String tail = modified.substring(modified.length() - ctx.translateableSeq.length());
		return !tail.equals(ctx.translateableSeq);
	}

	public static boolean invStartAltered(Context ctx) {
		if (!overlapsStartCodon(ctx)) return false;
		if (ctx.refPep == null || ctx.altPep == null) return false;
		return ctx.refPep.startsWith("M") && !ctx.altPep.startsWith("M");
	}

	public static boolean insDelStopAltered(Context ctx) {
		if (!overlapsStopCodon(ctx)) return false;
		if (ctx.translateableSeq == null || ctx.threePrimeUtr == null) return false;
		String cdsAndUtr = ctx.translateableSeq + ctx.threePrimeUtr;
		int editPos = ctx.cdsStart - 1;
		if (editPos < 0) editPos = 0;
		String modified;
		if (ctx.decreaseLength) {
			int delLen = ctx.refAllele.length();
			if (editPos + delLen > cdsAndUtr.length()) delLen = cdsAndUtr.length() - editPos;
			modified = cdsAndUtr.substring(0, editPos)
				+ (ctx.altAllele.isEmpty() ? "" : ctx.featureSeq != null ? ctx.featureSeq : ctx.altAllele)
				+ cdsAndUtr.substring(editPos + delLen);
		} else {
			String insSeq = ctx.featureSeq != null ? ctx.featureSeq : ctx.altAllele;
			modified = cdsAndUtr.substring(0, editPos) + insSeq + cdsAndUtr.substring(editPos);
		}
		if (modified.length() < ctx.translateableSeq.length()) return true;
		int stopIdx = ctx.translateableSeq.length() - 3;
		if (stopIdx + 3 > modified.length()) return true;
		String newStop = modified.substring(stopIdx, stopIdx + 3);
		return !CodonTable.isStop(newStop);
	}

	public static boolean frameshift(Context ctx) {
		if (ctx.hasCached("frameshift")) return ctx.getCached("frameshift");
		ctx.putCache("frameshift", false);
		if (ctx.cdsStart <= 0 && ctx.cdsEnd <= 0) return false;
		int varLen = Math.abs(ctx.cdsEnd - ctx.cdsStart) + 1;
		if (varLen <= 0) varLen = 0;
		boolean result = Math.abs(ctx.alleleLen - varLen) % 3 != 0;
		ctx.putCache("frameshift", result);
		return result;
	}

	public static boolean partialCodon(Context ctx) {
		if (ctx.translationStart <= 0) return false;
		if (ctx.translateableSeq == null) return false;
		int cdsLength = ctx.translateableSeq.length();
		int remainder = cdsLength % 3;
		if (remainder == 0) return false;
		int codonCdsStart = (ctx.translationStart * 3) - 2;
		int lastCodonLength = cdsLength - (codonCdsStart - 1);
		return lastCodonLength < 3 && lastCodonLength > 0;
	}

	public static boolean startLost(Context ctx) {
		if (ctx.hasCached("startLost")) return ctx.getCached("startLost");
		ctx.putCache("startLost", false);
		if (!overlapsStartCodon(ctx)) return false;

		// Path 1: insDelStartAltered for indels (non-SNV)
		if (ctx.increaseLength || ctx.decreaseLength) {
			if (insDelStartAltered(ctx)) {
				ctx.putCache("startLost", true);
				return true;
			}
			// Path 2: peptide check — ref starts with M, alt doesn't
			if (invStartAltered(ctx)) {
				ctx.putCache("startLost", true);
				return true;
			}
			// Path 3: for inframe indels blocked from insDelStartAltered,
			// check inframeInsertion/inframeDeletion calling startLost is handled by cache
			return false;
		}

		// SNV path: simple peptide check
		boolean result = invStartAltered(ctx);
		ctx.putCache("startLost", result);
		return result;
	}

	public static boolean startRetainedVariant(Context ctx) {
		if (!overlapsStartCodon(ctx)) return false;
		if (ctx.increaseLength || ctx.decreaseLength) {
			return !insDelStartAltered(ctx);
		}
		// SNV: ref and alt both start with M
		return ctx.refPep != null && ctx.altPep != null
			&& ctx.refPep.startsWith("M") && ctx.altPep.startsWith("M");
	}

	public static boolean synonymousVariant(Context ctx) {
		if (ctx.refPep == null || ctx.altPep == null) return false;
		if (ctx.refPep.equals(ctx.altPep)) {
			// Exclude stop_retained and X
			if (ctx.refPep.contains("*")) return false;
			if (ctx.refPep.contains("X")) return false;
			return true;
		}
		return false;
	}

	public static boolean missenseVariant(Context ctx) {
		if (ctx.hasCached("missenseVariant")) return ctx.getCached("missenseVariant");
		ctx.putCache("missenseVariant", false);
		if (ctx.refPep == null || ctx.altPep == null) return false;
		if (startLost(ctx)) return false;
		if (stopLost(ctx)) return false;
		if (stopGained(ctx)) return false;
		if (partialCodon(ctx)) return false;
		if (ctx.refPep.length() != 1 || ctx.altPep.length() != 1) return false;
		boolean result = !ctx.refPep.equals(ctx.altPep)
			&& !"*".equals(ctx.refPep) && !"*".equals(ctx.altPep);
		ctx.putCache("missenseVariant", result);
		return result;
	}

	public static boolean inframeInsertion(Context ctx) {
		if (ctx.hasCached("inframeInsertion")) return ctx.getCached("inframeInsertion");
		ctx.putCache("inframeInsertion", false);
		if (frameshift(ctx)) return false;
		if (!ctx.increaseLength) return false;
		if (startLost(ctx)) return false;
		if (ctx.refCodon == null || ctx.altCodon == null) return false;
		if (ctx.altCodon.length() <= ctx.refCodon.length()) return false;
		if (ctx.refPep == null || ctx.altPep == null) return false;

		// Check start_retained
		if (startRetainedVariant(ctx)) {
			ctx.putCache("inframeInsertion", true);
			return true;
		}

		// Trim alt after stop
		String altPepTrimmed = ctx.altPep;
		int stopIdx = altPepTrimmed.indexOf('*');
		if (stopIdx >= 0 && stopIdx < altPepTrimmed.length() - 1) {
			altPepTrimmed = altPepTrimmed.substring(0, stopIdx + 1);
		}

		boolean result = altPepTrimmed.startsWith(ctx.refPep) || altPepTrimmed.endsWith(ctx.refPep);
		ctx.putCache("inframeInsertion", result);
		return result;
	}

	public static boolean inframeDeletion(Context ctx) {
		if (ctx.hasCached("inframeDeletion")) return ctx.getCached("inframeDeletion");
		ctx.putCache("inframeDeletion", false);
		if (frameshift(ctx)) return false;
		if (!ctx.decreaseLength) return false;
		if (partialCodon(ctx)) return false;
		if (ctx.refCodon == null || ctx.altCodon == null) return false;
		if (ctx.altCodon.length() >= ctx.refCodon.length()) return false;

		// Check simple string match
		if (ctx.refCodon.startsWith(ctx.altCodon) || ctx.refCodon.endsWith(ctx.altCodon)) {
			ctx.putCache("inframeDeletion", true);
			return true;
		}

		// Check internal match via trim
		Object[] trimmed = Sequence.trimSequences(ctx.refCodon, ctx.altCodon, 0, 0, false, false);
		String trimmedRef = (String) trimmed[0];
		String trimmedAlt = (String) trimmed[1];

		boolean result = trimmedAlt.isEmpty() && trimmedRef.length() % 3 == 0;
		ctx.putCache("inframeDeletion", result);
		return result;
	}

	public static boolean stopGained(Context ctx) {
		if (ctx.hasCached("stopGained")) return ctx.getCached("stopGained");
		ctx.putCache("stopGained", false);
		if (ctx.refPep == null || ctx.altPep == null) return false;
		// Check stopRetained first — if both have stop at start, it's retained not gained
		if (stopRetained(ctx)) return false;
		boolean result = ctx.altPep.contains("*") && !ctx.refPep.contains("*");
		ctx.putCache("stopGained", result);
		return result;
	}

	public static boolean stopLost(Context ctx) {
		if (ctx.hasCached("stopLost")) return ctx.getCached("stopLost");
		ctx.putCache("stopLost", false);
		if (ctx.refPep == null || ctx.altPep == null) {
			// Fallback to insDelStopAltered
			if (ctx.increaseLength || ctx.decreaseLength) {
				boolean result = insDelStopAltered(ctx);
				ctx.putCache("stopLost", result);
				return result;
			}
			return false;
		}
		if (!ctx.altPep.contains("*") && ctx.refPep.contains("*")) {
			ctx.putCache("stopLost", true);
			return true;
		}
		// Fallback for indels
		if (ctx.increaseLength || ctx.decreaseLength) {
			boolean result = insDelStopAltered(ctx);
			ctx.putCache("stopLost", result);
			return result;
		}
		return false;
	}

	public static boolean stopRetained(Context ctx) {
		if (ctx.hasCached("stopRetained")) return ctx.getCached("stopRetained");
		ctx.putCache("stopRetained", false);
		if (ctx.refPep != null && ctx.altPep != null) {
			if (ctx.altPep.startsWith("*") && ctx.refPep.startsWith("*")) {
				ctx.putCache("stopRetained", true);
				return true;
			}
		}
		// Fallback: check fullPeptide for stop at same position
		if (ctx.fullPeptide != null && overlapsStopCodon(ctx)) {
			if (ctx.increaseLength || ctx.decreaseLength) {
				boolean notAltered = !insDelStopAltered(ctx);
				ctx.putCache("stopRetained", notAltered);
				return notAltered;
			}
		}
		return false;
	}

	public static boolean proteinAlteringVariant(Context ctx) {
		if (ctx.hasCached("proteinAlteringVariant")) return ctx.getCached("proteinAlteringVariant");
		ctx.putCache("proteinAlteringVariant", false);
		if (ctx.refPep == null || ctx.altPep == null) return false;
		if (ctx.refPep.equals(ctx.altPep)) return false;
		if (inframeDeletion(ctx)) return false;
		if (inframeInsertion(ctx)) return false;
		if (startLost(ctx)) return false;
		if (frameshift(ctx)) return false;
		boolean result = ctx.refPep.length() != ctx.altPep.length();
		ctx.putCache("proteinAlteringVariant", result);
		return result;
	}

	public static boolean complexIndel(Context ctx) {
		return (ctx.increaseLength || ctx.decreaseLength)
			&& !frameshift(ctx) && !inframeInsertion(ctx) && !inframeDeletion(ctx);
	}

	public static boolean codingUnknown(Context ctx) {
		if (ctx.cdsStart <= 0 && ctx.cdsEnd <= 0) return false;
		if (ctx.refPep == null || ctx.altPep == null) return true;
		if ("X".equals(ctx.refPep) || "X".equals(ctx.altPep)) return true;
		return false;
	}

	// Splice predicates reading from ctx.spliceTerms
	public static boolean spliceDonor(Context ctx) {
		return ctx.spliceTerms != null && ctx.spliceTerms.contains("splice_donor_variant");
	}

	public static boolean spliceAcceptor(Context ctx) {
		return ctx.spliceTerms != null && ctx.spliceTerms.contains("splice_acceptor_variant");
	}

	public static boolean spliceRegion(Context ctx) {
		return ctx.spliceTerms != null && ctx.spliceTerms.contains("splice_region_variant");
	}

	// ===================================================================
	// Splice predicates — merged from SpliceAnnotator.java
	// VEP VariationEffect.pm: donor_splice_site (696), acceptor_splice_site (718),
	// splice_donor_5th_base_variant (744), splice_donor_region_variant (774),
	// splice_polypyrimidine_tract_variant (808), splice_region (833)
	// VEP BaseTranscriptVariationAllele.pm: _intron_effects (line 100+)
	// ===================================================================

	/** Result of splice classification. */
	public static class SpliceResult {
		private final List<String> spliceTerms;
		private final boolean intronic;

		public SpliceResult(List<String> spliceTerms, boolean intronic) {
			this.spliceTerms = spliceTerms;
			this.intronic = intronic;
		}

		public List<String> getSpliceTerms() { return spliceTerms; }
		public boolean isIntronic() { return intronic; }
	}

	/**
	 * VEP _intron_effects (BaseTranscriptVariationAllele.pm) + splice predicates (VariationEffect.pm).
	 * Classifies splice consequences for a variant against a transcript.
	 */
	public static SpliceResult classifySplice(TranscriptModel transcript, int variantStart, int variantEnd) {
		List<String> spliceTerms = new ArrayList<>();
		boolean intronic = false;
		boolean positiveStrand = transcript.isPositiveStrand();

		boolean hasDonor = false;
		boolean hasAcceptor = false;
		boolean hasFifthBase = false;
		boolean hasDonorRegion = false;
		boolean hasPolypyrimidine = false;
		boolean hasSpliceRegion = false;

		for (int[] intron : transcript.getIntronIntervals()) {
			int intronStart = intron[0];
			int intronEnd = intron[1];

			// VEP skips frameshift introns (≤ 12bp)
			int intronLength = intronEnd - intronStart + 1;
			if (intronLength <= 12 && overlap(variantStart, variantEnd, intronStart, intronEnd)) {
				continue;
			}

			// VEP _intron_effects: intronic = overlap with intron interior (+3 to end-2)
			boolean insertion = variantStart == variantEnd + 1;
			if (overlap(variantStart, variantEnd, intronStart + 2, intronEnd - 2)
				|| (insertion && (variantStart == intronStart + 2 || variantEnd == intronEnd - 2))) {
				intronic = true;
			}

			// Donor/acceptor splice sites: first/last 2 bases of intron
			boolean startSpliceSite = overlap(variantStart, variantEnd, intronStart, intronStart + 1);
			boolean endSpliceSite = overlap(variantStart, variantEnd, intronEnd - 1, intronEnd);
			boolean isDonor = positiveStrand ? startSpliceSite : endSpliceSite;
			boolean isAcceptor = positiveStrand ? endSpliceSite : startSpliceSite;

			if (isDonor) hasDonor = true;
			if (isAcceptor) hasAcceptor = true;

			// 5th base: position +5 from donor end
			boolean fifthBase = positiveStrand
				? overlap(variantStart, variantEnd, intronStart + 4, intronStart + 4)
				: overlap(variantStart, variantEnd, intronEnd - 4, intronEnd - 4);
			if (fifthBase) hasFifthBase = true;

			// Donor region: positions +3 to +6
			boolean donorRegion = positiveStrand
				? overlap(variantStart, variantEnd, intronStart + 2, intronStart + 5)
				: overlap(variantStart, variantEnd, intronEnd - 5, intronEnd - 2);
			if (donorRegion) hasDonorRegion = true;

			// Polypyrimidine tract: 15 bases upstream of acceptor (-17 to -3)
			// Perl normalizes insertion coords: ($start,$end) = ($end,$start) if $start > $end
			int ppStart = Math.min(variantStart, variantEnd);
			int ppEnd = Math.max(variantStart, variantEnd);
			boolean polypyrimidine = positiveStrand
				? overlap(ppStart, ppEnd, intronEnd - 16, intronEnd - 2)
				: overlap(ppStart, ppEnd, intronStart + 2, intronStart + 16);
			if (polypyrimidine) hasPolypyrimidine = true;

			// Splice region: 3-8 bases into intron OR 1-3 bases of exon
			if (!isDonor && !isAcceptor) {
				boolean spliceRegion =
					overlap(variantStart, variantEnd, intronStart + 2, intronStart + 7) ||
					overlap(variantStart, variantEnd, intronEnd - 7, intronEnd - 2) ||
					overlap(variantStart, variantEnd, intronStart - 3, intronStart - 1) ||
					overlap(variantStart, variantEnd, intronEnd + 1, intronEnd + 3) ||
					(insertion && (variantStart == intronStart || variantEnd == intronEnd
						|| variantStart == intronStart + 2 || variantEnd == intronEnd - 2));
				if (spliceRegion) hasSpliceRegion = true;
			}
		}

		if (hasAcceptor) spliceTerms.add("splice_acceptor_variant");
		if (hasDonor) spliceTerms.add("splice_donor_variant");
		if (hasFifthBase) spliceTerms.add("splice_donor_5th_base_variant");
		if (hasDonorRegion && !hasFifthBase) spliceTerms.add("splice_donor_region_variant");
		if (hasPolypyrimidine) spliceTerms.add("splice_polypyrimidine_tract_variant");
		if (hasSpliceRegion && !hasDonor && !hasAcceptor && !hasFifthBase && !hasDonorRegion) {
			spliceTerms.add("splice_region_variant");
		}
		return new SpliceResult(spliceTerms, intronic);
	}
}
