package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.entities.AffectedGenomicModel;
import org.alliancegenome.neo4j.view.View;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"subject", "gene", "relation", "object", "primaryAnnotations"})
@JsonView({View.DiseaseAnnotationAll.class})
public class AGMPhenotypeAnnotationDocument extends PhenotypeAnnotationDocument {

	public static final String AGM_PHENOTYPE_ANNOTATION = "agm_phenotype_annotation";
	private AffectedGenomicModel subject;

	public AGMPhenotypeAnnotationDocument() {
		setCategory(AGM_PHENOTYPE_ANNOTATION);
	}

}
