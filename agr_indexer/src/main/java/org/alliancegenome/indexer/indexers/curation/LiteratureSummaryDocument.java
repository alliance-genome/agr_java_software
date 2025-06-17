package org.alliancegenome.indexer.indexers.curation;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

@Data
public class LiteratureSummaryDocument extends ESDocument {
	{
		category = "literature_summary";
		searchable = true;
	}

	//private JsonObject object;
	private ObjectNode object; //for search_after/scroll
}
