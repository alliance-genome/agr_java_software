package org.alliancegenome.core.document;

import org.alliancegenome.curation_api.model.entities.Allele;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public class AlleleDiseaseAnnotationDocument extends DiseaseAnnotationDocument {
	{
		category = "allele_disease_annotation";
	}

	private Allele subject;
}
