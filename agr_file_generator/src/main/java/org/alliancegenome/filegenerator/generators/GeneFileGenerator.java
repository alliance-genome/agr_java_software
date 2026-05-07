package org.alliancegenome.filegenerator.generators;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.writers.JsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneFileGenerator extends FileGenerator {

	private static final String AUTOMATED_NOTE_TYPE = "automated_gene_description";
	private static final String MOD_NOTE_TYPE = "MOD_provided_gene_description";
	private static final String UNIPROT_PREFIX = "UniProtKB:";

	public GeneFileGenerator(FileGeneratorConfig config) {
		super(config);
	}

	@Override
	protected void generate() throws Exception {
		scrollAndWrite();
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
		obj.put("_automatedDescription", findNoteText(hit, AUTOMATED_NOTE_TYPE));
		obj.put("_modDescription", findNoteText(hit, MOD_NOTE_TYPE));
		obj.put("_assembly", JsonPath.resolveString(hit, "gene.geneGenomicLocationAssociations.0.geneGenomicLocationAssociationObject.taxon.species.assembly_curie"));

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
		return String.join("|", out);
	}

	/**
	 * Bar-separated list of every crossReferences[].referencedCurie. The single GCRP cross
	 * reference (gene.gcrpCrossReference.referencedCurie) is matched against the list and that
	 * entry is suffixed with " (GCRP)" so consumers can identify which UniProtKB curie is the
	 * canonical Gene-Centric Reference Proteome entry.
	 */
	private static String buildCrossReferences(JsonNode hit) {
		JsonNode arr = JsonPath.resolve(hit, "gene.crossReferences");
		if (arr == null || !arr.isArray()) {
			return "";
		}
		String gcrp = JsonPath.resolveString(hit, "gene.gcrpCrossReference.referencedCurie");
		List<String> out = new ArrayList<>();
		for (JsonNode entry : arr) {
			String curie = entry.path("referencedCurie").asText("");
			if (curie.isEmpty()) {
				continue;
			}
			if (!gcrp.isEmpty() && curie.startsWith(UNIPROT_PREFIX) && curie.equals(gcrp)) {
				out.add(curie + " (GCRP)");
			} else {
				out.add(curie);
			}
		}
		return String.join("|", out);
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
