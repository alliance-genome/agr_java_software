package org.alliancegenome.cache.repository.helper;

import java.util.HashMap;
import java.util.Map;

import org.alliancegenome.es.model.query.FieldFilter;

public class ESFiltering {

	protected Map<FieldFilter, String> filterFieldMap = new HashMap<>();

	public String getFieldName(FieldFilter fieldFilter) {
		return filterFieldMap.get(fieldFilter);
	}
}
