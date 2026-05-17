package org.alliancegenome.api.es.search;

import lombok.Data;

@Data
public class RelatedDataLink {

	private String category;
	private String targetField;
	private String sourceName;
	private Long count;
	private String label;

}
