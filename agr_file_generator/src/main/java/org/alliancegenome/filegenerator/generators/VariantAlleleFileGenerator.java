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
 * Variants/Alleles generator. Source: {@code allele_summary} in {@code site_index} — same category
 * the agr_api uses for the gene-page allele table (see {@code AlleleESService.getAllelesByGene}).
 * One row per allele; variantList-derived columns are pipe-joined when the allele has multiple
 * known variants.
 *
 * NOT scrolled here: standalone variants from {@code variant_summary}. FMS files only emit
 * allele-centric rows (Category values are all "allele" or "allele with N known variants").
 */
@Slf4j
public class VariantAlleleFileGenerator extends FileGenerator {

	public VariantAlleleFileGenerator(FileGeneratorConfig config) {
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

	@Override
	protected List<String> additionalSourceIncludes() {
		return List.of("allele", "alleleOfGene", "variantList", "alterationType", "hasDisease", "hasPhenotype");
	}

	@Override
	protected JsonNode customizeRow(JsonNode hit) {
		if (!hit.isObject()) return hit;
		ObjectNode obj = (ObjectNode) hit;

		// Allele synonyms — pipe-joined displayText.
		obj.put("_alleleSynonyms", joinStrings(JsonPath.resolve(hit, "allele.alleleSynonyms"), "displayText"));

		JsonNode variantList = JsonPath.resolve(hit, "variantList");
		int variantCount = (variantList != null && variantList.isArray()) ? variantList.size() : 0;

		// Category — FMS uses numeric counts: "allele", "allele with 1 known variant",
		// "allele with N known variants". ES alterationType is bucketed (one / multiple);
		// derive the numeric form from variantList size to match FMS.
		String category;
		if (variantCount == 0) {
			category = "allele";
		} else if (variantCount == 1) {
			category = "allele with 1 known variant";
		} else {
			category = "allele with " + variantCount + " known variants";
		}
		obj.put("_category", category);

		List<String> variantIds = new ArrayList<>();
		List<String> variantSymbols = new ArrayList<>();
		List<String> variantSynonyms = new ArrayList<>();
		List<String> variantTypeIds = new ArrayList<>();
		List<String> variantTypeNames = new ArrayList<>();
		List<String> hgvsNames = new ArrayList<>();
		List<String> assemblies = new ArrayList<>();
		List<String> chromosomes = new ArrayList<>();
		List<String> startPositions = new ArrayList<>();
		List<String> endPositions = new ArrayList<>();
		List<String> referenceSeqs = new ArrayList<>();
		List<String> variantSeqs = new ArrayList<>();
		List<String> consequences = new ArrayList<>();
		List<String> affectedGeneIds = new ArrayList<>();
		List<String> affectedGeneSymbols = new ArrayList<>();

		if (variantList != null && variantList.isArray()) {
			for (JsonNode v : variantList) {
				addIfPresent(variantIds, v.path("primaryExternalId").asText(""));
				addIfPresent(variantSymbols, v.path("variantSymbol").path("displayText").asText(""));
				addIfPresent(variantTypeIds, v.path("variantType").path("curie").asText(""));
				addIfPresent(variantTypeNames, v.path("variantType").path("name").asText(""));

				JsonNode locs = v.path("curatedVariantGenomicLocations");
				if (locs.isArray()) {
					for (JsonNode loc : locs) {
						addIfPresent(hgvsNames, loc.path("hgvs").asText(""));
						addIfPresent(assemblies, loc.path("variantGenomicLocationAssociationObject").path("genomeAssembly").path("primaryExternalId").asText(""));
						addIfPresent(chromosomes, loc.path("variantGenomicLocationAssociationObject").path("name").asText(""));
						String s = loc.path("start").asText("");
						String e = loc.path("end").asText("");
						addIfPresent(startPositions, s);
						addIfPresent(endPositions, e);
						addIfPresent(referenceSeqs, loc.path("referenceSequence").asText(""));
						addIfPresent(variantSeqs, loc.path("variantSequence").asText(""));

						JsonNode msc = loc.path("mostSevereConsequence");
						String consequenceName = msc.path("variantConsequence").path("name").asText("");
						if (consequenceName.isEmpty()) {
							consequenceName = msc.path("name").asText("");
						}
						addIfPresent(consequences, consequenceName);

						JsonNode txGenes = msc.path("variantTranscript").path("transcriptGeneAssociations");
						if (txGenes.isArray()) {
							for (JsonNode tg : txGenes) {
								JsonNode g = tg.path("transcriptGeneAssociationObject");
								addIfPresent(affectedGeneIds, g.path("primaryExternalId").asText(""));
								addIfPresent(affectedGeneSymbols, g.path("geneSymbol").path("displayText").asText(""));
							}
						}
					}
				}
			}
		}

		obj.put("_variantId", joinList(variantIds));
		obj.put("_variantSymbol", joinList(variantSymbols));
		obj.put("_variantSynonyms", joinList(variantSynonyms));
		obj.put("_variantsTypeId", joinList(variantTypeIds));
		obj.put("_variantsTypeName", joinList(variantTypeNames));
		obj.put("_variantsHgvsNames", joinList(hgvsNames));
		obj.put("_assembly", joinList(dedup(assemblies)));
		obj.put("_chromosome", joinList(dedup(chromosomes)));
		obj.put("_startPosition", joinList(startPositions));
		obj.put("_endPosition", joinList(endPositions));
		obj.put("_sequenceOfReference", joinList(referenceSeqs));
		obj.put("_sequenceOfVariant", joinList(variantSeqs));
		obj.put("_mostSevereConsequence", joinList(dedup(consequences)));
		obj.put("_variantAffectedGeneId", joinList(dedup(affectedGeneIds)));
		obj.put("_variantAffectedGeneSymbol", joinList(dedup(affectedGeneSymbols)));

		// HasDisease / HasPhenotype: FMS uses "yes"/"-" not "true"/"false".
		obj.put("_hasDisease", boolToYesDash(hit.path("hasDisease")));
		obj.put("_hasPhenotype", boolToYesDash(hit.path("hasPhenotype")));

		return hit;
	}

	private static String boolToYesDash(JsonNode v) {
		if (v == null || v.isMissingNode() || v.isNull()) return "-";
		return v.asBoolean(false) ? "yes" : "-";
	}

	private static String joinStrings(JsonNode array, String childField) {
		if (array == null || !array.isArray()) return "";
		List<String> out = new ArrayList<>();
		for (JsonNode entry : array) {
			String v = entry.path(childField).asText("");
			if (!v.isEmpty()) out.add(v);
		}
		return String.join("|", out);
	}

	private static void addIfPresent(List<String> list, String value) {
		if (value != null && !value.isEmpty()) list.add(value);
	}

	private static String joinList(List<String> values) {
		return values.isEmpty() ? "" : String.join("|", values);
	}

	private static List<String> dedup(List<String> values) {
		Set<String> seen = new LinkedHashSet<>(values);
		return new ArrayList<>(seen);
	}
}
