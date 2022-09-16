package org.alliancegenome.es.index.site.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.DOTerm;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiseaseDocumentCache extends IndexerCache {

	private Map<String, DOTerm> diseaseMap = new HashMap<>();
	private Map<String, Set<String>> speciesMap = new HashMap<>();
	private Map<String, Set<String>> diseaseGroupMap = new HashMap<>();
	private Map<String, Set<String>> parentNameMap = new HashMap<>();

	@Override
	protected <D extends SearchableItemDocument> void addExtraCachedFields(D document) {
		String id = document.getPrimaryKey();
		document.setAssociatedSpecies(speciesMap.get(id));
		document.setDiseaseGroup(diseaseGroupMap.get(id));
		document.setParentDiseaseNames(parentNameMap.get(id));
		document.setAssociatedSpecies(speciesMap.get(id));
	}


}
