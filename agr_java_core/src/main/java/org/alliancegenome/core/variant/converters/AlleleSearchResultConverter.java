package org.alliancegenome.core.variant.converters;

import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.ontology.NCBITaxonTerm;
import org.alliancegenome.es.model.AlleleSearchResultDocument;
import org.alliancegenome.es.model.search.Category;
import org.alliancegenome.es.model.search.RelatedDataLink;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
				searchDoc.setSpecies(taxon.getName());
				if (allele.getAlleleSymbol() != null) {
					searchDoc.setNameKey(allele.getAlleleSymbol().getFormatText() + " (" + taxon.getName() + ")");
				}
			} else if (allele.getAlleleSymbol() != null) {
				searchDoc.setNameKey(allele.getAlleleSymbol().getFormatText());
			}

			if (CollectionUtils.isNotEmpty(allele.getAlleleSynonyms())) {
				List<String> synonyms = allele.getAlleleSynonyms().stream()
					.map(syn -> syn.getDisplayText())
					.filter(s -> s != null)
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

			List<RelatedDataLink> relatedData = new ArrayList<>();
			if (doc.getAlleleOfGene() != null && doc.getAlleleOfGene().getGeneSymbol() != null) {
				RelatedDataLink geneLink = new RelatedDataLink();
				geneLink.setCategory(Category.GENE.getName());
				geneLink.setTargetField("alleles");
				geneLink.setSourceName(searchDoc.getNameKey());
				geneLink.setCount(1L);
				relatedData.add(geneLink);
			}
			if (Boolean.TRUE.equals(doc.getHasDisease())) {
				RelatedDataLink diseaseLink = new RelatedDataLink();
				diseaseLink.setCategory(Category.DISEASE.getName());
				diseaseLink.setTargetField("alleles");
				diseaseLink.setSourceName(searchDoc.getNameKey());
				diseaseLink.setCount(1L);
				relatedData.add(diseaseLink);
			}
			if (!relatedData.isEmpty()) {
				searchDoc.setRelatedData(relatedData);
			}

			if (doc.getDiseases() != null && !doc.getDiseases().isEmpty()) {
				searchDoc.setDiseases(new ArrayList<>(doc.getDiseases()));
			}
			if (doc.getDiseasesAgrSlim() != null && !doc.getDiseasesAgrSlim().isEmpty()) {
				searchDoc.setDiseasesAgrSlim(new ArrayList<>(doc.getDiseasesAgrSlim()));
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
