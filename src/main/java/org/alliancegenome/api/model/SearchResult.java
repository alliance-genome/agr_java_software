package org.alliancegenome.api.model;

import java.util.ArrayList;
import java.util.Map;

public class SearchResult {

	public ArrayList<AggResult> aggregations;
	public ArrayList<Map<String, Object>> results;
	public long total;
	
}
