package org.alliancegenome.indexer.indexers.curation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;
import org.alliancegenome.curation_api.model.entities.GeneInteraction;
import org.alliancegenome.curation_api.model.entities.base.AuditedObject;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneModelInterface;
import org.alliancegenome.indexer.indexers.curation.interfaces.GenePhenotypeAnnotationInterface;
import si.mazi.rescu.RestProxyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ModelService extends BaseService {

	protected ObjectMapper mapper = RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();

	private final GeneModelInterface modelApi = RestProxyFactory.createProxy(GeneModelInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public Set<String> getAllGeneIds() {
		return getAllNeoAlleleIDs() ;
	}

	public Set<String> getAllModelIds() {
		return getAllNeoModelIDs();
	}
}
