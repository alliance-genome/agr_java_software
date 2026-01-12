package org.alliancegenome.core.variant.converters;

import htsjdk.variant.variantcontext.VariantContext;
import org.alliancegenome.api.entity.VariantSummaryDocument;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.model.entities.ontology.NCBITaxonTerm;
import org.alliancegenome.curation_api.model.entities.ontology.SOTerm;
import org.alliancegenome.curation_api.model.entities.slotAnnotations.GeneSymbolSlotAnnotation;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Converts VCF VariantContext to AlleleVariantSequenceCuration documents
 * using curation API entity classes.
 */
public class AlleleVariantSequenceCurationConverter {

	private static final Pattern VALID_ALLELES = Pattern.compile("[ACGTN\\-]+");
	private NCBITaxonTerm taxon;

	public List<VariantSummaryDocument> convertContextToDocument(
		VariantContext ctx,
		String[] header,
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
			List<PredictedVariantConsequence> consequences = getConsequences(ctx, vcfAllele.getBaseString(), header, geneCache, speciesType);
			if (consequences.isEmpty()) {
				continue;
			}

			// Get HGVS nomenclature from first consequence
			String hgvsNomenclature = null;
			if (!consequences.isEmpty()) {
				PredictedVariantConsequence firstConsequence = consequences.get(0);
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

			// Set location info
			AssemblyComponent chromosome = new AssemblyComponent();
			chromosome.setName(ctx.getContig());
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
			variantLocation.setHgvs(hgvsNomenclature);

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

					// Get gene info from first transcript
					if (firstTranscript && transcript != null && transcript.getTranscriptGeneAssociations() != null
						&& !transcript.getTranscriptGeneAssociations().isEmpty()) {
						Gene gene = transcript.getTranscriptGeneAssociations().get(0).getTranscriptGeneAssociationObject();
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
		String[] header,
		GeneDocumentCache geneCache,
		SpeciesType speciesType) throws Exception {

		List<PredictedVariantConsequence> consequences = new ArrayList<>();
		HashSet<String> alreadyAdded = new HashSet<>();

		// Find indices in header for fields we need
		int alleleIdx = findHeaderIndex(header, "Allele");
		int consequenceIdx = findHeaderIndex(header, "Consequence");
		int geneIdx = findHeaderIndex(header, "Gene");
		int geneSymbolIdx = findHeaderIndex(header, "SYMBOL");
		int featureIdx = findHeaderIndex(header, "Feature");
		int featureTypeIdx = findHeaderIndex(header, "Feature_type");
		int hgvsCIdx = findHeaderIndex(header, "HGVSc");
		int hgvsPIdx = findHeaderIndex(header, "HGVSp");
		int impactIdx = findHeaderIndex(header, "IMPACT");
		int polyphenIdx = findHeaderIndex(header, "PolyPhen");
		int siftIdx = findHeaderIndex(header, "SIFT");

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
			if (featureIdx >= 0 && StringUtils.isNotEmpty(infos[featureIdx])) {
				Transcript transcript = new Transcript();
				transcript.setCurie(infos[featureIdx]);
				transcript.setName(infos[featureIdx]);

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
}
