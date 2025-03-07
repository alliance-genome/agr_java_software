package org.alliancegenome.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.entities.GeneExpressionExperiment;
import org.alliancegenome.es.index.ESDocument;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneExpressionExperimentDocument extends ESDocument {

	String category = "gene_expression_experiment";
	GeneExpressionExperiment geneExpressionExperiment;

	@Override
	public String getType() {
		return category;
	}
}
