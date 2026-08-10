package org.alliancegenome.filegenerator.generators;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

	private static final String LINKML_README_URL = "https://alliance-genome.github.io/agr_curation_schema/DiseaseAnnotation/";

	// has_condition and induced_by establish the setup the disease phenotype was observed under; ameliorated_by and exacerbated_by qualify an already-established annotation. The two sets are reported in separate columns per Chris Grove's SCRUM-6274 spec, and any other relation type is reported in neither.
	private static final Set<String> EXPERIMENTAL_CONDITION_RELATIONS = Set.of("has_condition", "induced_by");

	private static final Set<String> CONDITION_MODIFIER_RELATIONS = Set.of("ameliorated_by", "exacerbated_by");

	// MGI, SGD and OMIM url templates already carry the prefix (e.g. ".../allele/MGI:[%s]") while their referencedCurie is prefixed too, so the template's copy is dropped before substitution to avoid doubling it. Mirrors DiseaseAnnotationToTdfTranslator, which builds the same Source URL column for the gene-page disease download.
	private static final Set<String> URL_PREFIX_IN_TEMPLATE = Set.of("MGI", "SGD", "OMIM");

	// Note types the download reports, each with the label the curators expect in the Notes column. Any other note type is omitted rather than emitted unlabelled.
	private static final Map<String, String> NOTE_TYPE_LABELS = Map.of("disease_note", "Note: ", "disease_summary", "Summary: ");

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
	protected String jsonReadmeOverride() {
		return LINKML_README_URL;
	}

	@Override
	protected String taxonPath() {
		return "subject.taxon.curie";
	}

	@Override
	protected String stringencyFilter() {
		return "stringent";
	}

	@Override
	protected List<String> extraHeaderLines() {
		return List.of("Orthology Filter: Stringent");
	}

	/**
	 * Row-format outputs route by the per-annotation taxon, not the consolidated doc's subject taxon. {@code _taxon} is populated inside customizeRows() from {@code primaryAnnotations[i].diseaseAnnotationSubject.taxon.curie}, so via-orthology fan-out rows land in the file matching their own subject's MOD instead of the parent doc's.
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
			String relationName = JsonPath.resolveString(pa, "relation.name");
			boolean isViaOrthology = relationName.contains("_via_orthology");

			// Annotation.uniqueId is the canonical dedup key computed by AnnotationUniqueIdHelper in curation. Skip empty values so the generator keeps working before the curation-side @JsonView change has been deployed and reindexed; once it lands, this becomes a real dedup.
			String uniqueId = JsonPath.resolveString(pa, "uniqueId");
			// A via_orthology annotation's uniqueId is identical across every ortholog-gene doc it fans out to (it is keyed on the source entity, not the ortholog gene), so include the enclosing doc's gene subject in the dedup key to keep one row per ortholog gene instead of collapsing them all into one.
			String dedupKey = isViaOrthology ? JsonPath.resolveString(customizedHit, "subject.primaryExternalId") + "|" + uniqueId : uniqueId;
			if (!uniqueId.isEmpty() && !seenUniqueIds.add(dedupKey)) {
				continue;
			}

			ObjectNode row = JsonNodeFactory.instance.objectNode();

			row.put("_uniqueId", uniqueId);

			// Exactly one of the three association columns is populated per row — the one naming the level the annotation was actually curated at. The other two levels are only ever reached by inference (inferred/asserted entities), so asserting a relation for them would invent an annotation that is not in the persistent store.
			String associationType = resolveAssociationType(pa);

			if (isViaOrthology) {
				// A via_orthology annotation lives inside the ortholog GENE's consolidated doc, but its diseaseAnnotationSubject still points at the source entity (allele/AGM/source gene). The row must be keyed on the enclosing doc's gene subject, so source every subject column from the top-level subject.
				// Per SCRUM-6274 these predicted annotations exist at the gene level only, so the model and allele columns stay blank.
				row.put("_taxon", JsonPath.resolveString(customizedHit, "subject.taxon.curie"));
				row.put("_speciesName", JsonPath.resolveString(customizedHit, "subject.taxon.species.fullName"));

				row.put("_modelId", "");
				row.put("_modelSymbol", "");
				row.put("_modelType", "");
				row.put("_modelAssociation", "");

				row.put("_alleleIds", "");
				row.put("_alleleSymbols", "");
				row.put("_alleleAssociation", "");

				row.put("_geneIds", JsonPath.resolveString(customizedHit, "subject.primaryExternalId"));
				row.put("_geneSymbols", JsonPath.resolveString(customizedHit, "subject.geneSymbol.displayText"));
				row.put("_geneAssociation", associationType);
			} else {
				row.put("_taxon", JsonPath.resolveString(pa, "diseaseAnnotationSubject.taxon.curie"));
				row.put("_speciesName", JsonPath.resolveString(pa, "diseaseAnnotationSubject.taxon.species.fullName"));

				// DiseaseAnnotation declares no generic subject field, so the Jackson type discriminator is the only way to tell whether diseaseAnnotationSubject is a gene, an allele or a model.
				String paType = JsonPath.resolveString(pa, "type");

				// Only an AGM annotation names a model at all — there is no inferred or asserted AGM to fall back on — so a gene or allele annotation leaves the model columns blank instead of repeating the subject under the wrong heading.
				boolean subjectIsModel = "AGMDiseaseAnnotation".equals(paType);
				row.put("_modelId", subjectIsModel ? JsonPath.resolveString(pa, "diseaseAnnotationSubject.primaryExternalId") : "");
				row.put("_modelSymbol", subjectIsModel ? resolveAgmSymbol(pa, "diseaseAnnotationSubject") : "");
				row.put("_modelType", subjectIsModel ? JsonPath.resolveString(pa, "diseaseAnnotationSubject.subtype.name") : "");
				row.put("_modelAssociation", subjectIsModel ? associationType : "");

				boolean subjectIsAllele = "AlleleDiseaseAnnotation".equals(paType);
				row.put("_alleleIds", resolveEntityField(pa, subjectIsAllele, "inferredAllele", "assertedAlleles", "primaryExternalId"));
				row.put("_alleleSymbols", resolveEntityField(pa, subjectIsAllele, "inferredAllele", "assertedAlleles", "alleleSymbol.displayText"));
				row.put("_alleleAssociation", subjectIsAllele ? associationType : "");

				boolean subjectIsGene = "GeneDiseaseAnnotation".equals(paType);
				row.put("_geneIds", resolveEntityField(pa, subjectIsGene, "inferredGene", "assertedGenes", "primaryExternalId"));
				row.put("_geneSymbols", resolveEntityField(pa, subjectIsGene, "inferredGene", "assertedGenes", "geneSymbol.displayText"));
				row.put("_geneAssociation", subjectIsGene ? associationType : "");
			}

			row.put("_diseaseQualifier", joinDiseaseQualifiers(pa));
			row.put("_doId", JsonPath.resolveString(pa, "diseaseAnnotationObject.curie"));
			row.put("_doTermName", JsonPath.resolveString(pa, "diseaseAnnotationObject.name"));

			row.put("_evidenceCode", joinEvidenceCodes(pa, "curie"));
			row.put("_evidenceCodeAbbreviation", joinEvidenceCodes(pa, "abbreviation"));
			row.put("_evidenceCodeName", joinEvidenceCodes(pa, "name"));

			row.put("_experimentalConditions", joinConditionSummaries(pa, EXPERIMENTAL_CONDITION_RELATIONS));
			row.put("_conditionModifiers", joinConditionSummaries(pa, CONDITION_MODIFIER_RELATIONS));

			row.put("_geneticModifierRelation", JsonPath.resolveString(pa, "diseaseGeneticModifierRelation.name"));
			row.put("_geneticModifierIds", joinGeneticModifiers(pa, false));
			row.put("_geneticModifierNames", joinGeneticModifiers(pa, true));

			// sgdStrainBackground is declared on GeneDiseaseAnnotation only, so allele and AGM rows leave these columns blank.
			row.put("_strainBackgroundId", JsonPath.resolveString(pa, "sgdStrainBackground.primaryExternalId"));
			row.put("_strainBackgroundName", resolveAgmSymbol(pa, "sgdStrainBackground"));

			row.put("_geneticSex", JsonPath.resolveString(pa, "geneticSex.name"));
			row.put("_notes", joinNotes(pa));
			row.put("_annotationType", JsonPath.resolveString(pa, "annotationType.name"));

			row.put("_basedOnId", joinBasedOnIds(pa));
			row.put("_basedOnSymbol", joinBasedOnSymbols(pa));

			// Per-annotation reference is a single evidenceItem (not a references[] array). Prefer the PMID-style referenceID over the AGRKB curie, matching how the parent-rooted code used to choose references[0].referenceID first.
			String reference = JsonPath.resolveString(pa, "evidenceItem.referenceID");
			if (reference.isEmpty()) {
				reference = JsonPath.resolveString(pa, "evidenceItem.curie");
			}
			row.put("_reference", reference);

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
			row.put("_sourceUrl", buildSourceUrl(pa));

			rows.add(row);
		}
		return rows;
	}

	private static String resolveAssociationType(JsonNode pa) {
		String relationName = JsonPath.resolveString(pa, "relation.name");
		if (relationName.isEmpty() || !pa.path("negated").asBoolean(false)) {
			return relationName;
		}
		if (relationName.equals("is_model_of")) {
			return "does_not_model";
		}
		return relationName.replaceFirst("_", "_not_");
	}

	/**
	 * Resolves one field of the allele or gene column pair. The annotation's own subject wins when the annotation is of that entity's type — a GeneDiseaseAnnotation names its gene directly — then the curated inferred entity, then the asserted entities pipe-joined.
	 * Which of the three sources wins is decided on primaryExternalId alone, so the IDs and the symbols always describe the same entities in the same order.
	 */
	private static String resolveEntityField(JsonNode pa, boolean subjectIsEntity, String inferredPath, String assertedPath, String field) {
		if (subjectIsEntity) {
			return JsonPath.resolveString(pa, "diseaseAnnotationSubject." + field);
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

	/** agmFullName is an optional slot, so fall back to the model's name before giving up and leaving the cell blank. */
	private static String resolveAgmSymbol(JsonNode pa, String agmPath) {
		String symbol = JsonPath.resolveString(pa, agmPath + ".agmFullName.displayText");
		if (symbol.isEmpty()) {
			symbol = JsonPath.resolveString(pa, agmPath + ".name");
		}
		return symbol;
	}

	private static String joinDiseaseQualifiers(JsonNode pa) {
		JsonNode qualifiers = pa.path("diseaseQualifiers");
		if (!qualifiers.isArray()) {
			return "";
		}
		LinkedHashSet<String> names = new LinkedHashSet<>();
		for (JsonNode qualifier : qualifiers) {
			String name = qualifier.path("name").asText("");
			if (!name.isEmpty()) {
				names.add(name.replace("_", " "));
			}
		}
		return String.join("|", names);
	}

	private static String joinEvidenceCodes(JsonNode pa, String field) {
		JsonNode evidenceCodes = pa.path("evidenceCodes");
		if (!evidenceCodes.isArray()) {
			return "";
		}
		List<String> values = new ArrayList<>();
		for (JsonNode ec : evidenceCodes) {
			String value = ec.path(field).asText("");
			if (!value.isEmpty()) {
				values.add(value);
			}
		}
		return String.join("|", values);
	}

	private static String joinConditionSummaries(JsonNode pa, Set<String> relationTypes) {
		JsonNode relations = pa.path("conditionRelations");
		if (!relations.isArray()) {
			return "";
		}
		LinkedHashSet<String> summaries = new LinkedHashSet<>();
		for (JsonNode relation : relations) {
			if (!relationTypes.contains(relation.path("conditionRelationType").path("name").asText(""))) {
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

	/**
	 * Genetic modifiers are split across three typed arrays in the annotation. They are walked alleles-then-genes-then-AGMs so the IDs and the names line up position for position, matching the order DiseaseAnnotationToTdfTranslator uses for the same two columns on the gene page download.
	 */
	private static String joinGeneticModifiers(JsonNode pa, boolean wantName) {
		List<String> values = new ArrayList<>();
		for (String arrayPath : List.of("diseaseGeneticModifierAlleles", "diseaseGeneticModifierGenes", "diseaseGeneticModifierAgms")) {
			JsonNode modifiers = pa.path(arrayPath);
			if (!modifiers.isArray()) {
				continue;
			}
			for (JsonNode modifier : modifiers) {
				String value = wantName ? resolveEntityName(modifier) : modifier.path("primaryExternalId").asText("");
				if (!value.isEmpty()) {
					values.add(value);
				}
			}
		}
		return String.join("|", values);
	}

	/** Names a modifier entity by whichever symbol slot its type carries — the modifier arrays hold alleles, genes and models side by side. */
	private static String resolveEntityName(JsonNode entity) {
		String name = JsonPath.resolveString(entity, "alleleSymbol.displayText");
		if (name.isEmpty()) {
			name = JsonPath.resolveString(entity, "geneSymbol.displayText");
		}
		if (name.isEmpty()) {
			name = JsonPath.resolveString(entity, "agmFullName.displayText");
		}
		if (name.isEmpty()) {
			name = JsonPath.resolveString(entity, "name");
		}
		return name;
	}

	private static String joinNotes(JsonNode pa) {
		JsonNode notes = pa.path("relatedNotes");
		if (!notes.isArray()) {
			return "";
		}
		LinkedHashSet<String> rendered = new LinkedHashSet<>();
		for (JsonNode note : notes) {
			String label = NOTE_TYPE_LABELS.get(note.path("noteType").path("name").asText(""));
			String freeText = note.path("freeText").asText("");
			if (label == null || freeText.isEmpty()) {
				continue;
			}
			rendered.add(label + freeText);
		}
		return String.join("|", rendered);
	}

	/**
	 * The source MOD's own page for this annotation, built from the annotation's data-provider cross reference. Blank when the annotation carries no cross reference (every via_orthology annotation, whose provider is the Alliance itself).
	 */
	private static String buildSourceUrl(JsonNode pa) {
		String curie = JsonPath.resolveString(pa, "dataProviderCrossReference.referencedCurie");
		String urlTemplate = JsonPath.resolveString(pa, "dataProviderCrossReference.resourceDescriptorPage.urlTemplate");
		if (curie.isEmpty() || urlTemplate.isEmpty()) {
			return "";
		}
		String provider = JsonPath.resolveString(pa, "dataProvider.abbreviation");
		if (URL_PREFIX_IN_TEMPLATE.contains(provider)) {
			urlTemplate = urlTemplate.replace(provider + ":", "");
		}
		return urlTemplate.replace("[%s]", curie);
	}

	private static String joinBasedOnIds(JsonNode pa) {
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

	private static String joinBasedOnSymbols(JsonNode pa) {
		JsonNode with = pa.path("with");
		if (!with.isArray()) {
			return "";
		}
		LinkedHashSet<String> symbols = new LinkedHashSet<>();
		for (JsonNode w : with) {
			String symbol = w.path("geneSymbol").path("displayText").asText("");
			if (symbol.isEmpty()) {
				continue;
			}
			String abbreviation = w.path("taxon").path("species").path("abbreviation").asText("");
			if (abbreviation.isEmpty()) {
				symbols.add(symbol);
			} else {
				symbols.add(symbol + " (" + abbreviation + ")");
			}
		}
		List<String> sorted = new ArrayList<>(symbols);
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
