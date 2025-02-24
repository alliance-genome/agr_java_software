package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.entities.AffectedGenomicModel;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.neo4j.view.View;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"subject", "gene", "relation", "object", "primaryAnnotations"})
@JsonView({View.DiseaseAnnotationAll.class})
public class AllelePhenotypeAnnotationDocument extends PhenotypeAnnotationDocument {

	public static final String ALLELE_PHENOTYPE_ANNOTATION = "allele_phenotype_annotation" ;
	private Allele subject;

	public AllelePhenotypeAnnotationDocument() {
		setCategory(ALLELE_PHENOTYPE_ANNOTATION);
	}

}
