package org.alliancegenome.api.entity;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.GeneExpressionAnnotation;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneExpressionAnnotationDocument extends ESDocument {
	{
		category = "gene_expression_annotation";
	}
	private GeneExpressionAnnotation geneExpressionAnnotation;
}
