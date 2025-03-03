package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.neo4j.view.View;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"subject", "gene", "relation", "object", "primaryAnnotations"})
@JsonView({View.PhenotypeAnnotationAll.class})
public class GenePhenotypeAnnotationDocument extends PhenotypeAnnotationDocument {

	public static final String GENE_PHENOTYPE_ANNOTATION = "gene_phenotype_annotation";
	private Gene subject;

	public GenePhenotypeAnnotationDocument() {
		setCategory(GENE_PHENOTYPE_ANNOTATION);
	}

}
