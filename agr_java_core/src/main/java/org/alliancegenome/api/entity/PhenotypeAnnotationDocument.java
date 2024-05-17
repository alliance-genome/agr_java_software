package org.alliancegenome.api.entity;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class PhenotypeAnnotationDocument extends SearchableItemDocument {

	public static final String GENE_PHENOTYPE_ANNOTATION = "gene_phenotype_annotation";

	private String uniqueId;
	private VocabularyTerm relation;
	private String phenotypeStatement;
	private List<ECOTerm> evidenceCodes;
	private Reference singleReference;
	private Set<Reference> references;

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

	public void addReference(Reference singleReference) {
		if (references == null) {
			references = new HashSet<>();
		}
		references.add(singleReference);
	}

	public void addEvidenceCodes(List<ECOTerm> ecoTerms) {
		if (CollectionUtils.isEmpty(ecoTerms))
			return;
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

	public void addPrimaryAnnotation(PhenotypeAnnotation da) {
		if (primaryAnnotations == null) {
			primaryAnnotations = new ArrayList<>();
		}
		primaryAnnotations.add(da);
	}

}
