package org.alliancegenome.api.entity;

import java.util.Map;

import org.alliancegenome.curation_api.model.document.es.ESDocument;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LiteratureSummaryDocument extends ESDocument {
	{
		category = "literature_summary";
	}
	

	private Map<String, Object> literatureSummary;
}
