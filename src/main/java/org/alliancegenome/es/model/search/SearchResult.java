package org.alliancegenome.es.model.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchResult {

	public ArrayList<AggResult> aggregations;
	public ArrayList<Map<String, Object>> results;
	public long total;
	public List<String> errorMessages;
}
