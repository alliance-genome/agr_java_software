package org.alliancegenome.core.variant.converters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.alliancegenome.api.entity.VariantSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.AssemblyComponent;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.GenomeAssembly;
import org.alliancegenome.curation_api.model.entities.PredictedVariantConsequence;
import org.alliancegenome.curation_api.model.entities.Transcript;
import org.alliancegenome.curation_api.model.entities.Variant;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.model.entities.associations.TranscriptGeneAssociation;
import org.alliancegenome.curation_api.model.entities.ontology.NCBITaxonTerm;
import org.alliancegenome.curation_api.model.entities.ontology.SOTerm;
import org.alliancegenome.curation_api.model.entities.slotAnnotations.GeneSymbolSlotAnnotation;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.apache.commons.lang3.StringUtils;

import htsjdk.variant.variantcontext.VariantContext;

/**
 * Converts VCF VariantContext to AlleleVariantSequenceCuration documents
 * using curation API entity classes.
 */
public class AlleleVariantSequenceCurationConverter {

	private static final Pattern VALID_ALLELES = Pattern.compile("[ACGTN\\-]+");
	private NCBITaxonTerm taxon;

	// Header index positions (initialized once per header)
	private String[] header;
	private int alleleIdx = -1;
	private int consequenceIdx = -1;
	private int geneIdx = -1;
	private int geneSymbolIdx = -1;
	private int featureIdx = -1;
	private int featureTypeIdx = -1;
	private int hgvsCIdx = -1;
	private int hgvsPIdx = -1;
	private int impactIdx = -1;
	private int polyphenIdx = -1;
	private int siftIdx = -1;
	private int intronIdx = -1;
	private int exonIdx = -1;
	private int biotypeIdx = -1;
	private int aminoAcidsIdx = -1;
	private int codonsIdx = -1;
	private int cdnaPosIdx = -1;
	private int cdsPosIdx = -1;
	private int proteinPosIdx = -1;
	private int hgvsgIdx = -1;

	public AlleleVariantSequenceCurationConverter(String[] header) {
		this.header = header;
		initializeHeaderIndices();
	}

	private void initializeHeaderIndices() {

		alleleIdx = findHeaderIndex(header, "Allele");
		consequenceIdx = findHeaderIndex(header, "Consequence");
		geneIdx = findHeaderIndex(header, "Gene");
		geneSymbolIdx = findHeaderIndex(header, "SYMBOL");
		featureIdx = findHeaderIndex(header, "Feature");
		featureTypeIdx = findHeaderIndex(header, "Feature_type");
		hgvsCIdx = findHeaderIndex(header, "HGVSc");
		hgvsPIdx = findHeaderIndex(header, "HGVSp");
		impactIdx = findHeaderIndex(header, "IMPACT");
		polyphenIdx = findHeaderIndex(header, "PolyPhen");
		siftIdx = findHeaderIndex(header, "SIFT");
		intronIdx = findHeaderIndex(header, "INTRON");
		exonIdx = findHeaderIndex(header, "EXON");
		biotypeIdx = findHeaderIndex(header, "BIOTYPE");
		aminoAcidsIdx = findHeaderIndex(header, "Amino_acids");
		codonsIdx = findHeaderIndex(header, "Codons");
		cdnaPosIdx = findHeaderIndex(header, "cDNA_position");
		cdsPosIdx = findHeaderIndex(header, "CDS_position");
		proteinPosIdx = findHeaderIndex(header, "Protein_position");
	}

	public List<VariantSummaryDocument> convertContextToDocument(
		VariantContext ctx,
		SpeciesType speciesType,
		GeneDocumentCache geneCache) throws Exception {

		List<VariantSummaryDocument> returnDocuments = new ArrayList<>();

		// Initialize taxon if not already done
		if (taxon == null) {
			taxon = new NCBITaxonTerm();
			taxon.setName(speciesType.getName());
			taxon.setCurie(speciesType.getTaxonID());
		}

		// Create variant type
		SOTerm variantType = new SOTerm();
		if (!"SYMBOLIC".equals(ctx.getType().name()) && !"MIXED".equals(ctx.getType().name())) {
			String typeName = ctx.getType().name().toUpperCase();
			if ("INDEL".equals(ctx.getType().name())) {
				typeName = "delins";
			}
			variantType.setName(typeName);
			variantType.setCurie("SO:" + typeName);
		}

		// Process each alternate allele in the VCF record
		for (htsjdk.variant.variantcontext.Allele vcfAllele : ctx.getAlternateAlleles()) {

			if (!alleleIsValid(vcfAllele.getBaseString())) {
				continue;
			}

			// Parse VEP consequences from CSQ field
			List<PredictedVariantConsequence> consequences = getConsequences(ctx, vcfAllele.getBaseString(), geneCache, speciesType);
			if (consequences.isEmpty()) {
				continue;
			}

			Set<String> hgvsGList = new HashSet<>();
			for (String s : ctx.getAttributeAsStringList("CSQ", "")) {
				String[] infos = s.split("\\|", -1);
				if (infos.length >= 30) {
					hgvsGList.add(infos[29]);
				}
			}

			// Get HGVS nomenclature from first consequence
			String hgvsNomenclature = null;
			if (!consequences.isEmpty()) {
				PredictedVariantConsequence firstConsequence = consequences.getFirst();
				hgvsNomenclature = firstConsequence.getHgvsProteinNomenclature();
				if (StringUtils.isEmpty(hgvsNomenclature)) {
					hgvsNomenclature = firstConsequence.getHgvsCodingNomenclature();
				}
			}

			// Create curation API Variant entity
			Variant variant = new Variant();
			variant.setVariantType(variantType);
			variant.setTaxon(taxon);

			// Create location association
			CuratedVariantGenomicLocationAssociation variantLocation = new CuratedVariantGenomicLocationAssociation();
			variantLocation.setVariantAssociationSubject(variant);
			variantLocation.setReferenceSequence(ctx.getReference().getBaseString());
			variantLocation.setVariantSequence(vcfAllele.getBaseString());
			variantLocation.getNucleotideChange();
			// Set location info
			AssemblyComponent chromosome = new AssemblyComponent();
			chromosome.setName(ctx.getContig());
			GenomeAssembly assembly = new GenomeAssembly();
			assembly.setPrimaryExternalId(speciesType.getAssembly());
			chromosome.setGenomeAssembly(assembly);
			variantLocation.setVariantGenomicLocationAssociationObject(chromosome);
			variantLocation.setStart(ctx.getStart());
			variantLocation.setEnd(ctx.getEnd());

			// Build variant name
			StringBuilder variantName = new StringBuilder();
			if (StringUtils.isNotEmpty(hgvsNomenclature)) {
				variantName.append('(')
					.append(speciesType.getAssembly())
					.append(')')
					.append(ctx.getContig())
					.append(':');
				if (hgvsNomenclature.contains(":")) {
					variantName.append(hgvsNomenclature.split(":")[1]);
				} else {
					variantName.append(hgvsNomenclature);
				}
			}
			// Note: Variant doesn't have setName(), so we store name info separately
			String variantDisplayName = variantName.toString();
			Optional<String> firstHGVS = hgvsGList.stream().findFirst();
			if(firstHGVS.isPresent()) {
				variantLocation.setHgvs(firstHGVS.get());
			} else {
				variantLocation.setHgvs(variantDisplayName);
			}

			// Set primary key
			String ctxId = ctx.getID();
			String primaryKey;
			if (StringUtils.isNotEmpty(ctxId) && !ctxId.equals(".")) {
				primaryKey = ctxId;
			} else if (hgvsNomenclature != null && hgvsNomenclature.length() < 100) {
				primaryKey = hgvsNomenclature;
			} else {
				primaryKey = ctx.getContig() + ":" + ctx.getStart() + ":" + vcfAllele.getBaseString();
			}
			variant.setCurie(primaryKey);
			variant.setModInternalId(primaryKey);
			variantLocation.setHgvs(primaryKey);

			// Create curation API Allele entity
			Allele allele = new Allele();
			allele.setCurie(primaryKey);
			allele.setModInternalId(primaryKey);
			allele.setTaxon(taxon);

			// Collect gene info and molecular consequences
			Set<String> molecularConsequenceNames = new HashSet<>();
			Set<String> genes = new HashSet<>();
			Set<String> geneIds = new HashSet<>();
			Set<String> geneSynonymSet = new HashSet<>();
			Set<String> geneCrossReferencesSet = new HashSet<>();
			HashSet<String> transcriptsProcessed = new HashSet<>();
			boolean firstTranscript = true;

			for (PredictedVariantConsequence consequence : consequences) {
				Transcript transcript = consequence.getVariantTranscript();
				String transcriptID = transcript != null ? transcript.getCurie() : null;

				if (transcriptID != null && !transcriptsProcessed.contains(transcriptID)) {
					transcriptsProcessed.add(transcriptID);

					// Get VEP consequences (now a list of SOTerms)
					List<SOTerm> vepConsequences = consequence.getVepConsequences();
					if (vepConsequences != null && !vepConsequences.isEmpty()) {
						for (SOTerm soTerm : vepConsequences) {
							if (soTerm != null && soTerm.getName() != null) {
								molecularConsequenceNames.add(soTerm.getName());
							}
						}
					}

					// Get gene info from the first transcript
					if (firstTranscript && transcript.getTranscriptGeneAssociations() != null && !transcript.getTranscriptGeneAssociations().isEmpty()) {
						Gene gene = transcript.getTranscriptGeneAssociations().getFirst().getTranscriptGeneAssociationObject();
						if (gene != null) {
							GeneSymbolSlotAnnotation geneSymbolSlot = gene.getGeneSymbol();
							String geneSymbol = geneSymbolSlot != null ? geneSymbolSlot.getDisplayText() : null;
							if (StringUtils.isNotEmpty(geneSymbol)) {
								genes.add(geneSymbol + " (" + speciesType.getAbbreviation() + ")");
								geneIds.add(gene.getCurie());

								// Get gene synonyms/cross-refs from cache
								if (geneCache != null) {
									Set<String> synonyms = geneCache.getSynonyms().get(gene.getCurie());
									if (synonyms != null) {
										geneSynonymSet.addAll(synonyms);
									}
									Set<String> crossRefs = geneCache.getCrossReferences().get(gene.getCurie());
									if (crossRefs != null) {
										geneCrossReferencesSet.addAll(crossRefs);
									}
								}
							}
						}
						firstTranscript = false;
					}
				}
			}

			// Link consequences to the variant location
			variantLocation.setPredictedVariantConsequences(consequences);

			// Create the document for each consequence (full flattening)
			VariantSummaryDocument doc = new VariantSummaryDocument();
			doc.setSubCategory("HTP_variant");
			doc.setAllele(allele);
//				doc.setVariant(variant);
			doc.setVariant(variantLocation);
//				doc.setConsequence(consequence);

			// Set searchable fields on the document
			doc.setPrimaryKey(primaryKey);
			doc.setNameKey(primaryKey);
			doc.setName(primaryKey);
			doc.setVariantName(variantDisplayName);
			doc.setAlterationType("variant");
			doc.setSpecies(speciesType.getName());
			doc.setChromosome(ctx.getContig());
			doc.setVariantType(Collections.singleton(variantType.getName()));
			doc.setMolecularConsequence(molecularConsequenceNames);
			doc.setGenes(genes);
			doc.setGeneIds(geneIds);
			doc.setGeneSynonyms(geneSynonymSet);
			doc.setGeneCrossReferences(geneCrossReferencesSet);

			returnDocuments.add(doc);
		}

		return returnDocuments;
	}

	/**
	 * Parse VEP CSQ annotations from VCF and create PredictedVariantConsequence objects
	 */
	private List<PredictedVariantConsequence> getConsequences(
		VariantContext ctx,
		String varNuc,
		GeneDocumentCache geneCache,
		SpeciesType speciesType) {

		List<PredictedVariantConsequence> consequences = new ArrayList<>();
		HashSet<String> alreadyAdded = new HashSet<>();

		for (String s : ctx.getAttributeAsStringList("CSQ", "")) {
			if (s.isEmpty()) {
				continue;
			}

			String[] infos = s.split("\\|", -1);

			if (header.length != infos.length) {
				// Header mismatch - skip this record
				continue;
			}

			// Check if this annotation matches our alternate allele
			if (alleleIdx >= 0 && !infos[alleleIdx].equalsIgnoreCase(varNuc)) {
				continue;
			}

			// Get transcript/feature ID to avoid duplicates
			String featureId = featureIdx >= 0 ? infos[featureIdx] : null;
			if (featureId == null || alreadyAdded.contains(featureId)) {
				continue;
			}
			alreadyAdded.add(featureId);

			// Create PredictedVariantConsequence
			PredictedVariantConsequence consequence = new PredictedVariantConsequence();

			// Set VEP consequence terms (list of SOTerms)
			if (consequenceIdx >= 0 && StringUtils.isNotEmpty(infos[consequenceIdx])) {
				List<SOTerm> soTerms = new ArrayList<>();
				// VEP consequences can be comma-separated
				for (String consequenceName : infos[consequenceIdx].split(",")) {
					SOTerm soTerm = new SOTerm();
					soTerm.setName(consequenceName.trim());
					soTerms.add(soTerm);
				}
				consequence.setVepConsequences(soTerms);
			}

			// Set transcript info
			if (StringUtils.isNotEmpty(infos[featureIdx])) {
				Transcript transcript = new Transcript();
				transcript.setCurie(infos[featureIdx]);
				transcript.setName(infos[featureIdx]);
				SOTerm type = new SOTerm();
				type.setName(biotypeIdx >= 0 ? infos[biotypeIdx] : "unknown");
				transcript.setTranscriptType(type);
				// Set gene info on transcript if available
				if (geneIdx >= 0 && StringUtils.isNotEmpty(infos[geneIdx])) {
					Gene gene = new Gene();
					gene.setCurie(infos[geneIdx]);
					gene.setTaxon(taxon);

					// Set gene symbol if available
					if (geneSymbolIdx >= 0 && StringUtils.isNotEmpty(infos[geneSymbolIdx])) {
						GeneSymbolSlotAnnotation geneSymbol = new GeneSymbolSlotAnnotation();
						geneSymbol.setDisplayText(infos[geneSymbolIdx]);
						gene.setGeneSymbol(geneSymbol);
					}
					TranscriptGeneAssociation association = new TranscriptGeneAssociation();
					association.setTranscriptAssociationSubject(transcript);
					association.setTranscriptGeneAssociationObject(gene);
					transcript.setTranscriptGeneAssociations(List.of(association));
					// Note: Can't directly attach gene to transcript without proper association objects
					// The gene info will be stored separately for searchability
				}

				consequence.setVariantTranscript(transcript);
			}

			// Set HGVS nomenclature
			if (hgvsCIdx >= 0 && StringUtils.isNotEmpty(infos[hgvsCIdx])) {
				consequence.setHgvsCodingNomenclature(infos[hgvsCIdx]);
			}
			if (hgvsPIdx >= 0 && StringUtils.isNotEmpty(infos[hgvsPIdx])) {
				consequence.setHgvsProteinNomenclature(infos[hgvsPIdx]);
			}
/*
			if (intron >= 0 && StringUtils.isNotEmpty(infos[intron])) {
				consequence.setIntrons(infos[intron]);
			}
			if (exon >= 0 && StringUtils.isNotEmpty(infos[exon])) {
				consequence.setExons(infos[exon]);
			}
*/

			// Set amino acids (format: "R/H" = reference/variant)
			if (aminoAcidsIdx >= 0 && StringUtils.isNotEmpty(infos[aminoAcidsIdx])) {
				String[] aminoAcids = infos[aminoAcidsIdx].split("/");
				if (aminoAcids.length >= 1) {
					consequence.setAminoAcidReference(aminoAcids[0]);
				}
				if (aminoAcids.length >= 2) {
					consequence.setAminoAcidVariant(aminoAcids[1]);
				}
			}

			// Set codons (format: "cGc/cAc" = reference/variant)
			if (codonsIdx >= 0 && StringUtils.isNotEmpty(infos[codonsIdx])) {
				String[] codons = infos[codonsIdx].split("/");
				if (codons.length >= 1) {
					consequence.setCodonReference(codons[0]);
				}
				if (codons.length >= 2) {
					consequence.setCodonVariant(codons[1]);
				}
			}

			// Set calculated cDNA position (format: "123" or "123-125")
			if (cdnaPosIdx >= 0 && StringUtils.isNotEmpty(infos[cdnaPosIdx])) {
				parseAndSetPosition(infos[cdnaPosIdx],
					consequence::setCalculatedCdnaStart,
					consequence::setCalculatedCdnaEnd);
			}

			// Set calculated CDS position (format: "123" or "123-125")
			if (cdsPosIdx >= 0 && StringUtils.isNotEmpty(infos[cdsPosIdx])) {
				parseAndSetPosition(infos[cdsPosIdx],
					consequence::setCalculatedCdsStart,
					consequence::setCalculatedCdsEnd);
			}

			// Set calculated protein position (format: "123" or "123-125")
			if (proteinPosIdx >= 0 && StringUtils.isNotEmpty(infos[proteinPosIdx])) {
				parseAndSetPosition(infos[proteinPosIdx],
					consequence::setCalculatedProteinStart,
					consequence::setCalculatedProteinEnd);
			}

			// Set impact
			if (impactIdx >= 0 && StringUtils.isNotEmpty(infos[impactIdx])) {
				VocabularyTerm impact = new VocabularyTerm();
				impact.setName(infos[impactIdx]);
				consequence.setVepImpact(impact);
			}

			// Set PolyPhen prediction
			if (polyphenIdx >= 0 && StringUtils.isNotEmpty(infos[polyphenIdx])) {
				VocabularyTerm polyphenTerm = new VocabularyTerm();
				polyphenTerm.setName(infos[polyphenIdx].split("\\(")[0]);
				consequence.setPolyphenPrediction(polyphenTerm);
			}

			// Set SIFT prediction
			if (siftIdx >= 0 && StringUtils.isNotEmpty(infos[siftIdx])) {
				VocabularyTerm siftTerm = new VocabularyTerm();
				siftTerm.setName(infos[siftIdx].split("\\(")[0]);
				consequence.setSiftPrediction(siftTerm);
			}

			consequences.add(consequence);
		}

		return consequences;
	}

	/**
	 * Find the index of a field in the VEP CSQ header
	 */
	private int findHeaderIndex(String[] header, String fieldName) {
		for (int i = 0; i < header.length; i++) {
			if (header[i].equalsIgnoreCase(fieldName)) {
				return i;
			}
		}
		return -1;
	}

	private boolean alleleIsValid(String allele) {
		return VALID_ALLELES.matcher(allele).matches();
	}

	/**
	 * Parse VEP position field (format: "123" or "123-125") and set start/end values
	 */
	private void parseAndSetPosition(String position,
									java.util.function.Consumer<Integer> startSetter,
									java.util.function.Consumer<Integer> endSetter) {
		try {
			if (position.contains("-")) {
				String[] parts = position.split("-");
				if (parts.length >= 1 && !parts[0].isEmpty()) {
					startSetter.accept(Integer.parseInt(parts[0]));
				}
				if (parts.length >= 2 && !parts[1].isEmpty()) {
					endSetter.accept(Integer.parseInt(parts[1]));
				}
			} else {
				// Single position - set both start and end to same value
				int pos = Integer.parseInt(position);
				startSetter.accept(pos);
				endSetter.accept(pos);
			}
		} catch (NumberFormatException e) {
			// Ignore invalid position values (e.g., "?" or "-")
		}
	}
}
