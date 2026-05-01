package org.alliancegenome.filegenerator.generators;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.writers.JsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExpressionFileGenerator extends FileGenerator {

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
				if (hit.isObject()) {
					((ObjectNode) hit).put("_sourceUrl", url);
				}
			}
		}
		return hit;
	}
}
