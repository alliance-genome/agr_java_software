package org.alliancegenome.filegenerator.generators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
	 * The consolidation in agr_curation's GeneExpressionDocumentBuilder pads crossReferences and referenceId so
	 * entry i of each describes the same underlying annotation, and one annotation is one publication. Group the
	 * cross references by the reference they are aligned to and emit one row per distinct reference, so the row
	 * count matches the annotation count while no SourceURL is reported against a publication it did not come
	 * from and no cross reference is dropped.
	 *
	 * referenceId is the aligned list; referenceXrefs is a deduplicated set in a different order and cannot be
	 * paired positionally.
	 *
	 * MGI and WB take their cross references from the expression experiment rather than the annotation, so a
	 * single publication commonly carries many assay URLs; those share one row with the URLs pipe-joined rather
	 * than fanning out into rows that would each claim a specific assay.
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

		// A doc with neither list still emits one row — location / stage / assay stand on their own.
		int pairCount = Math.max(1, Math.max(xrefSize, refSize));
		// Insertion-ordered so rows follow the document's reference order; the URL sets drop exact repeats.
		Map<String, Set<String>> urlsByReference = new LinkedHashMap<>();

		for (int i = 0; i < pairCount; i++) {
			/*
			 * Cross references past the end of the reference list belong to the first publication — the
			 * group.size() == 1 short-circuit upstream skips the padding, so the lists are not always equal
			 * length.
			 */
			String reference = refSize == 0 ? "" : (i < refSize ? refIds.get(i) : refIds.get(0)).asText("");
			String sourceUrl = buildSourceUrl(i < xrefSize ? xrefs.get(i) : null);
			Set<String> urls = urlsByReference.computeIfAbsent(reference, r -> new LinkedHashSet<>());
			if (!sourceUrl.isEmpty()) {
				urls.add(sourceUrl);
			}
		}

		List<JsonNode> rows = new ArrayList<>(urlsByReference.size());
		for (Map.Entry<String, Set<String>> entry : urlsByReference.entrySet()) {
			ObjectNode row = JsonNodeFactory.instance.objectNode();
			// Shallow copy: doc-level fields are shared by reference and never mutated, only the two below differ.
			row.setAll(obj);
			row.put("_sourceUrl", String.join(QUALIFIER_DELIMITER, entry.getValue()));
			row.put("_reference", entry.getKey());
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
