package org.alliancegenome.filegenerator.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.filegenerator.generators.DiseaseFileGenerator;
import org.alliancegenome.filegenerator.generators.ExpressionFileGenerator;
import org.alliancegenome.filegenerator.generators.FileGenerator;
import org.alliancegenome.filegenerator.generators.GeneFileGenerator;
import org.alliancegenome.filegenerator.generators.OrthologyFileGenerator;
import org.alliancegenome.filegenerator.generators.PhenotypeFileGenerator;
import org.alliancegenome.filegenerator.generators.VariantAlleleFileGenerator;
import org.alliancegenome.filegenerator.generators.VariantVcfFileGenerator;

public enum FileGeneratorConfig {

	Gene("Gene", "GENE", List.of("gene_summary"), List.of(new OutputSpec(Format.TSV, SplitMode.TAXON), new OutputSpec(Format.TSV, SplitMode.COMBINED), new OutputSpec(Format.JSON_RAW, SplitMode.TAXON), new OutputSpec(Format.JSON_RAW, SplitMode.COMBINED)),
		List.of("FB", "HUMAN", "MGI", "RGD", "SGD", "WB", "XBXL", "XBXT", "ZFIN"), geneFieldMap(), "readmes/gene.txt", GeneFileGenerator.class, 1000, 8),

	Disease("Disease", "DISEASE-ALLIANCE", List.of("gene_disease_annotation", "allele_disease_annotation", "agm_disease_annotation"),
		List.of(new OutputSpec(Format.TSV, SplitMode.TAXON), new OutputSpec(Format.TSV, SplitMode.COMBINED), new OutputSpec(Format.JSON_RAW, SplitMode.TAXON), new OutputSpec(Format.JSON_RAW, SplitMode.COMBINED)), List.of("FB", "HUMAN", "MGI", "RGD", "SGD", "WB", "XBXL", "XBXT", "ZFIN"),
		diseaseFieldMap(), "readmes/disease.txt", DiseaseFileGenerator.class, 1000, 8),

	Expression("Expression", "EXPRESSION-ALLIANCE", List.of("gene_expression_annotation"), List.of(new OutputSpec(Format.TSV, SplitMode.TAXON), new OutputSpec(Format.TSV, SplitMode.COMBINED), new OutputSpec(Format.JSON_RAW, SplitMode.TAXON), new OutputSpec(Format.JSON_RAW, SplitMode.COMBINED)),
		List.of("FB", "MGI", "RGD", "SGD", "WB", "XBXL", "XBXT", "ZFIN"), expressionFieldMap(), "readmes/expression.txt", ExpressionFileGenerator.class, 1000, 8),

	Orthology("Orthology", "ORTHOLOGY-ALLIANCE", List.of("gene_to_gene_orthology"), List.of(new OutputSpec(Format.TSV, SplitMode.COMBINED), new OutputSpec(Format.JSON_RAW, SplitMode.COMBINED)), List.of("FB", "HUMAN", "MGI", "RGD", "SGD", "WB", "XBXL", "XBXT", "ZFIN"), orthologyFieldMap(),
		"readmes/orthology.txt", OrthologyFileGenerator.class, 1000, 8),

	VariantsVcf("Variants", "VARIANT-CONSEQUENCE", List.of("variant_summary"), List.of(new OutputSpec(Format.VCF, SplitMode.TAXON)), // Filename split is by MOD (consistent with every other generator). The genome
																																												// assembly
		// goes inside the file content (## headers / per-record fields), not in the
		// filename.
		List.of("FB", "MGI", "RGD", "WB", "ZFIN"), vcfFieldMap(), "readmes/variants_vcf.txt", VariantVcfFileGenerator.class, 1000, 8),

	Phenotype("Phenotype", "PHENOTYPE-ALLIANCE", List.of("gene_phenotype_annotation", "allele_phenotype_annotation", "agm_phenotype_annotation"),
		List.of(new OutputSpec(Format.TSV, SplitMode.TAXON), new OutputSpec(Format.TSV, SplitMode.COMBINED), new OutputSpec(Format.JSON_RAW, SplitMode.TAXON), new OutputSpec(Format.JSON_RAW, SplitMode.COMBINED)),
		List.of("FB", "MGI", "RGD", "SGD", "WB", "XBXL", "XBXT", "ZFIN"), phenotypeFieldMap(), "readmes/phenotype.txt", PhenotypeFileGenerator.class, 1000, 8),

	VariantsAlleles("Variant/Allele", "VARIANT-ALLELE", List.of("allele_summary"), List.of(new OutputSpec(Format.TSV, SplitMode.TAXON), new OutputSpec(Format.JSON_RAW, SplitMode.TAXON)), List.of("FB", "MGI", "RGD", "SGD", "WB", "ZFIN"), variantAlleleFieldMap(), "readmes/variants_alleles.txt",
		VariantAlleleFileGenerator.class, 1000, 8),;

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

	FileGeneratorConfig(String filetypeLabel, String type, List<String> esCategories, List<OutputSpec> outputs, List<String> mods, Map<String, String> fieldMap, String readmeResourcePath, Class<? extends FileGenerator> generatorClazz, int bufferSize, int threadCount) {
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
	 * Orthology field map. 13 columns matching the FMS layout. Synthetic fields
	 * (`_algorithms`, `_algorithmsMatch`, `_outOfAlgorithms`) are built in
	 * {@code OrthologyFileGenerator.customizeRow()}.
	 */
	private static Map<String, String> orthologyFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Gene1ID", "geneToGeneOrthologyGenerated.subjectGene.primaryExternalId");
		m.put("Gene1Symbol", "geneToGeneOrthologyGenerated.subjectGene.geneSymbol.displayText");
		m.put("Gene1SpeciesTaxonID", "geneToGeneOrthologyGenerated.subjectGene.taxon.curie");
		m.put("Gene1SpeciesName", "geneToGeneOrthologyGenerated.subjectGene.taxon.species.fullName");
		m.put("Gene2ID", "geneToGeneOrthologyGenerated.objectGene.primaryExternalId");
		m.put("Gene2Symbol", "geneToGeneOrthologyGenerated.objectGene.geneSymbol.displayText");
		m.put("Gene2SpeciesTaxonID", "geneToGeneOrthologyGenerated.objectGene.taxon.curie");
		m.put("Gene2SpeciesName", "geneToGeneOrthologyGenerated.objectGene.taxon.species.fullName");
		m.put("Algorithms", "_algorithms");
		m.put("AlgorithmsMatch", "_algorithmsMatch");
		m.put("OutOfAlgorithms", "_outOfAlgorithms");
		m.put("IsBestScore", "geneToGeneOrthologyGenerated.isBestScore.name");
		m.put("IsBestRevScore", "geneToGeneOrthologyGenerated.isBestScoreReverse.name");
		return m;
	}

	/**
	 * Variant/Allele field map. Source: allele_summary in site_index (LTP only —
	 * does not include the HTP variant_index). Same source the agr_api uses for the
	 * gene-page allele table (see AlleleESService.getAllelesByGene).
	 *
	 * One row per allele. variantList-derived fields (VariantId, VariantSymbol,
	 * position, consequence, etc.) are pipe-joined when an allele has multiple
	 * known variants.
	 */
	private static Map<String, String> variantAlleleFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Taxon", "allele.taxon.curie");
		m.put("SpeciesName", "allele.taxon.species.fullName");
		m.put("AlleleId", "allele.primaryExternalId");
		m.put("AlleleSymbol", "allele.alleleSymbol.displayText");
		m.put("AlleleSynonyms", "_alleleSynonyms");
		m.put("VariantId", "_variantId");
		m.put("VariantSymbol", "_variantSymbol");
		m.put("VariantSynonyms", "_variantSynonyms");
		m.put("VariantCrossReferences", "allele.primaryExternalId");
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
		m.put("VariantInformationReference", "_variantInformationReference");
		m.put("HasDiseaseAnnotations", "_hasDisease");
		m.put("HasPhenotypeAnnotations", "_hasPhenotype");
		return m;
	}

	/**
	 * VCF field map. Field map keys must remain in this exact 8-column VCFv4.3
	 * order. All values are synthetic (`_*`) names populated in
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
	 * Gene field map. Source: gene_summary in site_index. List-valued cells
	 * (synonyms, secondary IDs, cross references) are pipe-joined in
	 * {@code GeneFileGenerator.customizeRow()}. Genome location uses the first
	 * entry of {@code geneGenomicLocationAssociations}.
	 */
	private static Map<String, String> geneFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Taxon", "gene.taxon.curie");
		m.put("SpeciesName", "gene.taxon.species.fullName");
		m.put("GeneId", "gene.primaryExternalId");
		m.put("GeneSymbol", "gene.geneSymbol.displayText");
		m.put("GeneSynonyms", "_geneSynonyms");
		m.put("GeneSystematicName", "gene.geneSystematicName.displayText");
		m.put("GeneSecondaryIds", "_geneSecondaryIds");
		m.put("GeneCrossReferences", "_geneCrossReferences");
		m.put("GeneBioTypeId", "gene.geneType.curie");
		m.put("GeneBioTypeName", "gene.geneType.name");
		m.put("GeneAllianceAutomatedDescription", "_allianceAutomatedDescription");
		m.put("GeneMODAutomatedDescription", "_modAutomatedDescription");
		m.put("GeneMODDescription", "_modDescription");
		m.put("Assembly", "_assembly");
		m.put("Chromosome", "gene.geneGenomicLocationAssociations.0.geneGenomicLocationAssociationObject.name");
		m.put("StartPosition", "gene.geneGenomicLocationAssociations.0.start");
		m.put("EndPosition", "gene.geneGenomicLocationAssociations.0.end");
		m.put("Strand", "gene.geneGenomicLocationAssociations.0.strand");
		return m;
	}

	/**
	 * Disease field map. Source: {gene,allele,agm}_disease_annotation in site_index
	 * — these docs are consolidated and carry primaryAnnotations[].
	 * DiseaseFileGenerator.customizeRows() expands each ES hit into N flattened
	 * rows (one per primaryAnnotations element) for TSV; JSON_RAW writes the
	 * consolidated doc verbatim. Every column is synthetic (`_*`) and populated
	 * per-row from primaryAnnotations[i]. Columns whose ES source is not currently
	 * indexed map to "_unavailable" (rendered as an empty cell).
	 */
	private static Map<String, String> diseaseFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Taxon", "_taxon");
		m.put("SpeciesName", "_speciesName");
		m.put("DBobjectType", "_dbObjectType");
		m.put("DBObjectID", "_dbObjectId");
		m.put("DBObjectSymbol", "_dbObjectSymbol");
		m.put("AssociationType", "_associationType");
		m.put("DOID", "_doId");
		m.put("DOtermName", "_doTermName");
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
	 * Phenotype field map. Source: gene_phenotype_annotation in site_index — these
	 * docs are consolidated and carry primaryAnnotations[].
	 * PhenotypeFileGenerator.customizeRows() expands each ES hit into N flattened
	 * rows (one per primaryAnnotations element) for TSV; JSON_RAW writes the
	 * consolidated doc verbatim. Every column is synthetic (`_*`) and populated
	 * per-row from primaryAnnotations[i].
	 */
	private static Map<String, String> phenotypeFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Phenotype", "_phenotype");
		m.put("Genetic Entity ID", "_geneticEntityId");
		m.put("Genetic Entity Name", "_geneticEntityName");
		m.put("Genetic Entity Type", "_geneticEntityType");
		m.put("Experimental Condition", "_experimentalCondition");
		m.put("Source", "_source");
		m.put("Reference", "_reference");
		return m;
	}

	/**
	 * Expression field map. Anatomy / sub-structure / cellular-component term IDs
	 * and names come straight off whereExpressed; the six qualifier list columns
	 * are pipe-joined inside ExpressionFileGenerator.customizeRow() into synthetic
	 * fields.
	 */
	private static Map<String, String> expressionFieldMap() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("Species", "geneExpressionAnnotation.expressionAnnotationSubject.taxon.species.fullName");
		m.put("SpeciesID", "geneExpressionAnnotation.expressionAnnotationSubject.taxon.curie");
		m.put("GeneID", "geneExpressionAnnotation.expressionAnnotationSubject.primaryExternalId");
		m.put("GeneSymbol", "geneExpressionAnnotation.expressionAnnotationSubject.geneSymbol.displayText");
		m.put("Location", "geneExpressionAnnotation.whereExpressedStatement");
		m.put("StageTerm", "geneExpressionAnnotation.whenExpressedStageName");
		m.put("AssayID", "geneExpressionAnnotation.expressionAssayUsed.curie");
		m.put("AssayTermName", "geneExpressionAnnotation.expressionAssayUsed.name");
		m.put("CellularComponentID", "geneExpressionAnnotation.expressionPattern.whereExpressed.cellularComponentTerm.curie");
		m.put("CellularComponentTerm", "geneExpressionAnnotation.expressionPattern.whereExpressed.cellularComponentTerm.name");
		m.put("CellularComponentQualifierIDs", "_cellularComponentQualifierIds");
		m.put("CellularComponentQualifierTermNames", "_cellularComponentQualifierNames");
		m.put("SubStructureID", "geneExpressionAnnotation.expressionPattern.whereExpressed.anatomicalSubstructure.curie");
		m.put("SubStructureName", "geneExpressionAnnotation.expressionPattern.whereExpressed.anatomicalSubstructure.name");
		m.put("SubStructureQualifierIDs", "_subStructureQualifierIds");
		m.put("SubStructureQualifierTermNames", "_subStructureQualifierNames");
		m.put("AnatomyTermID", "geneExpressionAnnotation.expressionPattern.whereExpressed.anatomicalStructure.curie");
		m.put("AnatomyTermName", "geneExpressionAnnotation.expressionPattern.whereExpressed.anatomicalStructure.name");
		m.put("AnatomyTermQualifierIDs", "_anatomyQualifierIds");
		m.put("AnatomyTermQualifierTermNames", "_anatomyQualifierNames");
		// SourceURL is built from the first crossReference's urlTemplate +
		// referencedCurie inside ExpressionFileGenerator.customizeRow(); this path
		// resolves the synthetic field.
		m.put("SourceURL", "_sourceUrl");
		m.put("Source", "geneExpressionAnnotation.dataProvider.abbreviation");
		m.put("Reference", "_reference");
		return m;
	}
}
