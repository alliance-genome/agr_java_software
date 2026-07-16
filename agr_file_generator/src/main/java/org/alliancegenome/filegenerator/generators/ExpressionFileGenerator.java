package org.alliancegenome.filegenerator.generators;

import java.util.LinkedHashSet;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.writers.JsonPath;

import com.fasterxml.jackson.databind.JsonNode;
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

		// Build the SourceURL from the first crossReference's urlTemplate + referencedCurie.
		JsonNode xrefs = JsonPath.resolve(hit, "geneExpressionAnnotation.crossReferences");
		if (xrefs != null && xrefs.isArray() && xrefs.size() > 0) {
			JsonNode first = xrefs.get(0);
			String curie = first.path("referencedCurie").asText("");
			String urlTemplate = first.path("resourceDescriptorPage").path("urlTemplate").asText("");
			if (!curie.isEmpty() && !urlTemplate.isEmpty()) {
				int idx = curie.indexOf(':');
				String localId = idx >= 0 ? curie.substring(idx + 1) : curie;
				String url = urlTemplate.replace("[%s]", localId);
				obj.put("_sourceUrl", url);
			}
		}

		// Pipe-join the three qualifier lists under whereExpressed into synthetic ID + name fields.
		String wherePath = "geneExpressionAnnotation.expressionPattern.whereExpressed";
		obj.put("_anatomyQualifierIds", joinField(hit, wherePath + ".anatomicalStructureQualifiers", "curie"));
		obj.put("_anatomyQualifierNames", joinField(hit, wherePath + ".anatomicalStructureQualifiers", "name"));
		obj.put("_subStructureQualifierIds", joinField(hit, wherePath + ".anatomicalSubstructureQualifiers", "curie"));
		obj.put("_subStructureQualifierNames", joinField(hit, wherePath + ".anatomicalSubstructureQualifiers", "name"));
		obj.put("_cellularComponentQualifierIds", joinField(hit, wherePath + ".cellularComponentQualifiers", "curie"));
		obj.put("_cellularComponentQualifierNames", joinField(hit, wherePath + ".cellularComponentQualifiers", "name"));

		// Pipe-join the references from the referenceXrefs array; the consolidation upstream in agr_curation packs all PMIDs supporting one annotation into this list.
		obj.put("_reference", joinField(hit, "referenceXrefs", "referencedCurie"));

		return hit;
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

	private static String joinScalarArray(JsonNode root, String arrayPath) {
		JsonNode array = JsonPath.resolve(root, arrayPath);
		if (array == null || !array.isArray() || array.size() == 0) {
			return "";
		}
		LinkedHashSet<String> values = new LinkedHashSet<>();
		for (JsonNode element : array) {
			String value = element.asText("");
			if (!value.isEmpty()) {
				values.add(value);
			}
		}
		return String.join(QUALIFIER_DELIMITER, values);
	}
}
