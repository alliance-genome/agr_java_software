package org.alliancegenome.core.document;

import org.alliancegenome.curation_api.model.entities.AffectedGenomicModel;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true)
public class AGMPhenotypeAnnotationDocument extends PhenotypeAnnotationDocument {
	{
		category = "agm_phenotype_annotation";
	}

	private AffectedGenomicModel subject;
}
