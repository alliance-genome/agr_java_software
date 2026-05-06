package org.alliancegenome.indexer.indexers.curation.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.util.ListUtils;
import org.alliancegenome.curation_api.interfaces.document.OntologyTermClosureDocumentInterface;
import org.alliancegenome.curation_api.model.entities.ontology.OntologyTermClosure;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.apache.commons.collections.CollectionUtils;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class DoTermService {

	private static final int BATCH_SIZE = 1000;
	private static final String ONTOLOGY_TERM_TYPE = "DOTerm";
	private static final String RELATION_TYPES = "is_a,part_of";

	private final OntologyTermClosureDocumentInterface closureApi = RestProxyFactory.createProxy(OntologyTermClosureDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private Map<String, Set<String>> closureMap;

	public Map<String, Set<String>> getClosureMap() {
		if (closureMap != null) {
			return closureMap;
		}

		try {
			SearchResponse<Long> idsResponse = closureApi.getAllIds(ONTOLOGY_TERM_TYPE, RELATION_TYPES);
			List<Long> allIds = idsResponse.getResults();
			log.info("Fetched {} DOTerm closure IDs", allIds.size());

			ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
			display.startProcess("Pulling DOTerm closures from curation", allIds.size());

			closureMap = new HashMap<>();
			for (List<Long> batch : ListUtils.partition(allIds, BATCH_SIZE)) {
				SearchResponse<OntologyTermClosure> response = closureApi.findByIds(batch);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					continue;
				}
				for (OntologyTermClosure row : response.getResults()) {
					String subjectCurie = row.getClosureSubject().getCurie();
					String objectCurie = row.getClosureObject().getCurie();
					closureMap.computeIfAbsent(subjectCurie, k -> new HashSet<>()).add(objectCurie);
				}
				display.progressProcess(response.getResults().size());
			}
			display.finishProcess();

			// Neo4j IS_A_PART_OF_CLOSURE included a self-edge for every term; ontologytermclosure does not.
			// Add self so disease annotations on a term match searches against parentSlimIDs that include the focus term.
			closureMap.forEach((curie, ancestors) -> ancestors.add(curie));

			return closureMap;
		} catch (Exception e) {
			log.error("Failed to fetch DOTerm closure map from curation API", e);
			ExceptionCatcher.report(e);
			System.exit(-1);
			return null;
		}
	}

}
