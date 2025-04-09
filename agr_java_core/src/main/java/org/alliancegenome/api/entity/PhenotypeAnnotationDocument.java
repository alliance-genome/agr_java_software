package org.alliancegenome.api.entity;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PhenotypeAnnotationDocument extends ESDocument {

	private String uniqueId;
	private VocabularyTerm relation;
	private String phenotypeStatement;
	private List<ECOTerm> evidenceCodes;
	private Reference singleReference;
	private Set<InformationContentEntity> references;

	private String generatedRelationString;
	private Set<String> diseaseQualifiers;
	private Set<String> pubmedPubModIDs;
	private List<PhenotypeAnnotation> primaryAnnotations;
	private List<ConditionRelation> conditionModifierList;
	private List<BiologicalEntity> geneticModifierList;
	private VocabularyTerm geneticModifierRelation;
	private String conditionModifierAggregated;
	private String geneticModifierAggregated;
	private List<ConditionRelation> experimentalConditionList;
	private String experimentalConditionsAggregated;
	// 1 true
	// 0 false

	public PhenotypeAnnotationDocument() {
		primaryAnnotations = new ArrayList<>();
	}

	public void addReference(InformationContentEntity singleReference) {
		if (references == null) {
			references = new HashSet<>();
		}
		references.add(singleReference);
	}

	public void addPubMedPubModID(String id) {
		if (pubmedPubModIDs == null) {
			pubmedPubModIDs = new HashSet<>();
		}
		pubmedPubModIDs.add(id);
	}

	public void addPrimaryAnnotation(PhenotypeAnnotation da) {
		if (primaryAnnotations == null) {
			primaryAnnotations = new ArrayList<>();
		}
		primaryAnnotations.add(da);
	}

}
