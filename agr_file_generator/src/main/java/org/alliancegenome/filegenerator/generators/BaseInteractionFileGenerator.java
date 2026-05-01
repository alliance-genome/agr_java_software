package org.alliancegenome.filegenerator.generators;

import java.util.List;
import java.util.Map;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.writers.JsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Shared PSI-MITAB row builder for Molecular and Genetic interactions. Subclasses provide the
 * doc root path (e.g. "geneMolecularInteraction") and the species lookup is reused across both.
 */
@Slf4j
public abstract class BaseInteractionFileGenerator extends FileGenerator {

	/**
	 * Map MOD/Alliance database codes to lowercase PSI-MI database prefixes used in the
	 * formatted IDs (e.g. "WB:WBGene00002996" → "wormbase:WBGene00002996").
	 */
	private static final Map<String, String> MOD_TO_PSIMI_PREFIX = Map.ofEntries(
			Map.entry("WB", "wormbase"),
			Map.entry("MGI", "mgi"),
			Map.entry("RGD", "rgd"),
			Map.entry("FB", "flybase"),
			Map.entry("ZFIN", "zfin"),
			Map.entry("SGD", "sgd"),
			Map.entry("HGNC", "hgnc"),
			Map.entry("XB", "xenbase")
	);

	/**
	 * Short taxon code (e.g. "caeel") used as the parenthesised species code in
	 * `taxid:N(<code>)|taxid:N(<full name>)`. Keyed by NCBITaxon curie.
	 */
	private static final Map<String, String> TAXON_CODE = Map.ofEntries(
			Map.entry("NCBITaxon:6239",   "caeel"),
			Map.entry("NCBITaxon:7227",   "drome"),
			Map.entry("NCBITaxon:7955",   "danre"),
			Map.entry("NCBITaxon:9606",   "human"),
			Map.entry("NCBITaxon:10090",  "mouse"),
			Map.entry("NCBITaxon:10116",  "rat"),
			Map.entry("NCBITaxon:559292", "yeast"),
			Map.entry("NCBITaxon:8355",   "xenla"),
			Map.entry("NCBITaxon:8364",   "xentr"),
			Map.entry("NCBITaxon:2697049", "sars2")
	);

	public BaseInteractionFileGenerator(FileGeneratorConfig config) {
		super(config);
	}

	@Override
	protected void generate() throws Exception {
		scrollAndWrite();
	}

	/** Each subclass returns its top-level field name in the ES doc. */
	protected abstract String docRoot();

	/**
	 * The PSI-MITAB field map is full of synthetic `_*` field names (built in customizeRow), so the
	 * default _source-include logic would strip the doc bare. Pull in the entire {@code docRoot()}
	 * subtree instead — that's where every ES path used by customizeRow lives.
	 */
	@Override
	protected List<String> additionalSourceIncludes() {
		return List.of(docRoot());
	}

	@Override
	protected String taxonPath() {
		return docRoot() + ".geneAssociationSubject.taxon.curie";
	}

	/**
	 * Each interaction is an A↔B pair across (potentially) two species. Routing only by the
	 * subject side hides cross-species interactions from the object side's per-MOD file
	 * (e.g. SARS-CoV-2 viral proteins as interactor B in human-virus interactions). Add the
	 * object's taxon so the row also lands in that MOD's TAXON writer when the two differ.
	 */
	@Override
	protected List<String> additionalTaxonCuries(JsonNode hit) {
		String objCurie = JsonPath.resolveString(hit, docRoot() + ".geneGeneAssociationObject.taxon.curie");
		return objCurie == null || objCurie.isEmpty() ? List.of() : List.of(objCurie);
	}

	@Override
	protected JsonNode customizeRow(JsonNode hit) {
		if (!hit.isObject()) return hit;
		ObjectNode obj = (ObjectNode) hit;
		String root = docRoot();

		JsonNode subj = JsonPath.resolve(hit, root + ".geneAssociationSubject");
		JsonNode objE = JsonPath.resolve(hit, root + ".geneGeneAssociationObject");

		obj.put("_idA", formatGeneId(subj));
		obj.put("_idB", formatGeneId(objE));
		obj.put("_aliasA", formatAlias(subj));
		obj.put("_aliasB", formatAlias(objE));
		obj.put("_taxidA", formatTaxid(subj));
		obj.put("_taxidB", formatTaxid(objE));
		obj.put("_typeA", formatPsiMi(JsonPath.resolve(hit, root + ".interactorAType")));
		obj.put("_typeB", formatPsiMi(JsonPath.resolve(hit, root + ".interactorBType")));
		obj.put("_expRoleA", formatPsiMi(JsonPath.resolve(hit, root + ".interactorARole")));
		obj.put("_expRoleB", formatPsiMi(JsonPath.resolve(hit, root + ".interactorBRole")));
		obj.put("_interactionType", formatPsiMi(JsonPath.resolve(hit, root + ".interactionType")));
		obj.put("_sourceDatabase", formatPsiMi(JsonPath.resolve(hit, root + ".aggregationDatabase")));
		obj.put("_detectionMethod", formatPsiMi(JsonPath.resolve(hit, root + ".detectionMethod")));

		// Interaction identifier — `<sourcePrefix>:<id>`. The interactionId already carries
		// `biogrid:...` from BioGRID; for native MOD interactions the prefix is the MOD's PSI-MI
		// name (e.g. wormbase:WBInteraction...).
		String interactionId = JsonPath.resolveString(hit, root + ".interactionId");
		obj.put("_interactionId", interactionId);

		// Publication
		String pmid = JsonPath.resolveString(hit, root + ".evidence.0.referenceID");
		obj.put("_pubmed", pmid.isEmpty() ? "" : pmid.toLowerCase().replace("pmid:", "pubmed:"));
		obj.put("_author", JsonPath.resolveString(hit, root + ".evidence.0.shortCitation"));

		// Dates — ISO timestamps; PSI-MITAB uses `YYYY/MM/DD`.
		obj.put("_creationDate", isoToPsiMiDate(JsonPath.resolveString(hit, root + ".dbDateCreated")));
		obj.put("_updateDate", isoToPsiMiDate(JsonPath.resolveString(hit, root + ".dbDateUpdated")));

		// Negation flag — PSI-MITAB requires "false" (not blank) when not negated.
		obj.put("_negative", "false");

		return hit;
	}

	private static String formatGeneId(JsonNode entity) {
		if (entity == null) return "";
		String curie = entity.path("primaryExternalId").asText("");
		return curieToPsiMi(curie);
	}

	private static String curieToPsiMi(String curie) {
		if (curie == null || curie.isEmpty()) return "";
		int idx = curie.indexOf(':');
		if (idx < 0) return curie;
		String prefix = curie.substring(0, idx);
		String localId = curie.substring(idx + 1);
		String psimi = MOD_TO_PSIMI_PREFIX.getOrDefault(prefix, prefix.toLowerCase());
		return psimi + ":" + localId;
	}

	private static String formatAlias(JsonNode entity) {
		if (entity == null) return "";
		String symbol = entity.path("geneSymbol").path("displayText").asText("");
		String curie = entity.path("primaryExternalId").asText("");
		if (symbol.isEmpty() || curie.isEmpty()) return "";
		int idx = curie.indexOf(':');
		String prefix = idx < 0 ? curie : curie.substring(0, idx);
		String psimi = MOD_TO_PSIMI_PREFIX.getOrDefault(prefix, prefix.toLowerCase());
		return psimi + ":" + symbol + "(public_name)";
	}

	private static String formatTaxid(JsonNode entity) {
		if (entity == null) return "";
		String curie = entity.path("taxon").path("curie").asText("");
		String name = entity.path("taxon").path("name").asText("");
		if (curie.isEmpty() || name.isEmpty()) return "";
		String txid = curie.replace("NCBITaxon:", "");
		String code = TAXON_CODE.getOrDefault(curie, "");
		StringBuilder sb = new StringBuilder();
		if (!code.isEmpty()) {
			sb.append("taxid:").append(txid).append("(").append(code).append(")|");
		}
		sb.append("taxid:").append(txid).append("(").append(name).append(")");
		return sb.toString();
	}

	private static String formatPsiMi(JsonNode term) {
		if (term == null || term.isMissingNode() || term.isNull()) return "";
		String curie = term.path("curie").asText("");
		String name = term.path("name").asText("");
		if (curie.isEmpty()) return "";
		return "psi-mi:\"" + curie + "\"(" + name + ")";
	}

	private static String isoToPsiMiDate(String iso) {
		if (iso == null || iso.length() < 10) return "";
		// "2024-06-27T..." -> "2024/06/27"
		return iso.substring(0, 10).replace("-", "/");
	}
}
