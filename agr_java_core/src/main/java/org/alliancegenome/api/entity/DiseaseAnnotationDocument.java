package org.alliancegenome.api.entity;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alliancegenome.curation_api.model.entities.BiologicalEntity;
import org.alliancegenome.curation_api.model.entities.ConditionRelation;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.model.entities.ontology.DOTerm;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiseaseAnnotationDocument extends SearchableItemDocument {

	private String uniqueId;
	private Integer count;
	private VocabularyTerm relation;
	private DOTerm object;
	private List<ECOTerm> evidenceCodes;
	private Reference singleReference;
	private Set<Reference> references;

	private String generatedRelationString;
	private Set<String> diseaseQualifiers;
	private Set<String> pubmedPubModIDs;
	private Set<String> pubModIDs;
	private Set<String> parentSlimIDs;
	private List<DiseaseAnnotation> primaryAnnotations;
	private List<Gene> basedOnGenes;
	private HashMap<String, Integer> speciesOrder;
	int phylogeneticSortingIndex;
	private List<ConditionRelation> conditionModifierList;
	private List<BiologicalEntity> geneticModifierList;
	private VocabularyTerm geneticModifierRelation;
	private String conditionModifierAggregated;
	private String geneticModifierAggregated;
	private List<ConditionRelation> experimentalConditionList;
	private String experimentalConditionsAggregated;
	// 1 true
	// 0 false
	@JsonIgnore
	private boolean isViaOrthologyAnnotation;

	public DiseaseAnnotationDocument() {
		primaryAnnotations = new ArrayList<>();
	}

	public void addReference(Reference singleReference) {
		if (references == null) {
			references = new HashSet<>();
		}
		references.add(singleReference);
	}

	public void addEvidenceCodes(List<ECOTerm> ecoTerms) {
		if (CollectionUtils.isEmpty(ecoTerms)) {
			return;
		}
		if (evidenceCodes == null) {
			evidenceCodes = new ArrayList<>();
		}
		List<String> ecoValues = evidenceCodes.stream().map(ECOTerm::getCurie).toList();
		// make unique list
		ecoTerms.forEach(ecoTerm -> {
			if (!ecoValues.contains(ecoTerm.getCurie())) {
				evidenceCodes.add(ecoTerm);
			}
		});
	}

	public void addPubMedPubModID(String id) {
		if (pubmedPubModIDs == null) {
			pubmedPubModIDs = new HashSet<>();
		}
		pubmedPubModIDs.add(id);
	}

	public void addPubModID(String id) {
		if (pubModIDs == null) {
			pubModIDs = new HashSet<>();
		}
		pubModIDs.add(id);
	}

	public void addPrimaryAnnotation(DiseaseAnnotation da) {
		if (primaryAnnotations == null) {
			primaryAnnotations = new ArrayList<>();
		}
		primaryAnnotations.add(da);
	}

	public void addBasedOnGenes(List<Gene> genes) {
		if (CollectionUtils.isEmpty(genes)) {
			return;
		}
		if (basedOnGenes == null) {
			basedOnGenes = new ArrayList<>();
		}
		genes.forEach(gene -> {
			if (!basedOnGenes.contains(gene)) {
				basedOnGenes.add(gene);
			}
		});
	}

	@JsonView({View.DiseaseAnnotationAll.class})
	public int getViaOrthologyOrder() {
		return isViaOrthologyAnnotation ? 1 : 0;
	}

	@JsonView({View.DiseaseAnnotationAll.class})
	public void setViaOrthologyOrder(int order) {
		isViaOrthologyAnnotation = order == 1;
	}

}