package org.alliancegenome.core.document;

import org.alliancegenome.curation_api.model.entities.AffectedGenomicModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public class AGMDiseaseAnnotationDocument extends DiseaseAnnotationDocument {
	{
		category = "agm_disease_annotation";
	}

	private AffectedGenomicModel subject;
}
