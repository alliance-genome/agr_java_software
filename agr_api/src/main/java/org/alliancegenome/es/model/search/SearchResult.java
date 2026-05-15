package org.alliancegenome.es.model.search;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SearchResult {

	private Map<String, Object> map;
	private List<String> relatedDataLinks;

}
