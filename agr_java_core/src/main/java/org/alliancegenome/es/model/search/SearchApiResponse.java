package org.alliancegenome.es.model.search;

import java.util.*;

import lombok.*;

@Getter @Setter
public class SearchApiResponse {

	private ArrayList<AggResult> aggregations;
	private ArrayList<Map<String, Object>> results;
	private List<SearchResult> searchResults;
	private long total;
	private List<String> errorMessages;
}
