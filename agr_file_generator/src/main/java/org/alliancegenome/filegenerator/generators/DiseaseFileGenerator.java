package org.alliancegenome.filegenerator.generators;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;
import org.alliancegenome.filegenerator.writers.JsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiseaseFileGenerator extends FileGenerator {

	private static final String FILE_GENERATION_DATE =
			LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD

	// Same primaryAnnotation appears across many consolidated docs (gene/allele/agm rollups + via_orthology fan-out). Dedup by the canonical Annotation.uniqueId so each annotation is emitted once per run. dispatch() runs from the parallel scroll pool, so this must be a concurrent set.
	private final Set<String> seenUniqueIds = ConcurrentHashMap.newKeySet();

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
		return hit;
	}

	/**
	 * The {gene,allele,agm}_disease_annotation ES docs are consolidated — each hit carries a primaryAnnotations[] array containing the individual disease annotations. JSON_RAW writes the consolidated doc verbatim (via customizeRow); TSV writes one flattened row per primaryAnnotations[i]. Every value is sourced from inside the per-annotation element; nothing is read from the parent doc.
	 */
	@Override
	protected List<JsonNode> customizeRows(JsonNode customizedHit) {
		JsonNode primary = JsonPath.resolve(customizedHit, "primaryAnnotations");
		if (primary == null || !primary.isArray() || primary.size() == 0) {
			return List.of();
		}
		List<JsonNode> rows = new ArrayList<>(primary.size());
		for (JsonNode pa : primary) {
			// Annotation.uniqueId is the canonical dedup key computed by AnnotationUniqueIdHelper in curation. Skip empty values so the generator keeps working before the curation-side @JsonView change has been deployed and reindexed; once it lands, this becomes a real dedup.
			String uniqueId = JsonPath.resolveString(pa, "uniqueId");
			if (!uniqueId.isEmpty() && !seenUniqueIds.add(uniqueId)) {
				continue;
			}

			ObjectNode row = JsonNodeFactory.instance.objectNode();

			row.put("_taxon", JsonPath.resolveString(pa, "diseaseAnnotationSubject.taxon.curie"));
			row.put("_speciesName", JsonPath.resolveString(pa, "diseaseAnnotationSubject.taxon.species.fullName"));

			String paType = JsonPath.resolveString(pa, "type");
			String dbObjectType;
			if ("GeneDiseaseAnnotation".equals(paType)) {
				dbObjectType = "gene";
			} else if ("AlleleDiseaseAnnotation".equals(paType)) {
				dbObjectType = "allele";
			} else if ("AGMDiseaseAnnotation".equals(paType)) {
				dbObjectType = "affected_genomic_model";
			} else {
				dbObjectType = paType.replace("DiseaseAnnotation", "").toLowerCase();
			}
			row.put("_dbObjectType", dbObjectType);

			row.put("_dbObjectId", JsonPath.resolveString(pa, "diseaseAnnotationSubject.primaryExternalId"));

			String subjectType = JsonPath.resolveString(pa, "diseaseAnnotationSubject.type");
			String symbol;
			if ("Gene".equals(subjectType)) {
				symbol = JsonPath.resolveString(pa, "diseaseAnnotationSubject.geneSymbol.displayText");
			} else if ("Allele".equals(subjectType)) {
				symbol = JsonPath.resolveString(pa, "diseaseAnnotationSubject.alleleSymbol.displayText");
			} else if ("AffectedGenomicModel".equals(subjectType)) {
				symbol = JsonPath.resolveString(pa, "diseaseAnnotationSubject.agmFullName.displayText");
				if (symbol.isEmpty()) {
					symbol = JsonPath.resolveString(pa, "diseaseAnnotationSubject.name");
				}
			} else {
				symbol = JsonPath.resolveString(pa, "diseaseAnnotationSubject.name");
			}
			row.put("_dbObjectSymbol", symbol);

			row.put("_associationType", JsonPath.resolveString(pa, "relation.name"));
			row.put("_doId", JsonPath.resolveString(pa, "diseaseAnnotationObject.curie"));
			row.put("_doTermName", JsonPath.resolveString(pa, "diseaseAnnotationObject.name"));

			row.put("_withOrtholog", joinWithOrthologs(pa));

			JsonNode evCodes = pa.path("evidenceCodes");
			List<String> curies = new ArrayList<>();
			List<String> names = new ArrayList<>();
			if (evCodes.isArray()) {
				for (JsonNode ec : evCodes) {
					String c = ec.path("curie").asText("");
					String n = ec.path("name").asText("");
					if (!c.isEmpty()) {
						curies.add(c);
					}
					if (!n.isEmpty()) {
						names.add(n);
					}
				}
			}
			row.put("_evidenceCode", String.join("|", curies));
			row.put("_evidenceCodeName", String.join("|", names));

			// Per-annotation reference is a single evidenceItem (not a references[] array). Prefer the PMID-style referenceID over the AGRKB curie, matching how the parent-rooted code used to choose references[0].referenceID first.
			String reference = JsonPath.resolveString(pa, "evidenceItem.referenceID");
			if (reference.isEmpty()) {
				reference = JsonPath.resolveString(pa, "evidenceItem.curie");
			}
			row.put("_reference", reference);

			String relationName = JsonPath.resolveString(pa, "relation.name");
			boolean isViaOrthology = relationName.contains("_via_orthology");

			// The per-annotation element does not carry dateUpdated; only dateCreated is present. Fall back to file-generation-date for via_orthology rows when the date is missing, preserving existing behavior.
			String iso = JsonPath.resolveString(pa, "dateUpdated");
			if (iso.isEmpty()) {
				iso = JsonPath.resolveString(pa, "dateCreated");
			}
			String date = isoToYyyyMmDd(iso);
			if (date.isEmpty() && isViaOrthology) {
				date = FILE_GENERATION_DATE;
			}
			row.put("_date", date);

			if (isViaOrthology) {
				String provider = JsonPath.resolveString(pa, "dataProvider.abbreviation");
				row.put("_source", provider.isEmpty()
						? JsonPath.resolveString(pa, "diseaseAnnotationSubject.taxon.species.displayName")
						: provider);
			} else {
				row.put("_source", JsonPath.resolveString(pa, "diseaseAnnotationSubject.taxon.species.displayName"));
			}

			rows.add(row);
		}
		return rows;
	}

	private static String joinWithOrthologs(JsonNode pa) {
		JsonNode with = pa.path("with");
		if (!with.isArray()) {
			return "";
		}
		LinkedHashSet<String> ids = new LinkedHashSet<>();
		for (JsonNode w : with) {
			String id = w.path("primaryExternalId").asText("");
			if (!id.isEmpty()) {
				ids.add(id);
			}
		}
		List<String> sorted = new ArrayList<>(ids);
		Collections.sort(sorted);
		return String.join("|", sorted);
	}

	private static String isoToYyyyMmDd(String iso) {
		if (iso == null || iso.length() < 10) {
			return "";
		}
		// "2024-06-27T..." -> "20240627"
		return iso.substring(0, 10).replace("-", "");
	}
}
