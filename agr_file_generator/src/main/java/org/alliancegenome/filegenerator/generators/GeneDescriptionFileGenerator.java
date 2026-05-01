package org.alliancegenome.filegenerator.generators;

import java.util.List;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.writers.JsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneDescriptionFileGenerator extends FileGenerator {

	public GeneDescriptionFileGenerator(FileGeneratorConfig config) {
		super(config);
	}

	@Override
	protected void generate() throws Exception {
		scrollAndWrite();
	}

	@Override
	protected JsonNode customizeRow(JsonNode hit) {
		String desc = JsonPath.resolveString(hit, "geneDescription");
		if (desc == null || desc.isEmpty()) {
			String fallback = JsonPath.resolveString(hit, "automatedGeneDescription");
			if (fallback != null && !fallback.isEmpty() && hit.isObject()) {
				((ObjectNode) hit).put("geneDescription", fallback);
			}
		}
		return hit;
	}

	@Override
	protected String taxonPath() {
		// gene_search_result has no taxon curie, only a `species` name string;
		// the base resolver falls through to SpeciesLookup.taxonForName(species).
		return "taxonId";
	}

	@Override
	protected List<String> additionalSourceIncludes() {
		return List.of("automatedGeneDescription");
	}
}
