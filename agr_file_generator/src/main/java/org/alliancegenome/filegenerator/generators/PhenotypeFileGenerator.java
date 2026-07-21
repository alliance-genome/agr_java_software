package org.alliancegenome.filegenerator.generators;

import java.util.ArrayList;
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
public class PhenotypeFileGenerator extends FileGenerator {

	private static final String LINKML_README_URL = "https://alliance-genome.github.io/agr_curation_schema/PhenotypeAnnotation/";

	private final Set<String> seenUniqueIds = ConcurrentHashMap.newKeySet();

	public PhenotypeFileGenerator(FileGeneratorConfig config) {
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
		return "subject.taxon.curie";
	}

	@Override
	protected String rowTaxonPath() {
		return "_taxon";
	}

	@Override
	protected JsonNode customizeRow(JsonNode hit) {
		return hit;
	}

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

			// Per-row taxon — drives row-level routing (see rowTaxonPath()). Pulled from the individual primaryAnnotations[i] entry so via-orthology fan-out rows land in the right per-MOD file.
			String taxonCurie = JsonPath.resolveString(pa, "phenotypeAnnotationSubject.taxon.curie");
			row.put("_taxon", taxonCurie);

			String speciesName = JsonPath.resolveString(pa, "phenotypeAnnotationSubject.taxon.species.fullName");
			if (speciesName.isEmpty() && species != null) {
				String looked = species.fullNameFor(taxonCurie);
				if (looked != null) {
					speciesName = looked;
				}
			}
			row.put("_speciesName", speciesName);

			// The phenotype statement is the full curated phrase (e.g. "decreased mating efficiency"); phenotypeTerms are its individual ontology components (e.g. "decreased", "mating efficiency"). Emit the statement and the terms in separate columns.
			row.put("_phenotypeStatement", JsonPath.resolveString(pa, "phenotypeAnnotationObject"));
			row.put("_phenotypeTerms", joinPhenotypeTerms(pa));

			row.put("_geneticEntityId", JsonPath.resolveString(pa, "phenotypeAnnotationSubject.primaryExternalId"));

			String subjectType = JsonPath.resolveString(pa, "phenotypeAnnotationSubject.type");
			String entityName;
			if ("Gene".equals(subjectType)) {
				entityName = JsonPath.resolveString(pa, "phenotypeAnnotationSubject.geneSymbol.displayText");
			} else if ("Allele".equals(subjectType)) {
				entityName = JsonPath.resolveString(pa, "phenotypeAnnotationSubject.alleleSymbol.displayText");
			} else if ("AffectedGenomicModel".equals(subjectType)) {
				entityName = JsonPath.resolveString(pa, "phenotypeAnnotationSubject.agmFullName.displayText");
				if (entityName.isEmpty()) {
					entityName = JsonPath.resolveString(pa, "phenotypeAnnotationSubject.name");
				}
			} else {
				entityName = JsonPath.resolveString(pa, "phenotypeAnnotationSubject.name");
			}
			row.put("_geneticEntityName", entityName);

			String paType = JsonPath.resolveString(pa, "type");
			String entityType;
			if ("GenePhenotypeAnnotation".equals(paType)) {
				entityType = "gene";
			} else if ("AllelePhenotypeAnnotation".equals(paType)) {
				entityType = "allele";
			} else if ("AGMPhenotypeAnnotation".equals(paType)) {
				entityType = "affected_genomic_model";
			} else {
				entityType = paType.replace("PhenotypeAnnotation", "").toLowerCase();
			}
			row.put("_geneticEntityType", entityType);

			// Experimental conditions live at conditionRelations[].conditions[].conditionSummary — both are arrays, so flatten and bar-separate the human-readable summaries. Empty when the annotation carries no conditions.
			row.put("_experimentalCondition", joinConditionSummaries(pa));

			row.put("_source", JsonPath.resolveString(pa, "dataProvider.abbreviation"));

			String reference = JsonPath.resolveString(pa, "evidenceItem.referenceID");
			if (reference.isEmpty()) {
				reference = JsonPath.resolveString(pa, "evidenceItem.curie");
			}
			row.put("_reference", reference);

			rows.add(row);
		}
		return rows;
	}

	private static String joinPhenotypeTerms(JsonNode pa) {
		JsonNode terms = pa.path("phenotypeTerms");
		if (!terms.isArray()) {
			return "";
		}
		LinkedHashSet<String> rendered = new LinkedHashSet<>();
		for (JsonNode term : terms) {
			String name = term.path("name").asText("");
			String curie = term.path("curie").asText("");
			if (name.isEmpty() && curie.isEmpty()) {
				continue;
			}
			if (curie.isEmpty()) {
				rendered.add(name);
			} else {
				rendered.add(name + " (" + curie + ")");
			}
		}
		return String.join("|", rendered);
	}

	private static String joinConditionSummaries(JsonNode pa) {
		JsonNode relations = pa.path("conditionRelations");
		if (!relations.isArray()) {
			return "";
		}
		LinkedHashSet<String> summaries = new LinkedHashSet<>();
		for (JsonNode relation : relations) {
			JsonNode conditions = relation.path("conditions");
			if (!conditions.isArray()) {
				continue;
			}
			for (JsonNode condition : conditions) {
				String summary = condition.path("conditionSummary").asText("");
				if (!summary.isEmpty()) {
					summaries.add(summary);
				}
			}
		}
		return String.join("|", summaries);
	}
}
