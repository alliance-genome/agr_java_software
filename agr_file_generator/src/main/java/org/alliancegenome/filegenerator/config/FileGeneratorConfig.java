package org.alliancegenome.filegenerator.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.filegenerator.generators.DiseaseFileGenerator;
import org.alliancegenome.filegenerator.generators.ExpressionFileGenerator;
import org.alliancegenome.filegenerator.generators.FileGenerator;
import org.alliancegenome.filegenerator.generators.GeneDescriptionFileGenerator;
import org.alliancegenome.filegenerator.generators.GeneticInteractionFileGenerator;
import org.alliancegenome.filegenerator.generators.MolecularInteractionFileGenerator;
import org.alliancegenome.filegenerator.generators.OrthologyFileGenerator;
import org.alliancegenome.filegenerator.generators.VariantAlleleFileGenerator;
import org.alliancegenome.filegenerator.generators.VariantVcfFileGenerator;

public enum FileGeneratorConfig {

	GeneDescriptions(
			"Gene Descriptions",
			"GENE-DESCRIPTION",
			List.of("gene_search_result"),
			List.of(
					new OutputSpec(Format.TSV, SplitMode.TAXON),
					new OutputSpec(Format.TXT, SplitMode.TAXON),
					new OutputSpec(Format.JSON_MAPPED, SplitMode.TAXON)
			),
			List.of("FB", "HUMAN", "MGI", "RGD", "SGD", "WB", "XBXL", "XBXT", "ZFIN"),
			geneDescriptionFieldMap(),
			"readmes/gene_descriptions.txt",
			GeneDescriptionFileGenerator.class,
			1000,
			8
	),

	Disease(
			"Disease",
			"DISEASE-ALLIANCE",
			List.of("gene_disease_annotation", "allele_disease_annotation", "agm_disease_annotation"),
			List.of(
					new OutputSpec(Format.TSV, SplitMode.TAXON),
					new OutputSpec(Format.TSV, SplitMode.COMBINED),
					new OutputSpec(Format.JSON_RAW, SplitMode.TAXON),
					new OutputSpec(Format.JSON_RAW, SplitMode.COMBINED)
			),
			List.of("FB", "HUMAN", "MGI", "RGD", "SGD", "WB", "XBXL", "XBXT", "ZFIN"),
			diseaseFieldMap(),
			"readmes/disease.txt",
			DiseaseFileGenerator.class,
			1000,
			8
	),

	Expression(
			"Expression",
			"EXPRESSION-ALLIANCE",
			List.of("gene_expression_annotation"),
			List.of(
					new OutputSpec(Format.TSV, SplitMode.TAXON),
					new OutputSpec(Format.TSV, SplitMode.COMBINED),
					new OutputSpec(Format.JSON_RAW, SplitMode.TAXON),
					new OutputSpec(Format.JSON_RAW, SplitMode.COMBINED)
			),
			List.of("FB", "MGI", "RGD", "SGD", "WB", "XBXL", "XBXT", "ZFIN"),
			expressionFieldMap(),
			"readmes/expression.txt",
			ExpressionFileGenerator.class,
			1000,
			8
	),

	Orthology(
			"Orthology",
			"ORTHOLOGY-ALLIANCE",
			List.of("gene_to_gene_orthology"),
			List.of(
					new OutputSpec(Format.TSV, SplitMode.COMBINED),
					new OutputSpec(Format.JSON_RAW, SplitMode.COMBINED)
			),
			List.of("FB", "HUMAN", "MGI", "RGD", "SGD", "WB", "XBXL", "XBXT", "ZFIN"),
			orthologyFieldMap(),
			"readmes/orthology.txt",
			OrthologyFileGenerator.class,
			1000,
			8
	),

	MolecularInteractions(
			"Molecular Interactions",
			"INTERACTION-MOL",
			List.of("gene_molecular_interaction"),
			List.of(
					new OutputSpec(Format.PSI_MI_TAB, SplitMode.TAXON),
					new OutputSpec(Format.PSI_MI_TAB, SplitMode.COMBINED)
			),
			List.of("FB", "HUMAN", "MGI", "RGD", "SARS-CoV-2", "SGD", "WB", "XBXL", "XBXT", "ZFIN"),
			interactionFieldMap(),
			"readmes/molecular_interactions.txt",
			MolecularInteractionFileGenerator.class,
			1000,
			8
	),

	GeneticInteractions(
			"Genetic Interactions",
			"INTERACTION-GEN",
			List.of("gene_genetic_interaction"),
			List.of(
					new OutputSpec(Format.PSI_MI_TAB, SplitMode.TAXON),
					new OutputSpec(Format.PSI_MI_TAB, SplitMode.COMBINED)
			),
			List.of("FB", "HUMAN", "MGI", "RGD", "SGD", "WB", "XBXL", "ZFIN"),
			interactionFieldMap(),
			"readmes/genetic_interactions.txt",
			GeneticInteractionFileGenerator.class,
			1000,
			8
	),

	VariantsVcf(
			"Variants",
			"VARIANT-CONSEQUENCE",
			List.of("variant_summary"),
			List.of(
					new OutputSpec(Format.VCF, SplitMode.TAXON)
			),
			// Filename split is by MOD (consistent with every other generator). The genome assembly
			// goes inside the file content (## headers / per-record fields), not in the filename.
			List.of("FB", "MGI", "RGD", "WB", "ZFIN"),
			vcfFieldMap(),
			"readmes/variants_vcf.txt",
			VariantVcfFileGenerator.class,
			1000,
			8
	),

	VariantsAlleles(
			"Variant/Allele",
			"VARIANT-ALLELE",
			List.of("allele_summary"),
			List.of(
					new OutputSpec(Format.TSV, SplitMode.TAXON),
					new OutputSpec(Format.JSON_RAW, SplitMode.TAXON)
			),
			List.of("FB", "MGI", "RGD", "SGD", "WB", "ZFIN"),
			variantAlleleFieldMap(),
			"readmes/variants_alleles.txt",
			VariantAlleleFileGenerator.class,
			1000,
			8
	),
	;

	private final String filetypeLabel;
	private final String type;
	private final List<String> esCategories;
	private final List<OutputSpec> outputs;
	private final List<String> mods;
	private final Map<String, String> fieldMap;
	private final String readmeResourcePath;
	private final Class<? extends FileGenerator> generatorClazz;
	private final int bufferSize;
	private final int threadCount;

	FileGeneratorConfig(String filetypeLabel,
			String type,
			List<String> esCategories,
			List<OutputSpec> outputs,
			List<String> mods,
			Map<String, String> fieldMap,
			String readmeResourcePath,
			Class<? extends FileGenerator> generatorClazz,
			int bufferSize,
			int threadCount) {
		this.filetypeLabel = filetypeLabel;
		this.type = type;
		this.esCategories = esCategories;
		this.outputs = outputs;
		this.mods = mods;
		this.fieldMap = fieldMap;
		this.readmeResourcePath = readmeResourcePath;
		this.generatorClazz = generatorClazz;
		this.bufferSize = bufferSize;
		this.threadCount = threadCount;
	}

	public String getFiletypeLabel() {
		return filetypeLabel;
	}

	public String getType() {
		return type;
	}

	public List<String> getEsCategories() {
		return esCategories;
	}

	public List<OutputSpec> getOutputs() {
		return outputs;
	}

	public List<String> getMods() {
		return mods;
	}

	public Map<String, String> getFieldMap() {
		return fieldMap;
	}

	public String getReadmeResourcePath() {
		return readmeResourcePath;
	}

	public Class<? extends FileGenerator> getGeneratorClazz() {
		return generatorClazz;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public int getThreadCount() {
		return threadCount;
	}

	/**
	 * PSI-MI TAB 2.7 field map shared by Molecular and Genetic interactions. The 42 columns are
	 * the standard PSI-MITAB layout. Most cells are formatted PSI-MI strings (e.g.
	 * `psi-mi:"MI:XXXX"(name)`, `taxid:N(species)`, `pubmed:N`) — those are built in
	 * {@code BaseInteractionFileGenerator.customizeRow()} and exposed via synthetic `_*` fields.
	 * Columns we don't (yet) populate from ES use "_unavailable" and render as `-` per spec.
	 */
	private static Map<String, String> interactionFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("ID(s) interactor A", "_idA");
		m.put("ID(s) interactor B", "_idB");
		m.put("Alt. ID(s) interactor A", "_unavailable");
		m.put("Alt. ID(s) interactor B", "_unavailable");
		m.put("Alias(es) interactor A", "_aliasA");
		m.put("Alias(es) interactor B", "_aliasB");
		m.put("Interaction detection method(s)", "_detectionMethod");
		m.put("Publication 1st author(s)", "_author");
		m.put("Publication Identifier(s)", "_pubmed");
		m.put("Taxid interactor A", "_taxidA");
		m.put("Taxid interactor B", "_taxidB");
		m.put("Interaction type(s)", "_interactionType");
		m.put("Source database(s)", "_sourceDatabase");
		m.put("Interaction identifier(s)", "_interactionId");
		m.put("Confidence value(s)", "_unavailable");
		m.put("Expansion method(s)", "_unavailable");
		m.put("Biological role(s) interactor A", "_unavailable");
		m.put("Biological role(s) interactor B", "_unavailable");
		m.put("Experimental role(s) interactor A", "_expRoleA");
		m.put("Experimental role(s) interactor B", "_expRoleB");
		m.put("Type(s) interactor A", "_typeA");
		m.put("Type(s) interactor B", "_typeB");
		m.put("Xref(s) interactor A", "_unavailable");
		m.put("Xref(s) interactor B", "_unavailable");
		m.put("Interaction Xref(s)", "_unavailable");
		m.put("Annotation(s) interactor A", "_unavailable");
		m.put("Annotation(s) interactor B", "_unavailable");
		m.put("Interaction annotation(s)", "_unavailable");
		m.put("Host organism(s)", "_unavailable");
		m.put("Interaction parameter(s)", "_unavailable");
		m.put("Creation date", "_creationDate");
		m.put("Update date", "_updateDate");
		m.put("Checksum(s) interactor A", "_unavailable");
		m.put("Checksum(s) interactor B", "_unavailable");
		m.put("Interaction Checksum(s)", "_unavailable");
		m.put("Negative", "_negative");
		m.put("Feature(s) interactor A", "_unavailable");
		m.put("Feature(s) interactor B", "_unavailable");
		m.put("Stoichiometry(s) interactor A", "_unavailable");
		m.put("Stoichiometry(s) interactor B", "_unavailable");
		m.put("Identification method participant A", "_unavailable");
		m.put("Identification method participant B", "_unavailable");
		return m;
	}

	/**
	 * Orthology field map. 13 columns matching the FMS layout. Synthetic fields
	 * (`_algorithms`, `_algorithmsMatch`, `_outOfAlgorithms`) are built in
	 * {@code OrthologyFileGenerator.customizeRow()}.
	 */
	private static Map<String, String> orthologyFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Gene1ID", "geneToGeneOrthologyGenerated.subjectGene.primaryExternalId");
		m.put("Gene1Symbol", "geneToGeneOrthologyGenerated.subjectGene.geneSymbol.displayText");
		m.put("Gene1SpeciesTaxonID", "geneToGeneOrthologyGenerated.subjectGene.taxon.curie");
		m.put("Gene1SpeciesName", "geneToGeneOrthologyGenerated.subjectGene.taxon.name");
		m.put("Gene2ID", "geneToGeneOrthologyGenerated.objectGene.primaryExternalId");
		m.put("Gene2Symbol", "geneToGeneOrthologyGenerated.objectGene.geneSymbol.displayText");
		m.put("Gene2SpeciesTaxonID", "geneToGeneOrthologyGenerated.objectGene.taxon.curie");
		m.put("Gene2SpeciesName", "geneToGeneOrthologyGenerated.objectGene.taxon.name");
		m.put("Algorithms", "_algorithms");
		m.put("AlgorithmsMatch", "_algorithmsMatch");
		m.put("OutOfAlgorithms", "_outOfAlgorithms");
		m.put("IsBestScore", "geneToGeneOrthologyGenerated.isBestScore.name");
		m.put("IsBestRevScore", "geneToGeneOrthologyGenerated.isBestScoreReverse.name");
		return m;
	}

	/**
	 * Variant/Allele field map. Source: allele_summary in site_index (LTP only — does not
	 * include the HTP variant_index). Same source the agr_api uses for the gene-page allele
	 * table (see AlleleESService.getAllelesByGene).
	 *
	 * One row per allele. variantList-derived fields (VariantId, VariantSymbol, position,
	 * consequence, etc.) are pipe-joined when an allele has multiple known variants.
	 */
	private static Map<String, String> variantAlleleFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Taxon", "allele.taxon.curie");
		m.put("SpeciesName", "allele.taxon.name");
		m.put("AlleleId", "allele.primaryExternalId");
		m.put("AlleleSymbol", "allele.alleleSymbol.displayText");
		m.put("AlleleSynonyms", "_alleleSynonyms");
		m.put("VariantId", "_variantId");
		m.put("VariantSymbol", "_variantSymbol");
		m.put("VariantSynonyms", "_variantSynonyms");
		m.put("VariantCrossReferences", "_unavailable");
		m.put("AlleleAssociatedGeneId", "alleleOfGene.primaryExternalId");
		m.put("AlleleAssociatedGeneSymbol", "alleleOfGene.geneSymbol.displayText");
		m.put("VariantAffectedGeneId", "_variantAffectedGeneId");
		m.put("VariantAffectedGeneSymbol", "_variantAffectedGeneSymbol");
		m.put("Category", "_category");
		m.put("VariantsTypeId", "_variantsTypeId");
		m.put("VariantsTypeName", "_variantsTypeName");
		m.put("VariantsHgvsNames", "_variantsHgvsNames");
		m.put("Assembly", "_assembly");
		m.put("Chromosome", "_chromosome");
		m.put("StartPosition", "_startPosition");
		m.put("EndPosition", "_endPosition");
		m.put("SequenceOfReference", "_sequenceOfReference");
		m.put("SequenceOfVariant", "_sequenceOfVariant");
		m.put("MostSevereConsequenceName", "_mostSevereConsequence");
		m.put("VariantInformationReference", "_unavailable");
		m.put("HasDiseaseAnnotations", "_hasDisease");
		m.put("HasPhenotypeAnnotations", "_hasPhenotype");
		return m;
	}

	/**
	 * VCF field map. Field map keys must remain in this exact 8-column VCFv4.3 order.
	 * All values are synthetic (`_*`) names populated in
	 * {@code VariantVcfFileGenerator.customizeRow()}.
	 */
	private static Map<String, String> vcfFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("CHROM", "_chrom");
		m.put("POS", "_pos");
		m.put("ID", "_id");
		m.put("REF", "_ref");
		m.put("ALT", "_alt");
		m.put("QUAL", "_qual");
		m.put("FILTER", "_filter");
		m.put("INFO", "_info");
		return m;
	}

	private static Map<String, String> geneDescriptionFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Gene ID", "curie");
		m.put("Gene Symbol", "symbol");
		m.put("Gene Description", "geneDescription");
		return m;
	}

	/**
	 * Disease field map. Columns whose ES source is not currently indexed map to "_unavailable"
	 * (rendered as empty cell). Columns derived from the doc category, from arrays, or that
	 * need transformation (date format, picking PMID over MOD curie) point at synthetic fields
	 * computed in DiseaseFileGenerator.customizeRow().
	 */
	private static Map<String, String> diseaseFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Taxon", "subject.taxon.curie");
		m.put("SpeciesName", "subject.taxon.name");
		m.put("DBobjectType", "_dbObjectType");
		m.put("DBObjectID", "subject.primaryExternalId");
		m.put("DBObjectSymbol", "_dbObjectSymbol");
		m.put("AssociationType", "relation.name");
		m.put("DOID", "object.curie");
		m.put("DOtermName", "object.name");
		m.put("WithOrtholog", "_withOrtholog");
		m.put("InferredFromID", "_unavailable");
		m.put("InferredFromSymbol", "_unavailable");
		m.put("ExperimentalCondition", "_unavailable");
		m.put("Modifier", "_unavailable");
		m.put("EvidenceCode", "_evidenceCode");
		m.put("EvidenceCodeName", "_evidenceCodeName");
		m.put("Reference", "_reference");
		m.put("Date", "_date");
		m.put("Source", "_source");
		return m;
	}

	/**
	 * Expression field map. Columns whose ES source is not currently indexed map to
	 * "_unavailable" — JsonPath returns "" for that, so the cell is rendered blank.
	 *
	 * Known gap: cellularComponent, sub-structure, and anatomy terms live under
	 * geneExpressionAnnotation.expressionPattern.whereExpressed.* on the entity, but the
	 * curation API's GeneExpressionDocument JsonView does not include `whereExpressed`,
	 * so those 12 columns can't be populated from ES today.
	 */
	private static Map<String, String> expressionFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Species", "geneExpressionAnnotation.expressionAnnotationSubject.taxon.name");
		m.put("SpeciesID", "geneExpressionAnnotation.expressionAnnotationSubject.taxon.curie");
		m.put("GeneID", "geneExpressionAnnotation.expressionAnnotationSubject.primaryExternalId");
		m.put("GeneSymbol", "geneExpressionAnnotation.expressionAnnotationSubject.geneSymbol.displayText");
		m.put("Location", "geneExpressionAnnotation.whereExpressedStatement");
		m.put("StageTerm", "geneExpressionAnnotation.whenExpressedStageName");
		m.put("AssayID", "geneExpressionAnnotation.expressionAssayUsed.curie");
		m.put("AssayTermName", "geneExpressionAnnotation.expressionAssayUsed.name");
		m.put("CellularComponentID", "_unavailable");
		m.put("CellularComponentTerm", "_unavailable");
		m.put("CellularComponentQualifierIDs", "_unavailable");
		m.put("CellularComponentQualifierTermNames", "_unavailable");
		m.put("SubStructureID", "_unavailable");
		m.put("SubStructureName", "_unavailable");
		m.put("SubStructureQualifierIDs", "_unavailable");
		m.put("SubStructureQualifierTermNames", "_unavailable");
		m.put("AnatomyTermID", "_unavailable");
		m.put("AnatomyTermName", "_unavailable");
		m.put("AnatomyTermQualifierIDs", "_unavailable");
		m.put("AnatomyTermQualifierTermNames", "_unavailable");
		// SourceURL is built from the first crossReference's urlTemplate + referencedCurie
		// inside ExpressionFileGenerator.customizeRow(); this path resolves the synthetic field.
		m.put("SourceURL", "_sourceUrl");
		m.put("Source", "geneExpressionAnnotation.dataProvider.abbreviation");
		m.put("Reference", "referenceId.0");
		return m;
	}
}
