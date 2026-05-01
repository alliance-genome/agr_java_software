package org.alliancegenome.filegenerator.generators;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
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

	public VariantVcfFileGenerator(FileGeneratorConfig config) {
		super(config);
	}

	@Override
	protected void generate() throws Exception {
		scrollAndWrite();
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
			pos = loc.path("start").asText("");
			id = loc.path("hgvs").asText("");
			ref = loc.path("referenceSequence").asText("");
			alt = loc.path("variantSequence").asText("");
		}

		obj.put("_chrom", chrom);
		obj.put("_pos", pos);
		obj.put("_id", id);
		obj.put("_ref", ref);
		obj.put("_alt", alt);
		obj.put("_qual", ".");
		obj.put("_filter", ".");
		obj.put("_info", buildInfo(hit, loc, id));

		return hit;
	}

	private static String buildInfo(JsonNode hit, JsonNode loc, String hgvs) {
		StringBuilder sb = new StringBuilder();

		appendKv(sb, "hgvs_nomenclature", hgvs);

		String geneLevel = loc == null ? "" : firstNonEmpty(
				loc.path("mostSevereConsequence").path("variantConsequence").path("name").asText(""),
				loc.path("mostSevereConsequence").path("vepConsequences").path(0).path("name").asText(""));
		appendKv(sb, "geneLevelConsequence", geneLevel);

		// transcriptLevelConsequence — comma-joined names from each predictedVariantConsequences entry's vepConsequences[0]
		List<String> txLevel = new ArrayList<>();
		List<String> txImpacts = new ArrayList<>();
		List<String> geneSymbols = new ArrayList<>();
		List<String> transcriptIds = new ArrayList<>();
		List<String> transcriptGff3Ids = new ArrayList<>();
		List<String> transcriptGff3Names = new ArrayList<>();
		if (loc != null) {
			JsonNode pvc = loc.path("predictedVariantConsequences");
			if (pvc.isArray()) {
				for (JsonNode entry : pvc) {
					String c = entry.path("vepConsequences").path(0).path("name").asText("");
					if (!c.isEmpty()) {
						txLevel.add(c);
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
						transcriptIds.add(tn);
					}
					String gff3Id = entry.path("variantTranscript").path("modCrossRefCompleteUrl").asText("");
					if (!gff3Id.isEmpty()) {
						transcriptGff3Ids.add(gff3Id);
					}
					String gff3Name = entry.path("variantTranscript").path("displayName").asText("");
					if (!gff3Name.isEmpty()) {
						transcriptGff3Names.add(gff3Name);
					}
				}
			}
		}
		appendKv(sb, "transcriptLevelConsequence", String.join(",", txLevel));

		String geneImpact = loc == null ? "" : loc.path("mostSevereConsequence").path("vepImpact").path("name").asText("");
		appendKv(sb, "geneImpact", geneImpact);
		appendKv(sb, "transcriptImpact", String.join(",", txImpacts));

		appendKv(sb, "allele_ids", JsonPath.resolveString(hit, "allele.primaryExternalId"));
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
		sb.append(key).append("=\"").append(value == null ? "" : value).append("\"");
	}

	private static String firstNonEmpty(String... values) {
		for (String v : values) {
			if (v != null && !v.isEmpty()) {
				return v;
			}
		}
		return "";
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

	private static List<String> dedup(List<String> values) {
		Set<String> seen = new LinkedHashSet<>(values);
		return new ArrayList<>(seen);
	}
}
