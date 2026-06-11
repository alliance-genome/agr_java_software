package org.alliancegenome.filegenerator.generators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.alliancegenome.core.util.SmartAlphaComparator;
import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.es.EsParallelFetcher;
import org.alliancegenome.filegenerator.writers.JsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * VCF v4.3 generator. Source: {@code variant_summary} in {@code site_index} (LTP only).
 * One row per variant_summary doc — each doc carries an allele plus a single-element
 * {@code variantList} with the variant. Filename splits by MOD; genome assembly metadata is
 * embedded in the VCF content (currently via the static `##fileformat`/`##INFO` header — per-file
 * `##contig` and `##assembly` lines are deferred since they require a pre-pass to enumerate
 * chromosomes).
 *
 * VCF column population (all built in customizeRow):
 *   CHROM  variantList[0].curatedVariantGenomicLocations[0].variantGenomicLocationAssociationObject.name
 *   POS    variantList[0].curatedVariantGenomicLocations[0].start
 *   ID     variantList[0].curatedVariantGenomicLocations[0].hgvs
 *   REF    variantList[0].curatedVariantGenomicLocations[0].referenceSequence
 *   ALT    variantList[0].curatedVariantGenomicLocations[0].variantSequence
 *   QUAL   "."
 *   FILTER "."
 *   INFO   key="value";... — see buildInfo() for the full set
 */
@Slf4j
public class VariantVcfFileGenerator extends FileGenerator {

	private static final String CHROM_PATH = "variantList.curatedVariantGenomicLocations.variantGenomicLocationAssociationObject.name";
	private static final String ASSEMBLY_PATH = "variantList.curatedVariantGenomicLocations.variantGenomicLocationAssociationObject.genomeAssembly.primaryExternalId";
	private static final String SPECIES_PATH = "allele.taxon.species.fullName";
	// IUPAC ambiguity codes that need to be wrapped in angle brackets to remain valid as a VCF ALT (they reference the corresponding ##ALT=<ID=..> declarations in the header).
	private static final Set<Character> IUPAC_AMBIGUITY_CODES = Set.of('R', 'Y', 'S', 'W', 'K', 'M', 'B', 'D', 'H', 'V');
	// VCF v4.3 INFO values cannot contain whitespace; appendKv replaces matches with underscore so the field parses while preserving the symbol token.
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private final Map<String, String> contigLinesByMod = new LinkedHashMap<>();

	public VariantVcfFileGenerator(FileGeneratorConfig config) {
		super(config);
	}

	@Override
	protected void generate() throws Exception {
		precomputeContigLines();
		scrollAndWrite();
	}

	@Override
	protected Map<String, String> headerSubstitutions(String mod) {
		String contigLines = contigLinesByMod.getOrDefault(mod, "");
		return Map.of("{contigLines}", contigLines);
	}

	/**
	 * One ES `_search` per MOD (size=0, multi_terms agg over chrom+assembly+species). Empty buckets yield empty contigLines for that MOD so the placeholder line collapses cleanly.
	 */
	private void precomputeContigLines() {
		EsParallelFetcher fetcher = new EsParallelFetcher(esIndex(), config.getEsCategories());
		for (String mod : config.getMods()) {
			String prefix = mod + ":";
			Map<String, Object> body = buildContigAggBody(prefix);
			Map<String, Object> resp = fetcher.search(body);
			if (resp == null) {
				log.warn("{}: contig pre-pass for MOD {} returned null response — contig header lines will be empty", getClass().getSimpleName(), mod);
			}
			String contigLines = renderContigLines(resp);
			contigLinesByMod.put(mod, contigLines);
			log.info("{}: contig pre-pass for MOD {} produced {} line(s)", getClass().getSimpleName(), mod, contigLines.isEmpty() ? 0 : contigLines.split("\n").length);
		}
	}

	private Map<String, Object> buildContigAggBody(String alleleIdPrefix) {
		Map<String, Object> categoryFilter = Map.of("terms", Map.of("category.keyword", config.getEsCategories()));
		Map<String, Object> prefixFilter = Map.of("prefix", Map.of("allele.primaryExternalId.keyword", alleleIdPrefix));
		Map<String, Object> bool = Map.of("bool", Map.of("filter", List.of(categoryFilter, prefixFilter)));

		Map<String, Object> multiTerms = new LinkedHashMap<>();
		multiTerms.put("terms", List.of(
				Map.of("field", CHROM_PATH + ".keyword"),
				Map.of("field", ASSEMBLY_PATH + ".keyword"),
				Map.of("field", SPECIES_PATH + ".keyword")
		));
		multiTerms.put("size", 1000);

		Map<String, Object> aggs = Map.of("contigs", Map.of("multi_terms", multiTerms));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("size", 0);
		body.put("query", bool);
		body.put("aggs", aggs);
		return body;
	}

	@SuppressWarnings("unchecked")
	private String renderContigLines(Map<String, Object> resp) {
		if (resp == null) {
			return "";
		}
		Map<String, Object> aggregations = (Map<String, Object>) resp.get("aggregations");
		if (aggregations == null) {
			return "";
		}
		Map<String, Object> contigs = (Map<String, Object>) aggregations.get("contigs");
		if (contigs == null) {
			return "";
		}
		List<Map<String, Object>> buckets = (List<Map<String, Object>>) contigs.get("buckets");
		if (buckets == null || buckets.isEmpty()) {
			return "";
		}

		List<String[]> tuples = new ArrayList<>();
		for (Map<String, Object> bucket : buckets) {
			Object keysObj = bucket.get("key");
			if (!(keysObj instanceof List<?> keys) || keys.size() < 3) {
				continue;
			}
			String chrom = String.valueOf(keys.get(0));
			String assembly = String.valueOf(keys.get(1));
			String spp = String.valueOf(keys.get(2));
			tuples.add(new String[] { chrom, assembly, spp });
		}

		// Smart-alpha (natural sort) on the chrom string keeps Drosophila's 2L/2R/3L/3R/4 in mod-order while still putting Mouse's 1..19 before MT/X/Y and Worm's I/II/III/IV before V/X.
		tuples.sort((a, b) -> SmartAlphaComparator.INSTANCE.compare(a[0], b[0]));

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tuples.size(); i++) {
			String[] t = tuples.get(i);
			if (i > 0) {
				sb.append("\n");
			}
			sb.append("##contig=<ID=").append(t[0])
					.append(",assembly=").append(t[1])
					.append(",species=\"").append(t[2]).append("\">");
		}
		return sb.toString();
	}

	@Override
	protected String taxonPath() {
		return "allele.taxon.curie";
	}

	/**
	 * The {@code site_index} alias resolves to BOTH the LTP {@code site_index_*} and the
	 * HTP {@code variant_index_*}. We want LTP-only — match the LTP index by wildcard so
	 * we get its ~65K {@code variant_summary} docs, not the 50M HTP variants. The
	 * {@code site_index_*} pattern is environment-agnostic (matches stage, prod, etc.)
	 * while still cleanly excluding {@code variant_index_*}.
	 */
	@Override
	protected String esIndex() {
		return "site_index_*";
	}

	/**
	 * Narrow _source includes — variant_summary docs are heavy (relatedNotes,
	 * dataProviderCrossReference, taxon.species nested object, etc.). Only request paths
	 * customizeRow actually walks. Cuts wire payload roughly in half on stage docs.
	 */
	@Override
	protected List<String> additionalSourceIncludes() {
		return List.of(
				"allele.primaryExternalId",
				"allele.alleleSymbol.displayText",
				"allele.alleleSymbol.formatText",
				"allele.taxon.curie",
				"variantList.variantType.name",
				"variantList.curatedVariantGenomicLocations.start",
				"variantList.curatedVariantGenomicLocations.end",
				"variantList.curatedVariantGenomicLocations.hgvs",
				"variantList.curatedVariantGenomicLocations.referenceSequence",
				"variantList.curatedVariantGenomicLocations.variantSequence",
				"variantList.curatedVariantGenomicLocations.variantGenomicLocationAssociationObject.name",
				"variantList.curatedVariantGenomicLocations.mostSevereConsequence.variantConsequence.name",
				"variantList.curatedVariantGenomicLocations.mostSevereConsequence.vepConsequences.name",
				"variantList.curatedVariantGenomicLocations.mostSevereConsequence.vepImpact.name",
				"variantList.curatedVariantGenomicLocations.predictedVariantConsequences.vepConsequences.name",
				"variantList.curatedVariantGenomicLocations.predictedVariantConsequences.vepImpact.name",
				"variantList.curatedVariantGenomicLocations.predictedVariantConsequences.variantTranscript.name",
				"variantList.curatedVariantGenomicLocations.predictedVariantConsequences.variantTranscript.displayName",
				"variantList.curatedVariantGenomicLocations.predictedVariantConsequences.variantTranscript.modCrossRefCompleteUrl",
				"variantList.curatedVariantGenomicLocations.predictedVariantConsequences.variantTranscript.transcriptGeneAssociations.transcriptGeneAssociationObject.geneSymbol.displayText",
				"geneIds"
		);
	}

	@Override
	protected JsonNode customizeRow(JsonNode hit) {
		if (!hit.isObject()) {
			return hit;
		}
		ObjectNode obj = (ObjectNode) hit;

		JsonNode loc = JsonPath.resolve(hit, "variantList.0.curatedVariantGenomicLocations.0");
		String chrom = "";
		String pos = "";
		String id = "";
		String ref = "";
		String alt = "";
		if (loc != null && loc.isObject()) {
			chrom = loc.path("variantGenomicLocationAssociationObject").path("name").asText("");
			id = loc.path("hgvs").asText("");
			long start = loc.path("start").asLong(0);
			String rawRef = loc.path("referenceSequence").asText("");
			// IUPAC ambiguity codes in the ALT have to be wrapped in angle brackets so they reference the ##ALT=<ID=R,...> header lines (VCF v4.3 symbolic alleles); the legacy python generator did the same substitution.
			String rawAlt = wrapIupacAmbiguityCodes(loc.path("variantSequence").asText(""));
			String paddedBase = loc.path("paddedBase").asText("");
			// VCF v4.3 §5.1: insertions/deletions/delins require a padding base at POS = start - 1 so REF and ALT are non-empty. When paddedBase is absent on an indel/delins we silently emit unpadded — matches the legacy python generator's quiet fall-through.
			boolean isInsertion = rawRef.isEmpty() && !rawAlt.isEmpty();
			boolean isDeletion = rawAlt.isEmpty() && !rawRef.isEmpty();
			boolean isDelins = !rawRef.isEmpty() && !rawAlt.isEmpty() && rawRef.length() != rawAlt.length();
			if (!paddedBase.isEmpty() && isInsertion) {
				ref = paddedBase;
				alt = paddedBase + rawAlt;
				pos = Long.toString(start);
			} else if (!paddedBase.isEmpty() && (isDeletion || isDelins)) {
				ref = paddedBase + rawRef;
				alt = paddedBase + rawAlt;
				pos = Long.toString(start - 1);
			} else {
				ref = rawRef;
				alt = rawAlt;
				pos = start > 0 ? Long.toString(start) : "";
			}
		}

		obj.put("_chrom", chrom);
		obj.put("_pos", pos);
		obj.put("_id", id);
		obj.put("_ref", ref);
		obj.put("_alt", alt);
		obj.put("_qual", ".");
		obj.put("_filter", ".");
		String alleleId = JsonPath.resolveString(hit, "allele.primaryExternalId");
		obj.put("_info", buildInfo(hit, loc, id, alleleId));

		return hit;
	}

	private static String buildInfo(JsonNode hit, JsonNode loc, String hgvs, String alleleId) {
		StringBuilder sb = new StringBuilder();

		appendKv(sb, "hgvs_nomenclature", hgvs);

		// MOD prefix derived from the allele curie (e.g., "WB:WBVar..." -> "WB:") — applied to transcript IDs to match legacy VCF format.
		String modPrefix = "";
		if (alleleId != null) {
			int colon = alleleId.indexOf(':');
			if (colon > 0) {
				modPrefix = alleleId.substring(0, colon + 1);
			}
		}

		// geneLevelConsequence — pipe-joined unique names across mostSevereConsequence.vepConsequences[*].
		LinkedHashSet<String> geneLevel = new LinkedHashSet<>();
		if (loc != null) {
			JsonNode vepCs = loc.path("mostSevereConsequence").path("vepConsequences");
			if (vepCs.isArray()) {
				for (JsonNode v : vepCs) {
					String n = v.path("name").asText("");
					if (!n.isEmpty()) {
						geneLevel.add(n);
					}
				}
			}
		}
		appendKv(sb, "geneLevelConsequence", String.join("|", geneLevel));

		// transcriptLevelConsequence — pipe-joined unique names across all predictedVariantConsequences[*].vepConsequences[*]. transcriptImpact and geneSymbols collected during the same pass.
		// Transcript IDs are kept per-transcript (comma-joined, dedup) — allele_of_transcript_ids gets the MOD prefix per legacy format, the two gff3_* keys carry the unprefixed transcript name.
		LinkedHashSet<String> txLevel = new LinkedHashSet<>();
		LinkedHashSet<String> txImpacts = new LinkedHashSet<>();
		List<String> geneSymbols = new ArrayList<>();
		List<String> transcriptIds = new ArrayList<>();
		List<String> transcriptGff3Ids = new ArrayList<>();
		List<String> transcriptGff3Names = new ArrayList<>();
		if (loc != null) {
			JsonNode pvc = loc.path("predictedVariantConsequences");
			if (pvc.isArray()) {
				for (JsonNode entry : pvc) {
					JsonNode vepCs = entry.path("vepConsequences");
					if (vepCs.isArray()) {
						for (JsonNode v : vepCs) {
							String n = v.path("name").asText("");
							if (!n.isEmpty()) {
								txLevel.add(n);
							}
						}
					}
					String imp = entry.path("vepImpact").path("name").asText("");
					if (!imp.isEmpty()) {
						txImpacts.add(imp);
					}
					String gs = entry.path("variantTranscript").path("transcriptGeneAssociations").path(0)
							.path("transcriptGeneAssociationObject").path("geneSymbol").path("displayText").asText("");
					if (!gs.isEmpty()) {
						geneSymbols.add(gs);
					}
					String tn = entry.path("variantTranscript").path("name").asText("");
					if (!tn.isEmpty()) {
						transcriptIds.add(modPrefix + tn);
						transcriptGff3Ids.add(tn);
						transcriptGff3Names.add(tn);
					}
				}
			}
		}
		appendKv(sb, "transcriptLevelConsequence", String.join("|", txLevel));

		String geneImpact = loc == null ? "" : loc.path("mostSevereConsequence").path("vepImpact").path("name").asText("");
		appendKv(sb, "geneImpact", geneImpact);
		appendKv(sb, "transcriptImpact", String.join("|", txImpacts));

		appendKv(sb, "allele_ids", alleleId);
		appendKv(sb, "allele_symbols", JsonPath.resolveString(hit, "allele.alleleSymbol.displayText"));
		appendKv(sb, "allele_symbols_text", JsonPath.resolveString(hit, "allele.alleleSymbol.formatText"));

		String soTerm = JsonPath.resolveString(hit, "variantList.0.variantType.name");
		appendKv(sb, "soTerm", soTerm);

		List<String> geneIds = collectArrayStrings(JsonPath.resolve(hit, "geneIds"));
		appendKv(sb, "allele_of_gene_ids", String.join(",", geneIds));
		appendKv(sb, "allele_of_gene_symbols", String.join(",", dedup(geneSymbols)));
		appendKv(sb, "allele_of_transcript_ids", String.join(",", dedup(transcriptIds)));
		appendKv(sb, "allele_of_transcript_gff3_ids", String.join(",", dedup(transcriptGff3Ids)));
		appendKv(sb, "allele_of_transcript_gff3_names", String.join(",", dedup(transcriptGff3Names)));

		return sb.toString();
	}

	private static void appendKv(StringBuilder sb, String key, String value) {
		if (sb.length() > 0) {
			sb.append(";");
		}
		// VCF v4.3 INFO values cannot contain whitespace (space/tab/newline); replace with underscore to keep the field parseable while preserving the symbol token.
		String escaped = value == null ? "" : WHITESPACE.matcher(value).replaceAll("_");
		sb.append(key).append("=\"").append(escaped).append("\"");
	}

	private static List<String> collectArrayStrings(JsonNode arr) {
		List<String> out = new ArrayList<>();
		if (arr == null || !arr.isArray()) {
			return out;
		}
		for (JsonNode entry : arr) {
			String s = entry.asText("");
			if (!s.isEmpty()) {
				out.add(s);
			}
		}
		return out;
	}

	// VCF v4.3 §1.4.1.1: REF/ALT bases must be one of A/C/G/T/N. IUPAC ambiguity codes (R/Y/S/W/K/M/B/D/H/V) are valid only as symbolic alleles — i.e. when the entire ALT is `<R>`, referencing the matching ##ALT=<ID=R,..> header line.
	// A single-base ALT made of an IUPAC code is therefore wrapped in angle brackets; the same code embedded in a longer multi-base ALT is collapsed to N (we lose the specific ambiguity but the row stays parseable; the legacy python generator wrapped unconditionally and produced files htsjdk refused to read).
	private static String wrapIupacAmbiguityCodes(String s) {
		if (s == null || s.isEmpty()) {
			return s;
		}
		if (s.length() == 1 && IUPAC_AMBIGUITY_CODES.contains(s.charAt(0))) {
			return "<" + s + ">";
		}
		StringBuilder out = null;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (IUPAC_AMBIGUITY_CODES.contains(c)) {
				if (out == null) {
					out = new StringBuilder(s.length());
					out.append(s, 0, i);
				}
				out.append('N');
			} else if (out != null) {
				out.append(c);
			}
		}
		return out == null ? s : out.toString();
	}

	private static List<String> dedup(List<String> values) {
		Set<String> seen = new LinkedHashSet<>(values);
		return new ArrayList<>(seen);
	}
}
