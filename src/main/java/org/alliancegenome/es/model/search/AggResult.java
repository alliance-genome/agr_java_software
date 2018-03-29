package org.alliancegenome.es.model.search;

import java.util.ArrayList;
import java.util.List;

public class AggResult {

	public String key;
	public List<AggDocCount> values = new ArrayList<AggDocCount>();

	public AggResult(String key) {
		this.key = key;
	}
}
