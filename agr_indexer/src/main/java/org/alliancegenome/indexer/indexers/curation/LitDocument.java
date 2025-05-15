package org.alliancegenome.indexer.indexers.curation;

import org.alliancegenome.curation_api.model.document.es.ESDocument;

import com.google.gson.JsonObject;

import lombok.Data;

@Data
public class LitDocument extends ESDocument {
	{
		category = "literature_summary";
	}

	private JsonObject object;
}
