package org.alliancegenome.core.document;

import java.util.List;
import java.util.Map;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.entities.orthology.GeneToGeneOrthologyGenerated;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneToGeneOrthologyDocument extends ESDocument {
	{
		category = "gene_to_gene_orthology";
	}
	private String stringencyFilter = "all";
	private List<Map<String, Object>> geneAnnotations;
	private Map<String, Map<String, Object>> geneAnnotationsMap;
	private GeneToGeneOrthologyGenerated geneToGeneOrthologyGenerated;
}
