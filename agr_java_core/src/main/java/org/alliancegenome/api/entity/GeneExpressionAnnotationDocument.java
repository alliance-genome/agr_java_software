package org.alliancegenome.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.entities.GeneExpressionAnnotation;
import org.alliancegenome.es.index.ESDocument;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneExpressionAnnotationDocument extends ESDocument {

    String category = "gene_expression_annotation";

    @Override
    public String getType() {
        return category;
    }

    GeneExpressionAnnotation geneExpressionAnnotation;
}
