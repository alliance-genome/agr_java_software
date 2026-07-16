package org.alliancegenome.filegenerator.generators;

import java.util.ArrayList;
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

	// Same primaryAnnotation appears across many consolidated docs. Dedup by the canonical Annotation.uniqueId so each annotation is emitted once per run. dispatch() runs from the parallel scroll pool, so this must be a concurrent set.
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

	/**
	 * Row-format outputs route by the per-annotation taxon, not the consolidated doc's subject taxon. {@code _taxon} is populated inside customizeRows() from {@code primaryAnnotations[i].phenotypeAnnotationSubject.taxon.curie}, so via-orthology fan-out rows land in the file matching their own subject's MOD instead of the parent doc's.
	 */
	@Override
	protected String rowTaxonPath() {
		return "_taxon";
	}

	@Override
	protected JsonNode customizeRow(JsonNode hit) {
		return hit;
	}

	/**
	 * The gene_phenotype_annotation ES docs are consolidated — each hit carries a primaryAnnotations[] array containing the individual phenotype annotations. JSON_RAW writes the consolidated doc verbatim (via customizeRow); TSV writes one flattened row per primaryAnnotations[i].
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

			// Per-row taxon — drives row-level routing (see rowTaxonPath()). Pulled from the individual primaryAnnotations[i] entry so via-orthology fan-out rows land in the right per-MOD file.
			row.put("_taxon", JsonPath.resolveString(pa, "phenotypeAnnotationSubject.taxon.curie"));

			String phenotype = JsonPath.resolveString(pa, "phenotypeTerms.0.name");
			if (phenotype.isEmpty()) {
				phenotype = JsonPath.resolveString(pa, "phenotypeAnnotationObject");
			}
			row.put("_phenotype", phenotype);

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

			// Reserved — the consolidated ES doc does not currently carry experimental conditions for phenotype annotations. Same empty-cell pattern Disease uses for unsupported columns.
			row.put("_experimentalCondition", "");

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
}
