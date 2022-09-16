package org.alliancegenome.es.model.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SearchApiResponse {

	private ArrayList<AggResult> aggregations;
	private ArrayList<Map<String, Object>> results;
	private List<SearchResult> searchResults;
	private long total;
	private List<String> errorMessages;
}
