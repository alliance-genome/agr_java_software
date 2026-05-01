package org.alliancegenome.filegenerator.generators;

import java.util.LinkedHashSet;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.writers.JsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiseaseFileGenerator extends FileGenerator {

	public DiseaseFileGenerator(FileGeneratorConfig config) {
		super(config);
	}

	@Override
	protected void generate() throws Exception {
		scrollAndWrite();
	}

	@Override
	protected String taxonPath() {
		return "subject.taxon.curie";
	}

	@Override
	protected JsonNode customizeRow(JsonNode hit) {
		if (!hit.isObject()) return hit;
		ObjectNode obj = (ObjectNode) hit;

		// DBobjectType: derive from doc category. gene_disease_annotation -> "gene",
		// allele_disease_annotation -> "allele", agm_disease_annotation -> "affected_genomic_model"
		// (FMS spelling).
		String category = JsonPath.resolveString(hit, "category");
		String dbObjectType;
		if ("gene_disease_annotation".equals(category)) {
			dbObjectType = "gene";
		} else if ("allele_disease_annotation".equals(category)) {
			dbObjectType = "allele";
		} else if ("agm_disease_annotation".equals(category)) {
			dbObjectType = "affected_genomic_model";
		} else {
			dbObjectType = category.replace("_disease_annotation", "");
		}
		obj.put("_dbObjectType", dbObjectType);

		// DBObjectSymbol: differs by subject type — gene/allele/agm carry distinct symbol fields.
		String type = JsonPath.resolveString(hit, "subject.type");
		String symbol = "";
		if ("Gene".equals(type)) {
			symbol = JsonPath.resolveString(hit, "subject.geneSymbol.displayText");
		} else if ("Allele".equals(type)) {
			symbol = JsonPath.resolveString(hit, "subject.alleleSymbol.displayText");
		} else if ("AffectedGenomicModel".equals(type)) {
			symbol = JsonPath.resolveString(hit, "subject.agmFullName.displayText");
			if (symbol.isEmpty()) symbol = JsonPath.resolveString(hit, "subject.name");
		} else {
			symbol = JsonPath.resolveString(hit, "subject.name");
		}
		obj.put("_dbObjectSymbol", symbol);

		// EvidenceCode + Name: first entry in top-level evidenceCodes[]; fallback to primaryAnnotations[0].evidenceCodes[0].
		JsonNode evCodes = JsonPath.resolve(hit, "evidenceCodes");
		if (evCodes == null || !evCodes.isArray() || evCodes.size() == 0) {
			evCodes = JsonPath.resolve(hit, "primaryAnnotations.0.evidenceCodes");
		}
		if (evCodes != null && evCodes.isArray() && evCodes.size() > 0) {
			obj.put("_evidenceCode", evCodes.get(0).path("curie").asText(""));
			obj.put("_evidenceCodeName", evCodes.get(0).path("name").asText(""));
		} else {
			obj.put("_evidenceCode", "");
			obj.put("_evidenceCodeName", "");
		}

		// Reference: prefer references[0].referenceID (the PMID) over references[0].curie (AGRKB).
		JsonNode refs = JsonPath.resolve(hit, "references");
		if (refs != null && refs.isArray() && refs.size() > 0) {
			JsonNode r0 = refs.get(0);
			String ref = r0.path("referenceID").asText("");
			if (ref.isEmpty()) ref = r0.path("curie").asText("");
			obj.put("_reference", ref);
		} else {
			obj.put("_reference", "");
		}

		// Date: format YYYYMMDD from the most relevant ISO timestamp.
		String iso = JsonPath.resolveString(hit, "primaryAnnotations.0.dateUpdated");
		if (iso.isEmpty()) iso = JsonPath.resolveString(hit, "primaryAnnotations.0.dateCreated");
		if (iso.isEmpty()) iso = JsonPath.resolveString(hit, "subject.dateUpdated");
		obj.put("_date", isoToYyyyMmDd(iso));

		// Source: MOD code from the subject's species displayName (e.g. "WB", "MGI").
		obj.put("_source", JsonPath.resolveString(hit, "subject.taxon.species.displayName"));

		// WithOrtholog: pipe-delimited list of primaryAnnotations[*].with[*].primaryExternalId.
		// Populated for gene-level annotations inferred via orthology.
		obj.put("_withOrtholog", joinWithOrthologs(hit));

		return hit;
	}

	private static String joinWithOrthologs(JsonNode hit) {
		JsonNode primary = JsonPath.resolve(hit, "primaryAnnotations");
		if (primary == null || !primary.isArray()) return "";
		LinkedHashSet<String> ids = new LinkedHashSet<>();
		for (JsonNode pa : primary) {
			JsonNode with = pa.path("with");
			if (with.isArray()) {
				for (JsonNode w : with) {
					String id = w.path("primaryExternalId").asText("");
					if (!id.isEmpty()) ids.add(id);
				}
			}
		}
		return String.join("|", ids);
	}

	private static String isoToYyyyMmDd(String iso) {
		if (iso == null || iso.length() < 10) return "";
		// "2024-06-27T..." -> "20240627"
		return iso.substring(0, 10).replace("-", "");
	}
}
