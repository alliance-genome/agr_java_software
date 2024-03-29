package org.alliancegenome.cache.repository.helper;

import org.alliancegenome.es.model.query.FieldFilter;

public class AlleleESFiltering extends ESFiltering {

	public AlleleESFiltering() {
		filterFieldMap.put(FieldFilter.SYMBOL, "name");
		filterFieldMap.put(FieldFilter.VARIANT_TYPE, "variant.variantType.name.keyword");
		filterFieldMap.put(FieldFilter.MOLECULAR_CONSEQUENCE, "transcriptLevelConsequences.geneLevelConsequence.keyword");
	}


}

