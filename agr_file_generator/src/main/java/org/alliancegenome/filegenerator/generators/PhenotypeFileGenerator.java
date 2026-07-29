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

	// Only has_condition and induced_by describe the setup the phenotype was observed under. The remaining relation types (ameliorated_by, exacerbated_by, ...) qualify the phenotype rather than establishing it, so their conditions are not reported as the experimental condition.
	private static final Set<String> EXPERIMENTAL_CONDITION_RELATIONS = Set.of("has_condition", "induced_by");

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

			// PhenotypeAnnotation declares no generic subject field, so the Jackson type discriminator is the only way to tell whether phenotypeAnnotationSubject is a gene, an allele or a model.
			String paType = JsonPath.resolveString(pa, "type");

			// Only an AGM annotation names a model at all — a gene or allele annotation has none, so its model columns stay blank instead of repeating the subject under the wrong heading.
			boolean subjectIsModel = "AGMPhenotypeAnnotation".equals(paType);
			row.put("_modelId", subjectIsModel ? JsonPath.resolveString(pa, "phenotypeAnnotationSubject.primaryExternalId") : "");
			row.put("_modelSymbol", subjectIsModel ? JsonPath.resolveString(pa, "phenotypeAnnotationSubject.agmFullName.displayText") : "");
			row.put("_modelType", subjectIsModel ? JsonPath.resolveString(pa, "phenotypeAnnotationSubject.subtype.name") : "");

			boolean subjectIsAllele = "AllelePhenotypeAnnotation".equals(paType);
			row.put("_alleleId", resolveEntityField(pa, subjectIsAllele, "inferredAllele", "assertedAlleles", "primaryExternalId"));
			row.put("_alleleSymbol", resolveEntityField(pa, subjectIsAllele, "inferredAllele", "assertedAlleles", "alleleSymbol.displayText"));

			boolean subjectIsGene = "GenePhenotypeAnnotation".equals(paType);
			row.put("_geneId", resolveEntityField(pa, subjectIsGene, "inferredGene", "assertedGenes", "primaryExternalId"));
			row.put("_geneSymbol", resolveEntityField(pa, subjectIsGene, "inferredGene", "assertedGenes", "geneSymbol.displayText"));

			// Experimental conditions live at conditionRelations[].conditions[].conditionSummary — both are arrays, so flatten and bar-separate the human-readable summaries. Empty when the annotation carries no conditions under an experimental relation.
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

	/**
	 * Resolves one field of the allele or gene column pair. The annotation's own subject wins when the annotation is of that entity's type — a GenePhenotypeAnnotation names its gene directly — then the curated inferred entity, then the asserted entities pipe-joined. Which of the three sources wins is decided on primaryExternalId alone, so the ID and the symbol always describe the same entity.
	 */
	private static String resolveEntityField(JsonNode pa, boolean subjectIsEntity, String inferredPath, String assertedPath, String field) {
		if (subjectIsEntity) {
			return JsonPath.resolveString(pa, "phenotypeAnnotationSubject." + field);
		}
		if (!JsonPath.resolveString(pa, inferredPath + ".primaryExternalId").isEmpty()) {
			return JsonPath.resolveString(pa, inferredPath + "." + field);
		}
		JsonNode asserted = pa.path(assertedPath);
		if (!asserted.isArray()) {
			return "";
		}
		LinkedHashSet<String> values = new LinkedHashSet<>();
		for (JsonNode entity : asserted) {
			String value = JsonPath.resolveString(entity, field);
			if (!value.isEmpty()) {
				values.add(value);
			}
		}
		return String.join("|", values);
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
			if (!EXPERIMENTAL_CONDITION_RELATIONS.contains(relation.path("conditionRelationType").path("name").asText(""))) {
				continue;
			}
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
