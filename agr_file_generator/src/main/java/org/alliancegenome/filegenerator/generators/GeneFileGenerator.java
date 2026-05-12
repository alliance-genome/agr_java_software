package org.alliancegenome.filegenerator.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.writers.JsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneFileGenerator extends FileGenerator {

	private static final String AUTOMATED_NOTE_TYPE = "automated_gene_description";
	private static final String MOD_AUTOMATED_NOTE_TYPE = "MOD_provided_automated_gene_description";
	private static final String MOD_NOTE_TYPE = "MOD_provided_gene_description";
	private static final String UNIPROT_PREFIX = "UniProtKB:";
	private static final String LINKML_README_URL = "https://alliance-genome.github.io/agr_curation_schema/Gene/";

	public GeneFileGenerator(FileGeneratorConfig config) {
		super(config);
	}

	@Override
	protected void generate() throws Exception {
		scrollAndWrite();
	}

	@Override
	protected String jsonReadmeOverride() {
		return LINKML_README_URL;
	}

	@Override
	protected String taxonPath() {
		return "gene.taxon.curie";
	}

	@Override
	protected List<String> additionalSourceIncludes() {
		return List.of(
				"gene.geneSynonyms",
				"gene.geneSecondaryIds",
				"gene.crossReferences",
				"gene.gcrpCrossReference",
				"gene.relatedNotes",
				"gene.geneGenomicLocationAssociations"
		);
	}

	@Override
	protected JsonNode customizeRow(JsonNode hit) {
		if (!hit.isObject()) {
			return hit;
		}
		ObjectNode obj = (ObjectNode) hit;

		obj.put("_geneSynonyms", joinNodeStrings(hit, "gene.geneSynonyms", "displayText"));
		obj.put("_geneSecondaryIds", joinNodeStrings(hit, "gene.geneSecondaryIds", "secondaryId"));
		obj.put("_geneCrossReferences", buildCrossReferences(hit));
		obj.put("_allianceAutomatedDescription", findNoteText(hit, AUTOMATED_NOTE_TYPE));
		obj.put("_modAutomatedDescription", findNoteText(hit, MOD_AUTOMATED_NOTE_TYPE));
		obj.put("_modDescription", findNoteText(hit, MOD_NOTE_TYPE));
		obj.put("_assembly", JsonPath.resolveString(hit, "gene.taxon.species.assembly_curie"));

		return hit;
	}

	private static String joinNodeStrings(JsonNode root, String arrayPath, String fieldName) {
		JsonNode arr = JsonPath.resolve(root, arrayPath);
		if (arr == null || !arr.isArray()) {
			return "";
		}
		List<String> out = new ArrayList<>();
		for (JsonNode entry : arr) {
			String v = entry.path(fieldName).asText("");
			if (!v.isEmpty()) {
				out.add(v);
			}
		}
		Collections.sort(out);
		return String.join("|", out);
	}

	/**
	 * Bar-separated list of crossReferences[].referencedCurie. The gene's own primaryExternalId is filtered out, and the single GCRP cross reference (gene.gcrpCrossReference.referencedCurie) is always emitted with a " (GCRP)" suffix — appended if it was not already present in the crossReferences list, or tagged in-place if it was. Output is de-duplicated and sorted alphabetically; the GCRP entry sorts naturally amongst the other UniProt IDs.
	 */
	private static String buildCrossReferences(JsonNode hit) {
		String selfId = JsonPath.resolveString(hit, "gene.primaryExternalId");
		String gcrp = JsonPath.resolveString(hit, "gene.gcrpCrossReference.referencedCurie");
		String gcrpTagged = gcrp.isEmpty() ? "" : gcrp + " (GCRP)";

		List<String> out = new ArrayList<>();
		JsonNode arr = JsonPath.resolve(hit, "gene.crossReferences");
		if (arr != null && arr.isArray()) {
			for (JsonNode entry : arr) {
				String curie = entry.path("referencedCurie").asText("");
				if (curie.isEmpty() || curie.equals(selfId)) {
					continue;
				}
				if (!gcrp.isEmpty() && curie.startsWith(UNIPROT_PREFIX) && curie.equals(gcrp)) {
					out.add(gcrpTagged);
				} else {
					out.add(curie);
				}
			}
		}
		if (!gcrpTagged.isEmpty() && !gcrp.equals(selfId) && !out.contains(gcrpTagged)) {
			out.add(gcrpTagged);
		}
		List<String> deduped = new ArrayList<>(new LinkedHashSet<>(out));
		Collections.sort(deduped);
		return String.join("|", deduped);
	}

	private static String findNoteText(JsonNode hit, String noteTypeName) {
		JsonNode notes = JsonPath.resolve(hit, "gene.relatedNotes");
		if (notes == null || !notes.isArray()) {
			return "";
		}
		for (JsonNode note : notes) {
			String type = note.path("noteType").path("name").asText("");
			if (noteTypeName.equals(type)) {
				return note.path("freeText").asText("");
			}
		}
		return "";
	}
}
