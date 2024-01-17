package org.alliancegenome.api.entity;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.model.entities.ontology.DOTerm;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiseaseAnnotationDocument extends SearchableItemDocument {

	private String uniqueId;
	private VocabularyTerm relation;
	private DOTerm object;
	private List<ECOTerm> evidenceCodes;
	private Reference singleReference;
	private Set<Reference> references;

	private String generatedRelationString;
	private Set<String> diseaseQualifiers;
	private Set<String> pubmedPubModIDs;
	private Set<String> parentSlimIDs;
	private List<DiseaseAnnotation> primaryAnnotations;
	private List<Gene> basedOnGenes;
	private HashMap<String, Integer> speciesOrder;

	public DiseaseAnnotationDocument() {
		primaryAnnotations = new ArrayList<>();
	}

	public void addReference(Reference singleReference) {
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

	public void addPrimaryAnnotation(DiseaseAnnotation da) {
		if (primaryAnnotations == null) {
			primaryAnnotations = new ArrayList<>();
		}
		primaryAnnotations.add(da);
	}

	public void addBasedOnGenes(List<Gene> genes) {
		if(CollectionUtils.isEmpty(genes))
			return;
		if (basedOnGenes == null) {
			basedOnGenes = new ArrayList<>();
		}
		genes.forEach(gene -> {
			if(!basedOnGenes.contains(gene)){
				basedOnGenes.add(gene);
			}
		});
	}

}
