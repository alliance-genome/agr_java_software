package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.orthology.GeneToGeneOrthologyGenerated;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneToGeneOrthologyGeneratedInterface;

import si.mazi.rescu.RestProxyFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneToGeneOrthologyService {
		private final GeneToGeneOrthologyGeneratedInterface orthologyApi = RestProxyFactory.createProxy(GeneToGeneOrthologyGeneratedInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

    public List<GeneToGeneOrthologyGenerated> getFiltered() {
        ProcessDisplayHelper display = new ProcessDisplayHelper(2000);

		List<GeneToGeneOrthologyGenerated> ret = new ArrayList<>();

		int batchSize = 1000;
		int page = 0;
		int pages;

        HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);

        do {
            SearchResponse<GeneToGeneOrthologyGenerated> response = orthologyApi.find(page, batchSize, params);
			Gene objectGene = response.getResults().get(0).getObjectGene();
			log.info("objectGene: " + objectGene.toString());
            for (GeneToGeneOrthologyGenerated geneGeneOrthology : response.getResults()) {
                Gene orthologousGene = geneGeneOrthology.getObjectGene();
				if (orthologousGene == null) {
					continue;
				}
                ret.add(geneGeneOrthology);
            }

            if (page == 0) {
				display.startProcess("Pulling Gene To Gene Orthology Generated from curation", response.getTotalResults());
			}

			display.progressProcess(response.getReturnedRecords().longValue());
			pages = (int) (response.getTotalResults() / batchSize);
			page++;

        } while(page < pages);

		display.finishProcess();

		return ret;
    }
}
