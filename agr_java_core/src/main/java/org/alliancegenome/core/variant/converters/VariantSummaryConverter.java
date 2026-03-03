package org.alliancegenome.core.variant.converters;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.AssemblyComponent;
import org.alliancegenome.curation_api.model.entities.CrossReference;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.GenomeAssembly;
import org.alliancegenome.curation_api.model.entities.PredictedVariantConsequence;
import org.alliancegenome.curation_api.model.entities.Transcript;
import org.alliancegenome.curation_api.model.entities.Variant;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.model.entities.associations.GeneGenomicLocationAssociation;
import org.alliancegenome.curation_api.model.entities.associations.TranscriptGeneAssociation;
import org.alliancegenome.curation_api.model.entities.ontology.NCBITaxonTerm;
import org.alliancegenome.curation_api.model.entities.ontology.SOTerm;
import org.alliancegenome.curation_api.model.entities.slotAnnotations.GeneSymbolSlotAnnotation;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import htsjdk.variant.variantcontext.VariantContext;

/**
 * Converts VCF VariantContext to AlleleVariantSequenceCuration documents using
 * curation API entity classes.
 */
public class VariantSummaryConverter {

	private static final Pattern PIPE_PATTERN = Pattern.compile("\\|");
	private NCBITaxonTerm taxon;

	// Header index positions (initialized once per header)
	private String[] header;
	private GeneDocumentCache geneCache;
	private Map<String, SOTerm> soTermCache = new ConcurrentHashMap<>();
	private Map<String, VocabularyTerm> vocabularyTermCache = new ConcurrentHashMap<>();

	private int alleleIdx = -1;
	private int consequenceIdx = -1;
	private int geneIdx = -1;
	private int geneSymbolIdx = -1;
	private int featureIdx = -1;
	private int featureTypeIdx = -1;
	private int hgvsCIdx = -1;
	private int hgvsPIdx = -1;
	private int impactIdx = -1;
	private int polyphenPredIdx = -1;
	private int polyphenScoreIdx = -1;
	private int siftPredIdx = -1;
	private int siftScoreIdx = -1;
	private int intronIdx = -1;
	private int exonIdx = -1;
	private int biotypeIdx = -1;
	private int aminoAcidsIdx = -1;
	private int codonsIdx = -1;
	private int cdnaPosIdx = -1;
	private int cdsPosIdx = -1;
	private int proteinPosIdx = -1;
	private int hgvsgIdx = -1;

	public VariantSummaryConverter(String[] header, GeneDocumentCache geneCache) {
		this.header = header;
		this.geneCache = geneCache;
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
		polyphenPredIdx = findHeaderIndex(header, "PolyPhen_prediction");
		polyphenScoreIdx = findHeaderIndex(header, "PolyPhen_score");
		siftPredIdx = findHeaderIndex(header, "SIFT_prediction");
		siftScoreIdx = findHeaderIndex(header, "SIFT_score");
		intronIdx = findHeaderIndex(header, "INTRON");
		exonIdx = findHeaderIndex(header, "EXON");
		biotypeIdx = findHeaderIndex(header, "BIOTYPE");
		aminoAcidsIdx = findHeaderIndex(header, "Amino_acids");
		codonsIdx = findHeaderIndex(header, "Codons");
		cdnaPosIdx = findHeaderIndex(header, "cDNA_position");
		cdsPosIdx = findHeaderIndex(header, "CDS_position");
		proteinPosIdx = findHeaderIndex(header, "Protein_position");
		hgvsgIdx = findHeaderIndex(header, "HGVSg");
	}

	public List<VariantSummaryDocument> convertContextToDocument(VariantContext ctx, SpeciesType speciesType) throws Exception {

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
			String typeCurie = "SO:" + typeName;
			if ("INDEL".equals(ctx.getType().name())) {
				// Sub-classify INDELs: htsjdk lumps insertions, deletions, and delins together
				String ref = ctx.getReference().getBaseString();
				String alt = ctx.getAlternateAlleles().getFirst().getBaseString();
				if (alt.startsWith(ref)) {
					typeName = "insertion";
					typeCurie = "SO:0000667";
				} else if (ref.startsWith(alt)) {
					typeName = "deletion";
					typeCurie = "SO:0000159";
				} else {
					typeName = "delins";
				}
			}
			variantType.setName(typeName);
			variantType.setCurie(typeCurie);
		}

		// Hoist CSQ list to a single call before the allele loop
		List<String> csqList = ctx.getAttributeAsStringList("CSQ", "");

		// Process each alternate allele in the VCF record
		String refBase = ctx.getReference().getBaseString();
		for (htsjdk.variant.variantcontext.Allele vcfAllele : ctx.getAlternateAlleles()) {
			if (!alleleIsValid(vcfAllele.getBaseString())) {
				System.out.println("Skipping invalid allele: " + vcfAllele.getBaseString());
				continue;
			}

			// Convert VCF allele to VEP CSQ representation by stripping common prefix
			String vepAllele = toVepAllele(refBase, vcfAllele.getBaseString());

			// Parse VEP consequences from CSQ field
			Set<String> hgvsGList = new HashSet<>();
			Pair<List<PredictedVariantConsequence>, HashSet<String>> resultPair = getConsequences(csqList, vepAllele, speciesType, hgvsGList);
			List<PredictedVariantConsequence> consequences = resultPair.getLeft();
			if (consequences.isEmpty()) {
				continue;
			}

			// Get HGVS nomenclature from first consequence
			String hgvsNomenclature = null;

			PredictedVariantConsequence firstConsequence = consequences.getFirst();
			hgvsNomenclature = firstConsequence.getHgvsProteinNomenclature();
			if (StringUtils.isEmpty(hgvsNomenclature)) {
				hgvsNomenclature = firstConsequence.getHgvsCodingNomenclature();
			}

			// Create curation API Variant entity
			Variant variant = new Variant();
			variant.setVariantType(variantType);
			variant.setTaxon(taxon);

			// Create location association
			String altBase = vcfAllele.getBaseString();
			// Strip common prefix (padding) to get actual changed bases
			int commonPrefix = 0;
			int minLen = Math.min(refBase.length(), altBase.length());
			while (commonPrefix < minLen && refBase.charAt(commonPrefix) == altBase.charAt(commonPrefix)) {
				commonPrefix++;
			}
			String actualRef = refBase.substring(commonPrefix);
			String actualAlt = altBase.substring(commonPrefix);

			CuratedVariantGenomicLocationAssociation cvgla = new CuratedVariantGenomicLocationAssociation();
			cvgla.setVariantAssociationSubject(variant);
			cvgla.setReferenceSequence(actualRef.isEmpty() ? null : actualRef);
			cvgla.setVariantSequence(actualAlt.isEmpty() ? null : actualAlt);
			if (commonPrefix > 0 && actualRef.isEmpty()) {
				// Insertion: entire REF is padding
				cvgla.setPaddedBase(refBase.substring(0, 1));
			} else if (commonPrefix > 0 && actualAlt.isEmpty()) {
				// Deletion: entire ALT is padding
				cvgla.setPaddedBase(altBase);
			}
			// SNPs and delins: no padded base
			// Set location info
			AssemblyComponent chromosome = new AssemblyComponent();
			chromosome.setName(ctx.getContig());
			GenomeAssembly assembly = new GenomeAssembly();
			assembly.setPrimaryExternalId(speciesType.getAssembly());
			chromosome.setGenomeAssembly(assembly);
			cvgla.setVariantGenomicLocationAssociationObject(chromosome);
			cvgla.setStart(ctx.getStart());
			cvgla.setEnd(ctx.getEnd());

			// Build variant name
			StringBuilder variantName = new StringBuilder();
			if (StringUtils.isNotEmpty(hgvsNomenclature)) {
				variantName.append('(').append(speciesType.getAssembly()).append(')').append(ctx.getContig()).append(':');
				int colonIdx = hgvsNomenclature.indexOf(':');
				if (colonIdx >= 0) {
					variantName.append(hgvsNomenclature, colonIdx + 1, hgvsNomenclature.length());
				} else {
					variantName.append(hgvsNomenclature);
				}
			}
			// Note: Variant doesn't have setName(), so we store name info separately
			String variantDisplayName = variantName.toString();
			Optional<String> firstHGVS = hgvsGList.stream().findFirst();
			if (firstHGVS.isPresent()) {
				cvgla.setHgvs(firstHGVS.get());
			} else {
				cvgla.setHgvs(variantDisplayName);
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

			// Add rsID as cross reference if available
			if (StringUtils.isNotEmpty(ctxId) && ctxId.startsWith("rs")) {
				CrossReference dbSnpRef = new CrossReference();
				dbSnpRef.setReferencedCurie(ctxId);
				dbSnpRef.setDisplayName(ctxId);
				variant.setCrossReferences(List.of(dbSnpRef));
			}

			// Create curation API Allele entity
			Allele allele = new Allele();
			allele.setCurie(primaryKey);
			// allele.setModInternalId(primaryKey);
			allele.setTaxon(taxon);
			// If we want to show

			cvgla.setPredictedVariantConsequences(consequences);
			// Create the document for each consequence (full flattening)
			VariantSummaryDocument doc = new VariantSummaryDocument();
			doc.setAlterationType("variant");
			doc.setAlterationTypeSortOrder(4);
			doc.setHasPhenotype(false);
			doc.setHasDisease(false);
			doc.setAllele(allele);
			Variant variantWrapper = new Variant();
			variantWrapper.setVariantType(variantType);
			variantWrapper.setCuratedVariantGenomicLocations(List.of(cvgla));
			doc.setVariants(List.of(variantWrapper));
			doc.setGeneIds(resultPair.getRight());

			returnDocuments.add(doc);
		}

		return returnDocuments;
	}

	/**
	 * Parse VEP CSQ annotations from VCF and create PredictedVariantConsequence
	 * objects
	 *
	 * @param hgvsGList
	 */
	private Pair<List<PredictedVariantConsequence>, HashSet<String>> getConsequences(List<String> csqList, String varNuc, SpeciesType speciesType, Set<String> hgvsGList) {

		List<PredictedVariantConsequence> consequences = new ArrayList<>();
		HashSet<String> alreadyAdded = new HashSet<>();
		HashSet<String> geneIds = new HashSet<>();

		for (String csq : csqList) {
			if (csq.isEmpty()) {
				continue;
			}

			// Pre-split allele filtering: check allele field before doing the full split
			if (alleleIdx >= 0) {
				String alleleField = extractField(csq, alleleIdx);
				if (alleleField == null || !alleleField.equalsIgnoreCase(varNuc)) {
					continue;
				}
			}

			String[] infos = PIPE_PATTERN.split(csq, -1);

			if (header.length != infos.length) {
				// Header mismatch - skip this record
				continue;
			}

			// Skip consequences without a gene
			if (infos[geneIdx].isEmpty()) {
				continue;
			}

			if (!infos[hgvsgIdx].isEmpty()) {
				hgvsGList.add(infos[hgvsgIdx]);
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
			if (!infos[consequenceIdx].isEmpty()) {
				List<SOTerm> soTerms = new ArrayList<>();
				// VEP consequences can be comma-separated
				for (String consequenceName : infos[consequenceIdx].split(",")) {
					SOTerm soTerm = getSOTerm(consequenceName.trim());
					soTerms.add(soTerm);
				}
				consequence.setVepConsequences(soTerms);
			}

			// Set transcript info
			if (!infos[featureIdx].isEmpty()) {
				Transcript transcript = new Transcript();
				transcript.setCurie(infos[featureIdx]);
				transcript.setName(infos[featureIdx]);
				SOTerm type = getSOTerm(!infos[biotypeIdx].isEmpty() ? infos[biotypeIdx] : "unknown");
				transcript.setTranscriptType(type);
				// Set gene info on transcript if available
				if (!infos[geneIdx].isEmpty()) {
					Gene gene = new Gene();
					gene.setCurie(infos[geneIdx]);
					gene.setTaxon(taxon);
					geneIds.add(infos[geneIdx]);

					// Set gene symbol if available
					if (!infos[geneSymbolIdx].isEmpty()) {
						GeneSymbolSlotAnnotation geneSymbol = new GeneSymbolSlotAnnotation();
						geneSymbol.setDisplayText(infos[geneSymbolIdx]);
						gene.setGeneSymbol(geneSymbol);
						if (geneCache != null) {
							// Set genome location from cache
							org.alliancegenome.neo4j.entity.node.Gene cachedGene = geneCache.getGeneMap().get(gene.getCurie());
							if (cachedGene != null && cachedGene.getGenomeLocations() != null && !cachedGene.getGenomeLocations().isEmpty()) {
								List<GeneGenomicLocationAssociation> geneLocations = new ArrayList<>();
								for (org.alliancegenome.neo4j.entity.relationship.GenomeLocation loc : cachedGene.getGenomeLocations()) {
									GeneGenomicLocationAssociation geneLocation = new GeneGenomicLocationAssociation();
									geneLocation.setStart(loc.getStart() != null ? loc.getStart().intValue() : null);
									geneLocation.setEnd(loc.getEnd() != null ? loc.getEnd().intValue() : null);
									AssemblyComponent geneChromosome = new AssemblyComponent();
									geneChromosome.setName(loc.getChromosome());
									geneLocation.setGeneGenomicLocationAssociationObject(geneChromosome);
									geneLocations.add(geneLocation);
								}
								gene.setGeneGenomicLocationAssociations(geneLocations);
							}
						}
					}
					TranscriptGeneAssociation association = new TranscriptGeneAssociation();
					association.setTranscriptAssociationSubject(transcript);
					association.setTranscriptGeneAssociationObject(gene);
					transcript.setTranscriptGeneAssociations(List.of(association));
					// Note: Can't directly attach gene to transcript without proper association
					// objects
					// The gene info will be stored separately for searchability
				}

				consequence.setVariantTranscript(transcript);
			}

			// Set HGVS nomenclature (VEP URL-encodes special characters like = in CSQ fields)
			consequence.setHgvsCodingNomenclature(URLDecoder.decode(infos[hgvsCIdx], StandardCharsets.UTF_8));
			consequence.setHgvsProteinNomenclature(URLDecoder.decode(infos[hgvsPIdx], StandardCharsets.UTF_8));
			consequence.setIntrons(infos[intronIdx]);
			consequence.setExons(infos[exonIdx]);

			// Set amino acids (format: "R/H" = reference/variant)
			if (!infos[aminoAcidsIdx].isEmpty()) {
				String[] aminoAcids = infos[aminoAcidsIdx].split("/");
				if (aminoAcids.length >= 1) {
					consequence.setAminoAcidReference(aminoAcids[0]);
				}
				if (aminoAcids.length >= 2) {
					consequence.setAminoAcidVariant(aminoAcids[1]);
				}
			}

			// Set codons (format: "cGc/cAc" = reference/variant)
			if (!infos[codonsIdx].isEmpty()) {
				String[] codons = infos[codonsIdx].split("/");
				if (codons.length >= 1) {
					consequence.setCodonReference(codons[0]);
				}
				if (codons.length >= 2) {
					consequence.setCodonVariant(codons[1]);
				}
			}

			// Set calculated cDNA position (format: "123" or "123-125")
			if (!infos[cdnaPosIdx].isEmpty()) {
				parseAndSetPosition(infos[cdnaPosIdx], consequence::setCalculatedCdnaStart, consequence::setCalculatedCdnaEnd);
			}

			// Set calculated CDS position (format: "123" or "123-125")
			if (!infos[cdsPosIdx].isEmpty()) {
				parseAndSetPosition(infos[cdsPosIdx], consequence::setCalculatedCdsStart, consequence::setCalculatedCdsEnd);
			}

			// Set calculated protein position (format: "123" or "123-125")
			if (!infos[proteinPosIdx].isEmpty()) {
				parseAndSetPosition(infos[proteinPosIdx], consequence::setCalculatedProteinStart, consequence::setCalculatedProteinEnd);
			}

			// Set impact
			if (!infos[impactIdx].isEmpty()) {
				consequence.setVepImpact(getVocabularyTerm(infos[impactIdx]));
			}

			// Set PolyPhen prediction and score
			if (polyphenPredIdx >= 0 && !infos[polyphenPredIdx].isEmpty()) {
				consequence.setPolyphenPrediction(getVocabularyTerm(infos[polyphenPredIdx]));
			}
			if (polyphenScoreIdx >= 0 && !infos[polyphenScoreIdx].isEmpty()) {
				try {
					consequence.setPolyphenScore(Float.parseFloat(infos[polyphenScoreIdx]));
				} catch (NumberFormatException e) {
					// skip invalid score
				}
			}

			// Set SIFT prediction and score
			if (siftPredIdx >= 0 && !infos[siftPredIdx].isEmpty()) {
				consequence.setSiftPrediction(getVocabularyTerm(infos[siftPredIdx]));
			}
			if (siftScoreIdx >= 0 && !infos[siftScoreIdx].isEmpty()) {
				try {
					consequence.setSiftScore(Float.parseFloat(infos[siftScoreIdx]));
				} catch (NumberFormatException e) {
					// skip invalid score
				}
			}

			consequences.add(consequence);
		}
		
		return Pair.of(consequences, geneIds);
	}

	/**
	 * Extract a single pipe-delimited field from a CSQ line without splitting the
	 * entire string.
	 */
	private static String extractField(String line, int fieldIndex) {
		int start = 0;
		for (int i = 0; i < fieldIndex; i++) {
			start = line.indexOf('|', start);
			if (start < 0) {
				return null;
			}
			start++;
		}
		int end = line.indexOf('|', start);
		return line.substring(start, end < 0 ? line.length() : end);
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

	/**
	 * Convert VCF REF/ALT to VEP CSQ Allele representation.
	 * VEP strips the common leading padding base from REF and ALT:
	 * - Deletion: REF=AAGGAACCACTC, ALT=A → VEP allele = "-"
	 * - Insertion: REF=A, ALT=ATCG → VEP allele = "TCG"
	 * - SNP: REF=A, ALT=T → VEP allele = "T"
	 */
	private static String toVepAllele(String ref, String alt) {
		int commonPrefix = 0;
		int minLen = Math.min(ref.length(), alt.length());
		while (commonPrefix < minLen && ref.charAt(commonPrefix) == alt.charAt(commonPrefix)) {
			commonPrefix++;
		}
		String remaining = alt.substring(commonPrefix);
		return remaining.isEmpty() ? "-" : remaining;
	}

	private static boolean alleleIsValid(String allele) {
		if (allele.isEmpty()) {
			return false;
		}
		for (int i = 0; i < allele.length(); i++) {
			char c = allele.charAt(i);
			if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N' || c == '-') {
				continue;
			}
			return false;
		}
		return true;
	}

	private SOTerm getSOTerm(String name) {
		if (name.isEmpty()) {
			name = "unknown";
		}
		return soTermCache.computeIfAbsent(name, k -> {
			SOTerm term = new SOTerm();
			term.setName(k);
			return term;
		});
	}

	private VocabularyTerm getVocabularyTerm(String name) {
		return vocabularyTermCache.computeIfAbsent(name, k -> {
			VocabularyTerm term = new VocabularyTerm();
			term.setName(k);
			return term;
		});
	}

	/**
	 * Parse VEP position field (format: "123" or "123-125") and set start/end
	 * values
	 */
	private void parseAndSetPosition(String position, Consumer<Integer> startSetter, Consumer<Integer> endSetter) {
		try {
			int dashIdx = position.indexOf('-');
			if (dashIdx >= 0) {
				if (dashIdx > 0) {
					startSetter.accept(Integer.parseInt(position.substring(0, dashIdx)));
				}
				if (dashIdx + 1 < position.length()) {
					endSetter.accept(Integer.parseInt(position.substring(dashIdx + 1)));
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
