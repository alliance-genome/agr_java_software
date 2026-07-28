package org.alliancegenome.filegenerator.generators;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.writers.JsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExpressionFileGenerator extends FileGenerator {

	private static final String QUALIFIER_DELIMITER = "|";

	public ExpressionFileGenerator(FileGeneratorConfig config) {
		super(config);
	}

	@Override
	protected void generate() throws Exception {
		scrollAndWrite();
	}

	@Override
	protected String taxonPath() {
		return "geneExpressionAnnotation.expressionAnnotationSubject.taxon.curie";
	}

	@Override
	protected JsonNode customizeRow(JsonNode hit) {
		if (!hit.isObject()) {
			return hit;
		}
		ObjectNode obj = (ObjectNode) hit;

		// Pipe-join the three qualifier lists under whereExpressed into synthetic ID + name fields.
		String wherePath = "geneExpressionAnnotation.expressionPattern.whereExpressed";
		obj.put("_anatomyQualifierIds", joinField(hit, wherePath + ".anatomicalStructureQualifiers", "curie"));
		obj.put("_anatomyQualifierNames", joinField(hit, wherePath + ".anatomicalStructureQualifiers", "name"));
		obj.put("_subStructureQualifierIds", joinField(hit, wherePath + ".anatomicalSubstructureQualifiers", "curie"));
		obj.put("_subStructureQualifierNames", joinField(hit, wherePath + ".anatomicalSubstructureQualifiers", "name"));
		obj.put("_cellularComponentQualifierIds", joinField(hit, wherePath + ".cellularComponentQualifiers", "curie"));
		obj.put("_cellularComponentQualifierNames", joinField(hit, wherePath + ".cellularComponentQualifiers", "name"));

		return hit;
	}

	/**
	 * The consolidation in agr_curation's GeneExpressionDocumentBuilder pads crossReferences and referenceId so entry i of
	 * each describes the same underlying annotation. Emit one row per pair so a SourceURL is never reported against a
	 * publication it did not come from, and so no cross reference beyond the first is dropped. referenceId is the aligned
	 * list; referenceXrefs is a deduplicated set in a different order and cannot be paired positionally.
	 */
	@Override
	protected List<JsonNode> customizeRows(JsonNode customizedHit) {
		if (!customizedHit.isObject()) {
			return List.of(customizedHit);
		}
		ObjectNode obj = (ObjectNode) customizedHit;

		JsonNode xrefs = JsonPath.resolve(customizedHit, "geneExpressionAnnotation.crossReferences");
		JsonNode refIds = JsonPath.resolve(customizedHit, "referenceId");
		int xrefSize = xrefs != null && xrefs.isArray() ? xrefs.size() : 0;
		int refSize = refIds != null && refIds.isArray() ? refIds.size() : 0;

		// A doc carrying neither list still emits its single row — the location / stage / assay columns stand on their own.
		int rowCount = Math.max(1, Math.max(xrefSize, refSize));
		List<JsonNode> rows = new ArrayList<>(rowCount);
		// Scoped to this document: the same figure + publication pair recurs legitimately under other locations and stages, so only exact repeats within one gene + location + stage + assay group are dropped.
		Set<String> seenPairs = new LinkedHashSet<>();

		for (int i = 0; i < rowCount; i++) {
			String sourceUrl = buildSourceUrl(i < xrefSize ? xrefs.get(i) : null);
			// Fewer references than cross references means one publication documented in several places; every row keeps that single reference.
			String reference = refSize == 0 ? "" : (i < refSize ? refIds.get(i) : refIds.get(0)).asText("");
			if (!seenPairs.add(sourceUrl + "\t" + reference)) {
				continue;
			}
			ObjectNode row = JsonNodeFactory.instance.objectNode();
			// Shallow copy: the doc-level fields are shared by reference and never mutated, only the two per-pair fields differ.
			row.setAll(obj);
			row.put("_sourceUrl", sourceUrl);
			row.put("_reference", reference);
			rows.add(row);
		}
		return rows;
	}

	private static String buildSourceUrl(JsonNode xref) {
		if (xref == null) {
			return "";
		}
		String curie = xref.path("referencedCurie").asText("");
		String urlTemplate = xref.path("resourceDescriptorPage").path("urlTemplate").asText("");
		if (curie.isEmpty() || urlTemplate.isEmpty()) {
			return "";
		}
		int idx = curie.indexOf(':');
		String localId = idx >= 0 ? curie.substring(idx + 1) : curie;
		return urlTemplate.replace("[%s]", localId);
	}

	private static String joinField(JsonNode root, String arrayPath, String fieldName) {
		JsonNode array = JsonPath.resolve(root, arrayPath);
		if (array == null || !array.isArray() || array.size() == 0) {
			return "";
		}
		LinkedHashSet<String> values = new LinkedHashSet<>();
		for (JsonNode element : array) {
			String value = element.path(fieldName).asText("");
			if (!value.isEmpty()) {
				values.add(value);
			}
		}
		return String.join(QUALIFIER_DELIMITER, values);
	}
}
