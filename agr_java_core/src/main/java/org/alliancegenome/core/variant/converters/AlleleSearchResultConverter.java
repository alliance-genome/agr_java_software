package org.alliancegenome.core.variant.converters;

import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.Species;
import org.alliancegenome.curation_api.model.entities.ontology.NCBITaxonTerm;
import org.alliancegenome.curation_api.model.entities.slotAnnotations.NameSlotAnnotation;
import org.alliancegenome.es.model.AlleleSearchResultDocument;
import org.alliancegenome.es.model.search.Category;
import org.alliancegenome.es.model.search.RelatedDataLink;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class AlleleSearchResultConverter {

	public List<AlleleSearchResultDocument> convert(List<AlleleSummaryDocument> alleleSummaryDocuments) {
		List<AlleleSearchResultDocument> result = new ArrayList<>();

		for (AlleleSummaryDocument doc : alleleSummaryDocuments) {
			Allele allele = doc.getAllele();
			if (allele == null) {
				continue;
			}

			AlleleSearchResultDocument searchDoc = new AlleleSearchResultDocument();
			searchDoc.setSearchable(true);
			searchDoc.setAlterationType(doc.getAlterationType());

			searchDoc.setPrimaryKey(allele.getPrimaryExternalId());
			searchDoc.setGlobalId(allele.getPrimaryExternalId());
			searchDoc.setLocalId(allele.getPrimaryExternalId());

			if (allele.getAlleleSymbol() != null) {
				searchDoc.setSymbol(allele.getAlleleSymbol().getDisplayText());
				searchDoc.setSymbolText(allele.getAlleleSymbol().getFormatText());
				searchDoc.setName(allele.getAlleleSymbol().getDisplayText());
			}

			NCBITaxonTerm taxon = allele.getTaxon();
			if (taxon != null) {
				Species species = taxon.getSpecies();
				if (species != null) {
					searchDoc.setSpecies(species.getFullName());
					if (allele.getAlleleSymbol() != null) {
						searchDoc.setNameKey(allele.getAlleleSymbol().getFormatText() + " (" + species.getAbbreviation() + ")");
					}
				} else {
					searchDoc.setSpecies(taxon.getName());
					if (allele.getAlleleSymbol() != null) {
						searchDoc.setNameKey(allele.getAlleleSymbol().getFormatText() + " (" + taxon.getName() + ")");
					}
				}
			} else if (allele.getAlleleSymbol() != null) {
				searchDoc.setNameKey(allele.getAlleleSymbol().getFormatText());
			}

			if (CollectionUtils.isNotEmpty(allele.getAlleleSynonyms())) {
				List<String> synonyms = allele.getAlleleSynonyms().stream()
					.map(NameSlotAnnotation::getDisplayText)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
				if (!synonyms.isEmpty()) {
					searchDoc.setSynonyms(synonyms);
				}
			}

			if (doc.getAlleleOfGene() != null && doc.getAlleleOfGene().getGeneSymbol() != null) {
				String geneSymbol = doc.getAlleleOfGene().getGeneSymbol().getDisplayText();
				if (taxon != null) {
					geneSymbol = geneSymbol + " (" + taxon.getName() + ")";
				}
				searchDoc.setGenes(List.of(geneSymbol));
			}

			if (doc.getCrossReference() != null && doc.getCrossReference().getReferencedCurie() != null) {
				searchDoc.setModCrossRefCompleteUrl(doc.getCrossReference().getReferencedCurie());
			}

			if (allele.getPopularity() != null) {
				searchDoc.setPopularity(allele.getPopularity());
			} else {
				searchDoc.setPopularity(0.0);
			}

			if (doc.getDiseases() != null && !doc.getDiseases().isEmpty()) {
				searchDoc.setDiseases(new ArrayList<>(doc.getDiseases()));
			}
			if (doc.getDiseasesAgrSlim() != null && !doc.getDiseasesAgrSlim().isEmpty()) {
				searchDoc.setDiseasesAgrSlim(new ArrayList<>(doc.getDiseasesAgrSlim()));
			}
			if (doc.getDiseasesWithParents() != null && !doc.getDiseasesWithParents().isEmpty()) {
				searchDoc.setDiseasesWithParents(new ArrayList<>(doc.getDiseasesWithParents()));
			}

			if (doc.getConstructExpressedComponents() != null && !doc.getConstructExpressedComponents().isEmpty()) {
				searchDoc.setConstructExpressedComponent(doc.getConstructExpressedComponents());
			}

			if (doc.getConstructRegulatoryRegions() != null && !doc.getConstructRegulatoryRegions().isEmpty()) {
				searchDoc.setConstructRegulatoryRegion(doc.getConstructRegulatoryRegions());
			}

			if (doc.getConstructKnockdownComponents() != null && !doc.getConstructKnockdownComponents().isEmpty()) {
				searchDoc.setConstructKnockdownComponent(doc.getConstructKnockdownComponents());
			}

			if (CollectionUtils.isNotEmpty(doc.getVariants())) {
				List<String> variantTypes = doc.getVariants().stream()
					.filter(v -> v.getVariantType() != null && v.getVariantType().getName() != null)
					.map(v -> v.getVariantType().getName())
					.distinct()
					.toList();
				if (!variantTypes.isEmpty()) {
					searchDoc.setVariantType(variantTypes);
				}

				Set<String> consequences = new HashSet<>();
				for (var variant : doc.getVariants()) {
					if (variant.getCuratedVariantGenomicLocations() == null) {
						continue;
					}
					for (var location : variant.getCuratedVariantGenomicLocations()) {
						if (location.getPredictedVariantConsequences() == null) {
							continue;
						}
						for (var pvc : location.getPredictedVariantConsequences()) {
							if (pvc.getVepConsequences() == null) {
								continue;
							}
							for (var soTerm : pvc.getVepConsequences()) {
								if (soTerm.getName() != null) {
									consequences.add(soTerm.getName());
								}
							}
						}
					}
				}
				if (!consequences.isEmpty()) {
					searchDoc.setMolecularConsequence(consequences);
				}
			}

			result.add(searchDoc);
		}
		return result;
	}

}
