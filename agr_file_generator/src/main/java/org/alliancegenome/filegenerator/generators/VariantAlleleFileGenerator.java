package org.alliancegenome.filegenerator.generators;

import java.util.ArrayList;
import java.util.Collections;
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
		if (!hit.isObject()) {
			return hit;
		}
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

		// HasDisease / HasPhenotype: FMS uses "yes"/"-" not "true"/"false".
		obj.put("_hasDisease", boolToYesDash(hit.path("hasDisease")));
		obj.put("_hasPhenotype", boolToYesDash(hit.path("hasPhenotype")));

		return hit;
	}

	@Override
	protected List<JsonNode> customizeRows(JsonNode customizedHit) {
		if (!customizedHit.isObject()) {
			return List.of(customizedHit);
		}
		ObjectNode base = (ObjectNode) customizedHit;

		String alleleTaxonCurie = JsonPath.resolveString(base, "allele.taxon.curie");
		String modPrefix = (alleleTaxonCurie != null && species != null) ? species.modFor(alleleTaxonCurie) : null;

		JsonNode variantList = JsonPath.resolve(base, "variantList");
		if (variantList == null || !variantList.isArray() || variantList.size() == 0) {
			ObjectNode row = base.deepCopy();
			populateEmptyVariantFields(row);
			return List.of(row);
		}

		List<JsonNode> rows = new ArrayList<>(variantList.size());
		for (JsonNode v : variantList) {
			ObjectNode row = base.deepCopy();
			populateVariantFields(row, v, modPrefix);
			rows.add(row);
		}
		return rows;
	}

	private static void populateEmptyVariantFields(ObjectNode row) {
		row.put("_variantSymbol", "");
		row.put("_variantSynonyms", "");
		row.put("_variantsTypeId", "");
		row.put("_variantsTypeName", "");
		row.put("_variantsHgvsNames", "");
		row.put("_assembly", "");
		row.put("_chromosome", "");
		row.put("_startPosition", "");
		row.put("_endPosition", "");
		row.put("_sequenceOfReference", "");
		row.put("_sequenceOfVariant", "");
		row.put("_mostSevereConsequence", "");
		row.put("_variantAffectedGeneId", "");
		row.put("_variantAffectedGeneSymbol", "");
		row.put("_variantInformationReference", "");
	}

	private static void populateVariantFields(ObjectNode row, JsonNode v, String modPrefix) {
		List<String> variantSynonyms = new ArrayList<>();
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
		List<String> referenceCuries = new ArrayList<>();

		// VariantInformationReference — for each reference, emit exactly ONE curie. Priority: PMID, then MOD curie matching the file's MOD, then any paper-prefixed MOD curie. Skips DOI / PMCID / etc.
		JsonNode refs = v.path("references");
		if (refs.isArray()) {
			for (JsonNode r : refs) {
				JsonNode xrefs = r.path("crossReferences");
				if (xrefs.isArray()) {
					String pmid = null;
					String modMatch = null;
					String anyPaper = null;
					for (JsonNode x : xrefs) {
						String c = x.path("referencedCurie").asText("");
						if (c.isEmpty()) {
							continue;
						}
						if (c.startsWith("PMID:")) {
							if (pmid == null) {
								pmid = c;
							}
						} else if (isPaperCurie(c)) {
							if (modPrefix != null && modMatch == null && c.startsWith(modPrefix + ":")) {
								modMatch = c;
							}
							if (anyPaper == null) {
								anyPaper = c;
							}
						}
					}
					String chosen;
					if (pmid != null) {
						chosen = pmid;
					} else if (modMatch != null) {
						chosen = modMatch;
					} else {
						chosen = anyPaper;
					}
					if (chosen != null) {
						referenceCuries.add(chosen);
					}
				}
			}
		}

		JsonNode locs = v.path("curatedVariantGenomicLocations");
		if (locs.isArray()) {
			for (JsonNode loc : locs) {
				String hgvs = loc.path("hgvs").asText("");
				String chromName = loc.path("variantGenomicLocationAssociationObject").path("name").asText("");
				addIfPresent(hgvsNames, hgvs);
				addIfPresent(assemblies, loc.path("variantGenomicLocationAssociationObject").path("genomeAssembly").path("primaryExternalId").asText(""));
				addIfPresent(chromosomes, chromName);
				// VariantSynonyms = RefSeq-form hgvs + chromosome-level form (RefSeq accession swapped for chromosome name).
				if (!hgvs.isEmpty()) {
					addIfPresent(variantSynonyms, hgvs);
					int colonIdx = hgvs.indexOf(':');
					if (!chromName.isEmpty() && colonIdx > 0) {
						addIfPresent(variantSynonyms, chromName + hgvs.substring(colonIdx));
					}
				}
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

		row.put("_variantSymbol", joinList(hgvsNames));
		row.put("_variantSynonyms", joinList(variantSynonyms));
		row.put("_variantsTypeId", v.path("variantType").path("curie").asText(""));
		row.put("_variantsTypeName", v.path("variantType").path("name").asText(""));
		row.put("_variantsHgvsNames", joinList(hgvsNames));
		row.put("_assembly", joinList(dedup(assemblies)));
		row.put("_chromosome", joinList(dedup(chromosomes)));
		row.put("_startPosition", joinList(startPositions));
		row.put("_endPosition", joinList(endPositions));
		row.put("_sequenceOfReference", joinList(referenceSeqs));
		row.put("_sequenceOfVariant", joinList(variantSeqs));
		row.put("_mostSevereConsequence", joinList(dedup(consequences)));
		row.put("_variantAffectedGeneId", joinList(dedup(affectedGeneIds)));
		row.put("_variantAffectedGeneSymbol", joinList(dedup(affectedGeneSymbols)));
		// Between references: comma-join (matches FMS); each reference contributes exactly one curie (PMID preferred, then file-MOD curie, then any paper curie).
		List<String> uniqueRefs = dedup(referenceCuries);
		row.put("_variantInformationReference", uniqueRefs.isEmpty() ? "" : String.join(",", uniqueRefs));
	}

	// Cross-reference prefixes that count as a paper identifier per ReferenceConstants.primaryXrefOrder; everything else (DOI, PMCID, ISBN, ...) is skipped.
	private static final Set<String> PAPER_PREFIXES = Set.of("PMID", "FB", "MGI", "RGD", "SGD", "WB", "XB", "ZFIN");

	private static boolean isPaperCurie(String curie) {
		int idx = curie.indexOf(':');
		if (idx <= 0) {
			return false;
		}
		return PAPER_PREFIXES.contains(curie.substring(0, idx));
	}

	private static String boolToYesDash(JsonNode v) {
		if (v == null || v.isMissingNode() || v.isNull()) {
			return "-";
		}
		return v.asBoolean(false) ? "yes" : "-";
	}

	private static String joinStrings(JsonNode array, String childField) {
		if (array == null || !array.isArray()) {
			return "";
		}
		List<String> out = new ArrayList<>();
		for (JsonNode entry : array) {
			String v = entry.path(childField).asText("");
			if (!v.isEmpty()) {
				out.add(v);
			}
		}
		Collections.sort(out);
		return String.join("|", out);
	}

	private static void addIfPresent(List<String> list, String value) {
		if (value != null && !value.isEmpty()) {
			list.add(value);
		}
	}

	private static String joinList(List<String> values) {
		return values.isEmpty() ? "" : String.join("|", values);
	}

	private static List<String> dedup(List<String> values) {
		Set<String> seen = new LinkedHashSet<>(values);
		return new ArrayList<>(seen);
	}
}
