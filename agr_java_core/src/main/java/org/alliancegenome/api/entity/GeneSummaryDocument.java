package org.alliancegenome.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.es.index.ESDocument;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneSummaryDocument extends ESDocument {

	protected String category = "gene_summary";
	private Map<String, Object> geneAnnotations;

	private Gene gene;

	@Override
	public String getType() {
		return category;
	}

}
