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

@Slf4j
public class OrthologyFileGenerator extends FileGenerator {

	public OrthologyFileGenerator(FileGeneratorConfig config) {
		super(config);
	}

	@Override
	protected void generate() throws Exception {
		scrollAndWrite();
	}

	@Override
	protected String taxonPath() {
		return "geneToGeneOrthologyGenerated.subjectGene.taxon.curie";
	}

	/**
	 * Object gene taxon — included so a row whose subject is HUMAN and whose object is e.g.
	 * MGI is still considered "in scope" if the MOD whitelist contains either side. Orthology
	 * only writes COMBINED, but the dispatch's MOD-whitelist gate uses this list to decide
	 * whether to keep or drop the row.
	 */
	@Override
	protected List<String> additionalTaxonCuries(JsonNode hit) {
		String objCurie = JsonPath.resolveString(hit, "geneToGeneOrthologyGenerated.objectGene.taxon.curie");
		return objCurie == null || objCurie.isEmpty() ? List.of() : List.of(objCurie);
	}

	private static final String STRINGENCY = "stringent";

	/** FMS only emits stringent orthologs. */
	@Override
	protected boolean shouldEmit(JsonNode hit) {
		String stringency = JsonPath.resolveString(hit, "stringencyFilter");
		return STRINGENCY.equalsIgnoreCase(stringency);
	}

	@Override
	protected String stringencyFilter() {
		return STRINGENCY;
	}

	/** Match FMS's `# Orthology Filter: Stringent` line, injected between Help Desk and Taxon IDs. */
	@Override
	protected List<String> extraHeaderLines() {
		return List.of("Orthology Filter: Stringent");
	}

	/** Pull the entire orthology subtree — synthetic field names in the field map would otherwise strip it. */
	@Override
	protected List<String> additionalSourceIncludes() {
		return List.of("geneToGeneOrthologyGenerated", "stringencyFilter");
	}

	@Override
	protected JsonNode customizeRow(JsonNode hit) {
		if (!hit.isObject()) {
			return hit;
		}
		ObjectNode obj = (ObjectNode) hit;

		List<String> matched = collectNames(hit, "geneToGeneOrthologyGenerated.predictionMethodsMatched");
		List<String> notMatched = collectNames(hit, "geneToGeneOrthologyGenerated.predictionMethodsNotMatched");

		// Algorithms is the pipe-delimited list of MATCHED methods, deduplicated and order-preserved.
		Set<String> matchedDedup = new LinkedHashSet<>(matched);
		obj.put("_algorithms", String.join("|", matchedDedup));

		// AlgorithmsMatch is the count of distinct matched methods.
		obj.put("_algorithmsMatch", String.valueOf(matchedDedup.size()));

		// OutOfAlgorithms is matched + notMatched (both counted distinct).
		Set<String> notMatchedDedup = new LinkedHashSet<>(notMatched);
		obj.put("_outOfAlgorithms", String.valueOf(matchedDedup.size() + notMatchedDedup.size()));

		return hit;
	}

	private static List<String> collectNames(JsonNode root, String arrayPath) {
		List<String> out = new ArrayList<>();
		JsonNode arr = JsonPath.resolve(root, arrayPath);
		if (arr == null || !arr.isArray()) {
			return out;
		}
		for (JsonNode entry : arr) {
			String n = entry.path("name").asText("");
			if (!n.isEmpty()) {
				out.add(n);
			}
		}
		return out;
	}
}
